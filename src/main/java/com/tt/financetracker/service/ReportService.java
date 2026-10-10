package com.tt.financetracker.service;

import com.tt.financetracker.dto.response.CategorySummaryListResponse;
import com.tt.financetracker.dto.response.CategorySummaryResponse;
import com.tt.financetracker.dto.response.MonthlyReportResponse;
import com.tt.financetracker.dto.response.TrendResponse;
import com.tt.financetracker.entity.TransactionType;
import com.tt.financetracker.entity.User;
import com.tt.financetracker.exception.BadRequestException;
import com.tt.financetracker.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class ReportService {
    private static final Map<String, String> COLORS = Map.of("Food", "#2f8c6a", "Travel", "#72b98d",
            "Entertainment", "#e7a948", "Bills", "#6689a2", "Shopping", "#d7795e",
            "Health", "#9c7dba", "Education", "#75aab0", "Other", "#87948c");
    private final TransactionRepository transactionRepository;
    private final AccountService accountService;

    public ReportService(TransactionRepository transactionRepository, AccountService accountService) {
        this.transactionRepository = transactionRepository;
        this.accountService = accountService;
    }

    public MonthlyReportResponse monthly(String email, int month, int year) {
        validateMonth(month, year);
        User user = accountService.requireByEmail(email);
        LocalDate start = LocalDate.of(year, month, 1);
        LocalDate end = start.plusMonths(1);
        BigDecimal income = total(user, TransactionType.INCOME, start, end);
        BigDecimal expense = total(user, TransactionType.EXPENSE, start, end);
        BigDecimal balance = income.subtract(expense);
        int savingsRate = income.signum() == 0 ? 0 : balance.multiply(BigDecimal.valueOf(100)).divide(income, 0, RoundingMode.HALF_UP).intValue();
        LocalDate previousStart = start.minusMonths(1);
        BigDecimal previousIncome = total(user, TransactionType.INCOME, previousStart, start);
        BigDecimal previousExpense = total(user, TransactionType.EXPENSE, previousStart, start);
        return new MonthlyReportResponse(month, year, income, expense, balance, savingsRate,
                categories(user, start, end, expense), new MonthlyReportResponse.Comparison(
                percentChange(previousIncome, income), percentChange(previousExpense, expense)));
    }

    public CategorySummaryListResponse categorySummary(String email, int month, int year) {
        validateMonth(month, year);
        User user = accountService.requireByEmail(email);
        LocalDate start = LocalDate.of(year, month, 1);
        BigDecimal expense = total(user, TransactionType.EXPENSE, start, start.plusMonths(1));
        return new CategorySummaryListResponse(categories(user, start, start.plusMonths(1), expense));
    }

    public TrendResponse trend(String email, int months) {
        if (months < 1 || months > 24) throw new BadRequestException("months must be between 1 and 24");
        User user = accountService.requireByEmail(email);
        LocalDate currentMonth = LocalDate.now().withDayOfMonth(1);
        LocalDate firstMonth = currentMonth.minusMonths(months - 1L);
        List<TrendResponse.TrendItem> items = java.util.stream.LongStream.range(0, months).mapToObj(offset -> {
            LocalDate start = firstMonth.plusMonths(offset);
            LocalDate end = start.plusMonths(1);
            return new TrendResponse.TrendItem(start.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH),
                    start.getMonthValue(), start.getYear(), total(user, TransactionType.INCOME, start, end),
                    total(user, TransactionType.EXPENSE, start, end));
        }).toList();
        return new TrendResponse(items);
    }

    private List<CategorySummaryResponse> categories(User user, LocalDate start, LocalDate end, BigDecimal totalExpense) {
        if (totalExpense.signum() == 0) return List.of();
        return transactionRepository.sumExpensesByCategory(user.getId(), start, end).stream().map(row -> {
            String name = (String) row[0];
            BigDecimal amount = (BigDecimal) row[1];
            int percentage = amount.multiply(BigDecimal.valueOf(100)).divide(totalExpense, 0, RoundingMode.HALF_UP).intValue();
            return new CategorySummaryResponse(name, amount, percentage, COLORS.getOrDefault(name, COLORS.get("Other")));
        }).toList();
    }

    private BigDecimal total(User user, TransactionType type, LocalDate start, LocalDate end) {
        return transactionRepository.sumByUserAndTypeAndDateRange(user.getId(), type, start, end);
    }

    private double percentChange(BigDecimal previous, BigDecimal current) {
        if (previous.signum() == 0) return current.signum() == 0 ? 0.0 : 100.0;
        return current.subtract(previous).multiply(BigDecimal.valueOf(100)).divide(previous, 1, RoundingMode.HALF_UP).doubleValue();
    }

    private void validateMonth(int month, int year) {
        if (month < 1 || month > 12 || year < 2000 || year > 2100) throw new BadRequestException("month must be 1-12 and year must be between 2000 and 2100");
    }
}
