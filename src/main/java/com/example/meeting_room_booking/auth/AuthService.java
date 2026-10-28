package com.example.meeting_room_booking.auth;

import com.example.meeting_room_booking.common.AppProperties;
import com.example.meeting_room_booking.user.User;
import com.example.meeting_room_booking.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AppProperties props;
    private final LoginAttemptService loginAttempts;

    public LoginResponse login(LoginRequest request, String clientIp) {
        String key = LoginAttemptService.key(request.email(), clientIp);
        if (loginAttempts.isBlocked(key)) {
            long minutes = Math.max(1, loginAttempts.remainingLock(key).toMinutes());
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Too many failed login attempts. Try again in " + minutes + " minute(s)");
        }

        User user = userRepository.findByEmailIgnoreCase(request.email())
                .filter(User::isActive)
                .orElse(null);
        if (user == null || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            loginAttempts.recordFailure(key);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }
        loginAttempts.reset(key);

        return new LoginResponse(
                jwtService.generateToken(user),
                "Bearer",
                props.jwt().expirationMinutes() * 60);
    }
}