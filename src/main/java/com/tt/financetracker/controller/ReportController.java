package com.tt.financetracker.controller;

import com.tt.financetracker.dto.response.CategorySummaryListResponse;
import com.tt.financetracker.dto.response.MonthlyReportResponse;
import com.tt.financetracker.dto.response.TrendResponse;
import com.tt.financetracker.service.ReportService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/reports")
public class ReportController {
    private final ReportService reportService;
    public ReportController(ReportService reportService) { this.reportService = reportService; }

    @GetMapping("/monthly")
    public MonthlyReportResponse monthly(Authentication authentication,
            @RequestParam(required = false) Integer month, @RequestParam(required = false) Integer year) {
        LocalDate now = LocalDate.now();
        return reportService.monthly(authentication.getName(), month == null ? now.getMonthValue() : month, year == null ? now.getYear() : year);
    }

    @GetMapping("/category-summary")
    public CategorySummaryListResponse categorySummary(Authentication authentication,
            @RequestParam(required = false) Integer month, @RequestParam(required = false) Integer year) {
        LocalDate now = LocalDate.now();
        return reportService.categorySummary(authentication.getName(), month == null ? now.getMonthValue() : month, year == null ? now.getYear() : year);
    }

    @GetMapping("/trend")
    public TrendResponse trend(Authentication authentication, @RequestParam(defaultValue = "6") int months) {
        return reportService.trend(authentication.getName(), months);
    }
}
