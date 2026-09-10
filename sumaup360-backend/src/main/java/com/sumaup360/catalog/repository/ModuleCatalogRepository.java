package com.sumaup360.catalog.repository;

import com.sumaup360.catalog.domain.ModuleCatalog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ModuleCatalogRepository extends JpaRepository<ModuleCatalog, Long> {
    List<ModuleCatalog> findByActiveTrue();
    Optional<ModuleCatalog> findByCode(String code);
}
