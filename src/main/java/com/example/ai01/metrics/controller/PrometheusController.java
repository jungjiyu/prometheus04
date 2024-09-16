package com.example.ai01.metrics.controller;

import com.example.ai01.metrics.dto.response.PrometheusResponse;
import com.example.ai01.metrics.service.PrometheusService;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@AllArgsConstructor
@RequestMapping("/api/prometheus")
public class PrometheusController {

    private final PrometheusService prometheusService;

    @GetMapping("/usage")
    public ResponseEntity<PrometheusResponse.UsageMetrics> getJsonFormatUserUsage() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userId = authentication.getName();

        try {
            // Pass the userId to the service without privateKey
            PrometheusResponse.UsageMetrics response = prometheusService.getJsonFormatUserUsage(userId);
            return ResponseEntity.status(HttpStatus.OK).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

}

