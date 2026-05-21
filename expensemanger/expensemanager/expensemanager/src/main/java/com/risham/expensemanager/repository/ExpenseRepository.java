package com.risham.expensemanager.repository;

import com.risham.expensemanager.entity.Expense;
import com.risham.expensemanager.entity.User;
import com.risham.expensemanager.enums.ExpenseCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    List<Expense> findByUser(User user);

    Long countByUser(User user);

    List<Expense> findByUserAndCategory(User user, ExpenseCategory category);

    List<Expense> findByUserAndExpenseDateBetween(
            User user,
            LocalDate startDate,
            LocalDate endDate
    );

    List<Expense> findTop5ByUserOrderByExpenseDateDescCreatedAtDesc(User user);

    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM Expense e WHERE e.user = :user")
    BigDecimal getTotalExpenseByUser(@Param("user") User user);

    @Query("""
            SELECT COALESCE(SUM(e.amount), 0)
            FROM Expense e
            WHERE e.user = :user
            AND e.expenseDate BETWEEN :startDate AND :endDate
            """)
    BigDecimal getMonthlyExpenseByUser(
            @Param("user") User user,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query("""
            SELECT e.category
            FROM Expense e
            WHERE e.user = :user
            GROUP BY e.category
            ORDER BY SUM(e.amount) DESC
            """)
    List<ExpenseCategory> findCategoriesOrderBySpendDesc(@Param("user") User user);
}