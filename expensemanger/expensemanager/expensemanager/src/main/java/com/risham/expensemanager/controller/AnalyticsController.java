package com.risham.expensemanager.controller;

import com.risham.expensemanager.dto.response.AnalyticsInsightResponse;
import com.risham.expensemanager.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/insights")
    public ResponseEntity<List<AnalyticsInsightResponse>> getInsights() {
        return ResponseEntity.ok(analyticsService.getInsights());
    }
}