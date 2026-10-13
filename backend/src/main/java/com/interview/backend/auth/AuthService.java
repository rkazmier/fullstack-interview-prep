package com.interview.backend.auth;

import com.interview.backend.auth.dto.AuthResponse;
import com.interview.backend.auth.dto.LoginRequest;
import com.interview.backend.auth.dto.RegisterRequest;
import com.interview.backend.security.JwtService;
import com.interview.backend.user.Role;
import com.interview.backend.user.User;
import com.interview.backend.user.UserRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new IllegalArgumentException("Username already taken");
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Email already registered");
        }
        User user = new User(
                request.username(),
                request.email(),
                passwordEncoder.encode(request.password()),
                Role.USER
        );
        userRepository.save(user);
        return new AuthResponse(
                jwtService.generateToken(user.getUsername(), user.getRole().name()),
                user.getUsername(),
                user.getRole().name()
        );
    }

    public AuthResponse login(LoginRequest request) {
        // throws BadCredentialsException on failure -> handled by @RestControllerAdvice
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password()));

        User user = userRepository.findByUsername(request.username()).orElseThrow();
        return new AuthResponse(
                jwtService.generateToken(user.getUsername(), user.getRole().name()),
                user.getUsername(),
                user.getRole().name()
        );
    }
}
