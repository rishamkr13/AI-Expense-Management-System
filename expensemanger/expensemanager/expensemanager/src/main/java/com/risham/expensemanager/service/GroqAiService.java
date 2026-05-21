package com.risham.expensemanager.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.risham.expensemanager.dto.response.GeminiReceiptResult;
import com.risham.expensemanager.entity.Budget;
import com.risham.expensemanager.entity.Expense;
import com.risham.expensemanager.entity.Income;
import com.risham.expensemanager.enums.ExpenseCategory;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class GroqAiService {

    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${groq.api.key}")
    private String groqApiKey;

    @Value("${groq.api.url:https://api.groq.com/openai/v1/chat/completions}")
    private String groqApiUrl;

    @Value("${groq.api.model:llama-3.1-8b-instant}")
    private String groqModel;

    public String askChatbot(
            String question,
            List<Income> incomes,
            List<Expense> expenses,
            List<Budget> budgets
    ) {
        try {
            String financialContext = buildFinancialContext(incomes, expenses, budgets);

            Map<String, Object> body = Map.of(
                    "model", groqModel,
                    "messages", List.of(
                            Map.of(
                                    "role", "system",
                                    "content", """
                                            You are a helpful AI financial assistant inside an expense management app.
                                            Use the user's actual income, expense, and budget data provided.
                                            Give short, practical, beginner-friendly advice.
                                            Use Indian Rupees.
                                            Do not invent data.
                                            If data is missing, clearly say what is missing.
                                            """
                            ),
                            Map.of(
                                    "role", "user",
                                    "content", financialContext + "\n\nUser question: " + question
                            )
                    ),
                    "temperature", 0.4
            );

            String response = webClientBuilder.build()
                    .post()
                    .uri(groqApiUrl)
                    .header("Authorization", "Bearer " + groqApiKey)
                    .header("Content-Type", "application/json")
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            JsonNode root = objectMapper.readTree(response);

            return root.path("choices")
                    .get(0)
                    .path("message")
                    .path("content")
                    .asText();

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Groq chatbot failed: " + e.getMessage());
        }
    }

    public GeminiReceiptResult extractReceiptDataFromText(String rawOcrText) {
        try {
            String prompt = """
                    You are an expert receipt and UPI transaction parser.

                    Extract expense details from the OCR text.

                    Return ONLY valid JSON.
                    Do not write explanation.
                    Do not use markdown.

                    JSON format:
                    {
                      "amount": 500,
                      "merchantName": "slice small finance bank",
                      "expenseDate": "2026-05-16",
                      "category": "OTHER"
                    }

                    Rules:
                    - amount must be actual paid amount only.
                    - Do not pick year, transaction ID, phone number, account number, or UPI ID as amount.
                    - If text contains ₹500, amount is 500.
                    - Merchant should be receiver/payee name.
                    - Date must be yyyy-MM-dd.
                    - Category must be one of:
                      FOOD, TRAVEL, SHOPPING, BILLS, HEALTH, EDUCATION, ENTERTAINMENT, OTHER

                    OCR text:
                    """ + rawOcrText;

            Map<String, Object> body = Map.of(
                    "model", groqModel,
                    "messages", List.of(
                            Map.of(
                                    "role", "system",
                                    "content", "You convert OCR receipt text into strict JSON only."
                            ),
                            Map.of(
                                    "role", "user",
                                    "content", prompt
                            )
                    ),
                    "temperature", 0.1
            );

            String response = webClientBuilder.build()
                    .post()
                    .uri(groqApiUrl)
                    .header("Authorization", "Bearer " + groqApiKey)
                    .header("Content-Type", "application/json")
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            JsonNode root = objectMapper.readTree(response);

            String content = root.path("choices")
                    .get(0)
                    .path("message")
                    .path("content")
                    .asText();

            content = cleanJson(content);

            JsonNode json = objectMapper.readTree(content);

            BigDecimal amount = new BigDecimal(json.path("amount").asText("0"));

            String merchantName = json.path("merchantName").asText("Unknown Merchant");

            LocalDate expenseDate;
            try {
                expenseDate = LocalDate.parse(json.path("expenseDate").asText());
            } catch (Exception e) {
                expenseDate = LocalDate.now();
            }

            ExpenseCategory category;
            try {
                category = ExpenseCategory.valueOf(json.path("category").asText("OTHER"));
            } catch (Exception e) {
                category = ExpenseCategory.OTHER;
            }

            return GeminiReceiptResult.builder()
                    .amount(amount)
                    .merchantName(merchantName)
                    .expenseDate(expenseDate)
                    .category(category)
                    .rawResponse(rawOcrText + "\n\nAI JSON:\n" + content)
                    .build();

        } catch (Exception e) {
            e.printStackTrace();

            return GeminiReceiptResult.builder()
                    .amount(BigDecimal.ZERO)
                    .merchantName("Unknown Merchant")
                    .expenseDate(LocalDate.now())
                    .category(ExpenseCategory.OTHER)
                    .rawResponse(rawOcrText)
                    .build();
        }
    }

    private String buildFinancialContext(
            List<Income> incomes,
            List<Expense> expenses,
            List<Budget> budgets
    ) {

        StringBuilder context = new StringBuilder();

        context.append("User financial data for current month:\n");

        context.append("\nIncomes:\n");
        if (incomes == null || incomes.isEmpty()) {
            context.append("No income records.\n");
        } else {
            for (Income income : incomes) {
                context.append("- Amount: ₹")
                        .append(income.getAmount())
                        .append(", Source: ")
                        .append(income.getSource())
                        .append(", Date: ")
                        .append(income.getIncomeDate())
                        .append("\n");
            }
        }

        context.append("\nExpenses:\n");
        if (expenses == null || expenses.isEmpty()) {
            context.append("No expense records.\n");
        } else {
            for (Expense expense : expenses) {
                context.append("- Amount: ₹")
                        .append(expense.getAmount())
                        .append(", Category: ")
                        .append(expense.getCategory())
                        .append(", Merchant: ")
                        .append(expense.getMerchantName())
                        .append(", Date: ")
                        .append(expense.getExpenseDate())
                        .append("\n");
            }
        }

        context.append("\nBudgets:\n");
        if (budgets == null || budgets.isEmpty()) {
            context.append("No budget records.\n");
        } else {
            for (Budget budget : budgets) {
                context.append("- Category: ")
                        .append(budget.getCategory())
                        .append(", Budget Amount: ₹")
                        .append(budget.getAmount())
                        .append("\n");
            }
        }

        return context.toString();
    }

    private String cleanJson(String content) {
        if (content == null) {
            return "{}";
        }

        return content
                .replace("```json", "")
                .replace("```", "")
                .trim();
    }
}