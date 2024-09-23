package com.example.ai01.ai.azure.controller;

import com.example.ai01.ai.azure.service.AzureComputerVisionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RequestMapping("/api/azure")
@RequiredArgsConstructor
@RestController
public class AzureComputerVisionController {

    private final AzureComputerVisionService azureComputerVisionService;

    @PostMapping("/caption")
    public String createImageCaption(@RequestParam("imageUrl") String imageUrl,
                                     @RequestParam(value = "lang", defaultValue = "en") String lang) {
        if ("kr".equalsIgnoreCase(lang)) {
            return azureComputerVisionService.createKoreanImageCaptionWithGroq(imageUrl);
        }
        return azureComputerVisionService.createImageCaption(imageUrl);
    }




}