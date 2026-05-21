package com.risham.expensemanager.entity;

import com.risham.expensemanager.enums.ExpenseCategory;
import com.risham.expensemanager.enums.PaymentMode;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "expenses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Expense {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ExpenseCategory category;

    private String merchantName;

    @Enumerated(EnumType.STRING)
    private PaymentMode paymentMode;

    private String description;

    @Column(nullable = false)
    private LocalDate expenseDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    public void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();

        if (expenseDate == null) {
            expenseDate = LocalDate.now();
        }

        if (paymentMode == null) {
            paymentMode = PaymentMode.OTHER;
        }

        if (category == null) {
            category = ExpenseCategory.OTHER;
        }

        if (amount == null) {
            amount = BigDecimal.ZERO;
        }
    }

    @PreUpdate
    public void onUpdate() {
        updatedAt = LocalDateTime.now();

        if (paymentMode == null) {
            paymentMode = PaymentMode.OTHER;
        }

        if (category == null) {
            category = ExpenseCategory.OTHER;
        }

        if (expenseDate == null) {
            expenseDate = LocalDate.now();
        }

        if (amount == null) {
            amount = BigDecimal.ZERO;
        }
    }
}