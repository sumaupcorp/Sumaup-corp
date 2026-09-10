package com.sumaup360.erp.documenttemplate.dto;

import com.sumaup360.erp.documenttemplate.enums.DocumentType;
import com.sumaup360.erp.documenttemplate.model.DocumentSeries;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public final class DocumentSeriesDtos {

    private DocumentSeriesDtos() {
    }

    public record CreateSeriesRequest(
            @NotNull UUID companyId,
            UUID branchId,
            @NotNull DocumentType documentType,
            @NotBlank @Size(max = 10) String series,
            Boolean active
    ) {
    }

    public record SeriesResponse(UUID id, UUID companyId, UUID branchId,
                                 DocumentType documentType, String series,
                                 long currentNumber, boolean active) {
        public static SeriesResponse from(DocumentSeries s) {
            return new SeriesResponse(s.getId(), s.getCompanyId(), s.getBranchId(),
                    s.getDocumentType(), s.getSeries(), s.getCurrentNumber(), s.isActive());
        }
    }
}
