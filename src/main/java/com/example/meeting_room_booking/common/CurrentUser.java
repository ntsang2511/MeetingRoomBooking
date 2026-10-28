package com.example.meeting_room_booking.common;

import org.springframework.security.oauth2.jwt.Jwt;

public record CurrentUser(Long id, boolean admin) {

    public static CurrentUser from(Jwt jwt) {
        return new CurrentUser(Long.valueOf(jwt.getSubject()), "ADMIN".equals(jwt.getClaimAsString("role")));
    }
}