package com.matrix.cloud.dto;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class SaveTranscriptRequest {
    private String timestamp;
}
