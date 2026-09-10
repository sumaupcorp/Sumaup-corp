package com.sumaup360.billing.domain;

import com.sumaup360.billing.enums.ProductLine;
import com.sumaup360.common.BaseEntity;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

/** Plan comercial. Para planes BUSINESS, moduleCodes es el techo de modulos (allowedModules). */
@Entity
@Table(name = "plan", schema = "billing")
@Getter
@Setter
public class Plan extends BaseEntity {

    @Column(name = "code", nullable = false, unique = true, length = 40)
    private String code;

    @Column(name = "name", nullable = false, length = 120)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "line", nullable = false, length = 20)
    private ProductLine line;

    @Column(name = "price", nullable = false, precision = 10, scale = 2)
    private BigDecimal price = BigDecimal.ZERO;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency = "PEN";

    @Column(name = "max_branches")
    private Integer maxBranches;

    @Column(name = "max_users")
    private Integer maxUsers;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @ElementCollection
    @CollectionTable(name = "plan_module", schema = "billing",
            joinColumns = @JoinColumn(name = "plan_id"))
    @Column(name = "module_code")
    private Set<String> moduleCodes = new HashSet<>();
}
