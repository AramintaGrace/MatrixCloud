package com.matrix.cloud.repository;

import com.matrix.cloud.entity.MeetingParticipant;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MeetingParticipantRepository extends JpaRepository<MeetingParticipant, Long> {
    List<MeetingParticipant> findByUserId(Long userId);
    List<MeetingParticipant> findByMeetingId(Long meetingId);
    List<MeetingParticipant> findByMeetingIdIn(List<Long> meetingIds);
    boolean existsByMeetingIdAndUserId(Long meetingId, Long userId);
}
