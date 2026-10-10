package com.tt.financetracker.dto.response;

import com.tt.financetracker.entity.Goal;

import java.math.BigDecimal;
import java.time.LocalDate;

public record GoalResponse(Long id, String name, BigDecimal targetAmount, BigDecimal currentAmount,
                           LocalDate deadline, int percentage, BigDecimal amountNeeded) {
    public static GoalResponse from(Goal goal) {
        BigDecimal saved = goal.getSavedAmount();
        int percentage = goal.getTargetAmount().signum() == 0 ? 0 : saved.multiply(BigDecimal.valueOf(100)).divide(goal.getTargetAmount(), 0, java.math.RoundingMode.HALF_UP).min(BigDecimal.valueOf(100)).intValue();
        return new GoalResponse(goal.getId(), goal.getName(), goal.getTargetAmount(), saved, goal.getDeadline(),
                percentage, goal.getTargetAmount().subtract(saved).max(BigDecimal.ZERO));
    }
}
