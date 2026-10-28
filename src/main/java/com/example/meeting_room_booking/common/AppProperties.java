package com.example.meeting_room_booking.common;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.LocalTime;
import java.time.ZoneId;

@Validated
@ConfigurationProperties(prefix = "app")
public record AppProperties(@Valid Jwt jwt, @Valid Admin admin, @Valid Booking booking) {

    public record Jwt(
            @NotBlank @Size(min = 32, message = "app.jwt.secret must be at least 32 characters") String secret,
            @Positive long expirationMinutes) {
    }

    public record Admin(
            @NotBlank String email,
            @NotBlank @Size(min = 8, message = "app.admin.password must be at least 8 characters") String password) {
    }

    public record Booking(
            @NotBlank String timezone,
            @NotNull LocalTime workStart,
            @NotNull LocalTime workEnd,
            @Positive long minDurationMinutes,
            @Positive long maxDurationMinutes,
            @Positive int maxAdvanceDays) {

        public ZoneId zoneId() {
            return ZoneId.of(timezone);
        }
    }
}