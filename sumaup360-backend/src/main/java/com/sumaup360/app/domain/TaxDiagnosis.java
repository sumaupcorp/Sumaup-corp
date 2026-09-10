package com.sumaup360.app.domain;

import com.sumaup360.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.util.UUID;

/** Resultado de un diagnostico tributario (el ultimo es el vigente). */
@Entity
@Table(name = "tax_diagnosis", schema = "app")
@Getter
@Setter
public class TaxDiagnosis extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "segment_code", length = 40)
    private String segmentCode;

    @Column(name = "monthly_income", precision = 12, scale = 2)
    private BigDecimal monthlyIncome;

    @Column(name = "recommended_plan_code", length = 40)
    private String recommendedPlanCode;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "answers", columnDefinition = "jsonb")
    private String answers;
}
