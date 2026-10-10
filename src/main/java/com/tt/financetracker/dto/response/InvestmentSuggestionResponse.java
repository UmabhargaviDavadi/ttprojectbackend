package com.tt.financetracker.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record InvestmentSuggestionResponse(List<InvestmentTip> items, String disclaimer) {
    public record InvestmentTip(Long id, String title, String name, String description, String riskLevel,
                                String horizon, BigDecimal suggestedMonthlyAmount) {}
}
