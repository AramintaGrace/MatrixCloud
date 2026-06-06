package com.matrix.cloud.controller;

import com.matrix.cloud.dto.ApiResponse;
import com.matrix.cloud.dto.CreateMeetingRequest;
import com.matrix.cloud.dto.JoinMeetingRequest;
import com.matrix.cloud.service.VideoMeetingService;
import com.matrix.cloud.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/meetings")
public class VideoMeetingController {

    @Autowired
    private VideoMeetingService meetingService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private com.matrix.cloud.service.LiveKitService liveKitService;

    private Long getUserIdFromToken(String authHeader) {
        String token = authHeader.substring(7);
        return jwtUtil.getUserIdFromToken(token);
    }

    @PostMapping
    public ApiResponse<Map<String, Object>> createMeeting(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody CreateMeetingRequest request) {
        Long userId = getUserIdFromToken(authHeader);
        Map<String, Object> meeting = meetingService.createMeeting(request, userId);
        return ApiResponse.success("会议创建成功", meeting);
    }

    @PostMapping("/join")
    public ApiResponse<Map<String, Object>> joinMeeting(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody JoinMeetingRequest request) {
        Long userId = getUserIdFromToken(authHeader);
        Map<String, Object> result = meetingService.joinMeeting(request, userId);
        return ApiResponse.success(result);
    }

    @PostMapping("/{meetingId}/end")
    public ApiResponse<Void> endMeeting(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long meetingId) {
        Long userId = getUserIdFromToken(authHeader);
        meetingService.endMeeting(meetingId, userId);
        return ApiResponse.success("会议已结束", null);
    }

    @GetMapping("/room/{roomName}")
    public ApiResponse<Map<String, Object>> getMeeting(@PathVariable String roomName) {
        Map<String, Object> meeting = meetingService.getMeeting(roomName);
        return ApiResponse.success(meeting);
    }

    @PostMapping("/token")
    public ApiResponse<Map<String, String>> getToken(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody Map<String, String> request) {
        Long userId = getUserIdFromToken(authHeader);
        String roomName = request.get("roomName");
        String identity = userId.toString();
        String name = request.getOrDefault("userName", identity);
        String token = liveKitService.createToken(roomName, identity, name);
        Map<String, String> result = Map.of("token", token, "livekitUrl", liveKitService.getLivekitUrl());
        return ApiResponse.success(result);
    }

    @GetMapping
    public ApiResponse<List<Map<String, Object>>> listActiveMeetings() {
        List<Map<String, Object>> meetings = meetingService.listActiveMeetings();
        return ApiResponse.success(meetings);
    }
}
