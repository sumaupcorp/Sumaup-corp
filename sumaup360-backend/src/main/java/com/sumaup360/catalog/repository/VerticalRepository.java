package com.sumaup360.catalog.repository;

import com.sumaup360.catalog.domain.Vertical;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VerticalRepository extends JpaRepository<Vertical, Long> {
    List<Vertical> findByActiveTrue();
    List<Vertical> findByActiveTrueAndBusinessTypeId(Long businessTypeId);
}
