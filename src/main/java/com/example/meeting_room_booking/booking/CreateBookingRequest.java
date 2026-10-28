package com.example.meeting_room_booking.booking;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;

public record CreateBookingRequest(
        @NotNull Long roomId,
        @NotBlank @Size(max = 200) String title,
        @NotNull OffsetDateTime startTime,
        @NotNull OffsetDateTime endTime) {
}