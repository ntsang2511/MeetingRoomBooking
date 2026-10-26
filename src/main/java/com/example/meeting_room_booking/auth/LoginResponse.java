package com.example.meeting_room_booking.auth;

public record LoginResponse(String accessToken, String tokenType, long expiresInSeconds) {}