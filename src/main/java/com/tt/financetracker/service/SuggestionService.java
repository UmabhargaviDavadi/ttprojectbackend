package com.tt.financetracker.service;

import com.tt.financetracker.dto.response.BudgetResponse;
import com.tt.financetracker.dto.response.InvestmentSuggestionResponse;
import com.tt.financetracker.dto.response.SavingsSuggestionResponse;
import com.tt.financetracker.entity.TransactionType;
import com.tt.financetracker.entity.User;
import com.tt.financetracker.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

@Service
@Transactional(readOnly = true)
public class SuggestionService {
    private static final String DISCLAIMER = "General guidance only, not financial advice.";
    private final AccountService accountService;
    private final TransactionRepository transactionRepository;
    private final BudgetService budgetService;

    public SuggestionService(AccountService accountService, TransactionRepository transactionRepository, BudgetService budgetService) {
        this.accountService = accountService;
        this.transactionRepository = transactionRepository;
        this.budgetService = budgetService;
    }

    public SavingsSuggestionResponse savings(String email) {
        User user = accountService.requireByEmail(email);
        LocalDate start = LocalDate.now().withDayOfMonth(1);
        LocalDate end = start.plusMonths(1);
        BigDecimal expenses = sum(user, TransactionType.EXPENSE, start, end);
        BigDecimal income = sum(user, TransactionType.INCOME, start, end);
        List<SavingsSuggestionResponse.SavingTip> tips = new ArrayList<>();
        AtomicLong ids = new AtomicLong(1);
        if (expenses.signum() == 0 && income.signum() == 0) {
            tips.add(tip(ids, "Start with a simple spending check-in", "Track a few everyday purchases to see what feels important to you.", "Build a clear baseline", "Getting started", "LOW"));
            tips.add(tip(ids, "Pick one small savings goal", "Choose a realistic amount and a date; consistency matters more than size.", "Build a savings habit", "Save first", "LOW"));
        } else {
            List<Object[]> categories = transactionRepository.sumExpensesByCategory(user.getId(), start, end);
            if (expenses.signum() > 0 && !categories.isEmpty()) {
                String top = (String) categories.get(0)[0];
                BigDecimal topAmount = (BigDecimal) categories.get(0)[1];
                if (topAmount.multiply(BigDecimal.valueOf(100)).divide(expenses, 2, RoundingMode.HALF_UP).compareTo(BigDecimal.valueOf(30)) > 0)
                    tips.add(tip(ids, "Take a closer look at " + top, top + " makes up a large share of this month's expenses. Try setting a comfortable weekly limit.", "Review your largest category", "Spending pattern", "MEDIUM"));
            }
            int savingsRate = income.signum() == 0 ? 0 : income.subtract(expenses).multiply(BigDecimal.valueOf(100)).divide(income, 0, RoundingMode.HALF_UP).intValue();
            if (savingsRate < 20) tips.add(tip(ids, "Try a small automatic transfer", "Your current savings rate is below 20%. A modest transfer on payday can help establish a routine.", "Aim for a manageable savings habit", "Save first", "HIGH"));
            BigDecimal previous = sum(user, TransactionType.EXPENSE, start.minusMonths(1), start);
            if (previous.signum() > 0 && expenses.compareTo(previous) > 0)
                tips.add(tip(ids, "Compare this month with last month", "Expenses are higher than last month so far. A quick category review can show what changed.", "Spot a month-over-month increase", "Monthly check-in", "MEDIUM"));
            List<BudgetResponse> budgets = budgetService.list(email, start.getMonthValue(), start.getYear());
            if (budgets.stream().anyMatch(budget -> budget.percentage() >= 100))
                tips.add(tip(ids, "Refresh a budget boundary", "At least one monthly budget is over its limit. Review the plan and adjust next month's limits to fit your actual needs.", "Keep plans realistic", "Budget", "HIGH"));
            if (tips.isEmpty()) tips.add(tip(ids, "Keep your steady rhythm", "Your current numbers look balanced. Keep tracking and protect the savings habit you have built.", "Stay consistent", "Good habit", "LOW"));
        }
        return new SavingsSuggestionResponse(tips);
    }

    public InvestmentSuggestionResponse investments(String email) {
        User user = accountService.requireByEmail(email);
        LocalDate start = LocalDate.now().withDayOfMonth(1);
        BigDecimal income = sum(user, TransactionType.INCOME, start, start.plusMonths(1));
        BigDecimal expenses = sum(user, TransactionType.EXPENSE, start, start.plusMonths(1));
        BigDecimal surplus = income.subtract(expenses).max(BigDecimal.ZERO);
        BigDecimal emergencyContribution = surplus.multiply(new BigDecimal("0.70")).setScale(2, RoundingMode.HALF_UP);
        BigDecimal laterContribution = surplus.subtract(emergencyContribution).divide(BigDecimal.valueOf(3), 2, RoundingMode.HALF_UP);
        List<InvestmentSuggestionResponse.InvestmentTip> tips = List.of(
                investment(1, "Emergency fund", "Build 3–6 months of essential expenses in an accessible savings account or suitable low-risk option before investing for growth.", "Low", "Immediate priority", emergencyContribution),
                investment(2, "SIP in diversified mutual funds", "After your emergency reserve is on track, learn about diversified index or mutual funds for long-term goals. Returns are not guaranteed.", "Moderate", "5+ years", laterContribution),
                investment(3, "Fixed or recurring deposit", "A fixed or recurring deposit can suit a known near-term goal where predictable returns matter more than growth; compare terms and early-withdrawal rules.", "Low", "1–3 years", laterContribution),
                investment(4, "Public Provident Fund (PPF)", "Explore PPF for long-term, tax-aware saving and understand the lock-in, contribution rules and withdrawal terms.", "Low", "15 years", laterContribution));
        return new InvestmentSuggestionResponse(tips, DISCLAIMER);
    }

    private BigDecimal sum(User user, TransactionType type, LocalDate from, LocalDate to) {
        return transactionRepository.sumByUserAndTypeAndDateRange(user.getId(), type, from, to);
    }

    private SavingsSuggestionResponse.SavingTip tip(AtomicLong ids, String title, String message, String impact, String category, String priority) {
        return new SavingsSuggestionResponse.SavingTip(ids.getAndIncrement(), title, message, impact, category, priority, message);
    }

    private InvestmentSuggestionResponse.InvestmentTip investment(long id, String name, String description, String risk,
                                                                  String horizon, BigDecimal amount) {
        return new InvestmentSuggestionResponse.InvestmentTip(id, name, name, description, risk, horizon, amount);
    }
}
