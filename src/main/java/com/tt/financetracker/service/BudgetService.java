package com.tt.financetracker.service;

import com.tt.financetracker.dto.request.BudgetRequest;
import com.tt.financetracker.dto.response.BudgetResponse;
import com.tt.financetracker.entity.Budget;
import com.tt.financetracker.entity.Category;
import com.tt.financetracker.entity.TransactionType;
import com.tt.financetracker.entity.User;
import com.tt.financetracker.exception.BadRequestException;
import com.tt.financetracker.exception.DuplicateResourceException;
import com.tt.financetracker.exception.ResourceNotFoundException;
import com.tt.financetracker.repository.BudgetRepository;
import com.tt.financetracker.repository.CategoryRepository;
import com.tt.financetracker.repository.TransactionRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class BudgetService {
    private final BudgetRepository budgetRepository;
    private final CategoryRepository categoryRepository;
    private final TransactionRepository transactionRepository;
    private final AccountService accountService;

    public BudgetService(BudgetRepository budgetRepository, CategoryRepository categoryRepository,
                         TransactionRepository transactionRepository, AccountService accountService) {
        this.budgetRepository = budgetRepository;
        this.categoryRepository = categoryRepository;
        this.transactionRepository = transactionRepository;
        this.accountService = accountService;
    }

    @Transactional(readOnly = true)
    public List<BudgetResponse> list(String email, int month, int year) {
        User user = accountService.requireByEmail(email);
        LocalDate start = LocalDate.of(year, month, 1);
        LocalDate end = start.plusMonths(1);
        return budgetRepository.findByUserIdAndMonthAndYearOrderById(user.getId(), month, year).stream()
                .map(budget -> BudgetResponse.from(budget, spent(user, budget, start, end))).toList();
    }

    @Transactional
    public BudgetResponse create(String email, BudgetRequest request) {
        User user = accountService.requireByEmail(email);
        Category category = resolveCategory(request.category());
        if (exists(user.getId(), category, request.month(), request.year(), null))
            throw new DuplicateResourceException("A budget already exists for this category and month");
        Budget budget = new Budget();
        budget.setUser(user);
        budget.setCategory(category);
        budget.setMonth(request.month());
        budget.setYear(request.year());
        budget.setLimitAmount(request.limit());
        budget = budgetRepository.save(budget);
        return BudgetResponse.from(budget, BigDecimal.ZERO);
    }

    @Transactional
    public BudgetResponse update(String email, Long id, BudgetRequest request) {
        User user = accountService.requireByEmail(email);
        Budget budget = ownedBudget(id, user.getId());
        Category category = resolveCategory(request.category());
        if (exists(user.getId(), category, request.month(), request.year(), id))
            throw new DuplicateResourceException("A budget already exists for this category and month");
        budget.setCategory(category);
        budget.setMonth(request.month());
        budget.setYear(request.year());
        budget.setLimitAmount(request.limit());
        Budget saved = budgetRepository.save(budget);
        LocalDate start = LocalDate.of(saved.getYear(), saved.getMonth(), 1);
        return BudgetResponse.from(saved, spent(user, saved, start, start.plusMonths(1)));
    }

    @Transactional
    public void delete(String email, Long id) {
        User user = accountService.requireByEmail(email);
        budgetRepository.delete(ownedBudget(id, user.getId()));
    }

    private Budget ownedBudget(Long id, Long userId) {
        Budget budget = budgetRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Budget not found"));
        if (!budget.getUser().getId().equals(userId)) throw new AccessDeniedException("This budget belongs to another user");
        return budget;
    }

    private boolean exists(Long userId, Category category, int month, int year, Long exceptId) {
        if (category == null) return exceptId == null
                ? budgetRepository.existsByUserIdAndCategoryIsNullAndMonthAndYear(userId, month, year)
                : budgetRepository.existsByUserIdAndCategoryIsNullAndMonthAndYearAndIdNot(userId, month, year, exceptId);
        return exceptId == null
                ? budgetRepository.existsByUserIdAndCategoryIdAndMonthAndYear(userId, category.getId(), month, year)
                : budgetRepository.existsByUserIdAndCategoryIdAndMonthAndYearAndIdNot(userId, category.getId(), month, year, exceptId);
    }

    private Category resolveCategory(String name) {
        if (name == null || name.isBlank() || name.equalsIgnoreCase("Overall")) return null;
        return categoryRepository.findByNameIgnoreCaseAndType(name.trim(), TransactionType.EXPENSE)
                .orElseThrow(() -> new BadRequestException("Unknown expense category: " + name));
    }

    private BigDecimal spent(User user, Budget budget, LocalDate start, LocalDate end) {
        if (budget.getCategory() == null)
            return transactionRepository.sumByUserAndTypeAndDateRange(user.getId(), TransactionType.EXPENSE, start, end);
        return transactionRepository.sumExpensesByCategoryAndDateRange(user.getId(), budget.getCategory().getId(), start, end);
    }
}
