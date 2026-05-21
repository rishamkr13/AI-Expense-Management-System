package com.risham.expensemanager.service;

import com.risham.expensemanager.entity.Budget;
import com.risham.expensemanager.entity.Expense;
import com.risham.expensemanager.entity.Income;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class GeminiChatService {

    private final WebClient.Builder webClientBuilder;

    @Value("${gemini.api.key:dummy-key}")
    private String geminiApiKey;

    @Value("${gemini.model:gemini-1.5-flash}")
    private String geminiModel;

    @Value("${gemini.api.url:https://generativelanguage.googleapis.com/v1beta/models}")
    private String geminiApiUrl;

    public String askGemini(
            String question,
            List<Income> incomes,
            List<Expense> expenses,
            List<Budget> budgets
    ) {

        if (geminiApiKey == null || geminiApiKey.isBlank() || geminiApiKey.equals("dummy-key")) {
            throw new RuntimeException("Gemini API key not configured");
        }

        String financialSummary = buildFinancialSummary(incomes, expenses, budgets);

        String prompt = """
                You are a helpful personal finance advisor inside an Expense Management System.
                Give practical, simple, and personalized advice.
                Use the user's actual income, expenses, and budgets.
                Use Indian Rupees symbol ₹.
                Keep answer short and clear.
                Do not give legal, tax, or risky investment advice.
                
                User financial summary:
                %s
                
                User question:
                %s
                """.formatted(financialSummary, question);

        Map<String, Object> requestBody = Map.of(
                "contents", List.of(
                        Map.of(
                                "parts", List.of(
                                        Map.of("text", prompt)
                                )
                        )
                ),
                "generationConfig", Map.of(
                        "temperature", 0.4,
                        "maxOutputTokens", 300
                )
        );

        String url = geminiApiUrl + "/" + geminiModel + ":generateContent?key=" + geminiApiKey.trim();

        Map response = webClientBuilder.build()
                .post()
                .uri(url)
                .header("Content-Type", "application/json")
                .bodyValue(requestBody)
                .retrieve()
                .onStatus(
                        status -> status.is4xxClientError() || status.is5xxServerError(),
                        clientResponse -> clientResponse.bodyToMono(String.class)
                                .map(errorBody -> new RuntimeException("Gemini API Error: " + errorBody))
                )
                .bodyToMono(Map.class)
                .block();

        if (response == null || !response.containsKey("candidates")) {
            throw new RuntimeException("Invalid Gemini response");
        }

        List candidates = (List) response.get("candidates");

        if (candidates == null || candidates.isEmpty()) {
            throw new RuntimeException("Gemini candidates empty");
        }

        Map firstCandidate = (Map) candidates.get(0);
        Map content = (Map) firstCandidate.get("content");

        if (content == null || content.get("parts") == null) {
            throw new RuntimeException("Gemini content missing");
        }

        List parts = (List) content.get("parts");

        if (parts.isEmpty()) {
            throw new RuntimeException("Gemini parts empty");
        }

        Map firstPart = (Map) parts.get(0);

        if (firstPart.get("text") == null) {
            throw new RuntimeException("Gemini text missing");
        }

        return firstPart.get("text").toString();
    }

    private String buildFinancialSummary(
            List<Income> incomes,
            List<Expense> expenses,
            List<Budget> budgets
    ) {

        BigDecimal totalIncome = incomes.stream()
                .map(Income::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalExpense = expenses.stream()
                .map(Expense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        StringBuilder summary = new StringBuilder();

        summary.append("Total income this month: ₹").append(totalIncome).append("\n");
        summary.append("Total expense this month: ₹").append(totalExpense).append("\n");
        summary.append("Savings this month: ₹").append(totalIncome.subtract(totalExpense)).append("\n\n");

        summary.append("Expenses:\n");

        if (expenses.isEmpty()) {
            summary.append("- No expenses added this month.\n");
        } else {
            for (Expense expense : expenses) {
                summary.append("- ")
                        .append(expense.getCategory())
                        .append(": ₹")
                        .append(expense.getAmount())
                        .append(" at ")
                        .append(expense.getMerchantName())
                        .append(" on ")
                        .append(expense.getExpenseDate())
                        .append("\n");
            }
        }

        summary.append("\nBudgets:\n");

        if (budgets.isEmpty()) {
            summary.append("- No budgets set this month.\n");
        } else {
            for (Budget budget : budgets) {
                summary.append("- ")
                        .append(budget.getCategory())
                        .append(": ₹")
                        .append(budget.getAmount())
                        .append(" for ")
                        .append(budget.getMonth())
                        .append("/")
                        .append(budget.getYear())
                        .append("\n");
            }
        }

        return summary.toString();
    }
}