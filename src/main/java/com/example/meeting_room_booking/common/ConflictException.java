package com.example.meeting_room_booking.common;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class ConflictException extends ResponseStatusException {
    public ConflictException(String message) {
        super(HttpStatus.CONFLICT, message);
    }
}