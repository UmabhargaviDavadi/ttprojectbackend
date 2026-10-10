package com.tt.financetracker.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record BudgetRequest(
        @Size(max = 40) String category,
        @NotNull @DecimalMin(value = "0.01") BigDecimal limit,
        @Min(1) @Max(12) int month,
        @Min(2000) @Max(2100) int year
) {}
