package com.example.ai01.metrics.dto.response;

import com.example.ai01.metrics.dto.ServiceMetricsDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

public class PrometheusResponse {
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
