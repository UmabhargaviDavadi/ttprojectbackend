package com.tt.financetracker.config;

import com.tt.financetracker.entity.Budget;
import com.tt.financetracker.entity.Category;
import com.tt.financetracker.entity.Goal;
import com.tt.financetracker.entity.Transaction;
import com.tt.financetracker.entity.TransactionType;
import com.tt.financetracker.entity.User;
import com.tt.financetracker.repository.BudgetRepository;
import com.tt.financetracker.repository.CategoryRepository;
import com.tt.financetracker.repository.GoalRepository;
import com.tt.financetracker.repository.TransactionRepository;
import com.tt.financetracker.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Component
public class DataLoader implements CommandLineRunner {
    private static final List<String> EXPENSES = List.of("Food", "Travel", "Entertainment", "Bills", "Shopping", "Health", "Education", "Other");
    private static final List<String> INCOMES = List.of("Salary", "Allowance", "Freelance", "Investment", "Other");
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;
    private final BudgetRepository budgetRepository;
    private final GoalRepository goalRepository;
    private final PasswordEncoder passwordEncoder;
    private final boolean seedDemo;

    public DataLoader(CategoryRepository categoryRepository, UserRepository userRepository,
                      TransactionRepository transactionRepository, BudgetRepository budgetRepository,
                      GoalRepository goalRepository, PasswordEncoder passwordEncoder,
                      @Value("${app.seed-demo:true}") boolean seedDemo) {
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
        this.transactionRepository = transactionRepository;
        this.budgetRepository = budgetRepository;
        this.goalRepository = goalRepository;
        this.passwordEncoder = passwordEncoder;
        this.seedDemo = seedDemo;
    }

    @Override
    @Transactional
    public void run(String... args) {
        EXPENSES.forEach(name -> seedCategory(name, TransactionType.EXPENSE));
        INCOMES.forEach(name -> seedCategory(name, TransactionType.INCOME));
        if (seedDemo) seedDemoUser();
    }

    private void seedCategory(String name, TransactionType type) {
        if (!categoryRepository.existsByNameIgnoreCaseAndType(name, type)) categoryRepository.save(new Category(name, type));
    }

    private void seedDemoUser() {
        User user = userRepository.findByEmailIgnoreCase("demo@example.com").orElseGet(() -> {
            User created = new User();
            created.setName("Demo User");
            created.setEmail("demo@example.com");
            created.setPassword(passwordEncoder.encode("Demo@1234"));
            created.setCurrency("INR");
            return userRepository.save(created);
        });
        if (transactionRepository.countByUserAndTypeAndDateRange(user.getId(), TransactionType.INCOME,
                LocalDate.now().withDayOfMonth(1), LocalDate.now().withDayOfMonth(1).plusMonths(1)) > 0) return;
        LocalDate today = LocalDate.now();
        addTransaction(user, "Salary", TransactionType.INCOME, "Monthly salary", "52000", today.withDayOfMonth(1));
        addTransaction(user, "Freelance", TransactionType.INCOME, "Freelance design project", "8500", today.minusDays(5));
        addTransaction(user, "Food", TransactionType.EXPENSE, "Groceries at Nature Basket", "1280", today.minusDays(1));
        addTransaction(user, "Travel", TransactionType.EXPENSE, "Metro card recharge", "600", today.minusDays(2));
        addTransaction(user, "Bills", TransactionType.EXPENSE, "Electricity bill", "1840", today.minusDays(3));
        addTransaction(user, "Food", TransactionType.EXPENSE, "Weekend brunch", "940", today.minusDays(4));
        addTransaction(user, "Entertainment", TransactionType.EXPENSE, "Netflix subscription", "649", today.minusDays(6));
        addTransaction(user, "Health", TransactionType.EXPENSE, "Pharmacy", "420", today.minusDays(8));
        addTransaction(user, "Shopping", TransactionType.EXPENSE, "New running shoes", "2899", today.minusDays(12));
        LocalDate monthStart = today.withDayOfMonth(1);
        seedBudget(user, null, 30000, monthStart);
        seedBudget(user, "Food", 8000, monthStart);
        seedBudget(user, "Travel", 4000, monthStart);
        seedBudget(user, "Entertainment", 2500, monthStart);
        if (goalRepository.findByUserIdOrderByCreatedAtDesc(user.getId()).isEmpty()) {
            Goal goal = new Goal();
            goal.setUser(user);
            goal.setName("Emergency cushion");
            goal.setTargetAmount(new BigDecimal("100000"));
            goal.setSavedAmount(new BigDecimal("42000"));
            goal.setDeadline(today.plusMonths(6));
            goalRepository.save(goal);
        }
    }

    private void addTransaction(User user, String categoryName, TransactionType type, String description, String amount, LocalDate date) {
        Transaction transaction = new Transaction();
        transaction.setUser(user);
        transaction.setCategory(categoryRepository.findByNameIgnoreCaseAndType(categoryName, type).orElseThrow());
        transaction.setType(type);
        transaction.setAmount(new BigDecimal(amount));
        transaction.setDescription(description);
        transaction.setPaymentMethod(type == TransactionType.INCOME ? "Bank transfer" : "UPI");
        transaction.setDate(date);
        transactionRepository.save(transaction);
    }

    private void seedBudget(User user, String categoryName, int limit, LocalDate month) {
        Category category = categoryName == null ? null : categoryRepository.findByNameIgnoreCaseAndType(categoryName, TransactionType.EXPENSE).orElseThrow();
        boolean exists = category == null
                ? budgetRepository.existsByUserIdAndCategoryIsNullAndMonthAndYear(user.getId(), month.getMonthValue(), month.getYear())
                : budgetRepository.existsByUserIdAndCategoryIdAndMonthAndYear(user.getId(), category.getId(), month.getMonthValue(), month.getYear());
        if (exists) return;
        Budget budget = new Budget();
        budget.setUser(user);
        budget.setCategory(category);
        budget.setMonth(month.getMonthValue());
        budget.setYear(month.getYear());
        budget.setLimitAmount(BigDecimal.valueOf(limit));
        budgetRepository.save(budget);
    }
}
