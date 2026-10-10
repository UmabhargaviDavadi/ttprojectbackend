package com.tt.financetracker.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TransactionRequest(
        @NotNull @DecimalMin(value = "0.01") BigDecimal amount,
        @NotBlank @Size(max = 120) String description,
        @Size(max = 40) String category,
        @NotBlank String type,
        @NotNull LocalDate date,
        @Size(max = 40) String paymentMethod
) {}
