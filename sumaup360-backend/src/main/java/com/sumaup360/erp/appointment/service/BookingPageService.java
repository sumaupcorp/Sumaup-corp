package com.sumaup360.erp.appointment.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sumaup360.common.error.BadRequestException;
import com.sumaup360.common.error.ResourceNotFoundException;
import com.sumaup360.erp.appointment.domain.BookingPage;
import com.sumaup360.erp.appointment.dto.BookingDtos.BookingPageView;
import com.sumaup360.erp.appointment.dto.BookingDtos.UpdateBookingPageRequest;
import com.sumaup360.erp.appointment.repository.BookingPageRepository;
import com.sumaup360.tenant.domain.Company;
import com.sumaup360.tenant.repository.CompanyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/** Configuracion de la pagina publica de reserva de citas (por empresa). */
@Service
public class BookingPageService {

    /** Campos base del formulario; el negocio puede editarlos desde el panel. */
    private static final String DEFAULT_FORM_CONFIG = """
            [
              {"key":"nombre","label":"Nombre completo","type":"text","required":true},
              {"key":"telefono","label":"Telefono / WhatsApp","type":"phone","required":true},
              {"key":"mascota","label":"Nombre de tu mascota","type":"text","required":false},
              {"key":"motivo","label":"Motivo de la cita","type":"textarea","required":false}
            ]""";

    private final BookingPageRepository bookingPageRepository;
    private final CompanyRepository companyRepository;
    private final ObjectMapper mapper;

    public BookingPageService(BookingPageRepository bookingPageRepository,
                              CompanyRepository companyRepository,
                              ObjectMapper mapper) {
        this.bookingPageRepository = bookingPageRepository;
        this.companyRepository = companyRepository;
        this.mapper = mapper;
    }

    /** Devuelve la configuracion de la empresa; la crea (deshabilitada) si no existe. */
    @Transactional
    public BookingPageView getOrCreate(UUID tenantId, UUID companyId) {
        Company company = requireCompany(tenantId, companyId);
        BookingPage page = bookingPageRepository.findByTenantIdAndCompanyId(tenantId, companyId)
                .orElseGet(() -> {
                    BookingPage p = new BookingPage();
                    p.setTenantId(tenantId);
                    p.setCompanyId(companyId);
                    p.setToken(newToken());
                    p.setEnabled(false);
                    p.setTitle(company.getLegalName());
                    p.setFormConfig(DEFAULT_FORM_CONFIG);
                    return bookingPageRepository.save(p);
                });
        return view(page);
    }

    @Transactional
    public BookingPageView update(UUID tenantId, UUID companyId, UpdateBookingPageRequest req) {
        requireCompany(tenantId, companyId);
        BookingPage page = requirePage(tenantId, companyId);
        if (req.enabled() != null) page.setEnabled(req.enabled());
        if (req.title() != null) page.setTitle(req.title());
        if (req.logoUrl() != null) page.setLogoUrl(req.logoUrl());
        if (req.welcomeText() != null) page.setWelcomeText(req.welcomeText());
        if (req.formConfig() != null) {
            if (!req.formConfig().isArray()) {
                throw new BadRequestException("La configuracion del formulario debe ser una lista de campos.");
            }
            page.setFormConfig(req.formConfig().toString());
        }
        if (req.qrStyle() != null) {
            if (!req.qrStyle().isObject()) {
                throw new BadRequestException("El estilo del QR debe ser un objeto.");
            }
            String json = req.qrStyle().toString();
            if (json.length() > 1000) {
                throw new BadRequestException("El estilo del QR es demasiado grande.");
            }
            page.setQrStyle(json);
        }
        return view(bookingPageRepository.save(page));
    }

    /** Invalida el enlace/QR anterior generando un token nuevo. */
    @Transactional
    public BookingPageView regenerateToken(UUID tenantId, UUID companyId) {
        requireCompany(tenantId, companyId);
        BookingPage page = requirePage(tenantId, companyId);
        page.setToken(newToken());
        return view(bookingPageRepository.save(page));
    }

    JsonNode parseFormConfig(BookingPage page) {
        try {
            return mapper.readTree(page.getFormConfig());
        } catch (Exception e) {
            return mapper.createArrayNode();
        }
    }

    private BookingPageView view(BookingPage page) {
        return BookingPageView.from(page, parseFormConfig(page), parseQrStyle(page));
    }

    private JsonNode parseQrStyle(BookingPage page) {
        if (page.getQrStyle() == null || page.getQrStyle().isBlank()) return null;
        try {
            return mapper.readTree(page.getQrStyle());
        } catch (Exception e) {
            return null;
        }
    }

    private BookingPage requirePage(UUID tenantId, UUID companyId) {
        return bookingPageRepository.findByTenantIdAndCompanyId(tenantId, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("La empresa no tiene pagina de reserva."));
    }

    private Company requireCompany(UUID tenantId, UUID companyId) {
        return companyRepository.findByIdAndTenantId(companyId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Empresa no encontrada en este tenant."));
    }

    private String newToken() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
