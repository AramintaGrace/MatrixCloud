package com.matrix.cloud.controller;

import com.matrix.cloud.dto.ApiResponse;
import com.matrix.cloud.entity.ChatSession;
import com.matrix.cloud.service.ChatService;
import com.matrix.cloud.service.FileStorageService;
import com.matrix.cloud.service.TeamService;
import com.matrix.cloud.util.JwtUtil;
import com.matrix.cloud.websocket.ChatWebSocketHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    @Autowired
    private ChatService chatService;

    @Autowired
    private TeamService teamService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private ChatWebSocketHandler webSocketHandler;

    @Autowired
    private FileStorageService fileStorageService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private Long getUserIdFromToken(String authHeader) {
        String token = authHeader.substring(7);
        return jwtUtil.getUserIdFromToken(token);
    }

    @PostMapping("/private")
    public ApiResponse<Map<String, Object>> createOrGetPrivateSession(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam Long targetUserId) {
        Long userId = getUserIdFromToken(authHeader);
        return ApiResponse.success(chatService.createOrGetPrivateSession(userId, targetUserId));
    }

    @GetMapping("/private/sessions")
    public ApiResponse<List<Map<String, Object>>> getPrivateSessions(
            @RequestHeader("Authorization") String authHeader) {
        Long userId = getUserIdFromToken(authHeader);
        return ApiResponse.success(chatService.getPrivateSessions(userId));
    }

    @GetMapping("/teams/{teamId}/session")
    public ApiResponse<Map<String, Object>> getTeamSession(@PathVariable Long teamId) {
        ChatSession session = chatService.getTeamSession(teamId);
        Map<String, Object> result = Map.of("sessionId", session.getId(), "teamId", session.getTeamId());
        return ApiResponse.success(result);
    }

    @GetMapping("/sessions/{sessionId}/messages")
    public ApiResponse<List<Map<String, Object>>> getMessages(@PathVariable Long sessionId,
                                                               @RequestParam(required = false) String keyword) {
        List<Map<String, Object>> messages;
        if (keyword != null && !keyword.trim().isEmpty()) {
            messages = chatService.searchMessages(sessionId, keyword.trim());
        } else {
            messages = chatService.getMessages(sessionId);
        }
        return ApiResponse.success(messages);
    }

    @PostMapping("/sessions/{sessionId}/messages")
    public ApiResponse<Map<String, Object>> sendMessage(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long sessionId,
            @RequestBody Map<String, String> request) {
        Long userId = getUserIdFromToken(authHeader);
        String content = request.get("content");
        String messageType = request.getOrDefault("messageType", "TEXT");
        String fileUrl = request.get("fileUrl");
        String fileName = request.get("fileName");
        Long fileSize = request.get("fileSize") != null ? Long.parseLong(request.get("fileSize")) : null;
        Map<String, Object> message = chatService.sendMessage(sessionId, userId, content, messageType, fileUrl, fileName, fileSize);

        try {
            List<Long> memberIds = chatService.getSessionMemberIds(sessionId);
            String json = objectMapper.writeValueAsString(Map.of(
                "type", "new_message",
                "data", message
            ));
            for (Long memberId : memberIds) {
                webSocketHandler.sendToUser(memberId, json);
            }
        } catch (Exception ignored) {
        }

        return ApiResponse.success(message);
    }

    @PostMapping("/sessions/{sessionId}/upload")
    public ApiResponse<Map<String, Object>> uploadFile(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long sessionId,
            @RequestParam("file") MultipartFile file) {
        Long userId = getUserIdFromToken(authHeader);
        String objectName = fileStorageService.uploadFile(file);
        String fileUrl = fileStorageService.getFileUrl(objectName);
        String contentType = file.getContentType();
        String messageType = "FILE";
        if (contentType != null) {
            if (contentType.startsWith("image/")) messageType = "IMAGE";
            else if (contentType.startsWith("video/")) messageType = "VIDEO";
            else if (contentType.startsWith("audio/")) messageType = "AUDIO";
        }

        Map<String, Object> message = chatService.sendMessage(sessionId, userId,
                objectName, messageType, fileUrl, file.getOriginalFilename(), file.getSize());

        try {
            List<Long> memberIds = chatService.getSessionMemberIds(sessionId);
            String json = objectMapper.writeValueAsString(Map.of(
                "type", "new_message",
                "data", message
            ));
            for (Long memberId : memberIds) {
                webSocketHandler.sendToUser(memberId, json);
            }
        } catch (Exception ignored) {
        }

        return ApiResponse.success(message);
    }

    @GetMapping("/sessions/{sessionId}/pinned")
    public ApiResponse<List<Map<String, Object>>> getPinnedMessages(@PathVariable Long sessionId) {
        return ApiResponse.success(chatService.getPinnedMessages(sessionId));
    }

    @PutMapping("/messages/{messageId}/pin")
    public ApiResponse<Map<String, Object>> pinMessage(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long messageId) {
        Long userId = getUserIdFromToken(authHeader);
        Map<String, Object> result = chatService.pinMessage(messageId, userId);
        return ApiResponse.success(result);
    }

    @PutMapping("/messages/{messageId}/unpin")
    public ApiResponse<Map<String, Object>> unpinMessage(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long messageId) {
        Long userId = getUserIdFromToken(authHeader);
        Map<String, Object> result = chatService.unpinMessage(messageId, userId);
        return ApiResponse.success(result);
    }

    @GetMapping("/files/{objectName}")
    public void getFile(@PathVariable String objectName, jakarta.servlet.http.HttpServletResponse response) {
        try (var inputStream = fileStorageService.getFile(objectName)) {
            String contentType = getContentTypeFromFilename(objectName);
            response.setContentType(contentType);
            response.setHeader("Content-Disposition", "attachment; filename=\"" + objectName.substring(objectName.indexOf('_') + 1) + "\"");
            org.springframework.util.StreamUtils.copy(inputStream, response.getOutputStream());
            response.flushBuffer();
        } catch (Exception e) {
            try {
                response.setStatus(404);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"message\":\"文件不存在或已过期\"}");
            } catch (Exception ignored) {}
        }
    }

    private String getContentTypeFromFilename(String filename) {
        String ext = filename.contains(".") ? filename.substring(filename.lastIndexOf('.')).toLowerCase() : "";
        return switch (ext) {
            case ".pdf" -> "application/pdf";
            case ".doc" -> "application/msword";
            case ".docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case ".xls" -> "application/vnd.ms-excel";
            case ".xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case ".ppt" -> "application/vnd.ms-powerpoint";
            case ".pptx" -> "application/vnd.openxmlformats-officedocument.presentationml.presentation";
            case ".jpg", ".jpeg" -> "image/jpeg";
            case ".png" -> "image/png";
            case ".gif" -> "image/gif";
            case ".svg" -> "image/svg+xml";
            case ".webp" -> "image/webp";
            case ".mp4" -> "video/mp4";
            case ".webm" -> "video/webm";
            case ".mp3" -> "audio/mpeg";
            case ".wav" -> "audio/wav";
            case ".zip" -> "application/zip";
            case ".rar" -> "application/x-rar-compressed";
            case ".7z" -> "application/x-7z-compressed";
            case ".txt" -> "text/plain";
            case ".json" -> "application/json";
            case ".xml" -> "application/xml";
            case ".html", ".htm" -> "text/html";
            default -> "application/octet-stream";
        };
    }
}
