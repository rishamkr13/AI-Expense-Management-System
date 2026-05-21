package com.risham.expensemanager.dto.response;

import com.risham.expensemanager.enums.ExpenseCategory;
import com.risham.expensemanager.enums.PaymentMode;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExpenseResponse {

    private Long id;
    private BigDecimal amount;
    private ExpenseCategory category;
    private String merchantName;
    private PaymentMode paymentMode;
    private String description;
    private LocalDate expenseDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}