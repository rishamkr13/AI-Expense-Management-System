package com.risham.expensemanager.service;

import com.risham.expensemanager.dto.response.BudgetStatusResponse;
import com.risham.expensemanager.dto.response.CategoryExpenseResponse;
import com.risham.expensemanager.dto.response.DashboardSummaryResponse;
import com.risham.expensemanager.dto.response.ExpenseResponse;
import com.risham.expensemanager.dto.response.MonthlyTrendResponse;
import com.risham.expensemanager.entity.Budget;
import com.risham.expensemanager.entity.Expense;
import com.risham.expensemanager.entity.Income;
import com.risham.expensemanager.entity.User;
import com.risham.expensemanager.enums.ExpenseCategory;
import com.risham.expensemanager.repository.BudgetRepository;
import com.risham.expensemanager.repository.ExpenseRepository;
import com.risham.expensemanager.repository.IncomeRepository;
import com.risham.expensemanager.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final ExpenseRepository expenseRepository;
    private final IncomeRepository incomeRepository;
    private final BudgetRepository budgetRepository;
    private final UserRepository userRepository;

    public DashboardSummaryResponse getSummary() {

        User currentUser = getCurrentUser();

        List<Expense> expenses = expenseRepository.findByUser(currentUser);
        List<Income> incomes = incomeRepository.findByUser(currentUser);

        BigDecimal totalExpense = calculateTotalExpense(expenses);
        BigDecimal totalIncome = calculateTotalIncome(incomes);
        BigDecimal balance = totalIncome.subtract(totalExpense);

        LocalDate today = LocalDate.now();
        LocalDate monthStart = today.withDayOfMonth(1);
        LocalDate monthEnd = today.withDayOfMonth(today.lengthOfMonth());

        BigDecimal thisMonthExpense = expenses.stream()
                .filter(expense ->
                        expense.getExpenseDate() != null &&
                                !expense.getExpenseDate().isBefore(monthStart) &&
                                !expense.getExpenseDate().isAfter(monthEnd)
                )
                .map(expense -> safeAmount(expense.getAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal thisMonthIncome = incomes.stream()
                .filter(income ->
                        income.getIncomeDate() != null &&
                                !income.getIncomeDate().isBefore(monthStart) &&
                                !income.getIncomeDate().isAfter(monthEnd)
                )
                .map(income -> safeAmount(income.getAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal thisMonthBalance = thisMonthIncome.subtract(thisMonthExpense);

        BigDecimal savingsPercentage = calculateSavingsPercentage(totalIncome, balance);

        ExpenseCategory highestExpenseCategory = getHighestCategory(expenses);

        return DashboardSummaryResponse.builder()
                .totalIncome(totalIncome)
                .totalExpense(totalExpense)
                .balance(balance)
                .thisMonthIncome(thisMonthIncome)
                .thisMonthExpense(thisMonthExpense)
                .thisMonthBalance(thisMonthBalance)
                .savingsPercentage(savingsPercentage)
                .totalIncomeTransactions((long) incomes.size())
                .totalExpenseTransactions((long) expenses.size())
                .highestExpenseCategory(highestExpenseCategory)
                .build();
    }

    public List<CategoryExpenseResponse> getCategoryWiseExpense() {

        User currentUser = getCurrentUser();

        List<Expense> expenses = expenseRepository.findByUser(currentUser);

        Map<ExpenseCategory, BigDecimal> categoryMap = expenses.stream()
                .filter(expense -> expense.getCategory() != null)
                .collect(Collectors.groupingBy(
                        Expense::getCategory,
                        Collectors.reducing(
                                BigDecimal.ZERO,
                                expense -> safeAmount(expense.getAmount()),
                                BigDecimal::add
                        )
                ));

        return categoryMap.entrySet()
                .stream()
                .map(entry -> CategoryExpenseResponse.builder()
                        .category(entry.getKey())
                        .amount(entry.getValue())
                        .build())
                .toList();
    }

    public List<ExpenseResponse> getRecentTransactions() {

        User currentUser = getCurrentUser();

        return expenseRepository.findTop5ByUserOrderByExpenseDateDescCreatedAtDesc(currentUser)
                .stream()
                .map(this::mapToExpenseResponse)
                .toList();
    }

    public List<MonthlyTrendResponse> getMonthlyTrend() {

        User currentUser = getCurrentUser();

        List<Expense> expenses = expenseRepository.findByUser(currentUser);

        Map<Month, BigDecimal> monthlyMap = expenses.stream()
                .filter(expense -> expense.getExpenseDate() != null)
                .collect(Collectors.groupingBy(
                        expense -> expense.getExpenseDate().getMonth(),
                        Collectors.reducing(
                                BigDecimal.ZERO,
                                expense -> safeAmount(expense.getAmount()),
                                BigDecimal::add
                        )
                ));

        List<MonthlyTrendResponse> response = new ArrayList<>();

        for (Month month : Month.values()) {
            BigDecimal amount = monthlyMap.getOrDefault(month, BigDecimal.ZERO);

            response.add(MonthlyTrendResponse.builder()
                    .month(month.getDisplayName(TextStyle.SHORT, Locale.ENGLISH))
                    .amount(amount)
                    .build());
        }

        return response;
    }

    public List<BudgetStatusResponse> getCurrentMonthBudgetStatus() {

        User currentUser = getCurrentUser();

        LocalDate today = LocalDate.now();

        Integer month = today.getMonthValue();
        Integer year = today.getYear();

        List<Budget> budgets = budgetRepository.findByUserAndMonthAndYear(
                currentUser,
                month,
                year
        );

        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());

        List<Expense> expenses = expenseRepository.findByUserAndExpenseDateBetween(
                currentUser,
                startDate,
                endDate
        );

        return budgets.stream()
                .map(budget -> {

                    BigDecimal budgetAmount = safeAmount(budget.getAmount());

                    BigDecimal spentAmount = expenses.stream()
                            .filter(expense ->
                                    expense.getCategory() != null &&
                                            expense.getCategory().equals(budget.getCategory())
                            )
                            .map(expense -> safeAmount(expense.getAmount()))
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    BigDecimal remainingAmount = budgetAmount.subtract(spentAmount);

                    BigDecimal usedPercentage = calculateUsedPercentage(spentAmount, budgetAmount);

                    boolean overspent = spentAmount.compareTo(budgetAmount) > 0;

                    return BudgetStatusResponse.builder()
                            .category(budget.getCategory())
                            .budgetAmount(budgetAmount)
                            .spentAmount(spentAmount)
                            .remainingAmount(remainingAmount)
                            .usedPercentage(usedPercentage)
                            .overspent(overspent)
                            .build();
                })
                .toList();
    }

    private BigDecimal calculateTotalExpense(List<Expense> expenses) {
        return expenses.stream()
                .map(expense -> safeAmount(expense.getAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal calculateTotalIncome(List<Income> incomes) {
        return incomes.stream()
                .map(income -> safeAmount(income.getAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal calculateSavingsPercentage(BigDecimal totalIncome, BigDecimal balance) {

        totalIncome = safeAmount(totalIncome);
        balance = safeAmount(balance);

        if (totalIncome.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        return balance
                .divide(totalIncome, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateUsedPercentage(BigDecimal spentAmount, BigDecimal budgetAmount) {

        spentAmount = safeAmount(spentAmount);
        budgetAmount = safeAmount(budgetAmount);

        if (budgetAmount.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        return spentAmount
                .divide(budgetAmount, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);
    }

    private ExpenseCategory getHighestCategory(List<Expense> expenses) {

        if (expenses == null || expenses.isEmpty()) {
            return null;
        }

        Map<ExpenseCategory, BigDecimal> categoryMap = expenses.stream()
                .filter(expense -> expense.getCategory() != null)
                .collect(Collectors.groupingBy(
                        Expense::getCategory,
                        Collectors.reducing(
                                BigDecimal.ZERO,
                                expense -> safeAmount(expense.getAmount()),
                                BigDecimal::add
                        )
                ));

        return categoryMap.entrySet()
                .stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
    }

    private User getCurrentUser() {

        Object principal = SecurityContextHolder.getContext()
                .getAuthentication()
                .getPrincipal();

        String email;

        if (principal instanceof UserDetails userDetails) {
            email = userDetails.getUsername();
        } else {
            email = principal.toString();
        }

        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Logged-in user not found: " + email));
    }

    private BigDecimal safeAmount(BigDecimal amount) {
        return amount != null ? amount : BigDecimal.ZERO;
    }

    private ExpenseResponse mapToExpenseResponse(Expense expense) {
        return ExpenseResponse.builder()
                .id(expense.getId())
                .amount(safeAmount(expense.getAmount()))
                .category(expense.getCategory())
                .merchantName(expense.getMerchantName())
                .paymentMode(expense.getPaymentMode())
                .description(expense.getDescription())
                .expenseDate(expense.getExpenseDate())
                .createdAt(expense.getCreatedAt())
                .updatedAt(expense.getUpdatedAt())
                .build();
    }
}