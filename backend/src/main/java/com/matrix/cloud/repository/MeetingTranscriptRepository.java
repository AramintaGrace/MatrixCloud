package com.matrix.cloud.repository;

import com.matrix.cloud.entity.MeetingTranscript;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MeetingTranscriptRepository extends JpaRepository<MeetingTranscript, Long> {
    List<MeetingTranscript> findByMeetingIdOrderByCreatedAtAsc(Long meetingId);
}
