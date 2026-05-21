package com.risham.expensemanager.service;

import com.risham.expensemanager.dto.response.AnalyticsInsightResponse;
import com.risham.expensemanager.entity.Budget;
import com.risham.expensemanager.entity.Expense;
import com.risham.expensemanager.entity.Income;
import com.risham.expensemanager.entity.User;
import com.risham.expensemanager.enums.ExpenseCategory;
import com.risham.expensemanager.enums.InsightPriority;
import com.risham.expensemanager.enums.InsightType;
import com.risham.expensemanager.repository.BudgetRepository;
import com.risham.expensemanager.repository.ExpenseRepository;
import com.risham.expensemanager.repository.IncomeRepository;
import com.risham.expensemanager.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final ExpenseRepository expenseRepository;
    private final IncomeRepository incomeRepository;
    private final BudgetRepository budgetRepository;
    private final UserRepository userRepository;

    public List<AnalyticsInsightResponse> getInsights() {

        User currentUser = getCurrentUser();

        LocalDate today = LocalDate.now();

        LocalDate currentMonthStart = today.withDayOfMonth(1);
        LocalDate currentMonthEnd = today.withDayOfMonth(today.lengthOfMonth());

        LocalDate previousMonthDate = today.minusMonths(1);
        LocalDate previousMonthStart = previousMonthDate.withDayOfMonth(1);
        LocalDate previousMonthEnd = previousMonthDate.withDayOfMonth(previousMonthDate.lengthOfMonth());

        List<Expense> currentMonthExpenses = expenseRepository.findByUserAndExpenseDateBetween(
                currentUser,
                currentMonthStart,
                currentMonthEnd
        );

        List<Expense> previousMonthExpenses = expenseRepository.findByUserAndExpenseDateBetween(
                currentUser,
                previousMonthStart,
                previousMonthEnd
        );

        List<Income> currentMonthIncomes = incomeRepository.findByUserAndIncomeDateBetween(
                currentUser,
                currentMonthStart,
                currentMonthEnd
        );

        List<Budget> currentMonthBudgets = budgetRepository.findByUserAndMonthAndYear(
                currentUser,
                today.getMonthValue(),
                today.getYear()
        );

        List<AnalyticsInsightResponse> insights = new ArrayList<>();

        addHighestSpendingCategoryInsight(insights, currentMonthExpenses);
        addMonthlySpendingComparisonInsight(insights, currentMonthExpenses, previousMonthExpenses);
        addSavingsInsight(insights, currentMonthIncomes, currentMonthExpenses);
        addBudgetOverspendingInsights(insights, currentMonthBudgets, currentMonthExpenses);
        addLowBudgetRemainingInsights(insights, currentMonthBudgets, currentMonthExpenses);

        if (insights.isEmpty()) {
            insights.add(
                    AnalyticsInsightResponse.builder()
                            .type(InsightType.INFO)
                            .title("No Insights Yet")
                            .message("Add income, expenses, and budgets to generate useful financial insights.")
                            .priority(InsightPriority.LOW)
                            .build()
            );
        }

        return insights;
    }

    private void addHighestSpendingCategoryInsight(
            List<AnalyticsInsightResponse> insights,
            List<Expense> expenses
    ) {

        if (expenses.isEmpty()) {
            return;
        }

        Map<ExpenseCategory, BigDecimal> categoryMap = expenses.stream()
                .collect(Collectors.groupingBy(
                        Expense::getCategory,
                        Collectors.reducing(
                                BigDecimal.ZERO,
                                Expense::getAmount,
                                BigDecimal::add
                        )
                ));

        Map.Entry<ExpenseCategory, BigDecimal> highestCategory = categoryMap.entrySet()
                .stream()
                .max(Map.Entry.comparingByValue())
                .orElse(null);

        if (highestCategory == null) {
            return;
        }

        insights.add(
                AnalyticsInsightResponse.builder()
                        .type(InsightType.INFO)
                        .title("Highest Spending Category")
                        .message("Your highest spending category this month is "
                                + highestCategory.getKey()
                                + " with ₹"
                                + highestCategory.getValue()
                                + " spent.")
                        .priority(InsightPriority.MEDIUM)
                        .build()
        );
    }

    private void addMonthlySpendingComparisonInsight(
            List<AnalyticsInsightResponse> insights,
            List<Expense> currentMonthExpenses,
            List<Expense> previousMonthExpenses
    ) {

        BigDecimal currentTotal = calculateTotalExpense(currentMonthExpenses);
        BigDecimal previousTotal = calculateTotalExpense(previousMonthExpenses);

        if (previousTotal.compareTo(BigDecimal.ZERO) == 0) {
            return;
        }

        BigDecimal difference = currentTotal.subtract(previousTotal);

        BigDecimal percentageChange = difference
                .divide(previousTotal, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);

        if (percentageChange.compareTo(BigDecimal.ZERO) > 0) {
            insights.add(
                    AnalyticsInsightResponse.builder()
                            .type(InsightType.WARNING)
                            .title("Monthly Spending Increased")
                            .message("You spent "
                                    + percentageChange
                                    + "% more than last month. Review unnecessary expenses.")
                            .priority(InsightPriority.HIGH)
                            .build()
            );
        } else if (percentageChange.compareTo(BigDecimal.ZERO) < 0) {
            insights.add(
                    AnalyticsInsightResponse.builder()
                            .type(InsightType.SUCCESS)
                            .title("Monthly Spending Reduced")
                            .message("Good job! You spent "
                                    + percentageChange.abs()
                                    + "% less than last month.")
                            .priority(InsightPriority.LOW)
                            .build()
            );
        }
    }

    private void addSavingsInsight(
            List<AnalyticsInsightResponse> insights,
            List<Income> incomes,
            List<Expense> expenses
    ) {

        BigDecimal totalIncome = calculateTotalIncome(incomes);
        BigDecimal totalExpense = calculateTotalExpense(expenses);

        if (totalIncome.compareTo(BigDecimal.ZERO) == 0) {
            insights.add(
                    AnalyticsInsightResponse.builder()
                            .type(InsightType.WARNING)
                            .title("No Income Added")
                            .message("You have not added income for this month, so savings percentage cannot be calculated.")
                            .priority(InsightPriority.MEDIUM)
                            .build()
            );
            return;
        }

        BigDecimal savings = totalIncome.subtract(totalExpense);

        BigDecimal savingsPercentage = savings
                .divide(totalIncome, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);

        if (savingsPercentage.compareTo(BigDecimal.valueOf(50)) >= 0) {
            insights.add(
                    AnalyticsInsightResponse.builder()
                            .type(InsightType.SUCCESS)
                            .title("Strong Savings")
                            .message("Your savings percentage is "
                                    + savingsPercentage
                                    + "%. Good financial control.")
                            .priority(InsightPriority.LOW)
                            .build()
            );
        } else if (savingsPercentage.compareTo(BigDecimal.valueOf(20)) >= 0) {
            insights.add(
                    AnalyticsInsightResponse.builder()
                            .type(InsightType.INFO)
                            .title("Moderate Savings")
                            .message("Your savings percentage is "
                                    + savingsPercentage
                                    + "%. You can improve by reducing optional spending.")
                            .priority(InsightPriority.MEDIUM)
                            .build()
            );
        } else {
            insights.add(
                    AnalyticsInsightResponse.builder()
                            .type(InsightType.WARNING)
                            .title("Low Savings")
                            .message("Your savings percentage is only "
                                    + savingsPercentage
                                    + "%. Try controlling high-spending categories.")
                            .priority(InsightPriority.HIGH)
                            .build()
            );
        }
    }

    private void addBudgetOverspendingInsights(
            List<AnalyticsInsightResponse> insights,
            List<Budget> budgets,
            List<Expense> expenses
    ) {

        for (Budget budget : budgets) {

            BigDecimal spentAmount = calculateCategorySpent(expenses, budget.getCategory());

            if (spentAmount.compareTo(budget.getAmount()) > 0) {
                BigDecimal overspentAmount = spentAmount.subtract(budget.getAmount());

                insights.add(
                        AnalyticsInsightResponse.builder()
                                .type(InsightType.DANGER)
                                .title("Overspending Alert")
                                .message("You have overspent in "
                                        + budget.getCategory()
                                        + " category by ₹"
                                        + overspentAmount
                                        + ".")
                                .priority(InsightPriority.HIGH)
                                .build()
                );
            }
        }
    }

    private void addLowBudgetRemainingInsights(
            List<AnalyticsInsightResponse> insights,
            List<Budget> budgets,
            List<Expense> expenses
    ) {

        for (Budget budget : budgets) {

            if (budget.getAmount().compareTo(BigDecimal.ZERO) == 0) {
                continue;
            }

            BigDecimal spentAmount = calculateCategorySpent(expenses, budget.getCategory());

            BigDecimal usedPercentage = spentAmount
                    .divide(budget.getAmount(), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(2, RoundingMode.HALF_UP);

            if (
                    usedPercentage.compareTo(BigDecimal.valueOf(80)) >= 0 &&
                            usedPercentage.compareTo(BigDecimal.valueOf(100)) < 0
            ) {
                insights.add(
                        AnalyticsInsightResponse.builder()
                                .type(InsightType.WARNING)
                                .title("Budget Almost Used")
                                .message("You have used "
                                        + usedPercentage
                                        + "% of your "
                                        + budget.getCategory()
                                        + " budget.")
                                .priority(InsightPriority.MEDIUM)
                                .build()
                );
            }
        }
    }

    private BigDecimal calculateCategorySpent(List<Expense> expenses, ExpenseCategory category) {
        return expenses.stream()
                .filter(expense -> expense.getCategory().equals(category))
                .map(Expense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal calculateTotalExpense(List<Expense> expenses) {
        return expenses.stream()
                .map(Expense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal calculateTotalIncome(List<Income> incomes) {
        return incomes.stream()
                .map(Income::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private User getCurrentUser() {

        String email = SecurityContextHolder.getContext()
                .getAuthentication()
                .getName();

        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Logged-in user not found"));
    }
}