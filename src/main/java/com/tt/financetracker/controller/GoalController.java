package com.tt.financetracker.controller;

import com.tt.financetracker.dto.request.GoalRequest;
import com.tt.financetracker.dto.response.GoalResponse;
import com.tt.financetracker.service.GoalService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/goals")
public class GoalController {
    private final GoalService goalService;
    public GoalController(GoalService goalService) { this.goalService = goalService; }
    @GetMapping public List<GoalResponse> list(Authentication authentication) { return goalService.list(authentication.getName()); }
    @PostMapping @ResponseStatus(HttpStatus.CREATED) public GoalResponse create(Authentication authentication, @Valid @RequestBody GoalRequest request) { return goalService.create(authentication.getName(), request); }
    @PutMapping("/{id}") public GoalResponse update(Authentication authentication, @PathVariable Long id, @Valid @RequestBody GoalRequest request) { return goalService.update(authentication.getName(), id, request); }
    @DeleteMapping("/{id}") public Map<String, Boolean> delete(Authentication authentication, @PathVariable Long id) { goalService.delete(authentication.getName(), id); return Map.of("success", true); }
}
