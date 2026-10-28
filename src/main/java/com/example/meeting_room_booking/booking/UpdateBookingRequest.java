package com.example.meeting_room_booking.booking;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;

public record UpdateBookingRequest(
        @Size(max = 200) String title,
        @Min(1) @Max(1000) Integer attendees,
        OffsetDateTime startTime,
        OffsetDateTime endTime) {
}