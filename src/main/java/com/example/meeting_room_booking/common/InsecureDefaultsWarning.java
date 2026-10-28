package com.example.meeting_room_booking.common;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Cảnh báo khi vẫn dùng giá trị mặc định của môi trường dev.
 * Với profile "prod", các biến môi trường là bắt buộc nên ứng dụng sẽ không khởi động nếu thiếu.
 */
@Slf4j
@Component
@Profile("!prod")
@RequiredArgsConstructor
public class InsecureDefaultsWarning implements ApplicationRunner {

    private static final Set<String> DEV_JWT_SECRETS = Set.of(
            "dev-only-secret-change-me-at-least-32-chars",
            "change-me-to-a-long-random-string-at-least-32-chars");
    private static final String DEV_ADMIN_PASSWORD = "Admin@12345";

    private final AppProperties props;

    @Override
    public void run(ApplicationArguments args) {
        if (DEV_JWT_SECRETS.contains(props.jwt().secret())) {
            log.warn("JWT_SECRET is still the development default. Set a random secret before deploying.");
        }
        if (DEV_ADMIN_PASSWORD.equals(props.admin().password())) {
            log.warn("ADMIN_PASSWORD is still the development default. Change it before deploying.");
        }
    }
}
