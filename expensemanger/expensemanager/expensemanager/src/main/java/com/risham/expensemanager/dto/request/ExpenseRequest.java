package com.risham.expensemanager.dto.request;

import com.risham.expensemanager.enums.ExpenseCategory;
import com.risham.expensemanager.enums.PaymentMode;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ExpenseRequest {

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    private BigDecimal amount;

    @NotNull(message = "Category is required")
    private ExpenseCategory category;

    private String merchantName;

    private PaymentMode paymentMode;

    private String description;

    private LocalDate expenseDate;
}