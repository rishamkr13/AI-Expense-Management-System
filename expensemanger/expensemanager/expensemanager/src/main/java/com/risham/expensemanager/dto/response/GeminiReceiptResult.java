package com.risham.expensemanager.dto.response;

import com.risham.expensemanager.enums.ExpenseCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeminiReceiptResult {

    private BigDecimal amount;
    private String merchantName;
    private LocalDate expenseDate;
    private ExpenseCategory category;
    private String rawResponse;
}