package com.matrix.cloud.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.matrix.cloud.dto.ApiResponse;
import com.matrix.cloud.service.SpeechToTextService;

@RestController
@RequestMapping("/api/stt")
public class SttTestController {

    @Autowired
    private SpeechToTextService speechToTextService;

    @PostMapping("/test")
    public ApiResponse<Map<String, String>> testTranscribe(
            @RequestParam("file") MultipartFile file) {
        String text = speechToTextService.transcribe(file);
        return ApiResponse.success("转写成功", Map.of("text", text));
    }
}
