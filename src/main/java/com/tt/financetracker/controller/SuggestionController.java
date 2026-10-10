package com.tt.financetracker.controller;

import com.tt.financetracker.dto.response.InvestmentSuggestionResponse;
import com.tt.financetracker.dto.response.SavingsSuggestionResponse;
import com.tt.financetracker.service.SuggestionService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/suggestions")
public class SuggestionController {
    private final SuggestionService suggestionService;
    public SuggestionController(SuggestionService suggestionService) { this.suggestionService = suggestionService; }
    @GetMapping("/savings") public SavingsSuggestionResponse savings(Authentication authentication) { return suggestionService.savings(authentication.getName()); }
    @GetMapping("/investments") public InvestmentSuggestionResponse investments(Authentication authentication) { return suggestionService.investments(authentication.getName()); }
}
