package com.risham.expensemanager.repository;

import com.risham.expensemanager.entity.Budget;
import com.risham.expensemanager.entity.User;
import com.risham.expensemanager.enums.ExpenseCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BudgetRepository extends JpaRepository<Budget, Long> {

    List<Budget> findByUser(User user);

    List<Budget> findByUserAndMonthAndYear(User user, Integer month, Integer year);

    Optional<Budget> findByUserAndCategoryAndMonthAndYear(
            User user,
            ExpenseCategory category,
            Integer month,
            Integer year
    );

    boolean existsByUserAndCategoryAndMonthAndYear(
            User user,
            ExpenseCategory category,
            Integer month,
            Integer year
    );
}