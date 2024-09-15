package com.example.ai01.ai.vllm.controller;

import com.example.ai01.ai.vllm.dto.request.VllmRequest;
import com.example.ai01.ai.vllm.service.VllmService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@Slf4j
@RestController
@RequestMapping("/api/vllm")
public class VllmController {

    private final VllmService vllmService;

    public VllmController(VllmService vllmService) {
        this.vllmService = vllmService;
    }

    @PostMapping("/analyze")
    public ResponseEntity<String> analyzeImage(@RequestBody VllmRequest.ImageAnalyzeRequestDTO imageUrlDTO) throws IOException {
        log.info("got imageurl: "+imageUrlDTO.getImageUrl());
        String result = vllmService.analyzeImage(imageUrlDTO.getImageUrl());
        log.info("got result: "+result);
        return ResponseEntity.ok(result);
    }
}