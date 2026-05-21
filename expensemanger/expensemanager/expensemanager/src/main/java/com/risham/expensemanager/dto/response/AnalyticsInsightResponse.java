package com.risham.expensemanager.dto.response;

import com.risham.expensemanager.enums.InsightPriority;
import com.risham.expensemanager.enums.InsightType;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnalyticsInsightResponse {

    private InsightType type;
    private String title;
    private String message;
    private InsightPriority priority;
}