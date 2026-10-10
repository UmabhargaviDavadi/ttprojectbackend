package com.tt.financetracker.repository;

import com.tt.financetracker.entity.Notification;
import com.tt.financetracker.entity.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByUserIdOrderByCreatedAtDesc(Long userId);
    long countByUserIdAndReadFalse(Long userId);
    boolean existsByBudgetIdAndBudgetMonthAndBudgetYearAndThresholdPercent(Long budgetId, Integer month, Integer year, Integer thresholdPercent);
}
