package com.tt.financetracker.service;

import com.tt.financetracker.entity.TransactionType;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class CategorizationService {
    private static final Map<String, List<String>> EXPENSE_KEYWORDS = Map.of(
            "Travel", List.of("uber", "ola", "bus", "train", "fuel", "metro", "taxi", "cab"),
            "Food", List.of("swiggy", "zomato", "restaurant", "grocery", "groceries", "food", "cafe"),
            "Entertainment", List.of("netflix", "movie", "spotify", "cinema", "prime video"),
            "Bills", List.of("electricity", "rent", "recharge", "wifi", "internet", "water bill"),
            "Shopping", List.of("amazon", "flipkart", "clothes", "shoes", "shopping"),
            "Health", List.of("pharmacy", "doctor", "hospital", "clinic", "medicine"),
            "Education", List.of("course", "books", "tuition", "class", "education")
    );

    public String categorize(String description, TransactionType type) {
        if (type == TransactionType.INCOME) {
            String text = description.toLowerCase(Locale.ROOT);
            if (text.contains("salary") || text.contains("payroll")) return "Salary";
            if (text.contains("freelance") || text.contains("client")) return "Freelance";
            if (text.contains("allowance")) return "Allowance";
            if (text.contains("investment") || text.contains("dividend")) return "Investment";
            return "Other";
        }
        String text = description.toLowerCase(Locale.ROOT);
        return EXPENSE_KEYWORDS.entrySet().stream()
                .filter(entry -> entry.getValue().stream().anyMatch(text::contains))
                .map(Map.Entry::getKey).findFirst().orElse("Other");
    }
}
