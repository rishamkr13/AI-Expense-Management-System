package com.risham.expensemanager.controller;

import com.risham.expensemanager.dto.response.BudgetStatusResponse;
import com.risham.expensemanager.dto.response.CategoryExpenseResponse;
import com.risham.expensemanager.dto.response.DashboardSummaryResponse;
import com.risham.expensemanager.dto.response.ExpenseResponse;
import com.risham.expensemanager.dto.response.MonthlyTrendResponse;
import com.risham.expensemanager.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    // GET /api/dashboard/summary
    @GetMapping("/summary")
    public ResponseEntity<DashboardSummaryResponse> getSummary() {
        return ResponseEntity.ok(dashboardService.getSummary());
    }

    // GET /api/dashboard/categories
    @GetMapping("/categories")
    public ResponseEntity<List<CategoryExpenseResponse>> getCategoryWiseExpense() {
        return ResponseEntity.ok(dashboardService.getCategoryWiseExpense());
    }

    // GET /api/dashboard/monthly
    @GetMapping("/monthly")
    public ResponseEntity<List<MonthlyTrendResponse>> getMonthlyTrend() {
        return ResponseEntity.ok(dashboardService.getMonthlyTrend());
    }

    // GET /api/dashboard/recent
    @GetMapping("/recent")
    public ResponseEntity<List<ExpenseResponse>> getRecentTransactions() {
        return ResponseEntity.ok(dashboardService.getRecentTransactions());
    }

    // GET /api/dashboard/budget
    @GetMapping("/budget")
    public ResponseEntity<List<BudgetStatusResponse>> getBudgetStatus() {
        return ResponseEntity.ok(dashboardService.getCurrentMonthBudgetStatus());
    }
}