package com.example.meeting_room_booking.booking;

import com.example.meeting_room_booking.common.CurrentUser;
import com.example.meeting_room_booking.common.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    @PostMapping("/bookings")
    @ResponseStatus(HttpStatus.CREATED)
    public BookingResponse create(@AuthenticationPrincipal Jwt jwt,
                                  @Valid @RequestBody CreateBookingRequest request) {
        return bookingService.create(CurrentUser.from(jwt), request);
    }

    @GetMapping("/bookings/me")
    public PageResponse<BookingResponse> myBookings(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) BookingStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to,
            @PageableDefault(size = 20, sort = "startTime", direction = Sort.Direction.DESC) Pageable pageable) {
        return bookingService.listMine(CurrentUser.from(jwt), status, from, to, pageable);
    }

    @GetMapping("/bookings")
    @PreAuthorize("hasRole('ADMIN')")
    public PageResponse<BookingResponse> search(
            @RequestParam(required = false) Long roomId,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) BookingStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to,
            @PageableDefault(size = 20, sort = "startTime", direction = Sort.Direction.DESC) Pageable pageable) {
        return bookingService.search(roomId, userId, status, from, to, pageable);
    }

    @GetMapping("/bookings/{id}")
    public BookingResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return bookingService.get(id, CurrentUser.from(jwt));
    }

    @PatchMapping("/bookings/{id}")
    public BookingResponse update(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
                                  @Valid @RequestBody UpdateBookingRequest request) {
        return bookingService.update(id, CurrentUser.from(jwt), request);
    }

    @PostMapping("/bookings/{id}/cancel")
    public BookingResponse cancel(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return bookingService.cancel(id, CurrentUser.from(jwt));
    }

    @GetMapping("/rooms/{roomId}/bookings")
    public PageResponse<BookingResponse> roomCalendar(
            @PathVariable Long roomId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to,
            @PageableDefault(size = 20, sort = "startTime") Pageable pageable) {
        return bookingService.listForRoom(roomId, from, to, pageable);
    }
}