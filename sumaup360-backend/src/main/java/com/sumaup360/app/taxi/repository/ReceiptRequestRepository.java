package com.sumaup360.app.taxi.repository;

import com.sumaup360.app.taxi.domain.ReceiptRequest;
import com.sumaup360.app.taxi.enums.ComprobanteType;
import com.sumaup360.app.taxi.enums.RequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReceiptRequestRepository extends JpaRepository<ReceiptRequest, UUID> {
    List<ReceiptRequest> findByTaxistaUserIdOrderByCreatedAtDesc(UUID taxistaUserId);
    long countByTaxistaUserId(UUID taxistaUserId);
    Optional<ReceiptRequest> findByIdAndTaxistaUserId(UUID id, UUID taxistaUserId);
    List<ReceiptRequest> findByEstadoOrderByCreatedAtDesc(RequestStatus estado);
    List<ReceiptRequest> findAllByOrderByCreatedAtDesc();

    /** Para evitar solicitudes duplicadas (mismo QR, doc, monto y tipo aun pendientes). */
    Optional<ReceiptRequest> findFirstByQrTokenAndDocNumberAndMontoAndTipoAndEstadoOrderByCreatedAtDesc(
            String qrToken, String docNumber, BigDecimal monto, ComprobanteType tipo, RequestStatus estado);
}
