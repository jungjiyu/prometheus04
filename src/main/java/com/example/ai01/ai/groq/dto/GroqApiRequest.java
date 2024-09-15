package com.example.ai01.ai.groq.dto;

import lombok.*;

import java.util.List;


public class GroqApiRequest {


    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Basic {
        private String prompt;
        private String modelType;
    }


    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TextCompose {
        private List<String> sentences;
        private String modelType;
    }

}
