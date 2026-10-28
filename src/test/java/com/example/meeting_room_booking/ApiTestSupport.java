package com.example.meeting_room_booking;

import com.example.meeting_room_booking.auth.JwtService;
import com.example.meeting_room_booking.room.Room;
import com.example.meeting_room_booking.room.RoomRepository;
import com.example.meeting_room_booking.user.Role;
import com.example.meeting_room_booking.user.User;
import com.example.meeting_room_booking.user.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
abstract class ApiTestSupport {

    protected static final String PASSWORD = "Secret12345";
    protected static final ZoneId COMPANY_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    protected static final String AUTH = HttpHeaders.AUTHORIZATION;

    @Autowired
    protected MockMvc mvc;
    @Autowired
    protected ObjectMapper objectMapper;
    @Autowired
    protected UserRepository userRepository;
    @Autowired
    protected RoomRepository roomRepository;
    @Autowired
    protected PasswordEncoder passwordEncoder;
    @Autowired
    protected JwtService jwtService;

    protected User createUser(Role role) {
        String email = role.name().toLowerCase() + "-" + UUID.randomUUID() + "@example.com";
        User user = new User();
        user.setEmail(email);
        user.setFullName(role == Role.ADMIN ? "Admin Test" : "User Test");
        user.setRole(role);
        user.setPasswordHash(passwordEncoder.encode(PASSWORD));
        return userRepository.save(user);
    }

    protected String token(User user) {
        return "Bearer " + jwtService.generateToken(user);
    }

    protected Room createRoom(int capacity) {
        Room room = new Room();
        room.setName("Room-" + UUID.randomUUID());
        room.setCapacity(capacity);
        room.setLocation("Floor 3");
        return roomRepository.save(room);
    }

    /** Thời điểm trong tương lai theo múi giờ công ty (Asia/Ho_Chi_Minh). */
    protected OffsetDateTime at(int daysFromToday, int hour, int minute) {
        return LocalDate.now(COMPANY_ZONE).plusDays(daysFromToday)
                .atTime(hour, minute)
                .atZone(COMPANY_ZONE)
                .toOffsetDateTime();
    }

    protected String bookingJson(long roomId, OffsetDateTime start, OffsetDateTime end, int attendees) throws Exception {
        return objectMapper.writeValueAsString(Map.of(
                "roomId", roomId,
                "title", "Team sync",
                "attendees", attendees,
                "startTime", start.toString(),
                "endTime", end.toString()));
    }

    protected long createBookingId(String bearer, long roomId, OffsetDateTime start,
                                   OffsetDateTime end, int attendees) throws Exception {
        MvcResult result = mvc.perform(post("/api/v1/bookings")
                        .header(AUTH, bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookingJson(roomId, start, end, attendees)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    protected ResultActions login(String email, String password) throws Exception {
        return mvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("email", email, "password", password))));
    }
}
