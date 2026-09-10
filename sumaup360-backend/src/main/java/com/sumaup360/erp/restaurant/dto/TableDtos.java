package com.sumaup360.erp.restaurant.dto;

import com.sumaup360.erp.restaurant.domain.RestaurantTable;
import com.sumaup360.erp.restaurant.enums.TableStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public final class TableDtos {

    private TableDtos() {
    }

    public record CreateTableRequest(
            @NotNull UUID branchId,
            @NotBlank @Size(max = 60) String name,
            @Size(max = 80) String zone,
            Integer capacity
    ) {
    }

    public record UpdateTableStatusRequest(@NotNull TableStatus status) {
    }

    public record TableResponse(UUID id, UUID branchId, String name, String zone,
                                int capacity, TableStatus status, boolean active) {
        public static TableResponse from(RestaurantTable t) {
            return new TableResponse(t.getId(), t.getBranchId(), t.getName(), t.getZone(),
                    t.getCapacity(), t.getStatus(), t.isActive());
        }
    }
}
