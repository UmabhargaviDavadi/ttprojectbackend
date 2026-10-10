package com.tt.financetracker.service;

import com.tt.financetracker.dto.request.GoalRequest;
import com.tt.financetracker.dto.response.GoalResponse;
import com.tt.financetracker.entity.Goal;
import com.tt.financetracker.entity.User;
import com.tt.financetracker.exception.BadRequestException;
import com.tt.financetracker.exception.ResourceNotFoundException;
import com.tt.financetracker.repository.GoalRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class GoalService {
    private final GoalRepository goalRepository;
    private final AccountService accountService;
    public GoalService(GoalRepository goalRepository, AccountService accountService) { this.goalRepository = goalRepository; this.accountService = accountService; }

    @Transactional(readOnly = true)
    public List<GoalResponse> list(String email) {
        User user = accountService.requireByEmail(email);
        return goalRepository.findByUserIdOrderByCreatedAtDesc(user.getId()).stream().map(GoalResponse::from).toList();
    }

    @Transactional
    public GoalResponse create(String email, GoalRequest request) {
        User user = accountService.requireByEmail(email);
        Goal goal = new Goal();
        goal.setUser(user);
        apply(goal, request);
        return GoalResponse.from(goalRepository.save(goal));
    }

    @Transactional
    public GoalResponse update(String email, Long id, GoalRequest request) {
        User user = accountService.requireByEmail(email);
        Goal goal = owned(id, user.getId());
        apply(goal, request);
        return GoalResponse.from(goalRepository.save(goal));
    }

    @Transactional
    public void delete(String email, Long id) {
        User user = accountService.requireByEmail(email);
        goalRepository.delete(owned(id, user.getId()));
    }

    private Goal owned(Long id, Long userId) {
        Goal goal = goalRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Goal not found"));
        if (!goal.getUser().getId().equals(userId)) throw new AccessDeniedException("This goal belongs to another user");
        return goal;
    }

    private void apply(Goal goal, GoalRequest request) {
        BigDecimal saved = request.currentAmount() == null ? BigDecimal.ZERO : request.currentAmount();
        if (saved.compareTo(request.targetAmount()) > 0) throw new BadRequestException("currentAmount cannot exceed targetAmount");
        goal.setName(request.name().trim());
        goal.setTargetAmount(request.targetAmount());
        goal.setSavedAmount(saved);
        goal.setDeadline(request.deadline());
    }
}
