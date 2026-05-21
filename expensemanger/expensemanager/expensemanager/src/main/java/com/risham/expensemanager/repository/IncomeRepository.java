package com.risham.expensemanager.repository;

import com.risham.expensemanager.entity.Income;
import com.risham.expensemanager.entity.User;
import com.risham.expensemanager.enums.IncomeSource;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface IncomeRepository extends JpaRepository<Income, Long> {

    List<Income> findByUser(User user);

    List<Income> findByUserAndSource(User user, IncomeSource source);

    List<Income> findByUserAndIncomeDateBetween(User user, LocalDate startDate, LocalDate endDate);

    List<Income> findTop5ByUserOrderByIncomeDateDescCreatedAtDesc(User user);
}