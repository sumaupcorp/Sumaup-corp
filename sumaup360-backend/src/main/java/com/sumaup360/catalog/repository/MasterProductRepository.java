package com.sumaup360.catalog.repository;

import com.sumaup360.catalog.domain.MasterProduct;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MasterProductRepository extends JpaRepository<MasterProduct, UUID> {

    Optional<MasterProduct> findByEan(String ean);

    /**
     * Busqueda del staff: por texto y/o rubro, incluye inactivos. `pattern` es el texto
     * tokenizado por el service ("coca cola" -> "%coca%cola%"): las palabras coinciden en
     * cualquier campo (marca+nombre+presentacion+categoria) sin exigir el texto literal.
     */
    @Query("""
            select distinct p from MasterProduct p left join p.rubros r
            where (:rubro is null or r = :rubro)
              and (:pattern is null
                   or lower(concat(coalesce(p.brand, ''), ' ', p.name, ' ',
                            coalesce(p.presentation, ''), ' ', coalesce(p.category, ''))) like :pattern
                   or lower(concat(p.name, ' ', coalesce(p.brand, ''))) like :pattern
                   or p.ean = :ean)
            order by p.name asc
            """)
    List<MasterProduct> adminSearch(@Param("pattern") String pattern, @Param("ean") String ean,
                                    @Param("rubro") String rubro, Pageable pageable);

    /** Busqueda/navegacion del tenant: solo activos del rubro de su empresa (pattern null = todos). */
    @Query("""
            select distinct p from MasterProduct p join p.rubros r
            where p.active = true and r = :rubro
              and (:pattern is null
                   or lower(concat(coalesce(p.brand, ''), ' ', p.name, ' ',
                            coalesce(p.presentation, ''), ' ', coalesce(p.category, ''))) like :pattern
                   or lower(concat(p.name, ' ', coalesce(p.brand, ''))) like :pattern
                   or p.ean = :ean)
            order by p.name asc
            """)
    List<MasterProduct> tenantSearch(@Param("pattern") String pattern, @Param("ean") String ean,
                                     @Param("rubro") String rubro, Pageable pageable);
}
