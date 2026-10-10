package com.tt.financetracker.dto.response;

import com.tt.financetracker.entity.Budget;

import java.math.BigDecimal;

public record BudgetResponse(Long id, String category, BigDecimal limit, int month, int year,
                             BigDecimal spent, BigDecimal remaining, int percentage) {
    public static BudgetResponse from(Budget budget, BigDecimal spent) {
        BigDecimal limit = budget.getLimitAmount();
        int percentage = limit.signum() == 0 ? 0 : spent.multiply(BigDecimal.valueOf(100)).divide(limit, 0, java.math.RoundingMode.HALF_UP).intValue();
        return new BudgetResponse(budget.getId(), budget.getCategory() == null ? "Overall" : budget.getCategory().getName(),
                limit, budget.getMonth(), budget.getYear(), spent, limit.subtract(spent), percentage);
    }
}
