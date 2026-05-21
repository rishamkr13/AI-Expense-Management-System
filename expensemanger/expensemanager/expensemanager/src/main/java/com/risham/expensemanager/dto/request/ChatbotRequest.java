package com.risham.expensemanager.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChatbotRequest {

    @NotBlank(message = "Question is required")
    private String question;
}