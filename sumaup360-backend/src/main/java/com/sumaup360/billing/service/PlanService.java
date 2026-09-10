package com.sumaup360.billing.service;

import com.sumaup360.billing.domain.Plan;
import com.sumaup360.billing.dto.BillingDtos.CreatePlanRequest;
import com.sumaup360.billing.dto.BillingDtos.PlanView;
import com.sumaup360.billing.enums.ProductLine;
import com.sumaup360.billing.repository.PlanRepository;
import com.sumaup360.common.error.ConflictException;
import com.sumaup360.common.error.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;

/** Planes comerciales. Lectura abierta a usuarios autenticados; gestion requiere plan:manage. */
@Service
public class PlanService {

    private final PlanRepository planRepository;

    public PlanService(PlanRepository planRepository) {
        this.planRepository = planRepository;
    }

    @Transactional(readOnly = true)
    public List<PlanView> list(ProductLine line) {
        List<Plan> plans = line == null ? planRepository.findByActiveTrue()
                : planRepository.findByActiveTrueAndLine(line);
        return plans.stream().map(PlanView::from).toList();   // moduleCodes inicializado en tx
    }

    @Transactional(readOnly = true)
    public PlanView getByCode(String code) {
        return PlanView.from(planRepository.findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("Plan no encontrado.")));
    }

    @Transactional
    public PlanView createPlan(CreatePlanRequest req) {
        return PlanView.from(create(req));
    }

    @Transactional
    public Plan create(CreatePlanRequest req) {
        planRepository.findByCode(req.code()).ifPresent(p -> {
            throw new ConflictException("Ya existe un plan con el codigo '" + req.code() + "'.");
        });
        Plan p = new Plan();
        p.setCode(req.code());
        p.setName(req.name());
        p.setLine(req.line());
        p.setPrice(req.price() != null ? req.price() : BigDecimal.ZERO);
        p.setMaxBranches(req.maxBranches());
        p.setMaxUsers(req.maxUsers());
        if (req.moduleCodes() != null) {
            p.setModuleCodes(new HashSet<>(req.moduleCodes()));
        }
        p.setActive(true);
        return planRepository.save(p);
    }
}
