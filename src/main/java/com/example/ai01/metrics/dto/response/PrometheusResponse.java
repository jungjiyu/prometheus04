package com.example.ai01.metrics.dto.response;

import com.example.ai01.metrics.dto.ServiceMetricsDTO;
import lombok.*;

import java.util.Map;

public class PrometheusResponse {

    @ToString
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UsageMetrics {
        private Map<String, ServiceMetricsDTO> requests;
        private Map<String, ServiceMetricsDTO> cost;
        private double totalRequestCount;
        private double totalCost;
        private String userId;
    }


}
