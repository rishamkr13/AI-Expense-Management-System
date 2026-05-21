package com.risham.expensemanager.service;

import com.risham.expensemanager.dto.request.ExpenseRequest;
import com.risham.expensemanager.dto.response.ExpenseResponse;
import com.risham.expensemanager.entity.Expense;
import com.risham.expensemanager.entity.User;
import com.risham.expensemanager.enums.ExpenseCategory;
import com.risham.expensemanager.repository.ExpenseRepository;
import com.risham.expensemanager.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final UserRepository userRepository;

    public ExpenseResponse createExpense(ExpenseRequest request) {

        User currentUser = getCurrentUser();

        Expense expense = Expense.builder()
                .amount(request.getAmount())
                .category(request.getCategory())
                .merchantName(request.getMerchantName())
                .paymentMode(request.getPaymentMode())
                .description(request.getDescription())
                .expenseDate(request.getExpenseDate())
                .user(currentUser)
                .build();

        Expense savedExpense = expenseRepository.save(expense);

        return mapToResponse(savedExpense);
    }

    public List<ExpenseResponse> getAllExpenses() {

        User currentUser = getCurrentUser();

        return expenseRepository.findByUser(currentUser)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    public ExpenseResponse getExpenseById(Long id) {

        User currentUser = getCurrentUser();

        Expense expense = expenseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Expense not found with id: " + id));

        if (!expense.getUser().getId().equals(currentUser.getId())) {
            throw new RuntimeException("You are not allowed to access this expense");
        }

        return mapToResponse(expense);
    }

    public ExpenseResponse updateExpense(Long id, ExpenseRequest request) {

        User currentUser = getCurrentUser();

        Expense expense = expenseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Expense not found with id: " + id));

        if (!expense.getUser().getId().equals(currentUser.getId())) {
            throw new RuntimeException("You are not allowed to update this expense");
        }

        expense.setAmount(request.getAmount());
        expense.setCategory(request.getCategory());
        expense.setMerchantName(request.getMerchantName());
        expense.setPaymentMode(request.getPaymentMode());
        expense.setDescription(request.getDescription());
        expense.setExpenseDate(request.getExpenseDate());

        Expense updatedExpense = expenseRepository.save(expense);

        return mapToResponse(updatedExpense);
    }

    public String deleteExpense(Long id) {

        User currentUser = getCurrentUser();

        Expense expense = expenseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Expense not found with id: " + id));

        if (!expense.getUser().getId().equals(currentUser.getId())) {
            throw new RuntimeException("You are not allowed to delete this expense");
        }

        expenseRepository.delete(expense);

        return "Expense deleted successfully";
    }

    public List<ExpenseResponse> getExpensesByCategory(ExpenseCategory category) {

        User currentUser = getCurrentUser();

        return expenseRepository.findByUserAndCategory(currentUser, category)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    private User getCurrentUser() {

        String email = SecurityContextHolder.getContext()
                .getAuthentication()
                .getName();

        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Logged-in user not found"));
    }

    private ExpenseResponse mapToResponse(Expense expense) {

        return ExpenseResponse.builder()
                .id(expense.getId())
                .amount(expense.getAmount())
                .category(expense.getCategory())
                .merchantName(expense.getMerchantName())
                .paymentMode(expense.getPaymentMode())
                .description(expense.getDescription())
                .expenseDate(expense.getExpenseDate())
                .createdAt(expense.getCreatedAt())
                .updatedAt(expense.getUpdatedAt())
                .build();
    }
}