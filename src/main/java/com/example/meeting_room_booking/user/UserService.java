package com.example.meeting_room_booking.user;

import com.example.meeting_room_booking.common.ConflictException;
import com.example.meeting_room_booking.common.CurrentUser;
import com.example.meeting_room_booking.common.NotFoundException;
import com.example.meeting_room_booking.common.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

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

    @Transactional(readOnly = true)
    public PageResponse<UserResponse> list(String query, Role role, Boolean active, Pageable pageable) {
        return PageResponse.from(
                userRepository.findAll(UserSpecs.filter(query, role, active), pageable),
                UserResponse::from);
    }

    @Transactional
    public UserResponse update(Long id, CurrentUser actor, UpdateUserRequest request) {
        User user = findOrThrow(id);
        if (request.active() != null && request.active() != user.isActive()) {
            if (id.equals(actor.id()) && !request.active()) {
                throw new ConflictException("You cannot deactivate your own account");
            }
            user.setActive(request.active());
        }
        if (request.role() != null && request.role() != user.getRole()) {
            if (id.equals(actor.id())) {
                throw new ConflictException("You cannot change your own role");
            }
            user.setRole(request.role());
        }
        if (request.fullName() != null) {
            if (request.fullName().isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "fullName must not be blank");
            }
            user.setFullName(request.fullName().trim());
        }
        return UserResponse.from(user);
    }

    @Transactional
    public void resetPassword(Long id, String password) {
        User user = findOrThrow(id);
        user.setPasswordHash(passwordEncoder.encode(password));
    }

    private User findOrThrow(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User " + id + " not found"));
    }
}