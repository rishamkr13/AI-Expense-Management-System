package com.risham.expensemanager.service;

import com.risham.expensemanager.dto.request.ChatbotRequest;
import com.risham.expensemanager.dto.response.ChatbotResponse;
import com.risham.expensemanager.entity.Budget;
import com.risham.expensemanager.entity.Expense;
import com.risham.expensemanager.entity.Income;
import com.risham.expensemanager.entity.User;
import com.risham.expensemanager.enums.ExpenseCategory;
import com.risham.expensemanager.repository.BudgetRepository;
import com.risham.expensemanager.repository.ExpenseRepository;
import com.risham.expensemanager.repository.IncomeRepository;
import com.risham.expensemanager.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatbotService {

    private final ExpenseRepository expenseRepository;
    private final IncomeRepository incomeRepository;
    private final BudgetRepository budgetRepository;
    private final UserRepository userRepository;

    private final GroqAiService groqAiService;

    /*
     * Rule-based chatbot endpoint.
     * This works without any external API.
     */
    public ChatbotResponse ask(ChatbotRequest request) {

        User currentUser = getCurrentUser();

        String question = request.getQuestion();

        if (question == null || question.isBlank()) {
            return ChatbotResponse.builder()
                    .question("")
                    .answer("Please ask a valid finance-related question.")
                    .source("RULE_BASED")
                    .build();
        }

        String normalizedQuestion = question.toLowerCase(Locale.ROOT);

        LocalDate today = LocalDate.now();
        LocalDate monthStart = today.withDayOfMonth(1);
        LocalDate monthEnd = today.withDayOfMonth(today.lengthOfMonth());

        List<Expense> expenses = expenseRepository.findByUserAndExpenseDateBetween(
                currentUser,
                monthStart,
                monthEnd
        );

        List<Income> incomes = incomeRepository.findByUserAndIncomeDateBetween(
                currentUser,
                monthStart,
                monthEnd
        );

        List<Budget> budgets = budgetRepository.findByUserAndMonthAndYear(
                currentUser,
                today.getMonthValue(),
                today.getYear()
        );

        String answer;

        if (containsAny(normalizedQuestion, "save", "saving", "savings")) {
            answer = answerSavingsQuestion(incomes, expenses);
        } else if (containsAny(normalizedQuestion, "overspend", "overspending", "over spending", "budget")) {
            answer = answerOverspendingQuestion(expenses, budgets);
        } else if (containsAny(normalizedQuestion, "food")) {
            answer = answerCategoryQuestion(expenses, budgets, ExpenseCategory.FOOD);
        } else if (containsAny(normalizedQuestion, "travel")) {
            answer = answerCategoryQuestion(expenses, budgets, ExpenseCategory.TRAVEL);
        } else if (containsAny(normalizedQuestion, "shopping")) {
            answer = answerCategoryQuestion(expenses, budgets, ExpenseCategory.SHOPPING);
        } else if (containsAny(normalizedQuestion, "bill", "bills")) {
            answer = answerCategoryQuestion(expenses, budgets, ExpenseCategory.BILLS);
        } else if (containsAny(normalizedQuestion, "health", "medical")) {
            answer = answerCategoryQuestion(expenses, budgets, ExpenseCategory.HEALTH);
        } else if (containsAny(normalizedQuestion, "education", "study", "college")) {
            answer = answerCategoryQuestion(expenses, budgets, ExpenseCategory.EDUCATION);
        } else if (containsAny(normalizedQuestion, "entertainment", "movie", "ott")) {
            answer = answerCategoryQuestion(expenses, budgets, ExpenseCategory.ENTERTAINMENT);
        } else if (containsAny(normalizedQuestion, "highest", "most", "maximum")) {
            answer = answerHighestSpendingCategory(expenses);
        } else if (containsAny(normalizedQuestion, "total expense", "spent", "spending", "expense")) {
            answer = answerTotalExpense(expenses);
        } else if (containsAny(normalizedQuestion, "income", "salary", "earning")) {
            answer = answerTotalIncome(incomes);
        } else if (containsAny(normalizedQuestion, "summary", "overview", "report")) {
            answer = answerGeneralSummary(incomes, expenses, budgets);
        } else {
            answer = answerGeneralSummary(incomes, expenses, budgets);
        }

        return ChatbotResponse.builder()
                .question(question)
                .answer(answer)
                .source("RULE_BASED")
                .build();
    }

    /*
     * Groq AI chatbot endpoint.
     * First tries Groq API.
     * If Groq fails, it automatically returns rule-based fallback answer.
     */
    public ChatbotResponse askWithGroqFallback(ChatbotRequest request) {

        User currentUser = getCurrentUser();

        String question = request.getQuestion();

        if (question == null || question.isBlank()) {
            return ChatbotResponse.builder()
                    .question("")
                    .answer("Please ask a valid finance-related question.")
                    .source("RULE_BASED_FALLBACK")
                    .build();
        }

        LocalDate today = LocalDate.now();
        LocalDate monthStart = today.withDayOfMonth(1);
        LocalDate monthEnd = today.withDayOfMonth(today.lengthOfMonth());

        List<Expense> expenses = expenseRepository.findByUserAndExpenseDateBetween(
                currentUser,
                monthStart,
                monthEnd
        );

        List<Income> incomes = incomeRepository.findByUserAndIncomeDateBetween(
                currentUser,
                monthStart,
                monthEnd
        );

        List<Budget> budgets = budgetRepository.findByUserAndMonthAndYear(
                currentUser,
                today.getMonthValue(),
                today.getYear()
        );

        try {
            String groqAnswer = groqAiService.askChatbot(
                    question,
                    incomes,
                    expenses,
                    budgets
            );

            if (groqAnswer == null || groqAnswer.isBlank()) {
                throw new RuntimeException("Groq returned empty answer");
            }

            return ChatbotResponse.builder()
                    .question(question)
                    .answer(groqAnswer)
                    .source("GROQ_API")
                    .build();

        } catch (Exception e) {

            System.out.println("GROQ API FAILED:");
            System.out.println(e.getMessage());
            e.printStackTrace();

            ChatbotResponse fallbackResponse = ask(request);

            return ChatbotResponse.builder()
                    .question(fallbackResponse.getQuestion())
                    .answer(fallbackResponse.getAnswer())
                    .source("RULE_BASED_FALLBACK")
                    .build();
        }
    }

    /*
     * Old method name kept for compatibility.
     * If your controller still calls askWithAiFallback(),
     * this method prevents compile error.
     */
    public ChatbotResponse askWithAiFallback(ChatbotRequest request) {
        return askWithGroqFallback(request);
    }

    /*
     * Old Gemini method kept for compatibility.
     * If your controller still calls askWithGeminiFallback(),
     * this method redirects to Groq.
     */
    public ChatbotResponse askWithGeminiFallback(
            ChatbotRequest request,
            GeminiChatService geminiChatService
    ) {
        return askWithGroqFallback(request);
    }

    private String answerSavingsQuestion(List<Income> incomes, List<Expense> expenses) {

        BigDecimal totalIncome = calculateTotalIncome(incomes);
        BigDecimal totalExpense = calculateTotalExpense(expenses);

        if (totalIncome.compareTo(BigDecimal.ZERO) == 0) {
            return "You have not added income for this month, so I cannot calculate your savings properly. Please add your monthly income first.";
        }

        BigDecimal savings = totalIncome.subtract(totalExpense);

        BigDecimal savingsPercentage = savings
                .divide(totalIncome, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);

        String highestCategory = getHighestCategoryText(expenses);

        if (savingsPercentage.compareTo(BigDecimal.valueOf(50)) >= 0) {
            return "Your total income this month is ₹" + totalIncome
                    + ", total expense is ₹" + totalExpense
                    + ", and savings is ₹" + savings
                    + ". Your savings percentage is " + savingsPercentage
                    + "%. You are saving well. " + highestCategory;
        } else if (savingsPercentage.compareTo(BigDecimal.valueOf(20)) >= 0) {
            return "Your savings this month is ₹" + savings
                    + " with a savings percentage of " + savingsPercentage
                    + "%. This is moderate. Try reducing optional spending like food delivery, shopping, or subscriptions. "
                    + highestCategory;
        } else {
            return "Your savings percentage is only " + savingsPercentage
                    + "%. This is low. You should review your highest spending category and set stricter budgets. "
                    + highestCategory;
        }
    }

    private String answerOverspendingQuestion(List<Expense> expenses, List<Budget> budgets) {

        if (budgets.isEmpty()) {
            return "You have not added any budgets for this month. Add category-wise budgets first so I can check overspending.";
        }

        StringBuilder response = new StringBuilder();
        boolean overspendingFound = false;

        for (Budget budget : budgets) {

            BigDecimal spent = calculateCategorySpent(expenses, budget.getCategory());

            if (spent.compareTo(budget.getAmount()) > 0) {

                BigDecimal extra = spent.subtract(budget.getAmount());

                response.append("You are overspending in ")
                        .append(budget.getCategory())
                        .append(". Budget: ₹")
                        .append(budget.getAmount())
                        .append(", Spent: ₹")
                        .append(spent)
                        .append(", Extra: ₹")
                        .append(extra)
                        .append(". ");

                overspendingFound = true;
            }
        }

        if (!overspendingFound) {
            return "You are not overspending in any budgeted category this month. Good financial control.";
        }

        return response.toString();
    }

    private String answerCategoryQuestion(
            List<Expense> expenses,
            List<Budget> budgets,
            ExpenseCategory category
    ) {

        BigDecimal spent = calculateCategorySpent(expenses, category);

        Budget categoryBudget = budgets.stream()
                .filter(budget -> budget.getCategory().equals(category))
                .findFirst()
                .orElse(null);

        if (categoryBudget == null) {
            return "You spent ₹" + spent + " on " + category
                    + " this month. No budget is set for this category.";
        }

        BigDecimal remaining = categoryBudget.getAmount().subtract(spent);

        if (remaining.compareTo(BigDecimal.ZERO) >= 0) {
            return "You spent ₹" + spent + " on " + category
                    + " this month. Your budget is ₹" + categoryBudget.getAmount()
                    + ", so remaining budget is ₹" + remaining + ".";
        } else {
            return "You spent ₹" + spent + " on " + category
                    + " this month. Your budget is ₹" + categoryBudget.getAmount()
                    + ", so you overspent by ₹" + remaining.abs() + ".";
        }
    }

    private String answerHighestSpendingCategory(List<Expense> expenses) {

        if (expenses.isEmpty()) {
            return "You have not added expenses for this month yet.";
        }

        Map<ExpenseCategory, BigDecimal> categoryTotals = expenses.stream()
                .collect(Collectors.groupingBy(
                        Expense::getCategory,
                        Collectors.reducing(
                                BigDecimal.ZERO,
                                Expense::getAmount,
                                BigDecimal::add
                        )
                ));

        Map.Entry<ExpenseCategory, BigDecimal> highest = categoryTotals.entrySet()
                .stream()
                .max(Map.Entry.comparingByValue())
                .orElse(null);

        if (highest == null) {
            return "I could not identify the highest spending category.";
        }

        return "Your highest spending category this month is "
                + highest.getKey()
                + " with ₹"
                + highest.getValue()
                + " spent.";
    }

    private String answerTotalExpense(List<Expense> expenses) {

        BigDecimal totalExpense = calculateTotalExpense(expenses);

        return "Your total expense this month is ₹" + totalExpense + ".";
    }

    private String answerTotalIncome(List<Income> incomes) {

        BigDecimal totalIncome = calculateTotalIncome(incomes);

        return "Your total income this month is ₹" + totalIncome + ".";
    }

    private String answerGeneralSummary(
            List<Income> incomes,
            List<Expense> expenses,
            List<Budget> budgets
    ) {

        BigDecimal totalIncome = calculateTotalIncome(incomes);
        BigDecimal totalExpense = calculateTotalExpense(expenses);
        BigDecimal savings = totalIncome.subtract(totalExpense);

        String highestCategoryText = getHighestCategoryText(expenses);

        return "Here is your financial summary for this month: Total income ₹"
                + totalIncome
                + ", total expense ₹"
                + totalExpense
                + ", savings ₹"
                + savings
                + ". "
                + highestCategoryText
                + " You have set "
                + budgets.size()
                + " budget categories.";
    }

    private String getHighestCategoryText(List<Expense> expenses) {

        if (expenses.isEmpty()) {
            return "You have not added expenses for this month yet.";
        }

        Map<ExpenseCategory, BigDecimal> categoryTotals = expenses.stream()
                .collect(Collectors.groupingBy(
                        Expense::getCategory,
                        Collectors.reducing(
                                BigDecimal.ZERO,
                                Expense::getAmount,
                                BigDecimal::add
                        )
                ));

        Map.Entry<ExpenseCategory, BigDecimal> highest = categoryTotals.entrySet()
                .stream()
                .max(Map.Entry.comparingByValue())
                .orElse(null);

        if (highest == null) {
            return "I could not identify your highest spending category.";
        }

        return "Your highest spending category is "
                + highest.getKey()
                + " with ₹"
                + highest.getValue()
                + " spent.";
    }

    private boolean containsAny(String text, String... keywords) {

        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }

        return false;
    }

    private BigDecimal calculateCategorySpent(List<Expense> expenses, ExpenseCategory category) {

        return expenses.stream()
                .filter(expense -> expense.getCategory().equals(category))
                .map(Expense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal calculateTotalExpense(List<Expense> expenses) {

        return expenses.stream()
                .map(Expense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal calculateTotalIncome(List<Income> incomes) {

        return incomes.stream()
                .map(Income::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private User getCurrentUser() {

        String email = SecurityContextHolder.getContext()
                .getAuthentication()
                .getName();

        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Logged-in user not found"));
    }
}