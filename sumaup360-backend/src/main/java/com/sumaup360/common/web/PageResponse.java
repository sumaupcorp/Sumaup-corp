package com.sumaup360.common.web;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

/**
 * Contrato estandar de paginacion de la API. Todo endpoint que devuelva volumen
 * creciente (ventas, movimientos, kardex, historiales) debe responder paginado
 * con este envoltorio en lugar de listas completas.
 */
public record PageResponse<T>(
        List<T> items,
        int page,
        int size,
        long totalItems,
        int totalPages
) {

    /** Mapea una pagina de Spring Data al contrato de la API. */
    public static <E, T> PageResponse<T> from(Page<E> page, Function<E, T> mapper) {
        return new PageResponse<>(
                page.getContent().stream().map(mapper).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages());
    }

    /** Normaliza page/size de la query: page >= 0, 1 <= size <= maxSize. */
    public static int sanitizePage(Integer page) {
        return page == null || page < 0 ? 0 : page;
    }

    public static int sanitizeSize(Integer size, int defaultSize, int maxSize) {
        if (size == null || size < 1) {
            return defaultSize;
        }
        return Math.min(size, maxSize);
    }
}
