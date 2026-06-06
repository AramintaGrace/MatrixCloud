package com.matrix.cloud.controller;

import com.matrix.cloud.dto.ApiResponse;
import com.matrix.cloud.dto.GenerateSummaryRequest;
import com.matrix.cloud.entity.MeetingSummary;
import com.matrix.cloud.service.MeetingSummaryManager;
import com.matrix.cloud.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/meetings/{meetingId}/summary")
public class MeetingSummaryController {

    @Autowired
    private MeetingSummaryManager summaryManager;

    @Autowired
    private JwtUtil jwtUtil;

    private Long getUserIdFromToken(String authHeader) {
        String token = authHeader.substring(7);
        return jwtUtil.getUserIdFromToken(token);
    }

    @PostMapping
    public ApiResponse<Map<String, Object>> generateSummary(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long meetingId,
            @RequestBody GenerateSummaryRequest request) {
        Long userId = getUserIdFromToken(authHeader);
        MeetingSummary summary = summaryManager.generateSummary(
                meetingId, userId, request.getTranscriptText());

        Map<String, Object> data = toSummaryMap(summary);
        return ApiResponse.success("会议纪要生成成功", data);
    }

    @GetMapping
    public ApiResponse<Map<String, Object>> getLatestSummary(
            @PathVariable Long meetingId) {
        MeetingSummary summary = summaryManager.getLatestSummary(meetingId);
        return ApiResponse.success(toSummaryMap(summary));
    }

    private Map<String, Object> toSummaryMap(MeetingSummary s) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", s.getId());
        map.put("meetingId", s.getMeetingId());
        map.put("summaryText", s.getSummaryText());
        map.put("chunkCount", s.getChunkCount());
        map.put("modelName", s.getModelName());
        map.put("status", s.getStatus().name());
        map.put("createdAt", s.getCreatedAt().toString());
        return map;
    }
}
