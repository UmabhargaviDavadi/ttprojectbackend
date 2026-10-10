package com.tt.financetracker.service;

import com.tt.financetracker.entity.TransactionType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CategorizationServiceTest {
    private final CategorizationService service = new CategorizationService();

    @Test
    void categorizesExpenseKeywordsCaseInsensitively() {
        assertThat(service.categorize("UBER ride", TransactionType.EXPENSE)).isEqualTo("Travel");
        assertThat(service.categorize("Netflix monthly", TransactionType.EXPENSE)).isEqualTo("Entertainment");
        assertThat(service.categorize("Zomato order", TransactionType.EXPENSE)).isEqualTo("Food");
    }

    @Test
    void categorizesIncomeAndFallsBackToOther() {
        assertThat(service.categorize("Monthly salary", TransactionType.INCOME)).isEqualTo("Salary");
        assertThat(service.categorize("Unknown purchase", TransactionType.EXPENSE)).isEqualTo("Other");
    }
}
