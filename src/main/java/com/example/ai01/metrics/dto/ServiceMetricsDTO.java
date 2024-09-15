package com.example.ai01.metrics.dto;

import lombok.*;

import java.util.HashMap;
import java.util.Map;


@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class ServiceMetricsDTO {
    private Map<String, Double> paths;
    private double total;

    public void addPathMetric(String path, double value) {
        if (this.paths == null)  this.paths = new HashMap<>();
        this.paths.put(path, value);
    }
}
