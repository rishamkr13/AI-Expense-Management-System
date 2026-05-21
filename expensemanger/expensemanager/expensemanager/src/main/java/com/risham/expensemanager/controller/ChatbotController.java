package com.risham.expensemanager.controller;

import com.risham.expensemanager.dto.request.ChatbotRequest;
import com.risham.expensemanager.dto.response.ChatbotResponse;
import com.risham.expensemanager.service.ChatbotService;
import com.risham.expensemanager.service.GeminiChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chatbot")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173")
public class ChatbotController {

    private final ChatbotService chatbotService;
    private final GeminiChatService geminiChatService;

    @PostMapping("/ask")
    public ResponseEntity<ChatbotResponse> ask(
            @Valid @RequestBody ChatbotRequest request
    ) {
        return ResponseEntity.ok(chatbotService.ask(request));
    }

    @PostMapping("/ask-ai")
    public ResponseEntity<ChatbotResponse> askAi(
            @Valid @RequestBody ChatbotRequest request
    ) {
        return ResponseEntity.ok(
                chatbotService.askWithGeminiFallback(request, geminiChatService)
        );
    }
}