package com.sumaup360.erp.appointment.repository;

import com.sumaup360.erp.appointment.domain.BookingPage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface BookingPageRepository extends JpaRepository<BookingPage, UUID> {

    Optional<BookingPage> findByTenantIdAndCompanyId(UUID tenantId, UUID companyId);

    Optional<BookingPage> findByToken(String token);
}
