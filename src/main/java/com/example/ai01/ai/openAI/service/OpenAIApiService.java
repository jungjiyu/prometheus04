package com.example.ai01.ai.openAI.service;

import com.example.ai01.ai.openAI.dto.OpenAIRequest;
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
                "{        \"model\": \"%s\",\n" +
                        "            \"messages\": [\n" +
                        "        {\n" +
                        "            \"role\": \"system\",\n" +
                        "            \"content\": \"You are a helpful assistant. Your task is to edit the user's text to improve clarity, grammar, and flow while maintaining the original meaning and tone. Do not change the context or style unless asked.\"\n" +
                        "        },\n" +
                        "        {\n" +
                        "            \"role\": \"user\",\n" +
                        "                \"content\": \"%s\"\n" +
                        "        }\n" +
                        "    ]}",
                openAIRequest.getModel(),
                openAIRequest.getPrompt()
        );

        log.info(jsonBody);

        RequestBody body = RequestBody.create(
                MediaType.get("application/json"), jsonBody
        );

        Request request = new Request.Builder()
                .url(OPENAI_API_URL)
                .header("Authorization", "Bearer " + apiKey)
                .post(body)
                .build();


        log.info("request: "+ request.toString());

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected code " + response);
            }
            return response.body().string();
        }
    }


    public String complete(OpenAIRequest.Basic openAIRequest) throws IOException {
        String jsonBody = String.format(
                "{        \"model\": \"%s\",\n" +
                        "            \"messages\": [\n" +
                        "        {\n" +
                        "            \"role\": \"system\",\n" +
                        "            \"content\": \"You are a helpful assistant.\"\n" +
                        "        },\n" +
                        "        {\n" +
                        "            \"role\": \"user\",\n" +
                        "                \"content\": \"%s\"\n" +
                        "        }\n" +
                        "    ]}",
                openAIRequest.getModel(),
                openAIRequest.getPrompt()
        );

        log.info(jsonBody);

        RequestBody body = RequestBody.create(
                MediaType.get("application/json"), jsonBody
        );

        Request request = new Request.Builder()
                .url(OPENAI_API_URL)
                .header("Authorization", "Bearer " + apiKey)
                .post(body)
                .build();


        log.info("request: "+ request.toString());

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected code " + response);
            }
            return response.body().string();
        }
    }


    //gpt-4o-mini 고정 사용
    public String evaluateHarmfulness(String prompt) throws IOException {
        log.info("jeeeeeeeee ");

        String safePrompt = prompt.replace("\"", "\\\"").replace("\n", "\\n");

        String jsonBody = String.format(
                "{\"model\": \"gpt-4o-mini\", \"messages\": [{\"role\": \"system\", \"content\": \"You are a content moderation assistant. Analyze the following text for any harmful, offensive, or inappropriate content. Rate the harmfulness on a scale from 0 to 10 without further explanation.\"}, {\"role\": \"user\", \"content\": \"%s\"}]}",
                safePrompt
        );
        log.info("jsonBody: " + jsonBody);
        RequestBody body = RequestBody.create(
                MediaType.get("application/json"), jsonBody
        );

        Request request = new Request.Builder()
                .url("https://api.openai.com/v1/chat/completions")
                .header("Authorization", "Bearer " + apiKey)
                .post(body)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected code " + response);
            }

            // API 응답을 처리하여 유해성 정도 추출
            String responseBody = response.body().string();
            log.info("resposne: "+responseBody);
            // 응답에서 유해성 점수를 추출하는 로직 추가
            String harmfulnessRating = extractHarmfulnessRating(responseBody);  // 적절한 JSON 파싱을 통해 유해성 점수 추출

            return harmfulnessRating; // 유해성 정도를 반환 (0 ~ 10 사이의 값)
        }
    }

    // JSON 응답에서 유해성 점수를 추출하는 메서드 (단순 예시)
    private String extractHarmfulnessRating(String responseBody) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode rootNode = objectMapper.readTree(responseBody);

        // 응답에서 'choices' 배열을 가져오고, 첫 번째 choice에서 메시지 내용 추출
        String content = rootNode.path("choices").get(0).path("message").path("content").asText();

        // '유해성 점수'라는 단어 뒤에 오는 숫자를 파싱
        String ratingText = content.replaceAll("[^0-9]", "");  // 숫자만 추출
        return ratingText.isEmpty() ? "0" : ratingText;  // 유해성 점수가 없으면 기본값 0
    }


    public String composeText( OpenAIRequest.TextCompose openAIRequest) throws IOException {
        StringBuilder composedText = new StringBuilder();
        for (String sentence : openAIRequest.getSentences()) {
            composedText.append(sentence).append(" ");
        }
        String prompt = composedText.toString().trim();

        String jsonBody = String.format(
                "{\"model\": \"%s\", \"messages\": [{\"role\": \"system\", \"content\": \"You are a writing assistant. Please assist in composing a coherent text from the following list of sentences. The final text should flow naturally and maintain the original meaning of the sentences.\"}, {\"role\": \"user\", \"content\": \"%s\"}]}",
                openAIRequest.getModel(),
                prompt
        );

        log.info("Compose Text Body: " + jsonBody);

        RequestBody body = RequestBody.create(
                MediaType.get("application/json"), jsonBody
        );

        Request request = new Request.Builder()
                .url(OPENAI_API_URL)
                .header("Authorization", "Bearer " + apiKey)
                .post(body)
                .build();

        log.info("request: " + request.toString());

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected code " + response);
            }
            return response.body().string();
        }
    }



}
