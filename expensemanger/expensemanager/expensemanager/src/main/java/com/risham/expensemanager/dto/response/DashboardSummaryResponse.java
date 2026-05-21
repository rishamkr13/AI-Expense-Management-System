package com.risham.expensemanager.dto.response;

import com.risham.expensemanager.enums.ExpenseCategory;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardSummaryResponse {

    private BigDecimal totalIncome;

    private BigDecimal totalExpense;

    private BigDecimal balance;

    private BigDecimal thisMonthIncome;

    private BigDecimal thisMonthExpense;

    private BigDecimal thisMonthBalance;

    private BigDecimal savingsPercentage;

    private Long totalIncomeTransactions;

    private Long totalExpenseTransactions;

    private ExpenseCategory highestExpenseCategory;
}