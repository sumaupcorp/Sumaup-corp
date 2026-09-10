package com.sumaup360.app.taxi.repository;

import com.sumaup360.app.taxi.domain.QrCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface QrCodeRepository extends JpaRepository<QrCode, UUID> {
    Optional<QrCode> findFirstByUserId(UUID userId);
    Optional<QrCode> findByTokenAndActiveTrue(String token);
}
