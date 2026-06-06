package com.matrix.cloud.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.util.Map;

@Controller
public class WebRTCSignalingController {

    private static final Logger log = LoggerFactory.getLogger(WebRTCSignalingController.class);

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/webrtc/join")
    public void handleJoin(@Payload Map<String, Object> message) {
        String roomId = String.valueOf(message.get("roomId"));
        log.info("User {} joined room {}", message.get("userId"), roomId);
        messagingTemplate.convertAndSend("/topic/room/" + roomId + "/user-joined", message);
    }

    @MessageMapping("/webrtc/leave")
    public void handleLeave(@Payload Map<String, Object> message) {
        String roomId = String.valueOf(message.get("roomId"));
        log.info("User {} left room {}", message.get("userId"), roomId);
        messagingTemplate.convertAndSend("/topic/room/" + roomId + "/user-left", message);
    }

    @MessageMapping("/webrtc/offer")
    public void handleOffer(@Payload Map<String, Object> message) {
        String roomId = String.valueOf(message.get("roomId"));
        log.info("Offer from {} in room {}", message.get("from"), roomId);
        messagingTemplate.convertAndSend("/topic/room/" + roomId + "/offer", message);
    }

    @MessageMapping("/webrtc/answer")
    public void handleAnswer(@Payload Map<String, Object> message) {
        String roomId = String.valueOf(message.get("roomId"));
        log.info("Answer from {} in room {}", message.get("from"), roomId);
        messagingTemplate.convertAndSend("/topic/room/" + roomId + "/answer", message);
    }

    @MessageMapping("/webrtc/ice-candidate")
    public void handleIceCandidate(@Payload Map<String, Object> message) {
        String roomId = String.valueOf(message.get("roomId"));
        messagingTemplate.convertAndSend("/topic/room/" + roomId + "/ice-candidate", message);
    }

    @MessageMapping("/webrtc/screen-share-started")
    public void handleScreenShareStarted(@Payload Map<String, Object> message) {
        String roomId = String.valueOf(message.get("roomId"));
        log.info("Screen share started by {} in room {}", message.get("from"), roomId);
        messagingTemplate.convertAndSend("/topic/room/" + roomId + "/screen-share-started", message);
    }

    @MessageMapping("/webrtc/screen-share-stopped")
    public void handleScreenShareStopped(@Payload Map<String, Object> message) {
        String roomId = String.valueOf(message.get("roomId"));
        log.info("Screen share stopped by {} in room {}", message.get("from"), roomId);
        messagingTemplate.convertAndSend("/topic/room/" + roomId + "/screen-share-stopped", message);
    }
}
