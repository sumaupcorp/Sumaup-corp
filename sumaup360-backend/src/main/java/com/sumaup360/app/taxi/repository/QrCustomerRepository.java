package com.sumaup360.app.taxi.repository;

import com.sumaup360.app.taxi.domain.QrCustomer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface QrCustomerRepository extends JpaRepository<QrCustomer, UUID> {
    Optional<QrCustomer> findFirstByDocNumber(String docNumber);
}
