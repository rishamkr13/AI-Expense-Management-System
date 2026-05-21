package com.risham.expensemanager.dto.response;

import com.risham.expensemanager.enums.ExpenseCategory;
import com.risham.expensemanager.enums.PaymentMode;
import com.risham.expensemanager.enums.ReceiptStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReceiptConfirmResponse {

    private Long receiptId;
    private Long expenseId;

    private BigDecimal amount;
    private ExpenseCategory category;
    private String merchantName;
    private PaymentMode paymentMode;
    private String description;
    private LocalDate expenseDate;

    private ReceiptStatus receiptStatus;

    private String message;
}