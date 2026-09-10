package com.sumaup360.erp.module.dto;

import jakarta.validation.constraints.NotNull;

public final class ModuleDtos {

    private ModuleDtos() {
    }

    public record CompanyModuleView(String moduleCode, String name, boolean enabled,
                                    String source, boolean core) {
    }

    public record SetModuleRequest(@NotNull Boolean enabled) {
    }
}
