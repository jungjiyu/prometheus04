package com.example.ai01.ai.vllm.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;

@RequiredArgsConstructor
@Service
public class VllmService {

    private final RestTemplate restTemplate;

    @Value("${vllm.base.url}")
    private String flaskApiUrl;


    public String analyzeImage(String imageUrl) throws IOException {
        // URL로부터 이미지 다운로드
        byte[] imageBytes = restTemplate.getForObject(imageUrl, byte[].class);

        // 다운로드 받은 이미지를 ByteArrayResource로 변환
        ByteArrayResource imageResource = new ByteArrayResource(imageBytes) {
            @Override
            public String getFilename() {
                return "image.jpg"; // 파일명을 지정
            }
        };

        // Flask 서버로 요청 전송
        String url = flaskApiUrl + "/analyze";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("image", imageResource);
//        body.add("instruction", "이미지에 대해 설명해주세요");

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
        ResponseEntity<String> response = restTemplate.postForEntity(url, requestEntity, String.class);

        return response.getBody();
    }
}