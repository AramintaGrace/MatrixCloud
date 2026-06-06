package com.matrix.cloud.service;

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
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class MeetingTranscriptService {

    @Autowired
    private MeetingTranscriptRepository transcriptRepository;

    @Autowired
    private VideoMeetingRepository meetingRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SpeechToTextService speechToTextService;

    @Autowired
    private MeetingParticipantRepository participantRepository;

    @Transactional
    public Map<String, Object> saveTranscript(Long meetingId, Long userId, MultipartFile audioFile, String timestamp) {
        VideoMeeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new ResourceNotFoundException("会议不存在"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("用户不存在"));

        String transcribedText = speechToTextService.transcribe(audioFile);

        MeetingTranscript transcript = new MeetingTranscript();
        transcript.setMeetingId(meetingId);
        transcript.setUserId(userId);
        transcript.setNickname(user.getNickname());
        transcript.setContent(transcribedText);
        transcript.setTimestamp(timestamp != null ? timestamp : null);
        transcriptRepository.save(transcript);

        Map<String, Object> result = new HashMap<>();
        result.put("id", transcript.getId());
        result.put("meetingId", transcript.getMeetingId());
        result.put("userId", transcript.getUserId());
        result.put("nickname", transcript.getNickname());
        result.put("content", transcript.getContent());
        result.put("timestamp", transcript.getTimestamp());
        result.put("createdAt", transcript.getCreatedAt().toString());
        return result;
    }

    public List<Map<String, Object>> getTranscripts(Long meetingId) {
        List<MeetingTranscript> transcripts = transcriptRepository.findByMeetingIdOrderByCreatedAtAsc(meetingId);
        return transcripts.stream().map(t -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", t.getId());
            map.put("meetingId", t.getMeetingId());
            map.put("userId", t.getUserId());
            map.put("nickname", t.getNickname());
            map.put("content", t.getContent());
            map.put("timestamp", t.getTimestamp());
            map.put("createdAt", t.getCreatedAt().toString());
            return map;
        }).collect(Collectors.toList());
    }

    public List<Map<String, Object>> getMeetingHistory(Long userId) {
        List<Long> participantMeetingIds = participantRepository.findByUserId(userId)
                .stream()
                .map(MeetingParticipant::getMeetingId)
                .toList();

        List<VideoMeeting> createdMeetings = meetingRepository.findByCreatorId(userId);
        List<VideoMeeting> joinedMeetings = participantMeetingIds.stream()
                .map(id -> meetingRepository.findById(id).orElse(null))
                .filter(m -> m != null && !m.getCreatorId().equals(userId))
                .toList();

        List<VideoMeeting> allMeetings = new ArrayList<>(createdMeetings);
        for (VideoMeeting m : joinedMeetings) {
            if (allMeetings.stream().noneMatch(existing -> existing.getId().equals(m.getId()))) {
                allMeetings.add(m);
            }
        }
        allMeetings.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));

        return allMeetings.stream().map(m -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", m.getId());
            map.put("roomName", m.getRoomName());
            map.put("creatorId", m.getCreatorId());
            map.put("status", m.getStatus().name());
            map.put("createdAt", m.getCreatedAt().toString());
            map.put("endedAt", m.getEndedAt() != null ? m.getEndedAt().toString() : null);
            map.put("transcriptCount", transcriptRepository.findByMeetingIdOrderByCreatedAtAsc(m.getId()).size());
            return map;
        }).collect(Collectors.toList());
    }

    public Map<String, Object> getMeetingHistoryDetail(Long meetingId) {
        VideoMeeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new ResourceNotFoundException("会议不存在"));

        User creator = userRepository.findById(meeting.getCreatorId())
                .orElse(null);

        List<Map<String, Object>> transcripts = getTranscripts(meetingId);

        Map<String, Object> result = new HashMap<>();
        result.put("id", meeting.getId());
        result.put("roomName", meeting.getRoomName());
        result.put("creatorId", meeting.getCreatorId());
        result.put("creatorName", creator != null ? creator.getNickname() : "未知");
        result.put("status", meeting.getStatus().name());
        result.put("createdAt", meeting.getCreatedAt().toString());
        result.put("endedAt", meeting.getEndedAt() != null ? meeting.getEndedAt().toString() : null);
        result.put("transcripts", transcripts);
        return result;
    }

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(MeetingTranscriptService.class);

    public Map<String, Object> getMeetingRecords(Long userId) {
        try {
            // Meetings created by the user
            List<VideoMeeting> createdMeetings = meetingRepository.findByCreatorId(userId);
            createdMeetings.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));

            // Meetings the user joined (but did not create)
            List<Long> participantMeetingIds = participantRepository.findByUserId(userId)
                    .stream().map(MeetingParticipant::getMeetingId).collect(Collectors.toList());
            List<VideoMeeting> joinedMeetings = new ArrayList<>(participantMeetingIds.stream()
                    .map(id -> meetingRepository.findById(id).orElse(null))
                    .filter(m -> m != null && !m.getCreatorId().equals(userId))
                    .toList());
            joinedMeetings.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));

            Map<String, Object> result = new HashMap<>();
            result.put("created", buildMeetingRecords(createdMeetings));
            result.put("joined", buildMeetingRecords(joinedMeetings));
            return result;
        } catch (Exception e) {
            e.printStackTrace();
            log.error("getMeetingRecords failed", e);
            throw new RuntimeException("获取会议记录失败: " + e.getClass().getName() + " - " + e.getMessage(), e);
        }
    }

    private List<Map<String, Object>> buildMeetingRecords(List<VideoMeeting> meetings) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (VideoMeeting m : meetings) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", m.getId());
            map.put("roomName", m.getRoomName());
            map.put("creatorId", m.getCreatorId());
            User creator = userRepository.findById(m.getCreatorId()).orElse(null);
            map.put("creatorName", creator != null ? creator.getNickname() : "未知");
            map.put("status", m.getStatus().name());
            map.put("createdAt", m.getCreatedAt() != null ? m.getCreatedAt().toString() : null);
            map.put("endedAt", m.getEndedAt() != null ? m.getEndedAt().toString() : null);

            // Meeting duration
            if (m.getCreatedAt() != null && m.getEndedAt() != null) {
                long sec = java.time.Duration.between(m.getCreatedAt(), m.getEndedAt()).getSeconds();
                map.put("duration", formatDuration(sec));
            } else {
                map.put("duration", null);
            }

            // Participants
            List<MeetingParticipant> participants = participantRepository.findByMeetingId(m.getId());
            map.put("participantCount", participants.size());
            List<Map<String, Object>> pList = new ArrayList<>();
            for (MeetingParticipant p : participants) {
                Map<String, Object> pMap = new HashMap<>();
                User u = userRepository.findById(p.getUserId()).orElse(null);
                pMap.put("userId", p.getUserId());
                pMap.put("nickname", u != null ? u.getNickname() : "未知用户");
                pMap.put("avatar", u != null ? u.getAvatar() : null);
                pMap.put("role", p.getRole().name());
                if (p.getJoinedAt() != null) {
                    pMap.put("joinedAt", p.getJoinedAt().toString());
                }
                // Per-participant duration
                java.time.LocalDateTime end = p.getLeftAt();
                if (end == null) end = m.getEndedAt();
                if (end != null && p.getJoinedAt() != null) {
                    long sec = java.time.Duration.between(p.getJoinedAt(), end).getSeconds();
                    pMap.put("duration", formatDuration(sec));
                } else {
                    pMap.put("duration", null);
                }
                pList.add(pMap);
            }
            map.put("participants", pList);

            map.put("transcriptCount", transcriptRepository.findByMeetingIdOrderByCreatedAtAsc(m.getId()).size());
            list.add(map);
        }
        return list;
    }

    private String formatDuration(long totalSeconds) {
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        if (hours > 0) {
            return String.format("%d小时%02d分%02d秒", hours, minutes, seconds);
        }
        return String.format("%d分%02d秒", minutes, seconds);
    }
}
