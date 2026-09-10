package com.sumaup360.app.ai;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.UUID;

public interface AiUsageLogRepository extends JpaRepository<AiUsageLog, UUID> {
    long countByUserIdAndConsumioCreditoTrueAndCreatedAtAfter(UUID userId, OffsetDateTime since);
}
