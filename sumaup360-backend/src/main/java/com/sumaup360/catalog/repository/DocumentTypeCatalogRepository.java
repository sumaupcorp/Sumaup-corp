package com.sumaup360.catalog.repository;

import com.sumaup360.catalog.domain.DocumentTypeCatalog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DocumentTypeCatalogRepository extends JpaRepository<DocumentTypeCatalog, Long> {
    List<DocumentTypeCatalog> findByActiveTrue();
}
