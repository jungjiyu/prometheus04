package com.example.ai01.metrics.dto;

import lombok.*;

import java.util.HashMap;
import java.util.Map;


@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class ServiceMetricsDTO {
    private Map<String, Double> paths = new HashMap<>(); // 기본적으로 빈 Map으로 초기화
    private double total;

    public void addPathMetric(String path, double value) {
        if (this.paths == null)  this.paths = new HashMap<>();
        this.paths.put(path, value);
    }
}
