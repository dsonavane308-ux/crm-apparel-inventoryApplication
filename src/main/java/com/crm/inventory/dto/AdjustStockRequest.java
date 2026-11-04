package com.crm.inventory.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class AdjustStockRequest {
    @NotNull(message = "warehouseId is required")
    private UUID warehouseId;

    @NotNull(message = "itemId is required")
    private UUID itemId;

    @NotNull(message = "quantityDelta is required")
    private Integer quantityDelta;

    @NotBlank(message = "reason is required")
    private String reason;
}
