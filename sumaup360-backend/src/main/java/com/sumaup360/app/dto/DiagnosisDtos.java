package com.sumaup360.app.dto;

import com.sumaup360.app.domain.PersonProfile;
import com.sumaup360.app.domain.TaxDiagnosis;

import java.math.BigDecimal;

public final class DiagnosisDtos {

    private DiagnosisDtos() {
    }

    public record DiagnoseRequest(String segmentCode, BigDecimal monthlyIncome, String answers) {
    }

    public record DiagnosisResponse(String segmentCode, BigDecimal monthlyIncome,
                                    String recommendedPlanCode) {
        public static DiagnosisResponse from(TaxDiagnosis d) {
            return new DiagnosisResponse(d.getSegmentCode(), d.getMonthlyIncome(),
                    d.getRecommendedPlanCode());
        }
    }

    public record ProfileRequest(String segmentCode, String ruc, String regime) {
    }

    public record ProfileResponse(String segmentCode, String ruc, String regime) {
        public static ProfileResponse from(PersonProfile p) {
            return new ProfileResponse(p.getSegmentCode(), p.getRuc(), p.getRegime());
        }
    }
}
