package com.sumaup360.chatbot.repository;

import com.sumaup360.chatbot.domain.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ConversationRepository extends JpaRepository<Conversation, UUID> {
    List<Conversation> findByUserIdOrderByCreatedAtDesc(UUID userId);
    Optional<Conversation> findByIdAndUserId(UUID id, UUID userId);
}
