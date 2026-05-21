package com.risham.expensemanager.dto.response;

import com.risham.expensemanager.enums.ExpenseCategory;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BudgetStatusResponse {

    private ExpenseCategory category;
    private BigDecimal budgetAmount;
    private BigDecimal spentAmount;
    private BigDecimal remainingAmount;
    private BigDecimal usedPercentage;
    private Boolean overspent;
}