package com.tt.financetracker.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ProfileUpdateRequest(
        @Size(min = 2, max = 100) String name,
        @Email @Size(max = 160) String email,
        @Pattern(regexp = "INR|USD|EUR|GBP") String currency,
        String currentPassword,
        @Size(min = 8, max = 72) String newPassword
) {}
