package com.sumaup360.app.service;

import com.sumaup360.app.domain.Income;
import com.sumaup360.app.dto.IncomeDtos.CreateIncomeRequest;
import com.sumaup360.app.repository.IncomeRepository;
import com.sumaup360.common.error.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/** Ingresos de la persona (scopeados por usuario). */
@Service
public class IncomeService {

    private final IncomeRepository incomeRepository;

    public IncomeService(IncomeRepository incomeRepository) {
        this.incomeRepository = incomeRepository;
    }

    @Transactional
    public Income create(UUID userId, CreateIncomeRequest req) {
        Income i = new Income();
        i.setUserId(userId);
        i.setTxDate(req.txDate());
        i.setAmount(req.amount());
        if (req.currency() != null && !req.currency().isBlank()) i.setCurrency(req.currency());
        i.setCategory(req.category());
        i.setPaymentMethod(req.paymentMethod());
        i.setDescription(req.description());
        i.setReceiptId(req.receiptId());
        return incomeRepository.save(i);
    }

    @Transactional(readOnly = true)
    public List<Income> list(UUID userId) {
        return incomeRepository.findByUserIdOrderByTxDateDesc(userId);
    }

    @Transactional(readOnly = true)
    public Income get(UUID id, UUID userId) {
        return incomeRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Ingreso no encontrado."));
    }

    @Transactional
    public void delete(UUID id, UUID userId) {
        incomeRepository.delete(get(id, userId));
    }
}
