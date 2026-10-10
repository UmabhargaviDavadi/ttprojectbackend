package com.tt.financetracker.repository;

import com.tt.financetracker.entity.Category;
import com.tt.financetracker.entity.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    Optional<Category> findByNameIgnoreCaseAndType(String name, TransactionType type);
    boolean existsByNameIgnoreCaseAndType(String name, TransactionType type);
}
