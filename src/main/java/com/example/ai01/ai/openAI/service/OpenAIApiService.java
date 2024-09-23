package com.example.ai01.ai.openAI.service;

import com.example.ai01.ai.openAI.dto.request.OpenAIRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Slf4j
@Service
public class OpenAIApiService {
    @Value("${openai.api.key}")
    private String apiKey;

    private static final String OPENAI_API_URL = "https://api.openai.com/v1/chat/completions";
    private final OkHttpClient client = new OkHttpClient();

    public String enhanceWriting(OpenAIRequest.Basic openAIRequest) throws IOException {
        String jsonBody = String.format(
                "{ \"model\": \"%s\", \"messages\": [" +
                        "{ \"role\": \"system\", \"content\": \"You are a helpful assistant. Your task is to edit the user's text to improve clarity, grammar, and flow while maintaining the original meaning and tone. Do not change the context or style unless asked.\"}," +
                        "{ \"role\": \"user\", \"content\": \"%s\"}]}",
                openAIRequest.getModel(),
                openAIRequest.getPrompt()
        );

        log.info("Request Body: {}", jsonBody);

        return executeRequest(jsonBody);
    }

    public String complete(OpenAIRequest.Basic openAIRequest) throws IOException {
        String jsonBody = String.format(
                "{ \"model\": \"%s\", \"messages\": [" +
                        "{ \"role\": \"system\", \"content\": \"You are a helpful assistant.\"}," +
                        "{ \"role\": \"user\", \"content\": \"%s\"}]}",
                openAIRequest.getModel(),
                openAIRequest.getPrompt()
        );

        log.info("Request Body: {}", jsonBody);

        return executeRequest(jsonBody);
    }

    // gpt-4o-mini 고정 사용
    public String evaluateHarmfulness(String prompt) throws IOException {
        String safePrompt = prompt.replace("\"", "\\\"").replace("\n", "\\n");

        String jsonBody = String.format(
                "{\"model\": \"gpt-4o-mini\", \"messages\": [" +
                        "{ \"role\": \"system\", \"content\": \"You are a content moderation assistant. Analyze the following text for any harmful, offensive, or inappropriate content. Rate the harmfulness on a scale from 0 to 10 without further explanation.\"}," +
                        "{ \"role\": \"user\", \"content\": \"%s\"}]}",
                safePrompt
        );

        log.info("Request Body for Harmfulness Evaluation: {}", jsonBody);

        String responseBody = executeRequest(jsonBody);
        return extractHarmfulnessRating(responseBody);
    }

    private String executeRequest(String jsonBody) throws IOException {
        RequestBody body = RequestBody.create(
                MediaType.get("application/json"), jsonBody
        );

        Request request = new Request.Builder()
                .url(OPENAI_API_URL)
                .header("Authorization", "Bearer " + apiKey)
                .post(body)
                .build();

        log.info("Request: {}", request);

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected code " + response);
            }
            return response.body().string();
        }
    }

    private String extractHarmfulnessRating(String responseBody) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode rootNode = objectMapper.readTree(responseBody);
        String content = rootNode.path("choices").get(0).path("message").path("content").asText();
        String ratingText = content.replaceAll("[^0-9]", "");
        return ratingText.isEmpty() ? "0" : ratingText;
    }

    public String composeText(OpenAIRequest.TextCompose openAIRequest) throws IOException {
        String prompt = String.join(" ", openAIRequest.getSentences()).trim();

        String jsonBody = String.format(
                "{\"model\": \"%s\", \"messages\": [" +
                        "{ \"role\": \"system\", \"content\": \"You are a writing assistant. Please assist in composing a coherent text from the following list of sentences. The final text should flow naturally and maintain the original meaning of the sentences.\"}," +
                        "{ \"role\": \"user\", \"content\": \"%s\"}]}",
                openAIRequest.getModel(),
                prompt
        );

        log.info("Compose Text Request Body: {}", jsonBody);

        return executeRequest(jsonBody);
    }

    public String summarizeNews(String newsLink) throws IOException {
        String jsonBody = String.format(
                "{\"model\": \"gpt-4o\", \"messages\": [{" +
                        "\"role\": \"system\", \"content\": \"You are a competent assistant. Analyze the news link provided below and provide a concise explanation in Korean.\"}," +
                        "{\"role\": \"user\", \"content\": \"{\\\"newsLink\\\": \\\"https://zdnet.co.kr/view/?no=20240917051057\\\"}\"}]}",
                newsLink
        );

        log.info("News Link Analysis Request Body (Korean): {}", jsonBody);

        return executeRequest(jsonBody);
    }


}
