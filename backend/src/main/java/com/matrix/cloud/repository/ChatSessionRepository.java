package com.matrix.cloud.repository;

import com.matrix.cloud.entity.ChatSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface ChatSessionRepository extends JpaRepository<ChatSession, Long> {
    Optional<ChatSession> findByTeamId(Long teamId);
    Optional<ChatSession> findByTypeAndTeamId(ChatSession.Type type, Long teamId);
}
