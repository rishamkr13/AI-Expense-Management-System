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
public class AiChatService {

    private final WebClient.Builder webClientBuilder;

    @Value("${ai.api.url:https://api.openai.com/v1/chat/completions}")
    private String aiApiUrl;

    @Value("${ai.api.key:dummy-key}")
    private String aiApiKey;

    @Value("${ai.model:gpt-4o-mini}")
    private String aiModel;

    public String askAi(
            String question,
            List<Income> incomes,
            List<Expense> expenses,
            List<Budget> budgets
    ) {

        if (aiApiKey == null || aiApiKey.isBlank() || aiApiKey.equals("dummy-key")) {
            throw new RuntimeException("AI API key not configured");
        }

        String financialSummary = buildFinancialSummary(incomes, expenses, budgets);

        String prompt = """
                You are a helpful personal finance advisor for an Expense Management System.
                Give practical, simple, personalized advice.
                Use the user's actual income, expenses, and budgets.
                Use Indian Rupees symbol ₹.
                Keep answer short and clear.
                Do not give legal, tax, or investment advice.

                User financial summary:
                %s

                User question:
                %s
                """.formatted(financialSummary, question);

        Map<String, Object> requestBody = Map.of(
                "model", aiModel,
                "messages", List.of(
                        Map.of(
                                "role", "system",
                                "content", "You are a personal finance assistant inside an expense management app."
                        ),
                        Map.of(
                                "role", "user",
                                "content", prompt
                        )
                ),
                "temperature", 0.4
        );

        Map response = webClientBuilder.build()
                .post()
                .uri(aiApiUrl)
                .header("Authorization", "Bearer " + aiApiKey)
                .header("Content-Type", "application/json")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        if (response == null || !response.containsKey("choices")) {
            throw new RuntimeException("Invalid AI response");
        }

        List choices = (List) response.get("choices");

        if (choices == null || choices.isEmpty()) {
            throw new RuntimeException("AI response choices empty");
        }

        Map firstChoice = (Map) choices.get(0);
        Map message = (Map) firstChoice.get("message");

        if (message == null || message.get("content") == null) {
            throw new RuntimeException("AI message content missing");
        }

        return message.get("content").toString();
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