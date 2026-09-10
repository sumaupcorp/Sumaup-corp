package com.sumaup360.app.taxi.repository;

import com.sumaup360.app.taxi.domain.AttachedFile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AttachedFileRepository extends JpaRepository<AttachedFile, UUID> {
    List<AttachedFile> findByEntityTypeAndEntityIdOrderByCreatedAtAsc(String entityType, UUID entityId);
}
