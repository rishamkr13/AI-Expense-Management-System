package com.risham.expensemanager.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.risham.expensemanager.dto.response.GeminiReceiptResult;
import com.risham.expensemanager.enums.ExpenseCategory;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class GroqReceiptService {

    @Value("${groq.api.key}")
    private String groqApiKey;

    @Value("${groq.api.base-url}")
    private String groqBaseUrl;

    // NOTE: This MUST be a vision-capable model.
    // Set groq.api.model=meta-llama/llama-4-scout-17b-16e-instruct in your
    // application.properties (or llama-3.2-11b-vision-preview as fallback).
    // Text-only models like llama3-8b-8192 will FAIL on image inputs.
    @Value("${groq.api.model}")
    private String groqModel;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // ─────────────────────────────────────────────────────────────────
    //  PRIMARY: Send image directly to Groq Vision (no Tesseract needed)
    // ─────────────────────────────────────────────────────────────────

    /**
     * Reads the receipt image file, encodes it as base64, and sends it
     * directly to the Groq vision model. This is more reliable than
     * piping Tesseract OCR text because vision models can read the
     * original pixel layout rather than garbled OCR output.
     *
     * @param filePath  absolute path to the saved receipt image
     * @param mimeType  e.g. "image/jpeg" or "image/png"
     */
    public GeminiReceiptResult extractReceiptDataFromImage(String filePath, String mimeType) {

        try {
            System.out.println("========== GROQ VISION: READING IMAGE ==========");
            System.out.println("File: " + filePath);
            System.out.println("Model: " + groqModel);

            if (mimeType == null || mimeType.isBlank()) {
                mimeType = "image/jpeg";
            }

            // Read and base64-encode the image
            byte[] imageBytes = Files.readAllBytes(Path.of(filePath));
            String base64Image = Base64.getEncoder().encodeToString(imageBytes);
            String dataUrl = "data:" + mimeType + ";base64," + base64Image;

            String systemPrompt = """
                    You are an expert receipt and UPI payment screenshot parser for Indian expenses.
                    Your only job is to extract structured data from the image.
                    Always return valid JSON with no markdown, no explanation, no extra text.
                    """;

            String userPrompt = """
                    Look at this receipt or payment screenshot carefully.

                    Extract EXACTLY these four fields and return ONLY a JSON object:

                    {
                      "amount": <number — the actual rupee amount paid, as a plain number like 500>,
                      "merchantName": "<name of the shop, restaurant, or payee>",
                      "expenseDate": "<date in yyyy-MM-dd format>",
                      "category": "<one of: FOOD, TRAVEL, SHOPPING, BILLS, HEALTH, EDUCATION, ENTERTAINMENT, OTHER>"
                    }

                    Critical rules for AMOUNT:
                    - Find the number that represents money paid/debited/total.
                    - For UPI/GPay/PhonePe screenshots: the big rupee number at the top is the amount.
                    - Look for: ₹, Rs, INR, "Amount", "Total", "Paid", "Debited", "Grand Total".
                    - NEVER use a year (2024, 2025, 2026) as the amount.
                    - NEVER use a time (10, 11, 12, 13..24) as the amount.
                    - NEVER use a transaction ID, UPI ID, phone number, or account number as amount.
                    - If you cannot find a clear amount, return 0.

                    Critical rules for DATE:
                    - Return in yyyy-MM-dd format only (e.g. 2025-05-16).
                    - If no date visible, return today's date.

                    Critical rules for MERCHANT:
                    - Return the actual business name, not "UPI", "PAYMENT", "TRANSACTION".
                    - For UPI transfers, use the payee name (the person/merchant receiving money).
                    - If unclear, return "Unknown Merchant".

                    Return ONLY the JSON object. No other text.
                    """;

            // Build request body for vision (image_url content type)
            ObjectNode requestJson = objectMapper.createObjectNode();
            requestJson.put("model", groqModel);
            requestJson.put("temperature", 0);
            requestJson.put("max_tokens", 512);

            ArrayNode messages = requestJson.putArray("messages");

            // System message
            ObjectNode systemMsg = messages.addObject();
            systemMsg.put("role", "system");
            systemMsg.put("content", systemPrompt);

            // User message with image + text
            ObjectNode userMsg = messages.addObject();
            userMsg.put("role", "user");

            ArrayNode contentArray = userMsg.putArray("content");

            // Text part
            ObjectNode textPart = contentArray.addObject();
            textPart.put("type", "text");
            textPart.put("text", userPrompt);

            // Image part
            ObjectNode imagePart = contentArray.addObject();
            imagePart.put("type", "image_url");
            ObjectNode imageUrlNode = imagePart.putObject("image_url");
            imageUrlNode.put("url", dataUrl);

            String requestBody = objectMapper.writeValueAsString(requestJson);

            System.out.println("========== GROQ VISION REQUEST SENT ==========");

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(groqBaseUrl + "/chat/completions"))
                    .header("Authorization", "Bearer " + groqApiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpClient client = HttpClient.newHttpClient();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            System.out.println("========== GROQ VISION STATUS: " + response.statusCode() + " ==========");
            System.out.println(response.body());

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new RuntimeException("Groq Vision API error: " + response.statusCode() + " " + response.body());
            }

            return parseGroqResponse(response.body());

        } catch (Exception e) {
            System.out.println("========== GROQ VISION FAILED ==========");
            e.printStackTrace();
            throw new RuntimeException("Groq vision receipt parsing failed: " + e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────
    //  FALLBACK: Text-only path (used when image bytes unavailable)
    // ─────────────────────────────────────────────────────────────────

    /**
     * Kept as a fallback in case the image file is not accessible.
     * Sends Tesseract-extracted text to Groq for structured parsing.
     * Less reliable than the vision path above.
     */
    public GeminiReceiptResult extractReceiptDataFromText(String ocrText) {

        try {
            System.out.println("========== GROQ TEXT FALLBACK ==========");

            if (ocrText == null || ocrText.isBlank()) {
                throw new RuntimeException("OCR text is empty — cannot send to Groq.");
            }

            String prompt = """
                    You are an expense receipt parser. Extract data from the following OCR text.

                    RULES:
                    - Return ONLY valid JSON, no markdown, no explanation.
                    - amount: actual paid amount as a number (e.g. 500). Rules:
                        * NEVER use a year (1900-2099) as amount.
                        * NEVER use a time value (0-24) as amount.
                        * NEVER use account numbers, UPI IDs, transaction IDs, phone numbers as amount.
                        * Look for: ₹, Rs, INR, "Amount", "Total", "Paid", "Debited", "Grand Total".
                        * Strip commas and currency symbols — just return the number.
                        * If not found clearly, return 0.
                    - merchantName: name of shop/restaurant/payee. Not "UPI" or "PAYMENT".
                    - expenseDate: yyyy-MM-dd format. If not found, return "".
                    - category: FOOD | TRAVEL | SHOPPING | BILLS | HEALTH | EDUCATION | ENTERTAINMENT | OTHER

                    Allowed categories: FOOD, TRAVEL, SHOPPING, BILLS, HEALTH, EDUCATION, ENTERTAINMENT, OTHER

                    Return exactly:
                    {
                      "amount": 0,
                      "merchantName": "",
                      "expenseDate": "",
                      "category": "OTHER"
                    }

                    OCR TEXT:
                    """ + ocrText;

            ObjectNode requestJson = objectMapper.createObjectNode();
            requestJson.put("model", groqModel);
            requestJson.put("temperature", 0);
            requestJson.put("max_tokens", 512);

            ArrayNode messages = requestJson.putArray("messages");
            ObjectNode userMessage = messages.addObject();
            userMessage.put("role", "user");
            userMessage.put("content", prompt);

            String requestBody = objectMapper.writeValueAsString(requestJson);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(groqBaseUrl + "/chat/completions"))
                    .header("Authorization", "Bearer " + groqApiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpClient client = HttpClient.newHttpClient();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            System.out.println("========== GROQ TEXT STATUS: " + response.statusCode() + " ==========");
            System.out.println(response.body());

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new RuntimeException("Groq API error: " + response.statusCode() + " " + response.body());
            }

            return parseGroqResponse(response.body());

        } catch (Exception e) {
            System.out.println("========== GROQ TEXT FALLBACK FAILED ==========");
            e.printStackTrace();
            throw new RuntimeException("Groq text receipt parsing failed: " + e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────
    //  Shared response parser
    // ─────────────────────────────────────────────────────────────────

    private GeminiReceiptResult parseGroqResponse(String responseBody) throws Exception {

        JsonNode root = objectMapper.readTree(responseBody);
        JsonNode choices = root.path("choices");

        if (!choices.isArray() || choices.isEmpty()) {
            throw new RuntimeException("Groq response has no choices: " + responseBody);
        }

        String content = choices.get(0).path("message").path("content").asText();

        System.out.println("========== GROQ RAW CONTENT ==========");
        System.out.println(content);

        content = cleanJson(content);

        System.out.println("========== GROQ CLEAN JSON ==========");
        System.out.println(content);

        JsonNode json = objectMapper.readTree(content);

        // --- Amount ---
        BigDecimal amount = BigDecimal.ZERO;
        if (json.hasNonNull("amount")) {
            String amountText = json.get("amount").asText()
                    .replaceAll("[₹,\\s]", "")
                    .replaceAll("(?i)(rs\\.?|inr)", "")
                    .trim();
            if (!amountText.isBlank()) {
                try {
                    amount = new BigDecimal(amountText);
                } catch (NumberFormatException e) {
                    System.out.println("Could not parse amount: " + amountText);
                    amount = BigDecimal.ZERO;
                }
            }
        }

        // --- Merchant ---
        String merchantName = "Unknown Merchant";
        if (json.hasNonNull("merchantName")) {
            String raw = json.get("merchantName").asText().trim();
            if (!raw.isBlank() && !raw.equalsIgnoreCase("null")) {
                merchantName = raw;
            }
        }

        // --- Date ---
        LocalDate expenseDate = LocalDate.now();
        if (json.hasNonNull("expenseDate")) {
            String dateText = json.get("expenseDate").asText().trim();
            if (!dateText.isBlank() && !dateText.equalsIgnoreCase("null")) {
                try {
                    expenseDate = LocalDate.parse(dateText);
                } catch (Exception ignored) {
                    System.out.println("Could not parse date: " + dateText + " — using today.");
                    expenseDate = LocalDate.now();
                }
            }
        }

        // --- Category ---
        ExpenseCategory category = ExpenseCategory.OTHER;
        if (json.hasNonNull("category")) {
            try {
                category = ExpenseCategory.valueOf(json.get("category").asText().toUpperCase().trim());
            } catch (Exception ignored) {
                category = ExpenseCategory.OTHER;
            }
        }

        System.out.println("========== PARSED RESULT ==========");
        System.out.println("Amount:   " + amount);
        System.out.println("Merchant: " + merchantName);
        System.out.println("Date:     " + expenseDate);
        System.out.println("Category: " + category);

        return GeminiReceiptResult.builder()
                .amount(amount)
                .merchantName(merchantName)
                .expenseDate(expenseDate)
                .category(category)
                .rawResponse(content)
                .build();
    }

    // ─────────────────────────────────────────────────────────────────
    //  Strip markdown fences and extract the JSON object
    // ─────────────────────────────────────────────────────────────────

    private String cleanJson(String content) {

        if (content == null) return "{}";

        content = content.trim()
                .replaceAll("(?i)```json", "")
                .replaceAll("```", "")
                .trim();

        int start = content.indexOf("{");
        int end   = content.lastIndexOf("}");

        if (start != -1 && end != -1 && end > start) {
            return content.substring(start, end + 1);
        }

        return content.isBlank() ? "{}" : content;
    }
}