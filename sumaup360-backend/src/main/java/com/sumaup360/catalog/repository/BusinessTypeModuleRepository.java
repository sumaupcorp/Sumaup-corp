package com.sumaup360.catalog.repository;

import com.sumaup360.catalog.domain.BusinessTypeModule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BusinessTypeModuleRepository extends JpaRepository<BusinessTypeModule, Long> {
    List<BusinessTypeModule> findByBusinessTypeCode(String businessTypeCode);
}
