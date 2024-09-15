package com.example.ai01.ai.openAI.controller;

import com.example.ai01.ai.openAI.dto.OpenAIRequest;
import com.example.ai01.ai.openAI.service.OpenAIApiService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/openai")
public class OpenAIApiController {

    private final OpenAIApiService openAIService;


    // 프롬프트 직접
    @PostMapping("/complete")
    public String complete(@RequestBody OpenAIRequest.Basic openAIRequest) throws IOException {
        return openAIService.complete(openAIRequest);
    }


    // 글을 첨삭
    @PostMapping("/advice")
    public String enhanceWriting(@RequestBody OpenAIRequest.Basic openAIRequest) throws IOException {
        return openAIService.enhanceWriting(openAIRequest);
    }
    

    //글의 유해성 판단
    @PostMapping("/harmfulness")
    public String evaluateHarmfulness(@RequestBody String prompt) throws IOException {
        return openAIService.evaluateHarmfulness(prompt);
    }

    // 여러 문장을 하나로 합쳐 글을 작성
    @PostMapping("/compose")
    public String composeText(@RequestBody  OpenAIRequest.TextCompose request) throws IOException {
        return openAIService.composeText(request);
    }




}
