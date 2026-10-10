package com.tt.financetracker.controller;

import com.tt.financetracker.dto.request.ProfileUpdateRequest;
import com.tt.financetracker.dto.response.UserResponse;
import com.tt.financetracker.service.ProfileService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users/profile")
public class ProfileController {
    private final ProfileService profileService;
    public ProfileController(ProfileService profileService) { this.profileService = profileService; }
    @GetMapping public UserResponse get(Authentication authentication) { return profileService.get(authentication.getName()); }
    @PutMapping public UserResponse update(Authentication authentication, @Valid @RequestBody ProfileUpdateRequest request) { return profileService.update(authentication.getName(), request); }
}
