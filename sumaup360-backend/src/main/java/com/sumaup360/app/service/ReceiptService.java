package com.sumaup360.app.service;

import com.sumaup360.app.domain.Receipt;
import com.sumaup360.app.dto.ReceiptDtos.CreateReceiptRequest;
import com.sumaup360.app.enums.ReceiptStatus;
import com.sumaup360.app.enums.ReceiptType;
import com.sumaup360.app.repository.ReceiptRepository;
import com.sumaup360.common.error.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Recibos subidos por la persona. Quedan en estado PENDING para que el Backoffice (contadores)
 * los procese (Fase 8). La app solo sube y consulta estado.
 */
@Service
public class ReceiptService {

    private final ReceiptRepository receiptRepository;

    public ReceiptService(ReceiptRepository receiptRepository) {
        this.receiptRepository = receiptRepository;
    }

    @Transactional
    public Receipt create(UUID userId, CreateReceiptRequest req) {
        Receipt r = new Receipt();
        r.setUserId(userId);
        r.setType(req.type() != null ? req.type() : ReceiptType.OTHER);
        r.setDocNumber(req.docNumber());
        r.setIssueDate(req.issueDate());
        r.setAmount(req.amount());
        if (req.currency() != null && !req.currency().isBlank()) r.setCurrency(req.currency());
        r.setFileUrl(req.fileUrl());
        r.setNotes(req.notes());
        r.setStatus(ReceiptStatus.PENDING);
        return receiptRepository.save(r);
    }

    @Transactional(readOnly = true)
    public List<Receipt> list(UUID userId) {
        return receiptRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Transactional(readOnly = true)
    public Receipt get(UUID id, UUID userId) {
        return receiptRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Recibo no encontrado."));
    }
}
