package com.tt.financetracker.repository;

import com.tt.financetracker.entity.Transaction;
import com.tt.financetracker.entity.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, Long>, JpaSpecificationExecutor<Transaction> {
    Optional<Transaction> findByIdAndUserId(Long id, Long userId);
    Page<Transaction> findAllByUserId(Long userId, Pageable pageable);

        @Query("select coalesce(sum(t.amount), 0) from Transaction t where t.user.id = :userId and t.type = :type and t.date >= :fromDate and t.date < :toDate")
        BigDecimal sumByUserAndTypeAndDateRange(@Param("userId") Long userId, @Param("type") TransactionType type,
                                                @Param("fromDate") LocalDate fromDate, @Param("toDate") LocalDate toDate);

    @Query("select c.name, coalesce(sum(t.amount), 0) from Transaction t join t.category c where t.user.id = :userId and t.type = com.tt.financetracker.entity.TransactionType.EXPENSE and t.date >= :fromDate and t.date < :toDate group by c.name order by sum(t.amount) desc")
    List<Object[]> sumExpensesByCategory(@Param("userId") Long userId, @Param("fromDate") LocalDate fromDate,
                                         @Param("toDate") LocalDate toDate);

    @Query("select coalesce(sum(t.amount), 0) from Transaction t where t.user.id = :userId and t.category.id = :categoryId and t.type = com.tt.financetracker.entity.TransactionType.EXPENSE and t.date >= :fromDate and t.date < :toDate")
    BigDecimal sumExpensesByCategoryAndDateRange(@Param("userId") Long userId, @Param("categoryId") Long categoryId,
                                                 @Param("fromDate") LocalDate fromDate, @Param("toDate") LocalDate toDate);

    @Query("select t.type, coalesce(sum(t.amount), 0) from Transaction t where t.user.id = :userId and t.date >= :fromDate and t.date < :toDate group by t.type")
    List<Object[]> sumByTypeAndDateRange(@Param("userId") Long userId, @Param("fromDate") LocalDate fromDate,
                                         @Param("toDate") LocalDate toDate);

    @Query("select count(t) from Transaction t where t.user.id = :userId and t.type = :type and t.date >= :fromDate and t.date < :toDate")
    long countByUserAndTypeAndDateRange(@Param("userId") Long userId, @Param("type") TransactionType type,
                                        @Param("fromDate") LocalDate fromDate, @Param("toDate") LocalDate toDate);

    List<Transaction> findTop10ByUserIdOrderByDateDesc(Long userId);
}
