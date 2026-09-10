package com.sumaup360.app.ai;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AiPromptRepository extends JpaRepository<AiPrompt, UUID> {
    Optional<AiPrompt> findFirstByOrderByCreatedAtAsc();
}
