package com.matrix.cloud.controller;

import com.matrix.cloud.dto.ApiResponse;
import com.matrix.cloud.entity.MeetingParticipant;
import com.matrix.cloud.entity.MeetingSummary;
import com.matrix.cloud.entity.VideoMeeting;
import com.matrix.cloud.repository.MeetingParticipantRepository;
import com.matrix.cloud.repository.VideoMeetingRepository;
import com.matrix.cloud.service.MeetingSummaryManager;
import com.matrix.cloud.service.MeetingTranscriptService;
import com.matrix.cloud.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/meetings")
public class MeetingTranscriptController {

    @Autowired
    private MeetingTranscriptService transcriptService;

    @Autowired
    private MeetingSummaryManager summaryManager;

    @Autowired
    private MeetingParticipantRepository participantRepository;

    @Autowired
    private VideoMeetingRepository meetingRepository;

    @Autowired
    private JwtUtil jwtUtil;

    private Long getUserIdFromToken(String authHeader) {
        String token = authHeader.substring(7);
        return jwtUtil.getUserIdFromToken(token);
    }

    @PostMapping("/{meetingId}/transcripts")
    public ApiResponse<Map<String, Object>> uploadTranscript(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long meetingId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "timestamp", required = false) String timestamp) {
        Long userId = getUserIdFromToken(authHeader);
        Map<String, Object> result = transcriptService.saveTranscript(meetingId, userId, file, timestamp);
        return ApiResponse.success("语音转文本保存成功", result);
    }

    @GetMapping("/{meetingId}/transcripts")
    public ApiResponse<List<Map<String, Object>>> getTranscripts(
            @PathVariable Long meetingId) {
        List<Map<String, Object>> transcripts = transcriptService.getTranscripts(meetingId);
        return ApiResponse.success(transcripts);
    }

    @GetMapping("/history")
    public ApiResponse<List<Map<String, Object>>> getMeetingHistory(
            @RequestHeader("Authorization") String authHeader) {
        Long userId = getUserIdFromToken(authHeader);
        List<Map<String, Object>> history = transcriptService.getMeetingHistory(userId);
        return ApiResponse.success(history);
    }

    @GetMapping("/{meetingId}/history")
    public ApiResponse<Map<String, Object>> getMeetingHistoryDetail(
            @PathVariable Long meetingId) {
        Map<String, Object> detail = transcriptService.getMeetingHistoryDetail(meetingId);
        return ApiResponse.success(detail);
    }

    @PostMapping("/{meetingId}/transcripts/generate-summary")
    public ApiResponse<Map<String, Object>> generateSummaryFromTranscripts(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long meetingId) {
        Long userId = getUserIdFromToken(authHeader);
        List<Map<String, Object>> transcripts = transcriptService.getTranscripts(meetingId);

        String combinedText = transcripts.stream()
                .map(t -> {
                    String ts = t.get("timestamp") != null ? "(" + t.get("timestamp") + ")" : "";
                    return "说话人 " + t.get("nickname") + " " + ts + ": " + t.get("content");
                })
                .collect(Collectors.joining("\n"));

        if (combinedText.trim().isEmpty()) {
            return ApiResponse.error("该会议没有语音记录");
        }

        MeetingSummary summary = summaryManager.generateSummary(meetingId, userId, combinedText);

        Map<String, Object> data = new java.util.HashMap<>();
        data.put("id", summary.getId());
        data.put("meetingId", summary.getMeetingId());
        data.put("summaryText", summary.getSummaryText());
        data.put("chunkCount", summary.getChunkCount());
        data.put("modelName", summary.getModelName());
        data.put("status", summary.getStatus().name());
        data.put("createdAt", summary.getCreatedAt().toString());
        return ApiResponse.success("会议纪要生成成功", data);
    }

    @GetMapping("/summaries/history")
    public ApiResponse<List<Map<String, Object>>> getSummaryHistory(
            @RequestHeader("Authorization") String authHeader) {
        Long userId = getUserIdFromToken(authHeader);

        List<Long> participantMeetingIds = participantRepository.findByUserId(userId)
                .stream().map(MeetingParticipant::getMeetingId).toList();
        List<Long> createdMeetingIds = meetingRepository.findByCreatorId(userId)
                .stream().map(VideoMeeting::getId).toList();

        List<Long> allMeetingIds = new ArrayList<>(participantMeetingIds);
        for (Long id : createdMeetingIds) {
            if (!allMeetingIds.contains(id)) allMeetingIds.add(id);
        }

        if (allMeetingIds.isEmpty()) {
            return ApiResponse.success(List.of());
        }

        List<Map<String, Object>> history = summaryManager.getSummaryHistory(allMeetingIds);

        // Enrich with meeting room names
        for (Map<String, Object> item : history) {
            Long meetingId = (Long) item.get("meetingId");
            meetingRepository.findById(meetingId).ifPresent(m -> {
                item.put("roomName", m.getRoomName());
            });
        }

        return ApiResponse.success(history);
    }

    @GetMapping("/records")
    public ApiResponse<Map<String, Object>> getMeetingRecords(
            @RequestHeader("Authorization") String authHeader) {
        Long userId = getUserIdFromToken(authHeader);
        Map<String, Object> records = transcriptService.getMeetingRecords(userId);
        return ApiResponse.success(records);
    }
}
