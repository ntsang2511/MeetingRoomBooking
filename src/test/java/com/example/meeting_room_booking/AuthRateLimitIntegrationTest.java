package com.example.meeting_room_booking;

import com.example.meeting_room_booking.user.Role;
import com.example.meeting_room_booking.user.User;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthRateLimitIntegrationTest extends ApiTestSupport {

    private static final String WRONG_PASSWORD = "WrongPassword1";

    @Test
    void accountIsLockedAfterFiveFailedAttempts() throws Exception {
        User user = createUser(Role.USER);

        for (int attempt = 0; attempt < 5; attempt++) {
            login(user.getEmail(), WRONG_PASSWORD).andExpect(status().isUnauthorized());
        }

        login(user.getEmail(), PASSWORD)
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.detail", containsString("Too many failed login attempts")));
    }

    @Test
    void successfulLoginResetsTheFailureCounter() throws Exception {
        User user = createUser(Role.USER);

        for (int attempt = 0; attempt < 4; attempt++) {
            login(user.getEmail(), WRONG_PASSWORD).andExpect(status().isUnauthorized());
        }
        login(user.getEmail(), PASSWORD).andExpect(status().isOk());

        for (int attempt = 0; attempt < 4; attempt++) {
            login(user.getEmail(), WRONG_PASSWORD).andExpect(status().isUnauthorized());
        }
        login(user.getEmail(), PASSWORD).andExpect(status().isOk());
    }
}
