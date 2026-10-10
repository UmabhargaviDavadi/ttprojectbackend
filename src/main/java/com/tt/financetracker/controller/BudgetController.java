package com.tt.financetracker.controller;

import com.tt.financetracker.dto.request.BudgetRequest;
import com.tt.financetracker.dto.response.BudgetResponse;
import com.tt.financetracker.service.BudgetService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/budgets")
public class BudgetController {
    private final BudgetService budgetService;
    public BudgetController(BudgetService budgetService) { this.budgetService = budgetService; }

    @GetMapping
    public List<BudgetResponse> list(Authentication authentication,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year) {
        LocalDate now = LocalDate.now();
        int selectedMonth = month == null ? now.getMonthValue() : month;
        int selectedYear = year == null ? now.getYear() : year;
        return budgetService.list(authentication.getName(), selectedMonth, selectedYear);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BudgetResponse create(Authentication authentication, @Valid @RequestBody BudgetRequest request) {
        return budgetService.create(authentication.getName(), request);
    }

    @PutMapping("/{id}")
    public BudgetResponse update(Authentication authentication, @PathVariable Long id, @Valid @RequestBody BudgetRequest request) {
        return budgetService.update(authentication.getName(), id, request);
    }

    @DeleteMapping("/{id}")
    public Map<String, Boolean> delete(Authentication authentication, @PathVariable Long id) {
        budgetService.delete(authentication.getName(), id);
        return Map.of("success", true);
    }
}
