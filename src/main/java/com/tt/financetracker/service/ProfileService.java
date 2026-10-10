package com.tt.financetracker.service;

import com.tt.financetracker.dto.request.ProfileUpdateRequest;
import com.tt.financetracker.dto.response.UserResponse;
import com.tt.financetracker.entity.User;
import com.tt.financetracker.exception.BadRequestException;
import com.tt.financetracker.exception.DuplicateResourceException;
import com.tt.financetracker.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Locale;

@Service
public class ProfileService {
    private final AccountService accountService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public ProfileService(AccountService accountService, UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.accountService = accountService;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public UserResponse get(String email) { return UserResponse.from(accountService.requireByEmail(email)); }

    @Transactional
    public UserResponse update(String email, ProfileUpdateRequest request) {
        User user = accountService.requireByEmail(email);
        boolean profileChange = request.name() != null || request.email() != null || request.currency() != null;
        boolean passwordChange = request.currentPassword() != null || request.newPassword() != null;
        if (!profileChange && !passwordChange) throw new BadRequestException("Provide profile fields or both password fields");
        if (passwordChange) {
            if (!StringUtils.hasText(request.currentPassword()) || !StringUtils.hasText(request.newPassword()))
                throw new BadRequestException("Both currentPassword and newPassword are required");
            if (!passwordEncoder.matches(request.currentPassword(), user.getPassword()))
                throw new BadRequestException("Current password is incorrect");
            user.setPassword(passwordEncoder.encode(request.newPassword()));
        }
        if (request.name() != null) user.setName(request.name().trim());
        if (request.email() != null) {
            String nextEmail = request.email().trim().toLowerCase(Locale.ROOT);
            if (!nextEmail.equalsIgnoreCase(user.getEmail()) && userRepository.existsByEmailIgnoreCase(nextEmail))
                throw new DuplicateResourceException("Email is already registered");
            user.setEmail(nextEmail);
        }
        if (request.currency() != null) user.setCurrency(request.currency());
        return UserResponse.from(userRepository.save(user));
    }
}
