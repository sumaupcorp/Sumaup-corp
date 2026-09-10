package com.sumaup360.catalog.repository;

import com.sumaup360.catalog.domain.Currency;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CurrencyRepository extends JpaRepository<Currency, Long> {
    List<Currency> findByActiveTrue();
}
