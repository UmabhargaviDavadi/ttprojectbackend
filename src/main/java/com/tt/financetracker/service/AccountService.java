package com.tt.financetracker.service;

import com.tt.financetracker.entity.User;
import com.tt.financetracker.exception.ResourceNotFoundException;
import com.tt.financetracker.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AccountService {
    private final UserRepository userRepository;
    public AccountService(UserRepository userRepository) { this.userRepository = userRepository; }

    public User requireByEmail(String email) {
        return userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("User account not found"));
    }
}
