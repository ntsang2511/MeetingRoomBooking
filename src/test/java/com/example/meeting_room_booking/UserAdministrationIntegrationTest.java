package com.example.meeting_room_booking;

import com.example.meeting_room_booking.user.Role;
import com.example.meeting_room_booking.user.User;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UserAdministrationIntegrationTest extends ApiTestSupport {

    @Test
    void adminCanSearchUsersAndNormalUserCannot() throws Exception {
        User admin = createUser(Role.ADMIN);
        User employee = createUser(Role.USER);

        mvc.perform(get("/api/v1/users")
                        .header(AUTH, token(admin))
                        .param("q", employee.getEmail()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].email").value(employee.getEmail()));

        mvc.perform(get("/api/v1/users").header(AUTH, token(employee)))
                .andExpect(status().isForbidden());
    }

    @Test
    void deactivatingAUserBlocksLoginAndReactivationRestoresIt() throws Exception {
        User admin = createUser(Role.ADMIN);
        User employee = createUser(Role.USER);

        mvc.perform(patch("/api/v1/users/" + employee.getId())
                        .header(AUTH, token(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"active\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        login(employee.getEmail(), PASSWORD).andExpect(status().isUnauthorized());

        mvc.perform(patch("/api/v1/users/" + employee.getId())
                        .header(AUTH, token(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"active\":true}"))
                .andExpect(status().isOk());

        login(employee.getEmail(), PASSWORD).andExpect(status().isOk());
    }

    @Test
    void adminCanResetAnotherUsersPassword() throws Exception {
        User admin = createUser(Role.ADMIN);
        User employee = createUser(Role.USER);

        mvc.perform(post("/api/v1/users/" + employee.getId() + "/reset-password")
                        .header(AUTH, token(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\":\"NewSecret123\"}"))
                .andExpect(status().isNoContent());

        login(employee.getEmail(), "NewSecret123").andExpect(status().isOk());
        login(employee.getEmail(), PASSWORD).andExpect(status().isUnauthorized());
    }

    @Test
    void emailUniquenessIgnoresCase() throws Exception {
        User admin = createUser(Role.ADMIN);
        String base = "case-" + UUID.randomUUID();
        String body = objectMapper.writeValueAsString(Map.of(
                "email", base + "@example.com",
                "password", "Secret12345",
                "fullName", "Case User",
                "role", "USER"));

        mvc.perform(post("/api/v1/users")
                        .header(AUTH, token(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        mvc.perform(post("/api/v1/users")
                        .header(AUTH, token(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body.replace(base + "@example.com", base.toUpperCase() + "@EXAMPLE.COM")))
                .andExpect(status().isConflict());
    }

    @Test
    void adminCannotDeactivateOwnAccount() throws Exception {
        User admin = createUser(Role.ADMIN);

        mvc.perform(patch("/api/v1/users/" + admin.getId())
                        .header(AUTH, token(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"active\":false}"))
                .andExpect(status().isConflict());
    }
}
