package com.example.meeting_room_booking.common;

import com.example.meeting_room_booking.user.Role;
import com.example.meeting_room_booking.user.User;
import com.example.meeting_room_booking.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminSeeder implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AppProperties props;

    @Override
    public void run(ApplicationArguments args) {
        String email = props.admin().email().toLowerCase();
        if (userRepository.existsByEmailIgnoreCase(email)) {
            return;
        }
        User admin = new User();
        admin.setEmail(email);
        admin.setFullName("System Admin");
        admin.setRole(Role.ADMIN);
        admin.setPasswordHash(passwordEncoder.encode(props.admin().password()));
        userRepository.save(admin);
        log.info("Created default admin account: {}", email);
    }
}