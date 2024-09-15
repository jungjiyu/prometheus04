package com.example.ai01.metrics.service;

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
    private final Map<String, String> jwtCache = new HashMap<>();


    // 사용자에 따른 JWT 토큰 재사용 로직 & 만료된 경우 갱신
    private String getJwtToken(String userId) {
        String token = jwtCache.get(userId);
        if (token == null || jwtUtil.isTokenExpired(token)) {
            token = jwtUtil.generateToken(userId);
            jwtCache.put(userId, token);
        }
        return token;
    }

    // Prometheus 쿼리 실행
    public String query(String query, String userId) {
        String prometheusUrl = UriComponentsBuilder.fromHttpUrl(this.prometheusUrl)
                .encode()
                .toUriString();

        // JWT 토큰 가져오기
        String jwtToken = getJwtToken(userId);

        // HTTP 헤더 설정
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(jwtToken);  // JWT Bearer Token 설정
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        // 요청 본문 설정 (쿼리를 form 데이터로 전송)
        String requestBody = "query=" + query;
        HttpEntity<String> requestEntity = new HttpEntity<>(requestBody, headers);

        // POST 요청 전송
        return restTemplate.postForObject(prometheusUrl, requestEntity, String.class);
    }

    public PrometheusResponse.UsageMetrics getJsonFormatUserUsage(String userId) {

        /*
        //groq
        String totalGroqQuery = String.format("sum(http_server_requests_user_total{user_id=\"%s\", path=\"/api/groq/.*\"})", userId);
        String completeGroqQuery = String.format("sum(http_server_requests_user_total{user_id=\"%s\", path=\"/api/groq/complete\"})", userId);
        String adviceGroqQuery = String.format("sum(http_server_requests_user_total{user_id=\"%s\", path=\"/api/groq/advice\"})", userId);
        String harmfulnessGroqQuery = String.format("sum(http_server_requests_user_total{user_id=\"%s\", path=\"/api/groq/harmfulness\"})", userId);
        String composeGroqQuery= String.format("sum(http_server_requests_user_total{user_id=\"%s\", path=\"/api/groq/compose\"})", userId);

        //openai
        String totalOpenaiQuery = String.format("sum(http_server_requests_user_total{user_id=\"%s\", path=\"/api/openai/.*\"})", userId);
        String completeOpenaiQuery = String.format("sum(http_server_requests_user_total{user_id=\"%s\", path=\"/api/openai/complete\"})", userId);
        String adviceOpenaiQuery = String.format("sum(http_server_requests_user_total{user_id=\"%s\", path=\"/api/openai/advice\"})", userId);
        String harmfulnessOpenaiQuery = String.format("sum(http_server_requests_user_total{user_id=\"%s\", path=\"/api/openai/harmfulness\"})", userId);
        String composeOpenaiQuery =  String.format("sum(http_server_requests_user_total{user_id=\"%s\", path=\"/api/openai/compose\"})", userId);

        //azure
        String totalAzureQuery = String.format("sum(http_server_requests_user_total{user_id=\"%s\", path=\"/api/azure/.*\"})", userId);
        String captionAzureQuery = String.format("sum(http_server_requests_user_total{user_id=\"%s\", path=\"/api/azure/caption\"})", userId);

*/


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
                String result = query(query, userId);

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

