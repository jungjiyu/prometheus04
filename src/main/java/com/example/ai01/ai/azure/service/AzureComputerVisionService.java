package com.example.ai01.ai.azure.service;

import com.example.ai01.ai.groq.dto.GroqApiRequest;
import com.example.ai01.ai.groq.service.GroqApiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.HashMap;
import java.util.Map;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;



@RequiredArgsConstructor
@Slf4j
@Service
public class AzureComputerVisionService {

    @Value("${azure.resource.name}")
    private String resourceName;

    @Value("${azure.api.version}")
    private String apiVersion;

    @Value("${azure.api.key}")
    private String apiKey;

    private final GroqApiService groqApiService;



    public String createImageCaption(String imageUrl) {

        String endpoint =
                "https://" + resourceName + ".cognitiveservices.azure.com/computervision/imageanalysis:analyze";

        log.info("Created endpoint: " + endpoint);

        // 헤더 설정
        HttpHeaders headers = new HttpHeaders();
        headers.set("Ocp-Apim-Subscription-Key", apiKey);
        headers.set("Content-Type", "application/json");

        // 바디 설정
        Map<String, Object> body = new HashMap<>();
        body.put("url", imageUrl);
        body.put("features", new String[]{"caption"});
        body.put("language", "en"); // 현재는 영어로만 이용 가능

        // JSON 변환
        ObjectMapper objectMapper = new ObjectMapper();
        String jsonBody = null;
        try {
            jsonBody = objectMapper.writeValueAsString(body);
            log.info("Created body: " + jsonBody);
        } catch (JsonProcessingException e) {
            log.error("Error creating JSON body", e);
            return "JSON 변환 오류가 발생했습니다.";
        }

        HttpEntity<String> request = new HttpEntity<>(jsonBody, headers);

        // URL 설정 (API 버전 및 기능)
        UriComponentsBuilder uriBuilder = UriComponentsBuilder
                .fromHttpUrl(endpoint)
                .queryParam("api-version", apiVersion)
                .queryParam("features", "caption");

        // API 호출
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<Map> response = restTemplate.exchange(
                uriBuilder.toUriString(),
                HttpMethod.POST,
                request,
                Map.class
        );

        if (response.getStatusCode() == HttpStatus.OK) {
            Map<String, Object> result = response.getBody();
            if (result != null) {
                Map<String, Object> captionResult = (Map<String, Object>) result.get("captionResult");
                return (String) captionResult.get("text");
            }
        }

        return "이미지 설명을 가져올 수 없습니다.";
    }


    public String createKoreanImageCaptionWithGroq(String imageUrl) {

        String engCaption = this.createImageCaption(imageUrl);
        GroqApiRequest.Basic groqApiRequest = GroqApiRequest.Basic.builder().
                prompt("Please translate \'"+engCaption+"\' into Korean without any explanation.").
                modelType("llama-3.1-70b-versatile").
                build();

        log.info("groqAPIRequest: "+groqApiRequest.toString());
        String result = groqApiService.complete(groqApiRequest);
        return result;

    }

}
