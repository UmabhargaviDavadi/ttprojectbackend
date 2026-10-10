package com.tt.financetracker.service;

import com.tt.financetracker.entity.Budget;
import com.tt.financetracker.entity.Category;
import com.tt.financetracker.entity.Notification;
import com.tt.financetracker.entity.NotificationType;
import com.tt.financetracker.entity.TransactionType;
import com.tt.financetracker.entity.User;
import com.tt.financetracker.repository.BudgetRepository;
import com.tt.financetracker.repository.NotificationRepository;
import com.tt.financetracker.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BudgetAlertServiceTest {
    @Mock private BudgetRepository budgetRepository;
    @Mock private TransactionRepository transactionRepository;
    @Mock private NotificationRepository notificationRepository;
    private BudgetAlertService service;
    private User user;
    private Budget budget;

    @BeforeEach
    void setUp() {
        service = new BudgetAlertService(budgetRepository, transactionRepository, notificationRepository);
        user = new User();
        user.setId(7L);
        Category category = new Category("Food", TransactionType.EXPENSE);
        category.setId(4L);
        budget = new Budget();
        budget.setId(9L);
        budget.setUser(user);
        budget.setCategory(category);
        budget.setLimitAmount(new BigDecimal("1000.00"));
        budget.setMonth(10);
        budget.setYear(2026);
    }

    @Test
    void createsWarningAndDangerWhenSpendingCrossesBothThresholds() {
        when(budgetRepository.findByUserIdAndMonthAndYearOrderById(7L, 10, 2026)).thenReturn(List.of(budget));
        when(transactionRepository.sumExpensesByCategoryAndDateRange(eq(7L), eq(4L), any(), any())).thenReturn(new BigDecimal("1100"));
        when(notificationRepository.existsByBudgetIdAndBudgetMonthAndBudgetYearAndThresholdPercent(eq(9L), eq(10), eq(2026), any())).thenReturn(false);

        service.checkForTransaction(user, LocalDate.of(2026, 10, 11));

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository, org.mockito.Mockito.times(2)).save(captor.capture());
        assertThat(captor.getAllValues()).extracting(Notification::getType)
                .containsExactly(NotificationType.WARNING, NotificationType.DANGER);
        assertThat(captor.getAllValues()).extracting(Notification::getThresholdPercent).containsExactly(80, 100);
    }

    @Test
    void doesNotCreateAlertBelowEightyPercent() {
        when(budgetRepository.findByUserIdAndMonthAndYearOrderById(7L, 10, 2026)).thenReturn(List.of(budget));
        when(transactionRepository.sumExpensesByCategoryAndDateRange(eq(7L), eq(4L), any(), any())).thenReturn(new BigDecimal("799.99"));

        service.checkForTransaction(user, LocalDate.of(2026, 10, 11));

        verify(notificationRepository, never()).save(any());
    }
}
