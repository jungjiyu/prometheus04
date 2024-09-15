package com.example.ai01.ai.groq.controller;

import com.example.ai01.ai.groq.dto.request.GroqApiRequest;
import com.example.ai01.ai.groq.service.GroqApiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("/api/groq")
public class GroqApiController {

    @Autowired
    private GroqApiService groqApiService;


    // 프롬프트 직접
    @PostMapping("/complete")
    public String complete(@RequestBody GroqApiRequest.Basic request) throws IOException {
        return groqApiService.complete(request);
    }


    // 글을 첨삭
    @PostMapping("/advice")
    public String enhanceWriting(@RequestBody GroqApiRequest.Basic request) throws IOException {
        return groqApiService.enhanceWriting(request);
    }


    //글의 유해성 판단
    @PostMapping("/harmfulness")
    public String evaluateHarmfulness(@RequestBody  GroqApiRequest.Basic request) throws IOException {
        return groqApiService.evaluateHarmfulness(request);
    }

    // 여러 문장을 하나로 합쳐 글을 작성
    @PostMapping("/compose")
    public String composeText(@RequestBody GroqApiRequest.TextCompose request) throws IOException {
        return groqApiService.composeText(request);
    }



}