package com.risham.expensemanager.service;

import com.risham.expensemanager.dto.request.BudgetRequest;
import com.risham.expensemanager.dto.response.BudgetResponse;
import com.risham.expensemanager.dto.response.BudgetStatusResponse;
import com.risham.expensemanager.entity.Budget;
import com.risham.expensemanager.entity.Expense;
import com.risham.expensemanager.entity.User;
import com.risham.expensemanager.repository.BudgetRepository;
import com.risham.expensemanager.repository.ExpenseRepository;
import com.risham.expensemanager.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BudgetService {

    private final BudgetRepository budgetRepository;
    private final ExpenseRepository expenseRepository;
    private final UserRepository userRepository;

    public BudgetResponse createBudget(BudgetRequest request) {

        User currentUser = getCurrentUser();

        boolean exists = budgetRepository.existsByUserAndCategoryAndMonthAndYear(
                currentUser,
                request.getCategory(),
                request.getMonth(),
                request.getYear()
        );

        if (exists) {
            throw new RuntimeException("Budget already exists for this category, month and year");
        }

        Budget budget = Budget.builder()
                .category(request.getCategory())
                .amount(request.getAmount())
                .month(request.getMonth())
                .year(request.getYear())
                .user(currentUser)
                .build();

        Budget savedBudget = budgetRepository.save(budget);

        return mapToResponse(savedBudget);
    }

    public List<BudgetResponse> getAllBudgets() {

        User currentUser = getCurrentUser();

        return budgetRepository.findByUser(currentUser)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    public BudgetResponse getBudgetById(Long id) {

        User currentUser = getCurrentUser();

        Budget budget = budgetRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Budget not found with id: " + id));

        if (!budget.getUser().getId().equals(currentUser.getId())) {
            throw new RuntimeException("You are not allowed to access this budget");
        }

        return mapToResponse(budget);
    }

    public BudgetResponse updateBudget(Long id, BudgetRequest request) {

        User currentUser = getCurrentUser();

        Budget budget = budgetRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Budget not found with id: " + id));

        if (!budget.getUser().getId().equals(currentUser.getId())) {
            throw new RuntimeException("You are not allowed to update this budget");
        }

        budget.setCategory(request.getCategory());
        budget.setAmount(request.getAmount());
        budget.setMonth(request.getMonth());
        budget.setYear(request.getYear());

        Budget updatedBudget = budgetRepository.save(budget);

        return mapToResponse(updatedBudget);
    }

    public String deleteBudget(Long id) {

        User currentUser = getCurrentUser();

        Budget budget = budgetRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Budget not found with id: " + id));

        if (!budget.getUser().getId().equals(currentUser.getId())) {
            throw new RuntimeException("You are not allowed to delete this budget");
        }

        budgetRepository.delete(budget);

        return "Budget deleted successfully";
    }

    public List<BudgetResponse> getBudgetsByMonthAndYear(Integer month, Integer year) {

        User currentUser = getCurrentUser();

        return budgetRepository.findByUserAndMonthAndYear(currentUser, month, year)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    public List<BudgetStatusResponse> getBudgetStatus(Integer month, Integer year) {

        User currentUser = getCurrentUser();

        List<Budget> budgets = budgetRepository.findByUserAndMonthAndYear(currentUser, month, year);

        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());

        List<Expense> expenses = expenseRepository.findByUserAndExpenseDateBetween(
                currentUser,
                startDate,
                endDate
        );

        return budgets.stream()
                .map(budget -> {
                    BigDecimal spentAmount = expenses.stream()
                            .filter(expense -> expense.getCategory().equals(budget.getCategory()))
                            .map(Expense::getAmount)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    BigDecimal remainingAmount = budget.getAmount().subtract(spentAmount);

                    BigDecimal usedPercentage = calculateUsedPercentage(
                            spentAmount,
                            budget.getAmount()
                    );

                    boolean overspent = spentAmount.compareTo(budget.getAmount()) > 0;

                    return BudgetStatusResponse.builder()
                            .category(budget.getCategory())
                            .budgetAmount(budget.getAmount())
                            .spentAmount(spentAmount)
                            .remainingAmount(remainingAmount)
                            .usedPercentage(usedPercentage)
                            .overspent(overspent)
                            .build();
                })
                .toList();
    }

    private BigDecimal calculateUsedPercentage(BigDecimal spentAmount, BigDecimal budgetAmount) {

        if (budgetAmount.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        return spentAmount
                .divide(budgetAmount, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);
    }

    private User getCurrentUser() {

        String email = SecurityContextHolder.getContext()
                .getAuthentication()
                .getName();

        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Logged-in user not found"));
    }

    private BudgetResponse mapToResponse(Budget budget) {
        return BudgetResponse.builder()
                .id(budget.getId())
                .category(budget.getCategory())
                .amount(budget.getAmount())
                .month(budget.getMonth())
                .year(budget.getYear())
                .createdAt(budget.getCreatedAt())
                .updatedAt(budget.getUpdatedAt())
                .build();
    }
}