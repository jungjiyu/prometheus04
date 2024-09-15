package com.example.ai01.ai.groq.service;

import com.example.ai01.ai.groq.dto.request.GroqApiRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;


@Slf4j
@Service
public class GroqApiService {

    @Autowired
    private RestTemplate restTemplate;

    @Value("${groq.api.key}")
    private String apiKey;

    public String complete( GroqApiRequest.Basic  request) {
        String url = "https://api.groq.com/openai/v1/chat/completions";

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + apiKey);
        headers.set("Content-Type", "application/json");

        String requestJson = "{\"messages\": [{\"role\": \"user\", \"content\": \"" + request.getPrompt() + "\"}], \"model\": \"" + request.getModelType() + "\"}";
        HttpEntity<String> entity = new HttpEntity<>(requestJson, headers);

        return restTemplate.exchange(url, HttpMethod.POST, entity, String.class).getBody();
    }


    public String enhanceWriting( GroqApiRequest.Basic  request) {
        String url = "https://api.groq.com/openai/v1/chat/completions";

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + apiKey);
        headers.set("Content-Type", "application/json");

        String requestJson = "{\"messages\": [{\"role\": \"user\", \"content\": \"\'" + request.getPrompt() + "\'이 글을 좀 다듬어서 다시 작성해줘.\"}], \"model\": \"" + request.getModelType() + "\"}";
        HttpEntity<String> entity = new HttpEntity<>(requestJson, headers);
        log.info("requestJson: "+ requestJson);

        return restTemplate.exchange(url, HttpMethod.POST, entity, String.class).getBody();
    }

    public String evaluateHarmfulness( GroqApiRequest.Basic  request) {
        String url = "https://api.groq.com/openai/v1/chat/completions";

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + apiKey);
        headers.set("Content-Type", "application/json");

        String requestJson = "{\"messages\": [{\"role\": \"user\", \"content\": \" \'" + request.getPrompt() + "\' 이 글의 유해성 정도를 별도의 설명없이 0~10 의 숫자로만 반환해줘.\"}], \"model\": \"" + request.getModelType() + "\"}";
        HttpEntity<String> entity = new HttpEntity<>(requestJson, headers);

        return restTemplate.exchange(url, HttpMethod.POST, entity, String.class).getBody();
    }

    public String composeText(GroqApiRequest.TextCompose request) {
        List<String> sentences = request.getSentences();
        String modelType = request.getModelType();



        // Concatenate all sentences into a single prompt
        StringBuilder composedText = new StringBuilder();
        for (String sentence : sentences) {
            composedText.append(sentence).append(" ");
        }

        // Create a new basic request using the concatenated text
        String prompt = composedText.toString().trim();


        GroqApiRequest.Basic basicRequest = new GroqApiRequest.Basic(prompt, modelType);

        // Use the existing complete method to send the request
        return enhanceWriting(basicRequest);
    }





}