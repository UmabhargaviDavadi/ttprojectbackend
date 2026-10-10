package com.tt.financetracker.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record GoalRequest(
        @NotBlank @Size(min = 2, max = 100) String name,
        @NotNull @DecimalMin(value = "0.01") BigDecimal targetAmount,
        @DecimalMin(value = "0.00") BigDecimal currentAmount,
        @NotNull LocalDate deadline
) {}
