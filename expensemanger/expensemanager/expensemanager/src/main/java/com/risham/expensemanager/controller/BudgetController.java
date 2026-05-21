package com.risham.expensemanager.controller;

import com.risham.expensemanager.dto.request.BudgetRequest;
import com.risham.expensemanager.dto.response.BudgetResponse;
import com.risham.expensemanager.dto.response.BudgetStatusResponse;
import com.risham.expensemanager.service.BudgetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/budgets")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173")
public class BudgetController {

    private final BudgetService budgetService;

    @PostMapping
    public ResponseEntity<BudgetResponse> createBudget(@Valid @RequestBody BudgetRequest request) {
        return ResponseEntity.ok(budgetService.createBudget(request));
    }

    @GetMapping
    public ResponseEntity<List<BudgetResponse>> getAllBudgets() {
        return ResponseEntity.ok(budgetService.getAllBudgets());
    }

    @GetMapping("/{id}")
    public ResponseEntity<BudgetResponse> getBudgetById(@PathVariable Long id) {
        return ResponseEntity.ok(budgetService.getBudgetById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<BudgetResponse> updateBudget(
            @PathVariable Long id,
            @Valid @RequestBody BudgetRequest request
    ) {
        return ResponseEntity.ok(budgetService.updateBudget(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteBudget(@PathVariable Long id) {
        return ResponseEntity.ok(budgetService.deleteBudget(id));
    }

    @GetMapping("/month/{month}/year/{year}")
    public ResponseEntity<List<BudgetResponse>> getBudgetsByMonthAndYear(
            @PathVariable Integer month,
            @PathVariable Integer year
    ) {
        return ResponseEntity.ok(budgetService.getBudgetsByMonthAndYear(month, year));
    }

    @GetMapping("/status/month/{month}/year/{year}")
    public ResponseEntity<List<BudgetStatusResponse>> getBudgetStatus(
            @PathVariable Integer month,
            @PathVariable Integer year
    ) {
        return ResponseEntity.ok(budgetService.getBudgetStatus(month, year));
    }
}