package com.risham.expensemanager.dto.response;

import com.risham.expensemanager.enums.ExpenseCategory;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategoryExpenseResponse {

    private ExpenseCategory category;
    private BigDecimal amount;
}