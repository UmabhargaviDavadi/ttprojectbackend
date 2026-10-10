package com.tt.financetracker.repository;

import com.tt.financetracker.entity.Budget;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BudgetRepository extends JpaRepository<Budget, Long> {
    List<Budget> findByUserIdAndMonthAndYearOrderById(Long userId, int month, int year);
    boolean existsByUserIdAndCategoryIdAndMonthAndYear(Long userId, Long categoryId, int month, int year);
    boolean existsByUserIdAndCategoryIsNullAndMonthAndYear(Long userId, int month, int year);
    boolean existsByUserIdAndCategoryIdAndMonthAndYearAndIdNot(Long userId, Long categoryId, int month, int year, Long id);
    boolean existsByUserIdAndCategoryIsNullAndMonthAndYearAndIdNot(Long userId, int month, int year, Long id);
}
