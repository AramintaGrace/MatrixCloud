package com.matrix.cloud.repository;

import com.matrix.cloud.entity.VideoMeeting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface VideoMeetingRepository extends JpaRepository<VideoMeeting, Long> {
    Optional<VideoMeeting> findFirstByRoomNameOrderByCreatedAtDesc(String roomName);
    Optional<VideoMeeting> findByRoomNameAndStatus(String roomName, VideoMeeting.Status status);
    List<VideoMeeting> findByStatus(VideoMeeting.Status status);
    List<VideoMeeting> findByStatusOrderByCreatedAtDesc(VideoMeeting.Status status);
    List<VideoMeeting> findByCreatorIdAndStatusOrderByCreatedAtDesc(Long creatorId, VideoMeeting.Status status);
    List<VideoMeeting> findByCreatorId(Long creatorId);
}
