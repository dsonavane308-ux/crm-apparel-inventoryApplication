package com.crm.inventory.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class ReceiveStockRequest {
    @NotNull(message = "warehouseId is required")
    private UUID warehouseId;

    @NotNull(message = "itemId is required")
    private UUID itemId;

    @NotNull(message = "quantity is required")
    @Min(value = 1, message = "quantity must be greater than or equal to 1")
    private Integer quantity;

    private BigDecimal unitCost;
    private String poReference;
    private String notes;
}
