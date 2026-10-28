package com.example.meeting_room_booking.booking;

import java.time.Instant;

public record BookingResponse(
        Long id,
        Long roomId,
        String roomName,
        Long userId,
        String userFullName,
        String title,
        Instant startTime,
        Instant endTime,
        BookingStatus status,
        Instant createdAt,
        Instant cancelledAt) {

    public static BookingResponse from(Booking b) {
        return new BookingResponse(
                b.getId(),
                b.getRoom().getId(),
                b.getRoom().getName(),
                b.getUser().getId(),
                b.getUser().getFullName(),
                b.getTitle(),
                b.getStartTime(),
                b.getEndTime(),
                b.getStatus(),
                b.getCreatedAt(),
                b.getCancelledAt());
    }
}