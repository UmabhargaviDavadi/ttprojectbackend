package com.tt.financetracker.dto.response;

import java.util.List;

public record SavingsSuggestionResponse(List<SavingTip> items) {
    public record SavingTip(Long id, String title, String description, String impact, String category, String priority, String message) {}
}
