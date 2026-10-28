package com.example.meeting_room_booking.booking;

import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;

public record UpdateBookingRequest(
        @Size(max = 200) String title,
        OffsetDateTime startTime,
        OffsetDateTime endTime) {
}