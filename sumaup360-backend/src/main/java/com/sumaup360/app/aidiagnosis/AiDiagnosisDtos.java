package com.sumaup360.app.aidiagnosis;

import java.time.OffsetDateTime;
import java.util.UUID;

public final class AiDiagnosisDtos {

    private AiDiagnosisDtos() {
    }

    /** Solicitud de diagnostico: el usuario ingresa su DNI o RUC. */
    public record GenerateRequest(String doc, String docType) {
    }

    public record ReportView(UUID id, String resultado, String modelo, OffsetDateTime createdAt) {
        public static ReportView from(DiagnosisReport r) {
            return new ReportView(r.getId(), r.getResultado(), r.getModelo(), r.getCreatedAt());
        }
    }

    /** Estado para la app: si tiene su intento disponible y su ultimo reporte (si ya lo hizo). */
    public record StatusView(boolean available, ReportView latest) {
        public static StatusView of(DiagnosisReportService.DiagnosisStatus s) {
            return new StatusView(s.available(), s.latest() == null ? null : ReportView.from(s.latest()));
        }
    }
}
