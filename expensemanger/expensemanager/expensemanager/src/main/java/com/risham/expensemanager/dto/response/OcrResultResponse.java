package com.risham.expensemanager.dto.response;

import com.risham.expensemanager.enums.ExpenseCategory;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OcrResultResponse {

    private Long receiptId;
    private Long expenseId;

    private String extractedText;

    private BigDecimal extractedAmount;
    private String extractedMerchantName;
    private LocalDate extractedDate;
    private ExpenseCategory category;

    private String message;
}