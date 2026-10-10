package com.tt.financetracker.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record TrendResponse(List<TrendItem> items) {
    public record TrendItem(String month, int monthNumber, int year, BigDecimal income, BigDecimal expense) {}
}
