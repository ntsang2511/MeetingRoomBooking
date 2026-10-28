package com.example.meeting_room_booking;

import com.example.meeting_room_booking.room.Room;
import com.example.meeting_room_booking.user.Role;
import com.example.meeting_room_booking.user.User;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class BookingIntegrationTest extends ApiTestSupport {

    @Test
    void overlappingBookingIsRejected() throws Exception {
        User user = createUser(Role.USER);
        Room room = createRoom(10);
        createBookingId(token(user), room.getId(), at(1, 9, 0), at(1, 10, 0), 4);

        mvc.perform(post("/api/v1/bookings")
                        .header(AUTH, token(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookingJson(room.getId(), at(1, 9, 30), at(1, 10, 30), 4)))
                .andExpect(status().isConflict());
    }

    @Test
    void cancellingABookingFreesTheSlot() throws Exception {
        User user = createUser(Role.USER);
        Room room = createRoom(10);
        long bookingId = createBookingId(token(user), room.getId(), at(1, 13, 0), at(1, 14, 0), 4);

        mvc.perform(post("/api/v1/bookings/" + bookingId + "/cancel")
                        .header(AUTH, token(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        createBookingId(token(user), room.getId(), at(1, 13, 0), at(1, 14, 0), 4);
    }

    @Test
    void userCannotModifyOrCancelAnotherUsersBooking() throws Exception {
        User owner = createUser(Role.USER);
        User other = createUser(Role.USER);
        Room room = createRoom(10);
        long bookingId = createBookingId(token(owner), room.getId(), at(1, 15, 0), at(1, 16, 0), 4);

        mvc.perform(patch("/api/v1/bookings/" + bookingId)
                        .header(AUTH, token(other))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"hijacked\"}"))
                .andExpect(status().isForbidden());

        mvc.perform(post("/api/v1/bookings/" + bookingId + "/cancel")
                        .header(AUTH, token(other)))
                .andExpect(status().isForbidden());

        mvc.perform(get("/api/v1/bookings/" + bookingId)
                        .header(AUTH, token(other)))
                .andExpect(status().isForbidden());
    }

    @Test
    void attendeesCannotExceedRoomCapacity() throws Exception {
        User user = createUser(Role.USER);
        Room smallRoom = createRoom(4);

        mvc.perform(post("/api/v1/bookings")
                        .header(AUTH, token(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookingJson(smallRoom.getId(), at(1, 9, 0), at(1, 10, 0), 5)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("capacity")));
    }

    @Test
    void bookingMustBeWithinWorkingHours() throws Exception {
        User user = createUser(Role.USER);
        Room room = createRoom(10);

        mvc.perform(post("/api/v1/bookings")
                        .header(AUTH, token(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookingJson(room.getId(), at(2, 7, 30), at(2, 8, 30), 4)))
                .andExpect(status().isBadRequest());

        mvc.perform(post("/api/v1/bookings")
                        .header(AUTH, token(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookingJson(room.getId(), at(2, 17, 0), at(2, 19, 0), 4)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void bookingMustNotBeInThePastOrTooFarInAdvance() throws Exception {
        User user = createUser(Role.USER);
        Room room = createRoom(10);

        mvc.perform(post("/api/v1/bookings")
                        .header(AUTH, token(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookingJson(room.getId(), at(-1, 9, 0), at(-1, 10, 0), 4)))
                .andExpect(status().isBadRequest());

        mvc.perform(post("/api/v1/bookings")
                        .header(AUTH, token(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookingJson(room.getId(), at(61, 9, 0), at(61, 10, 0), 4)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void availableRoomsExcludeRoomsThatAreAlreadyBooked() throws Exception {
        User user = createUser(Role.USER);
        Room busyRoom = createRoom(6);
        Room freeRoom = createRoom(6);
        createBookingId(token(user), busyRoom.getId(), at(2, 9, 0), at(2, 10, 0), 4);

        String body = mvc.perform(get("/api/v1/rooms/available")
                        .header(AUTH, token(user))
                        .param("start", at(2, 9, 0).toString())
                        .param("end", at(2, 10, 0).toString()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        List<Long> roomIds = new ArrayList<>();
        JsonNode content = objectMapper.readTree(body).get("content");
        content.forEach(node -> roomIds.add(node.get("id").asLong()));

        assertThat(roomIds)
                .contains(freeRoom.getId())
                .doesNotContain(busyRoom.getId());
    }

    @Test
    void normalUserCannotManageRooms() throws Exception {
        User user = createUser(Role.USER);

        mvc.perform(post("/api/v1/rooms")
                        .header(AUTH, token(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Nope\",\"capacity\":4}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void myBookingsOnlyReturnsOwnBookings() throws Exception {
        User alice = createUser(Role.USER);
        User bob = createUser(Role.USER);
        Room room = createRoom(10);
        createBookingId(token(alice), room.getId(), at(3, 9, 0), at(3, 10, 0), 2);
        createBookingId(token(bob), room.getId(), at(3, 11, 0), at(3, 12, 0), 2);

        String body = mvc.perform(get("/api/v1/bookings/me")
                        .header(AUTH, token(alice)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        JsonNode content = objectMapper.readTree(body).get("content");
        assertThat(content.size()).isEqualTo(1);
        assertThat(content.get(0).get("userId").asLong()).isEqualTo(alice.getId());
        assertThat(content.get(0).get("attendees").asInt()).isEqualTo(2);
    }
}
