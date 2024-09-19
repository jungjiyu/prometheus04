package com.example.ai01.metrics.service;

import com.example.ai01.metrics.dto.ServiceMetricsDTO;
import com.example.ai01.metrics.dto.response.PrometheusResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.*;



@Slf4j
@RequiredArgsConstructor
@Service
public class PrometheusService {
    private final RestTemplate restTemplate;

    @Value("${prometheus.api.url}")
    private String prometheusUrl;

    @Value("${cost.groq}")
    private double groqCost;

    @Value("${cost.vllm}")
    private double vllmCost;

    @Value("${cost.azure}")
    private double azureCost;

    @Value("${cost.openai}")
    private double openaiCost;

    // Prometheus 쿼리 실행
    public String query(String query, String userId) throws Exception {
        log.debug("Building Prometheus query URL for userId: {}", userId);

        String prometheusUrl = UriComponentsBuilder.fromHttpUrl(this.prometheusUrl)
                .encode()
                .toUriString();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        String requestBody = "query=" + query;
        HttpEntity<String> requestEntity = new HttpEntity<>(requestBody, headers);

        log.info("Sending Prometheus query for userId: {}, query: {}", userId, query);
        try {
            return restTemplate.postForObject(prometheusUrl, requestEntity, String.class);
        } catch (Exception e) {
            log.error("Error while querying Prometheus API for user {}: {}", userId, e.getMessage());
            throw new Exception("Error querying Prometheus API", e);
        }
    }

    // 사용자의 메트릭 데이터를 Prometheus에서 조회
    public PrometheusResponse.UsageMetrics getJsonFormatUserUsage(String userId) throws Exception {
        log.info("Fetching Prometheus usage metrics for userId: {}", userId);

        Map<String, String[]> paths = Map.of(
                "groq", new String[]{"/api/groq/.*", "/api/groq/complete", "/api/groq/advice", "/api/groq/harmfulness", "/api/groq/compose", "/api/groq/news"},
                "openai", new String[]{"/api/openai/.*", "/api/openai/complete", "/api/openai/advice", "/api/openai/harmfulness", "/api/openai/compose"},
                "azure", new String[]{"/api/azure/.*", "/api/azure/caption"},
                "vllm", new String[]{"/api/vllm/.*"}
        );

        Map<String, Double> costMap = Map.of(
                "groq", groqCost,
                "openai", openaiCost,
                "azure", azureCost,
                "vllm", vllmCost
        );

        PrometheusResponse.UsageMetrics usageMetrics = new PrometheusResponse.UsageMetrics();
        Map<String, ServiceMetricsDTO> requestsData = new HashMap<>();
        Map<String, ServiceMetricsDTO> costData = new HashMap<>();

        for (String serviceName : paths.keySet()) {
            String[] servicePaths = paths.get(serviceName);
            double serviceCost = costMap.get(serviceName);

            ServiceMetricsDTO serviceRequests = new ServiceMetricsDTO();
            ServiceMetricsDTO serviceCosts = new ServiceMetricsDTO();

            for (String path : servicePaths) {
                String query = buildPrometheusQuery(userId, path);
                log.debug("Running query for service: {}, path: {}", serviceName, path);
                String result = query(query, userId);

                log.debug("Prometheus query result for service: {}, path: {}, result: {}", serviceName, path, result);
                double requestCount = processQueryResult(result, path, serviceCost).orElse(0.0);

                String cleanedPath = path.replace("/api/", "").replace("/", "_");

                if (path.contains(".*")) {
                    serviceRequests.setTotal(requestCount);
                    serviceCosts.setTotal(requestCount * serviceCost);
                } else {
                    serviceRequests.addPathMetric(cleanedPath, requestCount);
                    serviceCosts.addPathMetric(cleanedPath, requestCount * serviceCost);
                }
            }

            requestsData.put(serviceName, serviceRequests);
            costData.put(serviceName, serviceCosts);
        }

        double totalRequests = requestsData.values().stream()
                .flatMap(map -> map.getPaths().values().stream())
                .mapToDouble(Double::doubleValue)
                .sum();

        double totalCost = costData.values().stream()
                .flatMap(map -> map.getPaths().values().stream())
                .mapToDouble(Double::doubleValue)
                .sum();

        usageMetrics.setRequests(requestsData);
        usageMetrics.setCost(costData);
        usageMetrics.setTotalRequestCount(totalRequests);
        usageMetrics.setTotalCost(totalCost);
        usageMetrics.setUserId(userId);

        log.info("Successfully fetched Prometheus usage metrics for userId: {}", userId);
        return usageMetrics;
    }


    // Prometheus 쿼리 빌드
    private String buildPrometheusQuery(String userId, String path) {
        String query = String.format("sum(http_server_requests_user_total{user_id=\"%s\", path=~\"%s\"})", userId, path);
        log.debug("Built Prometheus query: {}", query);
        return query;
    }

    // 쿼리 결과 처리
    private Optional<Double> processQueryResult(String result, String path, double costPerRequest) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            JsonNode rootNode = objectMapper.readTree(result);
            JsonNode dataNode = rootNode.path("data").path("result");
            if (dataNode.isArray() && dataNode.size() > 0) {
                JsonNode valueNode = dataNode.get(0).path("value");
                if (valueNode.isArray() && valueNode.size() > 1) {
                    log.debug("Processing query result for path: {}, value: {}", path, valueNode.get(1).asDouble());
                    return Optional.of(valueNode.get(1).asDouble());
                }
            } else {
                log.warn("No data returned from Prometheus for path: {}", path);
                return Optional.of(0.0); // 데이터가 없을 때 기본값 0 반환
            }
        } catch (JsonProcessingException e) {
            log.error("Error processing Prometheus response for path {}: {}", path, e.getMessage());
        }
        log.warn("No valid data found for path: {}", path);
        return Optional.empty();
    }
}