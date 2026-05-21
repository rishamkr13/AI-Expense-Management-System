package com.risham.expensemanager.dto.response;

import com.risham.expensemanager.enums.IncomeSource;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IncomeResponse {

    private Long id;
    private BigDecimal amount;
    private IncomeSource source;
    private String description;
    private LocalDate incomeDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}