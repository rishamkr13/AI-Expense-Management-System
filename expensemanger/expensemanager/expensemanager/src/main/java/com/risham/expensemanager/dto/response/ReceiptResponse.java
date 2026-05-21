package com.risham.expensemanager.dto.response;

import com.risham.expensemanager.enums.ExpenseCategory;
import com.risham.expensemanager.enums.PaymentMode;
import com.risham.expensemanager.enums.ReceiptStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReceiptResponse {

    private Long id;

    private String originalFileName;
    private String storedFileName;

    private String fileType;
    private Long fileSize;

    private ReceiptStatus status;

    private String extractedText;

    private Long expenseId;

    private BigDecimal amount;
    private String merchantName;
    private ExpenseCategory category;
    private PaymentMode paymentMode;
    private LocalDate expenseDate;

    private LocalDateTime uploadedAt;
    private LocalDateTime processedAt;
}