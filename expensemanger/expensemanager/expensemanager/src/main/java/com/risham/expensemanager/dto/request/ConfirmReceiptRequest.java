package com.risham.expensemanager.dto.request;

import com.risham.expensemanager.enums.ExpenseCategory;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class ConfirmReceiptRequest {

    private BigDecimal amount;

    private String merchantName;

    private LocalDate expenseDate;

    private ExpenseCategory category;
}