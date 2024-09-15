package com.example.ai01.ai.openAI.dto;

import lombok.*;

import java.util.List;


public class OpenAIRequest {


    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Basic {
        private String model;
        private String prompt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TextCompose {
        private String model;
        private List<String> sentences;
    }



}
