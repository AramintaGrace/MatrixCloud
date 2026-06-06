package com.matrix.cloud.service;

import com.matrix.cloud.entity.ChatMessage;
import com.matrix.cloud.entity.ChatParticipant;
import com.matrix.cloud.entity.ChatSession;
import com.matrix.cloud.entity.TeamMember;
import com.matrix.cloud.entity.User;
import com.matrix.cloud.repository.ChatMessageRepository;
import com.matrix.cloud.repository.ChatParticipantRepository;
import com.matrix.cloud.repository.ChatSessionRepository;
import com.matrix.cloud.repository.TeamMemberRepository;
import com.matrix.cloud.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ChatService {

    @Autowired
    private ChatMessageRepository messageRepository;

    @Autowired
    private ChatSessionRepository sessionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TeamMemberRepository teamMemberRepository;

    @Autowired
    private ChatParticipantRepository participantRepository;

    public ChatSession getTeamSession(Long teamId) {
        return sessionRepository.findByTeamId(teamId)
                .orElseThrow(() -> new RuntimeException("团队聊天室不存在"));
    }

    public List<Map<String, Object>> getMessages(Long sessionId) {
        List<ChatMessage> messages = messageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId);
        return messages.stream().map(this::formatMessage).toList();
    }

    @Transactional
    public Map<String, Object> sendMessage(Long sessionId, Long senderId, String content, String messageType,
                                            String fileUrl, String fileName, Long fileSize) {
        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        ChatMessage message = new ChatMessage();
        message.setSessionId(sessionId);
        message.setSenderId(senderId);
        message.setContent(content);
        message.setMessageType(ChatMessage.MessageType.valueOf(messageType.toUpperCase()));
        message.setFileUrl(fileUrl);
        message.setFileName(fileName);
        message.setFileSize(fileSize);
        message.setCreatedAt(LocalDateTime.now());
        messageRepository.save(message);

        return formatMessage(message);
    }

    public List<Map<String, Object>> searchMessages(Long sessionId, String keyword) {
        List<ChatMessage> messages = messageRepository
                .findBySessionIdAndContentContainingIgnoreCaseOrderByCreatedAtAsc(sessionId, keyword);
        return messages.stream().map(this::formatMessage).toList();
    }

    public List<Long> getSessionMemberIds(Long sessionId) {
        ChatSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("聊天室不存在"));
        if (session.getType() == ChatSession.Type.PRIVATE) {
            return participantRepository.findBySessionId(sessionId)
                    .stream().map(ChatParticipant::getUserId).toList();
        }
        if (session.getTeamId() == null) return List.of();
        return teamMemberRepository.findByTeamId(session.getTeamId())
                .stream().map(TeamMember::getUserId).toList();
    }

    @Transactional
    public Map<String, Object> pinMessage(Long messageId, Long userId) {
        ChatMessage msg = messageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("消息不存在"));
        checkIsTeamAdmin(msg.getSessionId(), userId);
        msg.setPinned(true);
        messageRepository.save(msg);
        return formatMessage(msg);
    }

    @Transactional
    public Map<String, Object> unpinMessage(Long messageId, Long userId) {
        ChatMessage msg = messageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("消息不存在"));
        checkIsTeamAdmin(msg.getSessionId(), userId);
        msg.setPinned(false);
        messageRepository.save(msg);
        return formatMessage(msg);
    }

    private void checkIsTeamAdmin(Long sessionId, Long userId) {
        ChatSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("聊天室不存在"));
        if (session.getTeamId() == null) return;
        TeamMember member = teamMemberRepository.findByTeamIdAndUserId(session.getTeamId(), userId)
                .orElseThrow(() -> new RuntimeException("您不是团队成员"));
        if (member.getRole() != TeamMember.Role.ADMIN) {
            throw new RuntimeException("只有管理员可以置顶消息");
        }
    }

    public List<Map<String, Object>> getPinnedMessages(Long sessionId) {
        return messageRepository.findBySessionIdAndPinnedTrueOrderByCreatedAtAsc(sessionId)
                .stream().map(this::formatMessage).toList();
    }

    @Transactional
    public Map<String, Object> createOrGetPrivateSession(Long userId, Long targetUserId) {
        if (userId.equals(targetUserId)) {
            throw new RuntimeException("不能和自己聊天");
        }

        // Find existing private session between these two users
        List<Long> commonIds = participantRepository.findCommonPrivateSessionIds(userId, targetUserId);
        for (Long sid : commonIds) {
            ChatSession session = sessionRepository.findById(sid).orElse(null);
            if (session != null && session.getType() == ChatSession.Type.PRIVATE) {
                User target = userRepository.findById(targetUserId).orElse(null);
                Map<String, Object> result = new HashMap<>();
                result.put("sessionId", session.getId());
                result.put("targetUserId", targetUserId);
                result.put("targetName", target != null ? target.getNickname() : "未知用户");
                return result;
            }
        }

        // Create new private session
        ChatSession session = new ChatSession();
        session.setType(ChatSession.Type.PRIVATE);
        session = sessionRepository.save(session);

        ChatParticipant p1 = new ChatParticipant();
        p1.setSessionId(session.getId());
        p1.setUserId(userId);
        participantRepository.save(p1);

        ChatParticipant p2 = new ChatParticipant();
        p2.setSessionId(session.getId());
        p2.setUserId(targetUserId);
        participantRepository.save(p2);

        User target = userRepository.findById(targetUserId).orElse(null);
        Map<String, Object> result = new HashMap<>();
        result.put("sessionId", session.getId());
        result.put("targetUserId", targetUserId);
        result.put("targetName", target != null ? target.getNickname() : "未知用户");
        return result;
    }

    public List<Map<String, Object>> getPrivateSessions(Long userId) {
        List<ChatParticipant> myParts = participantRepository.findByUserId(userId);
        return myParts.stream().map(cp -> {
            ChatSession session = sessionRepository.findById(cp.getSessionId()).orElse(null);
            if (session == null || session.getType() != ChatSession.Type.PRIVATE) return null;
            List<ChatParticipant> others = participantRepository.findBySessionId(session.getId())
                    .stream().filter(p -> !p.getUserId().equals(userId)).toList();
            if (others.isEmpty()) return null;
            Long otherUserId = others.get(0).getUserId();
            User other = userRepository.findById(otherUserId).orElse(null);
            ChatMessage lastMsg = messageRepository.findTop50BySessionIdOrderByCreatedAtDesc(session.getId())
                    .stream().findFirst().orElse(null);
            Map<String, Object> map = new HashMap<>();
            map.put("sessionId", session.getId());
            map.put("targetUserId", otherUserId);
            map.put("targetName", other != null ? other.getNickname() : "未知用户");
            map.put("lastMessage", lastMsg != null ? lastMsg.getContent() : "");
            map.put("lastTime", lastMsg != null ? lastMsg.getCreatedAt().format(DateTimeFormatter.ofPattern("MM-dd HH:mm")) : "");
            return map;
        }).filter(m -> m != null).toList();
    }

    private Map<String, Object> formatMessage(ChatMessage msg) {
        User sender = userRepository.findById(msg.getSenderId()).orElse(null);
        Map<String, Object> map = new HashMap<>();
        map.put("id", msg.getId());
        map.put("sessionId", msg.getSessionId());
        map.put("senderId", msg.getSenderId());
        map.put("senderName", sender != null ? sender.getNickname() : "未知用户");
        map.put("content", msg.getContent());
        map.put("messageType", msg.getMessageType().name());
        map.put("fileUrl", msg.getFileUrl());
        map.put("fileName", msg.getFileName());
        map.put("fileSize", msg.getFileSize());
        map.put("pinned", Boolean.TRUE.equals(msg.getPinned()));
        map.put("time", msg.getCreatedAt().format(DateTimeFormatter.ofPattern("HH:mm")));
        map.put("createdAt", msg.getCreatedAt().toString());
        return map;
    }
}
