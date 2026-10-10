package com.tt.financetracker.dto.response;

import com.tt.financetracker.entity.Transaction;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Locale;

public record TransactionResponse(Long id, LocalDate date, String description, String category,
                                  String type, BigDecimal amount, String paymentMethod) {
    public static TransactionResponse from(Transaction transaction) {
        return new TransactionResponse(transaction.getId(), transaction.getDate(), transaction.getDescription(),
                transaction.getCategory().getName(), transaction.getType().name().toLowerCase(Locale.ROOT),
                transaction.getAmount(), transaction.getPaymentMethod());
    }
}
