package com.tt.financetracker.service;

import com.tt.financetracker.dto.request.TransactionRequest;
import com.tt.financetracker.dto.response.PageResponse;
import com.tt.financetracker.dto.response.TransactionResponse;
import com.tt.financetracker.entity.Category;
import com.tt.financetracker.entity.Transaction;
import com.tt.financetracker.entity.TransactionType;
import com.tt.financetracker.entity.User;
import com.tt.financetracker.exception.BadRequestException;
import com.tt.financetracker.exception.ResourceNotFoundException;
import com.tt.financetracker.repository.CategoryRepository;
import com.tt.financetracker.repository.TransactionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Locale;

@Service
public class TransactionService {
    private final TransactionRepository transactionRepository;
    private final CategoryRepository categoryRepository;
    private final AccountService accountService;
    private final CategorizationService categorizationService;
    private final BudgetAlertService budgetAlertService;

    public TransactionService(TransactionRepository transactionRepository, CategoryRepository categoryRepository,
                              AccountService accountService, CategorizationService categorizationService,
                              BudgetAlertService budgetAlertService) {
        this.transactionRepository = transactionRepository;
        this.categoryRepository = categoryRepository;
        this.accountService = accountService;
        this.categorizationService = categorizationService;
        this.budgetAlertService = budgetAlertService;
    }

    @Transactional(readOnly = true)
    public PageResponse<TransactionResponse> search(String email, int page, int size, String category, String type,
                                                    LocalDate from, LocalDate to, String search,
                                                    BigDecimal minAmount, BigDecimal maxAmount, String sort) {
        if (page < 0 || size < 1 || size > 1000) throw new BadRequestException("page must be nonnegative and size must be between 1 and 1000");
        if (from != null && to != null && from.isAfter(to)) throw new BadRequestException("from must be on or before to");
        if (minAmount != null && maxAmount != null && minAmount.compareTo(maxAmount) > 0) throw new BadRequestException("minAmount must not exceed maxAmount");
        User user = accountService.requireByEmail(email);
        TransactionType transactionType = parseType(type);
        Pageable pageable = PageRequest.of(page, size, parseSort(sort));
        String normalizedCategory = blankToNull(category);
        String normalizedSearch = blankToNull(search);
        Specification<Transaction> filters = (root, query, builder) -> {
            var predicates = new ArrayList<jakarta.persistence.criteria.Predicate>();
            var categoryJoin = root.join("category");
            predicates.add(builder.equal(root.get("user").get("id"), user.getId()));
            if (normalizedCategory != null) predicates.add(builder.equal(builder.lower(categoryJoin.get("name")), normalizedCategory.toLowerCase(Locale.ROOT)));
            if (transactionType != null) predicates.add(builder.equal(root.get("type"), transactionType));
            if (from != null) predicates.add(builder.greaterThanOrEqualTo(root.get("date"), from));
            if (to != null) predicates.add(builder.lessThanOrEqualTo(root.get("date"), to));
            if (minAmount != null) predicates.add(builder.greaterThanOrEqualTo(root.get("amount"), minAmount));
            if (maxAmount != null) predicates.add(builder.lessThanOrEqualTo(root.get("amount"), maxAmount));
            if (normalizedSearch != null) {
                String pattern = "%" + normalizedSearch.toLowerCase(Locale.ROOT) + "%";
                predicates.add(builder.or(builder.like(builder.lower(root.get("description")), pattern),
                        builder.like(builder.lower(categoryJoin.get("name")), pattern)));
            }
            return builder.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
        Page<Transaction> result = transactionRepository.findAll(filters, pageable);
        return PageResponse.from(result, TransactionResponse::from);
    }

    @Transactional
    public TransactionResponse create(String email, TransactionRequest request) {
        User user = accountService.requireByEmail(email);
        TransactionType type = parseType(request.type());
        Category category = resolveCategory(request.category(), request.description(), type);
        Transaction transaction = new Transaction();
        transaction.setUser(user);
        apply(transaction, request, type, category);
        transaction = transactionRepository.save(transaction);
        if (type == TransactionType.EXPENSE) budgetAlertService.checkForTransaction(user, transaction.getDate());
        return TransactionResponse.from(transaction);
    }

    @Transactional
    public TransactionResponse update(String email, Long id, TransactionRequest request) {
        User user = accountService.requireByEmail(email);
        Transaction transaction = ownedTransaction(id, user.getId());
        TransactionType previousType = transaction.getType();
        LocalDate previousDate = transaction.getDate();
        TransactionType type = parseType(request.type());
        Category category = resolveCategory(request.category(), request.description(), type);
        apply(transaction, request, type, category);
        TransactionResponse response = TransactionResponse.from(transactionRepository.save(transaction));
        if (previousType == TransactionType.EXPENSE) budgetAlertService.checkForTransaction(user, previousDate);
        if (type == TransactionType.EXPENSE) budgetAlertService.checkForTransaction(user, transaction.getDate());
        return response;
    }

    @Transactional
    public void delete(String email, Long id) {
        User user = accountService.requireByEmail(email);
        transactionRepository.delete(ownedTransaction(id, user.getId()));
    }

    private Transaction ownedTransaction(Long id, Long userId) {
        return transactionRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found"));
    }

    private Category resolveCategory(String requested, String description, TransactionType type) {
        String name = StringUtils.hasText(requested) ? requested.trim() : categorizationService.categorize(description, type);
        return categoryRepository.findByNameIgnoreCaseAndType(name, type)
                .orElseThrow(() -> new BadRequestException("Category '" + name + "' is not valid for " + type.name().toLowerCase(Locale.ROOT)));
    }

    private void apply(Transaction transaction, TransactionRequest request, TransactionType type, Category category) {
        transaction.setType(type);
        transaction.setCategory(category);
        transaction.setAmount(request.amount());
        transaction.setDescription(request.description().trim());
        transaction.setPaymentMethod(StringUtils.hasText(request.paymentMethod()) ? request.paymentMethod().trim() : null);
        transaction.setDate(request.date());
    }

    private TransactionType parseType(String value) {
        if (!StringUtils.hasText(value)) return null;
        try { return TransactionType.valueOf(value.trim().toUpperCase(Locale.ROOT)); }
        catch (IllegalArgumentException exception) { throw new BadRequestException("type must be income or expense"); }
    }

    private Sort parseSort(String value) {
        if (!StringUtils.hasText(value)) return Sort.by(Sort.Direction.DESC, "date");
        String[] parts = value.split(",", 2);
        String field = parts[0].trim();
        if (!field.equals("date") && !field.equals("amount") && !field.equals("description")) throw new BadRequestException("sort field must be date, amount, or description");
        Sort.Direction direction = parts.length == 1 || parts[1].isBlank() ? Sort.Direction.DESC : Sort.Direction.fromOptionalString(parts[1].trim()).orElseThrow(() -> new BadRequestException("sort direction must be asc or desc"));
        return Sort.by(direction, field);
    }

    private String blankToNull(String value) { return StringUtils.hasText(value) ? value.trim() : null; }
}
