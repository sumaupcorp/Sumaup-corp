package com.sumaup360.app.service;

import com.sumaup360.app.dto.SummaryDtos.SummaryResponse;
import com.sumaup360.app.repository.ExpenseRepository;
import com.sumaup360.app.repository.IncomeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.UUID;

/** Resumen financiero del mes de la persona: ingresos, gastos y utilidad estimada. */
@Service
public class SummaryService {

    private final IncomeRepository incomeRepository;
    private final ExpenseRepository expenseRepository;

    public SummaryService(IncomeRepository incomeRepository, ExpenseRepository expenseRepository) {
        this.incomeRepository = incomeRepository;
        this.expenseRepository = expenseRepository;
    }

    @Transactional(readOnly = true)
    public SummaryResponse monthly(UUID userId, YearMonth period) {
        LocalDate from = period.atDay(1);
        LocalDate to = period.atEndOfMonth();
        BigDecimal income = incomeRepository.sumInRange(userId, from, to);
        BigDecimal expense = expenseRepository.sumInRange(userId, from, to);
        BigDecimal utility = income.subtract(expense);
        return new SummaryResponse(period.toString(), income, expense, utility, "PEN");
    }
}
