package com.matrix.cloud.dto;

import lombok.Data;

@Data
public class AsrTranscriptionRequest {
    private String taskId;
    private Long meetingId;
    private String status;
    private String result;
}
