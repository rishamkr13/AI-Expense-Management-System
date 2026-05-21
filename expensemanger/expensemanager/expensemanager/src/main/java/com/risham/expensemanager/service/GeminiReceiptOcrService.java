package com.risham.expensemanager.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.risham.expensemanager.dto.response.GeminiReceiptResult;
import com.risham.expensemanager.enums.ExpenseCategory;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

//@Service
@RequiredArgsConstructor
public class GeminiReceiptOcrService {

    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.model}")
    private String model;

    @Value("${gemini.api.base-url}")
    private String baseUrl;

    public GeminiReceiptResult extractReceiptData(String filePath, String mimeType) {

        try {
            if (mimeType == null || mimeType.isBlank()) {
                mimeType = "image/jpeg";
            }

            byte[] imageBytes = Files.readAllBytes(Path.of(filePath));
            String base64Image = Base64.getEncoder().encodeToString(imageBytes);

            String prompt = """
                    You are an expert receipt and UPI payment screenshot parser.

                    Extract these fields from the image:
                    - amount
                    - merchantName
                    - date
                    - category

                    Very important rules:
                    1. Amount must be the actual paid amount only.
                    2. Do not use year, transaction id, bank account number, time, phone number as amount.
                    3. For Google Pay / PhonePe / Paytm / UPI screenshots, the large rupee value near the top is the paid amount.
                    4. If image shows ₹500, amount must be 500, not 2026.
                    5. Date must be in yyyy-MM-dd format.
                    6. Category must be exactly one of:
                       FOOD, TRAVEL, SHOPPING, BILLS, HEALTH, EDUCATION, ENTERTAINMENT, OTHER
                    7. If it is UPI/bank transfer and purpose is unknown, category should be OTHER.
                    8. Return only valid JSON. No markdown. No explanation.

                    Return JSON exactly like this:
                    {
                      "amount": 500,
                      "merchantName": "slice small finance bank",
                      "date": "2026-05-16",
                      "category": "OTHER"
                    }
                    """;

            Map<String, Object> textPart = new HashMap<>();
            textPart.put("text", prompt);

            Map<String, Object> inlineData = new HashMap<>();
            inlineData.put("mime_type", mimeType);
            inlineData.put("data", base64Image);

            Map<String, Object> imagePart = new HashMap<>();
            imagePart.put("inline_data", inlineData);

            Map<String, Object> content = new HashMap<>();
            content.put("parts", List.of(textPart, imagePart));

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("contents", List.of(content));

            String finalUrl = baseUrl + "/" + model + ":generateContent?key=" + apiKey;

            System.out.println("Calling Gemini URL: " + baseUrl + "/" + model + ":generateContent");

            String response = webClientBuilder.build()
                    .post()
                    .uri(finalUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            System.out.println("========== GEMINI RESPONSE START ==========");
            System.out.println(response);
            System.out.println("========== GEMINI RESPONSE END ==========");

            return parseGeminiResponse(response);

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Gemini receipt OCR failed: " + e.getMessage());
        }
    }

    private GeminiReceiptResult parseGeminiResponse(String response) {

        try {
            JsonNode root = objectMapper.readTree(response);

            JsonNode candidates = root.path("candidates");

            if (!candidates.isArray() || candidates.isEmpty()) {
                throw new RuntimeException("No candidates returned from Gemini");
            }

            String text = candidates
                    .get(0)
                    .path("content")
                    .path("parts")
                    .get(0)
                    .path("text")
                    .asText();

            text = cleanJsonText(text);

            System.out.println("========== CLEANED GEMINI JSON ==========");
            System.out.println(text);
            System.out.println("=========================================");

            JsonNode json = objectMapper.readTree(text);

            BigDecimal amount = new BigDecimal(json.path("amount").asText("0"));

            String merchantName = json.path("merchantName").asText("Unknown Merchant");

            String dateText = json.path("date").asText(LocalDate.now().toString());

            String categoryText = json.path("category").asText("OTHER");

            ExpenseCategory category;

            try {
                category = ExpenseCategory.valueOf(categoryText.toUpperCase());
            } catch (Exception e) {
                category = ExpenseCategory.OTHER;
            }

            return GeminiReceiptResult.builder()
                    .amount(amount)
                    .merchantName(merchantName)
                    .expenseDate(LocalDate.parse(dateText))
                    .category(category)
                    .rawResponse(text)
                    .build();

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to parse Gemini OCR response: " + e.getMessage());
        }
    }

    private String cleanJsonText(String text) {

        if (text == null || text.isBlank()) {
            return "{}";
        }

        text = text.trim();

        text = text
                .replace("```json", "")
                .replace("```JSON", "")
                .replace("```", "")
                .trim();

        int start = text.indexOf("{");
        int end = text.lastIndexOf("}");

        if (start != -1 && end != -1 && end > start) {
            text = text.substring(start, end + 1);
        }

        return text;
    }
}