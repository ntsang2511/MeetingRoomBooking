package com.example.meeting_room_booking.auth;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Chặn dò mật khẩu: sau 5 lần sai liên tiếp, cặp (email, IP) bị khoá 15 phút.
 * Bộ đếm nằm trong bộ nhớ nên chỉ chính xác khi chạy một instance; xem README.
 */
@Component
public class LoginAttemptService {

    private static final int MAX_ATTEMPTS = 5;
    private static final Duration LOCK_DURATION = Duration.ofMinutes(15);
    private static final int CLEANUP_THRESHOLD = 1_000;

    private final Map<String, Attempt> attempts = new ConcurrentHashMap<>();

    public static String key(String email, String clientIp) {
        return email.trim().toLowerCase() + "|" + (clientIp == null || clientIp.isBlank() ? "unknown" : clientIp);
    }

    public boolean isBlocked(String key) {
        Attempt attempt = attempts.get(key);
        if (attempt == null || attempt.lockedUntil() == null) {
            return false;
        }
        if (attempt.lockedUntil().isAfter(Instant.now())) {
            return true;
        }
        attempts.remove(key);
        return false;
    }

    public Duration remainingLock(String key) {
        Attempt attempt = attempts.get(key);
        if (attempt == null || attempt.lockedUntil() == null) {
            return Duration.ZERO;
        }
        Duration remaining = Duration.between(Instant.now(), attempt.lockedUntil());
        return remaining.isNegative() ? Duration.ZERO : remaining;
    }

    public void recordFailure(String key) {
        attempts.compute(key, (k, previous) -> {
            int failures = (previous == null ? 0 : previous.failures()) + 1;
            Instant lockedUntil = failures >= MAX_ATTEMPTS ? Instant.now().plus(LOCK_DURATION) : null;
            return new Attempt(failures, lockedUntil);
        });
        if (attempts.size() > CLEANUP_THRESHOLD) {
            purgeExpired();
        }
    }

    public void reset(String key) {
        attempts.remove(key);
    }

    private void purgeExpired() {
        Instant now = Instant.now();
        attempts.entrySet().removeIf(entry -> {
            Instant lockedUntil = entry.getValue().lockedUntil();
            return lockedUntil != null && lockedUntil.isBefore(now);
        });
    }

    private record Attempt(int failures, Instant lockedUntil) {
    }
}
