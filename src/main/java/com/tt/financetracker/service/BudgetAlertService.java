package com.tt.financetracker.service;

import com.tt.financetracker.entity.Budget;
import com.tt.financetracker.entity.Notification;
import com.tt.financetracker.entity.NotificationType;
import com.tt.financetracker.entity.TransactionType;
import com.tt.financetracker.entity.User;
import com.tt.financetracker.repository.BudgetRepository;
import com.tt.financetracker.repository.NotificationRepository;
import com.tt.financetracker.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

@Service
public class BudgetAlertService {
    private final BudgetRepository budgetRepository;
    private final TransactionRepository transactionRepository;
    private final NotificationRepository notificationRepository;

    public BudgetAlertService(BudgetRepository budgetRepository, TransactionRepository transactionRepository,
                              NotificationRepository notificationRepository) {
        this.budgetRepository = budgetRepository;
        this.transactionRepository = transactionRepository;
        this.notificationRepository = notificationRepository;
    }

    @Transactional
    public void checkForTransaction(User user, LocalDate transactionDate) {
        int month = transactionDate.getMonthValue();
        int year = transactionDate.getYear();
        LocalDate start = transactionDate.withDayOfMonth(1);
        LocalDate end = start.plusMonths(1);
        List<Budget> budgets = budgetRepository.findByUserIdAndMonthAndYearOrderById(user.getId(), month, year);
        for (Budget budget : budgets) {
            BigDecimal spent = budget.getCategory() == null
                    ? transactionRepository.sumByUserAndTypeAndDateRange(user.getId(), TransactionType.EXPENSE, start, end)
                    : transactionRepository.sumExpensesByCategoryAndDateRange(user.getId(), budget.getCategory().getId(), start, end);
            BigDecimal limit = budget.getLimitAmount();
            BigDecimal scaledSpent = spent.multiply(BigDecimal.valueOf(100));
            if (scaledSpent.compareTo(limit.multiply(BigDecimal.valueOf(80))) >= 0)
                createOnce(user, budget, month, year, 80, NotificationType.WARNING, spent);
            if (scaledSpent.compareTo(limit.multiply(BigDecimal.valueOf(100))) >= 0)
                createOnce(user, budget, month, year, 100, NotificationType.DANGER, spent);
        }
    }

    private void createOnce(User user, Budget budget, int month, int year, int threshold,
                            NotificationType type, BigDecimal spent) {
        if (notificationRepository.existsByBudgetIdAndBudgetMonthAndBudgetYearAndThresholdPercent(budget.getId(), month, year, threshold)) return;
        String category = budget.getCategory() == null ? "Overall" : budget.getCategory().getName();
        Notification notification = new Notification();
        notification.setUser(user);
        notification.setBudgetId(budget.getId());
        notification.setBudgetMonth(month);
        notification.setBudgetYear(year);
        notification.setThresholdPercent(threshold);
        notification.setType(type);
        notification.setTitle(category + " budget " + (threshold == 100 ? "exceeded" : "at 80%"));
        notification.setMessage(category + " spending is " + spent.toPlainString() + " against a monthly limit of " + budget.getLimitAmount().toPlainString() + ".");
        notificationRepository.save(notification);
    }
}
