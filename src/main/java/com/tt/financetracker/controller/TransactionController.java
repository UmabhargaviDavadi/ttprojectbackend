package com.tt.financetracker.controller;

import com.tt.financetracker.dto.request.TransactionRequest;
import com.tt.financetracker.dto.response.PageResponse;
import com.tt.financetracker.dto.response.TransactionResponse;
import com.tt.financetracker.service.TransactionService;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/transactions")
public class TransactionController {
    private final TransactionService transactionService;
    public TransactionController(TransactionService transactionService) { this.transactionService = transactionService; }

    @GetMapping
    public PageResponse<TransactionResponse> list(Authentication authentication,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String category, @RequestParam(required = false) String type,
            @RequestParam(required = false) LocalDate from, @RequestParam(required = false) LocalDate to,
            @RequestParam(required = false) String search, @RequestParam(required = false) BigDecimal minAmount,
            @RequestParam(required = false) BigDecimal maxAmount, @RequestParam(required = false) String sort) {
        return transactionService.search(authentication.getName(), page, size, category, type, from, to, search, minAmount, maxAmount, sort);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TransactionResponse create(Authentication authentication, @Valid @RequestBody TransactionRequest request) {
        return transactionService.create(authentication.getName(), request);
    }

    @PutMapping("/{id}")
    public TransactionResponse update(Authentication authentication, @PathVariable Long id, @Valid @RequestBody TransactionRequest request) {
        return transactionService.update(authentication.getName(), id, request);
    }

    @DeleteMapping("/{id}")
    public Map<String, Boolean> delete(Authentication authentication, @PathVariable Long id) {
        transactionService.delete(authentication.getName(), id);
        return Map.of("success", true);
    }
}
