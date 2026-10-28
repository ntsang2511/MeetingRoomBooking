package com.example.meeting_room_booking.user;

import jakarta.validation.constraints.Size;

public record UpdateUserRequest(
        @Size(max = 100) String fullName,
        Role role,
        Boolean active) {
}
