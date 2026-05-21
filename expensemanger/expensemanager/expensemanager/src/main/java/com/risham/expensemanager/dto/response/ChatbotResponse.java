package com.risham.expensemanager.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatbotResponse {

    private String question;
    private String answer;
    private String source;
}