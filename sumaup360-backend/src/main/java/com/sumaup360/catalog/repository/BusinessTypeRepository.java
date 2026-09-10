package com.sumaup360.catalog.repository;

import com.sumaup360.catalog.domain.BusinessType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BusinessTypeRepository extends JpaRepository<BusinessType, Long> {
    List<BusinessType> findByActiveTrue();
    Optional<BusinessType> findByCode(String code);
}
