package com.sumaup360.app.service;

import com.sumaup360.app.domain.Expense;
import com.sumaup360.app.dto.ExpenseDtos.CreateExpenseRequest;
import com.sumaup360.app.repository.ExpenseRepository;
import com.sumaup360.common.error.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/** Gastos de la persona (scopeados por usuario). */
@Service
public class ExpenseService {

    private final ExpenseRepository expenseRepository;

    public ExpenseService(ExpenseRepository expenseRepository) {
        this.expenseRepository = expenseRepository;
    }

    @Transactional
    public Expense create(UUID userId, CreateExpenseRequest req) {
        Expense e = new Expense();
        e.setUserId(userId);
        e.setTxDate(req.txDate());
        e.setAmount(req.amount());
        if (req.currency() != null && !req.currency().isBlank()) e.setCurrency(req.currency());
        e.setCategory(req.category());
        e.setPaymentMethod(req.paymentMethod());
        e.setDescription(req.description());
        e.setReceiptId(req.receiptId());
        return expenseRepository.save(e);
    }

    @Transactional(readOnly = true)
    public List<Expense> list(UUID userId) {
        return expenseRepository.findByUserIdOrderByTxDateDesc(userId);
    }

    @Transactional(readOnly = true)
    public Expense get(UUID id, UUID userId) {
        return expenseRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Gasto no encontrado."));
    }

    @Transactional
    public void delete(UUID id, UUID userId) {
        expenseRepository.delete(get(id, userId));
    }
}
