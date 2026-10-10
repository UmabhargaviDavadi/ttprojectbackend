package com.tt.financetracker.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record MonthlyReportResponse(int month, int year, BigDecimal income, BigDecimal expense,
                                    BigDecimal balance, int savingsRate, List<CategorySummaryResponse> categories,
                                    Comparison comparison) {
    public record Comparison(double incomeChange, double expenseChange) {}
}
