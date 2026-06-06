package com.matrix.cloud.controller;

import com.matrix.cloud.dto.ApiResponse;
import com.matrix.cloud.dto.AsrTranscriptionRequest;
import com.matrix.cloud.entity.MeetingSummary;
import com.matrix.cloud.entity.VideoMeeting;
import com.matrix.cloud.repository.MeetingSummaryRepository;
import com.matrix.cloud.repository.VideoMeetingRepository;
import com.matrix.cloud.service.MeetingSummaryManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

@RestController
@RequestMapping("/api/asr")
public class AsrWebhookController {

    private static final Logger log = LoggerFactory.getLogger(AsrWebhookController.class);

    @Autowired
    private MeetingSummaryManager summaryManager;

    @Autowired
    private VideoMeetingRepository meetingRepository;

    @Autowired
    private MeetingSummaryRepository summaryRepository;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    @Qualifier("meetingSummaryExecutor")
    private ExecutorService executor;

    @PostMapping("/webhook")
    public ApiResponse<Map<String, Object>> handleTranscription(
            @RequestBody AsrTranscriptionRequest request) {
        log.info("ASR webhook received: taskId={}, meetingId={}, status={}",
                request.getTaskId(), request.getMeetingId(), request.getStatus());

        if (!"SUCCESS".equalsIgnoreCase(request.getStatus())) {
            return ApiResponse.error("ASR task not successful: " + request.getStatus());
        }

        if (request.getResult() == null || request.getResult().trim().isEmpty()) {
            return ApiResponse.error("Transcription result is empty");
        }

        VideoMeeting meeting = meetingRepository.findById(request.getMeetingId())
                .orElseThrow(() -> new RuntimeException("会议不存在: " + request.getMeetingId()));

        Map<String, Object> response = new HashMap<>();
        response.put("taskId", request.getTaskId());
        response.put("meetingId", request.getMeetingId());
        response.put("message", "转写结果已接收，正在异步生成会议纪要");

        CompletableFuture.runAsync(() -> {
            try {
                log.info("Starting async summary generation for meeting {}", request.getMeetingId());
                MeetingSummary summary = summaryManager.generateSummary(
                        request.getMeetingId(),
                        meeting.getCreatorId(),
                        request.getResult()
                );

                Map<String, Object> notification = new HashMap<>();
                notification.put("type", "summary-completed");
                notification.put("summaryId", summary.getId());
                notification.put("meetingId", summary.getMeetingId());
                notification.put("status", summary.getStatus().name());
                notification.put("chunkCount", summary.getChunkCount());
                notification.put("message", "会议纪要已生成，点击查看");

                messagingTemplate.convertAndSend(
                        "/topic/room/" + request.getMeetingId() + "/summary-completed",
                        notification
                );
                log.info("Summary completed and notification sent for meeting {}", request.getMeetingId());
            } catch (Exception e) {
                log.error("Async summary generation failed for meeting {}: {}",
                        request.getMeetingId(), e.getMessage());

                Map<String, Object> errorNotification = new HashMap<>();
                errorNotification.put("type", "summary-failed");
                errorNotification.put("meetingId", request.getMeetingId());
                errorNotification.put("message", "会议纪要生成失败: " + e.getMessage());

                messagingTemplate.convertAndSend(
                        "/topic/room/" + request.getMeetingId() + "/summary-completed",
                        errorNotification
                );
            }
        }, executor);

        return ApiResponse.success(response);
    }
}
