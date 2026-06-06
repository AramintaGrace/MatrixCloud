package com.matrix.cloud.repository;

import com.matrix.cloud.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findBySessionIdOrderByCreatedAtAsc(Long sessionId);
    List<ChatMessage> findTop50BySessionIdOrderByCreatedAtDesc(Long sessionId);
    List<ChatMessage> findBySessionIdAndContentContainingIgnoreCaseOrderByCreatedAtAsc(Long sessionId, String content);
    List<ChatMessage> findBySessionIdAndPinnedTrueOrderByCreatedAtAsc(Long sessionId);
}
