package com.example.ai01.ai.vllm.dto;

import lombok.*;


public class VllmRequest {

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class ImageAnalyzeRequestDTO {
            private String imageUrl;
        }

    }

