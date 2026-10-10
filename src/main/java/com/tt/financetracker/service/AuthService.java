package com.tt.financetracker.service;

import com.tt.financetracker.dto.request.LoginRequest;
import com.tt.financetracker.dto.request.RegisterRequest;
import com.tt.financetracker.dto.response.AuthResponse;
import com.tt.financetracker.dto.response.UserResponse;
import com.tt.financetracker.entity.User;
import com.tt.financetracker.exception.DuplicateResourceException;
import com.tt.financetracker.repository.UserRepository;
import com.tt.financetracker.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByEmailIgnoreCase(email)) throw new DuplicateResourceException("Email is already registered");
        User user = new User();
        user.setName(request.name().trim());
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setCurrency("INR");
        user = userRepository.save(user);
        return new AuthResponse(jwtService.generate(user.getEmail()), UserResponse.from(user));
    }

    public AuthResponse login(LoginRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email, request.password()));
        User user = userRepository.findByEmailIgnoreCase(email).orElseThrow();
        return new AuthResponse(jwtService.generate(user.getEmail()), UserResponse.from(user));
    }
}
