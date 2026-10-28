package com.example.meeting_room_booking.room;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RoomRequest(
        @NotBlank @Size(max = 100) String name,
        @NotNull @Min(1) @Max(1000) Integer capacity,
        @Size(max = 200) String location,
        @Size(max = 2000) String description) {
}