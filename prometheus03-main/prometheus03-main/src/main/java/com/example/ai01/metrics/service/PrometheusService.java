package com.example.ai01.metrics.service;

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

import java.util.HashMap;
import java.util.Map;


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


    public String query(String query) {
        // 서비스명을 hostname으로 하여 Prometheus에 HTTP 쿼리날리기
        // Prometheus POST 요청 URL << JSON 데이터 보낼때 get 요청 쓰면  "Not enough variable values available to expand" 에러가 발생한다
        String prometheusUrl = UriComponentsBuilder.fromHttpUrl(this.prometheusUrl)
                .encode()
                .toUriString();

        // HTTP 헤더 설정
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        // 요청 본문 설정 (쿼리를 form 데이터로 전송)
        String requestBody = "query=" + query;
        HttpEntity<String> requestEntity = new HttpEntity<>(requestBody, headers);

        // POST 요청 전송
        return restTemplate.postForObject(prometheusUrl, requestEntity, String.class);
    }

    public Map<String, Object> getJsonFormatUserUsage(String userId) {
        // 각 경로별 요청 수를 구하는 Prometheus 쿼리
        String queryGroq = String.format("sum(http_server_requests_user_total{user_id=\"%s\", path=\"/api/groq/complete\"})", userId);
        String queryVllm = String.format("sum(http_server_requests_user_total{user_id=\"%s\", path=\"/api/vllm/complete\"})", userId);

        String resultGroq = query(queryGroq);
        String resultVllm = query(queryVllm);

        log.info("Prometheus /api/groq/complete 쿼리 결과: {}", resultGroq);
        log.info("Prometheus /api/vllm/complete 쿼리 결과: {}", resultVllm);

        Map<String, Object> usageData = new HashMap<>();
        double totalCost = 0.0;
        double totalRequestCount = 0.0;

        // Groq 쿼리 결과 처리
        double groqRequestCount = processQueryResult(resultGroq, "/api/groq/complete", groqCost, usageData);
        // Vllm 쿼리 결과 처리
        double vllmRequestCount = processQueryResult(resultVllm, "/api/vllm/complete", vllmCost, usageData);

        // 총 요청 수와 총 비용 계산
        totalRequestCount = groqRequestCount + vllmRequestCount;
        totalCost = (groqRequestCount * groqCost) + (vllmRequestCount * vllmCost);

        usageData.put("total_request_count", totalRequestCount);  // 전체 요청 수
        usageData.put("total_cost", totalCost);                  // 전체 비용
        usageData.put("user_id", userId);

        return usageData;
    }

    // 쿼리 결과를 처리하는 함수
    private double processQueryResult(String result, String path, double costPerRequest, Map<String, Object> usageData) {
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode rootNode;
        try {
            rootNode = objectMapper.readTree(result);
        } catch (JsonProcessingException e) {
            log.error("Error processing Prometheus response for {}: {}", path, e.getMessage());
            usageData.put("error", "Error processing Prometheus response");
            return 0.0;
        }

        JsonNode dataNode = rootNode.path("data").path("result");
        log.info("dataNode for {}: {}", path, dataNode.toString());

        double requestCount = 0.0;
        if (dataNode.isArray() && dataNode.size() > 0) {
            for (JsonNode node : dataNode) {
                JsonNode valueNode = node.path("value");
                log.info("valueNode for {}: {}", path, valueNode.toString());

                if (valueNode.isArray() && valueNode.size() > 1) {
                    requestCount = valueNode.get(1).asDouble();
                    double cost = requestCount * costPerRequest;
                    usageData.put(path + "_request_count", requestCount);  // 경로별 요청 수
                    usageData.put(path + "_cost", cost);                  // 경로별 비용
                } else {
                    log.warn("valueNode가 예상과 다른 형식입니다 for {}: {}", path, valueNode.toString());
                }
            }
        } else {
            log.warn("No data found for path: {}", path);
            usageData.put(path + "_request_count", 0);
            usageData.put(path + "_cost", 0.0);
        }

        return requestCount;
    }

}