package com.example.ai01.metrics.service;

import com.example.ai01.global.exception.JwtTokenExpiredException;
import com.example.ai01.metrics.dto.ServiceMetricsDTO;
import com.example.ai01.metrics.dto.response.PrometheusResponse;
import com.example.ai01.security.util.JwtUtil;
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
    private final JwtUtil jwtUtil;  // JWT 유틸리티 추가

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

    // 사용자별 JWT 토큰 캐싱(크기 제한 없이 사용)
    private final Map<String, String> jwtCache = new HashMap<>(); // jwtCache 정의 추가



    // 사용자에 따른 JWT 토큰 검증 로직 (토큰 갱신 없이)
    private String getJwtToken(String userId) throws Exception {
        String token = jwtCache.get(userId);
        if (token == null || jwtUtil.isTokenExpired(token)) {
            throw new Exception("JWT token expired or not found for user: " + userId);
        }
        return token;
    }

    // Prometheus 쿼리 실행
    public String query(String query, String userId) throws Exception {
        String prometheusUrl = UriComponentsBuilder.fromHttpUrl(this.prometheusUrl)
                .encode()
                .toUriString();

        // JWT 토큰 가져오기 (갱신 없이 만료된 경우 에러 반환)
        String jwtToken = getJwtToken(userId);

        // HTTP 헤더 설정
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(jwtToken);  // JWT Bearer Token 설정
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        // 요청 본문 설정 (쿼리를 form 데이터로 전송)
        String requestBody = "query=" + query;
        HttpEntity<String> requestEntity = new HttpEntity<>(requestBody, headers);

        // POST 요청 전송
        try {
            return restTemplate.postForObject(prometheusUrl, requestEntity, String.class);
        } catch (Exception e) {
            log.error("Error while querying Prometheus API for user {}: {}", userId, e.getMessage());
            throw new Exception("Error querying Prometheus API", e);
        }
    }

    public PrometheusResponse.UsageMetrics getJsonFormatUserUsage(String userId) throws Exception {

        // 각 서비스별 경로 정보 정의
        Map<String, String[]> paths = Map.of(
                "groq", new String[]{"/api/groq/.*", "/api/groq/complete", "/api/groq/advice", "/api/groq/harmfulness", "/api/groq/compose"},
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
                String query = buildPrometheusQuery(userId, path);  // 쿼리 생성 로직 분리
                String result = query(query, userId);  // JWT만으로 요청

                log.debug("Prometheus {} query result for path {}: {}", serviceName, path, result);
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

        // 총 요청 수 및 비용 계산
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

        return usageMetrics;
    }

    // Prometheus 쿼리 빌드 로직 분리
    private String buildPrometheusQuery(String userId, String path) {
        return String.format("sum(http_server_requests_user_total{user_id=\"%s\", path=\"%s\"})", userId, path);
    }

    // 쿼리 결과를 처리하는 함수
    private Optional<Double> processQueryResult(String result, String path, double costPerRequest) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            JsonNode rootNode = objectMapper.readTree(result);
            JsonNode dataNode = rootNode.path("data").path("result");
            if (dataNode.isArray() && dataNode.size() > 0) {
                JsonNode valueNode = dataNode.get(0).path("value");
                if (valueNode.isArray() && valueNode.size() > 1) {
                    return Optional.of(valueNode.get(1).asDouble());
                }
            }
        } catch (JsonProcessingException e) {
            log.error("Error processing Prometheus response for {}: {}", path, e.getMessage());
        }
        log.warn("No valid data found for path: {}", path);
        return Optional.empty();
    }
}