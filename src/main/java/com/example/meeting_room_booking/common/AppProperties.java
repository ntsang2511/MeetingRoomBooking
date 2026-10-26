package com.example.meeting_room_booking.common;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(Jwt jwt, Admin admin, Booking booking) {

    public record Jwt(String secret, long expirationMinutes) {}

    public record Admin(String email, String password) {}

    public record Booking(long minDurationMinutes, long maxDurationMinutes) {}
}