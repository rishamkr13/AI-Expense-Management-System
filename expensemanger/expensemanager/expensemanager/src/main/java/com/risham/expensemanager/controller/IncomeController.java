package com.risham.expensemanager.controller;

import com.risham.expensemanager.dto.request.IncomeRequest;
import com.risham.expensemanager.dto.response.IncomeResponse;
import com.risham.expensemanager.enums.IncomeSource;
import com.risham.expensemanager.service.IncomeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/incomes")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173")
public class IncomeController {

    private final IncomeService incomeService;

    @PostMapping
    public ResponseEntity<IncomeResponse> createIncome(@Valid @RequestBody IncomeRequest request) {
        return ResponseEntity.ok(incomeService.createIncome(request));
    }

    @GetMapping
    public ResponseEntity<List<IncomeResponse>> getAllIncomes() {
        return ResponseEntity.ok(incomeService.getAllIncomes());
    }

    @GetMapping("/{id}")
    public ResponseEntity<IncomeResponse> getIncomeById(@PathVariable Long id) {
        return ResponseEntity.ok(incomeService.getIncomeById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<IncomeResponse> updateIncome(
            @PathVariable Long id,
            @Valid @RequestBody IncomeRequest request
    ) {
        return ResponseEntity.ok(incomeService.updateIncome(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteIncome(@PathVariable Long id) {
        return ResponseEntity.ok(incomeService.deleteIncome(id));
    }

    @GetMapping("/source/{source}")
    public ResponseEntity<List<IncomeResponse>> getIncomesBySource(@PathVariable IncomeSource source) {
        return ResponseEntity.ok(incomeService.getIncomesBySource(source));
    }
}