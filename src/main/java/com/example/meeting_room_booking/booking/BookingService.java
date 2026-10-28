package com.example.meeting_room_booking.booking;

import com.example.meeting_room_booking.common.AppProperties;
import com.example.meeting_room_booking.common.ConflictException;
import com.example.meeting_room_booking.common.CurrentUser;
import com.example.meeting_room_booking.common.NotFoundException;
import com.example.meeting_room_booking.common.PageResponse;
import com.example.meeting_room_booking.room.Room;
import com.example.meeting_room_booking.room.RoomRepository;
import com.example.meeting_room_booking.user.User;
import com.example.meeting_room_booking.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;
    private final RoomRepository roomRepository;
    private final UserRepository userRepository;
    private final AppProperties props;

    @Transactional
    public BookingResponse create(CurrentUser currentUser, CreateBookingRequest request) {
        Instant start = request.startTime().toInstant();
        Instant end = request.endTime().toInstant();
        validateTimes(start, end);

        Room room = roomRepository.findById(request.roomId())
                .orElseThrow(() -> new NotFoundException("Room " + request.roomId() + " not found"));
        if (!room.isActive()) {
            throw new ConflictException("Room is not active");
        }
        validateAttendees(request.attendees(), room);
        User user = userRepository.findById(currentUser.id())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));

        Booking booking = new Booking();
        booking.setRoom(room);
        booking.setUser(user);
        booking.setTitle(request.title().trim());
        booking.setAttendees(request.attendees());
        booking.setStartTime(start);
        booking.setEndTime(end);
        // saveAndFlush: ràng buộc chống trùng lịch được kiểm tra ngay tại đây
        return BookingResponse.from(bookingRepository.saveAndFlush(booking));
    }

    @Transactional(readOnly = true)
    public BookingResponse get(Long id, CurrentUser currentUser) {
        Booking booking = findOrThrow(id);
        assertCanAccess(booking, currentUser);
        return BookingResponse.from(booking);
    }

    @Transactional
    public BookingResponse update(Long id, CurrentUser currentUser, UpdateBookingRequest request) {
        Booking booking = findOrThrow(id);
        assertCanAccess(booking, currentUser);
        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new ConflictException("Booking is cancelled");
        }
        if (!booking.getStartTime().isAfter(Instant.now())) {
            throw new ConflictException("Booking has already started and can no longer be modified");
        }

        if (request.title() != null) {
            if (request.title().isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "title must not be blank");
            }
            booking.setTitle(request.title().trim());
        }

        int attendees = request.attendees() != null ? request.attendees() : booking.getAttendees();
        validateAttendees(attendees, booking.getRoom());
        booking.setAttendees(attendees);

        Instant start = request.startTime() != null ? request.startTime().toInstant() : booking.getStartTime();
        Instant end = request.endTime() != null ? request.endTime().toInstant() : booking.getEndTime();
        if (!start.equals(booking.getStartTime()) || !end.equals(booking.getEndTime())) {
            validateTimes(start, end);
            booking.setStartTime(start);
            booking.setEndTime(end);
        }
        return BookingResponse.from(bookingRepository.saveAndFlush(booking));
    }

    @Transactional
    public BookingResponse cancel(Long id, CurrentUser currentUser) {
        Booking booking = findOrThrow(id);
        assertCanAccess(booking, currentUser);
        if (booking.getStatus() == BookingStatus.CANCELLED) {
            return BookingResponse.from(booking);
        }
        if (!booking.getEndTime().isAfter(Instant.now())) {
            throw new ConflictException("Booking has already finished");
        }
        booking.setStatus(BookingStatus.CANCELLED);
        booking.setCancelledAt(Instant.now());
        return BookingResponse.from(booking);
    }

    @Transactional(readOnly = true)
    public PageResponse<BookingResponse> listMine(CurrentUser currentUser, BookingStatus status,
                                                  OffsetDateTime from, OffsetDateTime to, Pageable pageable) {
        return search(null, currentUser.id(), status, from, to, pageable);
    }

    @Transactional(readOnly = true)
    public PageResponse<BookingResponse> listForRoom(Long roomId, OffsetDateTime from,
                                                     OffsetDateTime to, Pageable pageable) {
        if (!roomRepository.existsById(roomId)) {
            throw new NotFoundException("Room " + roomId + " not found");
        }
        return search(roomId, null, BookingStatus.CONFIRMED, from, to, pageable);
    }

    @Transactional(readOnly = true)
    public PageResponse<BookingResponse> search(Long roomId, Long userId, BookingStatus status,
                                                OffsetDateTime from, OffsetDateTime to, Pageable pageable) {
        if (from != null && to != null && !to.isAfter(from)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "to must be after from");
        }
        Page<Booking> page = bookingRepository.findAll(
                BookingSpecs.filter(roomId, userId, status,
                        from != null ? from.toInstant() : null,
                        to != null ? to.toInstant() : null),
                pageable);
        return PageResponse.from(page, BookingResponse::from);
    }

    private Booking findOrThrow(Long id) {
        return bookingRepository.findDetailById(id)
                .orElseThrow(() -> new NotFoundException("Booking " + id + " not found"));
    }

    private void assertCanAccess(Booking booking, CurrentUser currentUser) {
        if (!currentUser.admin() && !booking.getUser().getId().equals(currentUser.id())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only manage your own bookings");
        }
    }

    private void validateAttendees(int attendees, Room room) {
        if (attendees > room.getCapacity()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "attendees (" + attendees + ") exceeds the room capacity (" + room.getCapacity() + ")");
        }
    }

    private void validateTimes(Instant start, Instant end) {
        if (!end.isAfter(start)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "endTime must be after startTime");
        }
        if (start.isBefore(Instant.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot book in the past");
        }
        long minutes = Duration.between(start, end).toMinutes();
        long min = props.booking().minDurationMinutes();
        long max = props.booking().maxDurationMinutes();
        if (minutes < min || minutes > max) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Booking duration must be between " + min + " and " + max + " minutes");
        }

        AppProperties.Booking rules = props.booking();
        ZoneId zone = rules.zoneId();
        ZonedDateTime localStart = start.atZone(zone);
        ZonedDateTime localEnd = end.atZone(zone);
        if (!localStart.toLocalDate().equals(localEnd.toLocalDate())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Bookings must start and end on the same day (" + rules.timezone() + ")");
        }
        if (localStart.toLocalTime().isBefore(rules.workStart()) || localEnd.toLocalTime().isAfter(rules.workEnd())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Bookings must be within working hours " + rules.workStart() + " - " + rules.workEnd()
                            + " (" + rules.timezone() + ")");
        }
        if (start.isAfter(Instant.now().plus(rules.maxAdvanceDays(), ChronoUnit.DAYS))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Bookings can be made at most " + rules.maxAdvanceDays() + " days in advance");
        }
    }
}