package com.example.meeting_room_booking.common;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class NotFoundException extends ResponseStatusException {
    public NotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, message);
    }
}