package com.sumaup360.catalog.repository;

import com.sumaup360.catalog.domain.PersonSegment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PersonSegmentRepository extends JpaRepository<PersonSegment, Long> {
    List<PersonSegment> findByActiveTrue();
}
