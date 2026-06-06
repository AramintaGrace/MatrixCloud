package com.matrix.cloud.repository;

import com.matrix.cloud.entity.MeetingSummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface MeetingSummaryRepository extends JpaRepository<MeetingSummary, Long> {
    List<MeetingSummary> findByMeetingIdOrderByCreatedAtDesc(Long meetingId);
    Optional<MeetingSummary> findTopByMeetingIdOrderByCreatedAtDesc(Long meetingId);
    Optional<MeetingSummary> findTopByMeetingIdAndStatusOrderByCreatedAtDesc(Long meetingId, MeetingSummary.Status status);
    List<MeetingSummary> findByMeetingIdInAndStatusOrderByCreatedAtDesc(List<Long> meetingIds, MeetingSummary.Status status);
}
