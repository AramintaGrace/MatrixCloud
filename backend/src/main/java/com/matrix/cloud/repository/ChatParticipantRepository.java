package com.matrix.cloud.repository;

import com.matrix.cloud.entity.ChatParticipant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ChatParticipantRepository extends JpaRepository<ChatParticipant, Long> {
    List<ChatParticipant> findByUserId(Long userId);
    List<ChatParticipant> findBySessionId(Long sessionId);

    @Query("SELECT cp.sessionId FROM ChatParticipant cp WHERE cp.sessionId IN " +
           "(SELECT cp2.sessionId FROM ChatParticipant cp2 WHERE cp2.userId = :userId) " +
           "AND cp.userId = :otherUserId")
    List<Long> findCommonPrivateSessionIds(Long userId, Long otherUserId);
}
