package com.tt.financetracker.service;

import com.tt.financetracker.entity.TransactionType;
import com.tt.financetracker.entity.User;
import com.tt.financetracker.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {
    @Mock private TransactionRepository transactionRepository;
    @Mock private AccountService accountService;
    private ReportService service;
    private User user;

    @BeforeEach
    void setUp() {
        service = new ReportService(transactionRepository, accountService);
        user = new User();
        user.setId(4L);
        when(accountService.requireByEmail("user@example.com")).thenReturn(user);
    }

    @Test
    void calculatesMonthlyBalanceSavingsRateCategoriesAndComparison() {
        when(transactionRepository.sumByUserAndTypeAndDateRange(eq(4L), eq(TransactionType.INCOME), any(), any()))
                .thenReturn(new BigDecimal("1000"), new BigDecimal("1000"));
        when(transactionRepository.sumByUserAndTypeAndDateRange(eq(4L), eq(TransactionType.EXPENSE), any(), any()))
                .thenReturn(new BigDecimal("400"), new BigDecimal("200"));
        when(transactionRepository.sumExpensesByCategory(eq(4L), any(), any())).thenReturn(
                List.of(new Object[]{"Food", new BigDecimal("300")}, new Object[]{"Travel", new BigDecimal("100")}));

        var report = service.monthly("user@example.com", 10, 2026);

        assertThat(report.income()).isEqualByComparingTo("1000");
        assertThat(report.expense()).isEqualByComparingTo("400");
        assertThat(report.balance()).isEqualByComparingTo("600");
        assertThat(report.savingsRate()).isEqualTo(60);
        assertThat(report.categories()).extracting("category").containsExactly("Food", "Travel");
        assertThat(report.comparison().incomeChange()).isEqualTo(0.0);
        assertThat(report.comparison().expenseChange()).isEqualTo(100.0);
    }

    @Test
    void emptyMonthReturnsZeroTotalsAndNoCategories() {
        when(transactionRepository.sumByUserAndTypeAndDateRange(eq(4L), any(), any(), any())).thenReturn(BigDecimal.ZERO);

        var report = service.monthly("user@example.com", 10, 2026);

        assertThat(report.income()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(report.expense()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(report.savingsRate()).isZero();
        assertThat(report.categories()).isEmpty();
        assertThat(report.comparison().incomeChange()).isZero();
    }
}
