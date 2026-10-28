package com.example.meeting_room_booking.user;

import com.example.meeting_room_booking.common.ConflictException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UserResponse create(CreateUserRequest request) {
        String email = request.email().trim().toLowerCase();
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ConflictException("Email already in use");
        }
        User user = new User();
        user.setEmail(email);
        user.setFullName(request.fullName().trim());
        user.setRole(request.role());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        return UserResponse.from(userRepository.save(user));
    }
}