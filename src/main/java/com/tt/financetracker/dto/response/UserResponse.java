package com.tt.financetracker.dto.response;

import com.tt.financetracker.entity.User;

public record UserResponse(Long id, String name, String email, String currency) {
    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getName(), user.getEmail(), user.getCurrency());
    }
}
