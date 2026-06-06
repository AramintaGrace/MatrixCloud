package com.matrix.cloud.service;

import com.matrix.cloud.dto.CreateMeetingRequest;
import com.matrix.cloud.dto.JoinMeetingRequest;
import com.matrix.cloud.entity.MeetingParticipant;
import com.matrix.cloud.entity.MeetingTranscript;
import com.matrix.cloud.entity.User;
import com.matrix.cloud.entity.VideoMeeting;
import com.matrix.cloud.exception.ResourceNotFoundException;
import com.matrix.cloud.repository.MeetingParticipantRepository;
import com.matrix.cloud.repository.MeetingTranscriptRepository;
import com.matrix.cloud.repository.UserRepository;
import com.matrix.cloud.repository.VideoMeetingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class VideoMeetingService {

    @Autowired
    private VideoMeetingRepository meetingRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MeetingParticipantRepository participantRepository;

    @Autowired
    private MeetingTranscriptRepository transcriptRepository;

    @Autowired
    private MeetingSummaryManager summaryManager;

    @Transactional
    public Map<String, Object> createMeeting(CreateMeetingRequest request, Long creatorId) {
        User creator = userRepository.findById(creatorId)
                .orElseThrow(() -> new ResourceNotFoundException("用户不存在"));

        if (request.getRoomName() == null || request.getRoomName().trim().isEmpty()) {
            throw new RuntimeException("会议名称不能为空");
        }

        if (meetingRepository.findByRoomNameAndStatus(request.getRoomName().trim(), VideoMeeting.Status.ACTIVE).isPresent()) {
            throw new RuntimeException("会议名称已被使用中");
        }

        VideoMeeting meeting = new VideoMeeting();
        meeting.setRoomName(request.getRoomName().trim());
        meeting.setCreatorId(creatorId);
        meeting.setLivekitRoomId(UUID.randomUUID().toString().replace("-", ""));
        meeting.setStatus(VideoMeeting.Status.ACTIVE);

        // Parse meeting type from request, default to NORMAL
        if (request.getMeetingType() != null && "AI_SMART".equalsIgnoreCase(request.getMeetingType())) {
            meeting.setMeetingType(VideoMeeting.MeetingType.AI_SMART);
        } else {
            meeting.setMeetingType(VideoMeeting.MeetingType.NORMAL);
        }

        meeting.setCreatedAt(LocalDateTime.now());

        if (request.getPassword() != null && !request.getPassword().trim().isEmpty()) {
            meeting.setPassword(request.getPassword().trim());
        }

        meetingRepository.save(meeting);

        MeetingParticipant participant = new MeetingParticipant();
        participant.setMeetingId(meeting.getId());
        participant.setUserId(creatorId);
        participant.setRole(MeetingParticipant.Role.ADMIN);
        participantRepository.save(participant);

        Map<String, Object> result = new HashMap<>();
        result.put("id", meeting.getId());
        result.put("roomName", meeting.getRoomName());
        result.put("livekitRoomId", meeting.getLivekitRoomId());
        result.put("hasPassword", meeting.getPassword() != null);
        result.put("status", meeting.getStatus().name());
        result.put("meetingType", meeting.getMeetingType().name());
        result.put("creatorId", creatorId);
        result.put("creatorName", creator.getNickname());
        result.put("createdAt", meeting.getCreatedAt().toString());
        return result;
    }

    public Map<String, Object> joinMeeting(JoinMeetingRequest request, Long userId) {
        if (request.getRoomName() == null || request.getRoomName().trim().isEmpty()) {
            throw new RuntimeException("请输入会议名称");
        }

        VideoMeeting meeting = meetingRepository.findByRoomNameAndStatus(request.getRoomName().trim(), VideoMeeting.Status.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("会议不存在或已结束"));

        if (meeting.getStatus() == VideoMeeting.Status.ENDED) {
            throw new RuntimeException("会议已结束");
        }

        boolean isCreator = meeting.getCreatorId().equals(userId);

        if (!isCreator && meeting.getPassword() != null && !meeting.getPassword().isEmpty()) {
            if (request.getPassword() == null || !request.getPassword().equals(meeting.getPassword())) {
                throw new RuntimeException("会议密码错误");
            }
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("用户不存在"));

        if (!participantRepository.existsByMeetingIdAndUserId(meeting.getId(), userId)) {
            MeetingParticipant participant = new MeetingParticipant();
            participant.setMeetingId(meeting.getId());
            participant.setUserId(userId);
            participant.setRole(MeetingParticipant.Role.PARTICIPANT);
            participantRepository.save(participant);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("id", meeting.getId());
        result.put("roomName", meeting.getRoomName());
        result.put("livekitRoomId", meeting.getLivekitRoomId());
        result.put("creatorId", meeting.getCreatorId());
        result.put("status", meeting.getStatus().name());
        result.put("meetingType", meeting.getMeetingType().name());
        result.put("userName", user.getNickname());
        return result;
    }

    @Transactional
    public void endMeeting(Long meetingId, Long userId) {
        VideoMeeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new ResourceNotFoundException("会议不存在"));

        if (!meeting.getCreatorId().equals(userId)) {
            throw new RuntimeException("只有会议创建者可以结束会议");
        }

        meeting.setStatus(VideoMeeting.Status.ENDED);
        meeting.setEndedAt(LocalDateTime.now());
        meetingRepository.save(meeting);

        // AI智能会议结束时自动生成纪要
        if (meeting.getMeetingType() == VideoMeeting.MeetingType.AI_SMART) {
            try {
                List<MeetingTranscript> transcripts = transcriptRepository
                        .findByMeetingIdOrderByCreatedAtAsc(meetingId);
                if (!transcripts.isEmpty()) {
                    String combinedText = transcripts.stream()
                            .map(t -> {
                                String ts = t.getTimestamp() != null ? "(" + t.getTimestamp() + ")" : "";
                                return "说话人 " + t.getNickname() + " " + ts + ": " + t.getContent();
                            })
                            .collect(Collectors.joining("\n"));
                    summaryManager.generateSummary(meetingId, meeting.getCreatorId(), combinedText);
                }
            } catch (Exception e) {
                // Summary generation failure is non-fatal for ending the meeting
            }
        }
    }

    public Map<String, Object> getMeeting(String roomName) {
        VideoMeeting meeting = meetingRepository.findFirstByRoomNameOrderByCreatedAtDesc(roomName)
                .orElseThrow(() -> new ResourceNotFoundException("会议不存在"));

        Map<String, Object> result = new HashMap<>();
        result.put("id", meeting.getId());
        result.put("roomName", meeting.getRoomName());
        result.put("hasPassword", meeting.getPassword() != null && !meeting.getPassword().isEmpty());
        result.put("status", meeting.getStatus().name());
        result.put("meetingType", meeting.getMeetingType().name());
        result.put("createdAt", meeting.getCreatedAt().toString());
        return result;
    }

    public List<Map<String, Object>> listActiveMeetings() {
        List<VideoMeeting> meetings = meetingRepository.findByStatus(VideoMeeting.Status.ACTIVE);
        return meetings.stream().map(m -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", m.getId());
            map.put("roomName", m.getRoomName());
            map.put("hasPassword", m.getPassword() != null && !m.getPassword().isEmpty());
            map.put("meetingType", m.getMeetingType().name());
            map.put("creatorId", m.getCreatorId());
            map.put("createdAt", m.getCreatedAt().toString());
            return map;
        }).toList();
    }
}
