package com.crm.inventory.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class ReleaseStockRequest {
    @NotNull(message = "warehouseId is required")
    private UUID warehouseId;

    @NotNull(message = "itemId is required")
    private UUID itemId;

    @NotNull(message = "quantity is required")
    @Min(value = 1, message = "quantity must be at least 1")
    private Integer quantity;

    @NotBlank(message = "crmReferenceId is required")
    private String crmReferenceId;
}
