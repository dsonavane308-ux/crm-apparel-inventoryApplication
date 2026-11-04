package com.crm.inventory.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class TransferStockRequest {
    @NotNull(message = "fromWarehouseId is required")
    private UUID fromWarehouseId;

    @NotNull(message = "toWarehouseId is required")
    private UUID toWarehouseId;

    @NotNull(message = "itemId is required")
    private UUID itemId;

    @NotNull(message = "quantity is required")
    @Min(value = 1, message = "quantity must be at least 1")
    private Integer quantity;

    private String reason;
}
