package com.example.meeting_room_booking.room;

import com.example.meeting_room_booking.common.ConflictException;
import com.example.meeting_room_booking.common.NotFoundException;
import com.example.meeting_room_booking.common.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class RoomService {

    private final RoomRepository roomRepository;

    @Transactional(readOnly = true)
    public PageResponse<RoomResponse> list(int minCapacity, boolean includeInactive, Pageable pageable) {
        Page<Room> rooms = includeInactive
                ? roomRepository.findByCapacityGreaterThanEqual(minCapacity, pageable)
                : roomRepository.findByActiveTrueAndCapacityGreaterThanEqual(minCapacity, pageable);
        return PageResponse.from(rooms, RoomResponse::from);
    }

    @Transactional(readOnly = true)
    public PageResponse<RoomResponse> findAvailable(Instant start, Instant end, int minCapacity, Pageable pageable) {
        if (!end.isAfter(start)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "end must be after start");
        }
        return PageResponse.from(roomRepository.findAvailable(start, end, minCapacity, pageable), RoomResponse::from);
    }

    @Transactional(readOnly = true)
    public RoomResponse get(Long id) {
        return RoomResponse.from(findOrThrow(id));
    }

    @Transactional
    public RoomResponse create(RoomRequest request) {
        if (roomRepository.existsByNameIgnoreCase(request.name().trim())) {
            throw new ConflictException("A room with this name already exists");
        }
        Room room = new Room();
        apply(room, request);
        return RoomResponse.from(roomRepository.save(room));
    }

    @Transactional
    public RoomResponse update(Long id, RoomRequest request) {
        Room room = findOrThrow(id);
        if (roomRepository.existsByNameIgnoreCaseAndIdNot(request.name().trim(), id)) {
            throw new ConflictException("A room with this name already exists");
        }
        apply(room, request);
        return RoomResponse.from(room);
    }

    @Transactional
    public void deactivate(Long id) {
        Room room = findOrThrow(id);
        if (!room.isActive()) {
            return;
        }
        if (roomRepository.hasFutureBookings(id)) {
            throw new ConflictException("Room still has upcoming bookings; cancel them first");
        }
        room.setActive(false);
        room.setUpdatedAt(Instant.now());
    }

    @Transactional
    public RoomResponse activate(Long id) {
        Room room = findOrThrow(id);
        room.setActive(true);
        room.setUpdatedAt(Instant.now());
        return RoomResponse.from(room);
    }

    private Room findOrThrow(Long id) {
        return roomRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Room " + id + " not found"));
    }

    private void apply(Room room, RoomRequest request) {
        room.setName(request.name().trim());
        room.setCapacity(request.capacity());
        room.setLocation(request.location());
        room.setDescription(request.description());
        room.setUpdatedAt(Instant.now());
    }
}