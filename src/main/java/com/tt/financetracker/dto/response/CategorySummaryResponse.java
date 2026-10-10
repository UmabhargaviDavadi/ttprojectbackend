package com.tt.financetracker.dto.response;

import java.math.BigDecimal;

public record CategorySummaryResponse(String category, BigDecimal amount, int percentage, String color) {}
