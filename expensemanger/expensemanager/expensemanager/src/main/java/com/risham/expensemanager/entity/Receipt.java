package com.risham.expensemanager.entity;

import com.risham.expensemanager.enums.ReceiptStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "receipts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Receipt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String originalFileName;

    private String storedFileName;

    private String filePath;

    private String fileType;

    private Long fileSize;

    @Enumerated(EnumType.STRING)
    private ReceiptStatus status;

    @Column(columnDefinition = "TEXT")
    private String extractedText;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "expense_id")
    private Expense expense;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private LocalDateTime uploadedAt;

    private LocalDateTime processedAt;

    @PrePersist
    public void onCreate() {
        uploadedAt = LocalDateTime.now();

        if (status == null) {
            status = ReceiptStatus.UPLOADED;
        }
    }
}