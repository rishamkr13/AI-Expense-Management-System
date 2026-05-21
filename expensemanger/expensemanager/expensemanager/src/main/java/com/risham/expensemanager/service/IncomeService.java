package com.risham.expensemanager.service;

import com.risham.expensemanager.dto.request.IncomeRequest;
import com.risham.expensemanager.dto.response.IncomeResponse;
import com.risham.expensemanager.entity.Income;
import com.risham.expensemanager.entity.User;
import com.risham.expensemanager.enums.IncomeSource;
import com.risham.expensemanager.repository.IncomeRepository;
import com.risham.expensemanager.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class IncomeService {

    private final IncomeRepository incomeRepository;
    private final UserRepository userRepository;

    public IncomeResponse createIncome(IncomeRequest request) {

        User currentUser = getCurrentUser();

        Income income = Income.builder()
                .amount(request.getAmount())
                .source(request.getSource())
                .description(request.getDescription())
                .incomeDate(request.getIncomeDate())
                .user(currentUser)
                .build();

        Income savedIncome = incomeRepository.save(income);

        return mapToResponse(savedIncome);
    }

    public List<IncomeResponse> getAllIncomes() {

        User currentUser = getCurrentUser();

        return incomeRepository.findByUser(currentUser)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    public IncomeResponse getIncomeById(Long id) {

        User currentUser = getCurrentUser();

        Income income = incomeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Income not found with id: " + id));

        if (!income.getUser().getId().equals(currentUser.getId())) {
            throw new RuntimeException("You are not allowed to access this income");
        }

        return mapToResponse(income);
    }

    public IncomeResponse updateIncome(Long id, IncomeRequest request) {

        User currentUser = getCurrentUser();

        Income income = incomeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Income not found with id: " + id));

        if (!income.getUser().getId().equals(currentUser.getId())) {
            throw new RuntimeException("You are not allowed to update this income");
        }

        income.setAmount(request.getAmount());
        income.setSource(request.getSource());
        income.setDescription(request.getDescription());
        income.setIncomeDate(request.getIncomeDate());

        Income updatedIncome = incomeRepository.save(income);

        return mapToResponse(updatedIncome);
    }

    public String deleteIncome(Long id) {

        User currentUser = getCurrentUser();

        Income income = incomeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Income not found with id: " + id));

        if (!income.getUser().getId().equals(currentUser.getId())) {
            throw new RuntimeException("You are not allowed to delete this income");
        }

        incomeRepository.delete(income);

        return "Income deleted successfully";
    }

    public List<IncomeResponse> getIncomesBySource(IncomeSource source) {

        User currentUser = getCurrentUser();

        return incomeRepository.findByUserAndSource(currentUser, source)
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

    private IncomeResponse mapToResponse(Income income) {
        return IncomeResponse.builder()
                .id(income.getId())
                .amount(income.getAmount())
                .source(income.getSource())
                .description(income.getDescription())
                .incomeDate(income.getIncomeDate())
                .createdAt(income.getCreatedAt())
                .updatedAt(income.getUpdatedAt())
                .build();
    }
}