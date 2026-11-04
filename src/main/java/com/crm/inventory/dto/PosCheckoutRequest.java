package com.crm.inventory.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
public class PosCheckoutRequest {
    @NotNull(message = "warehouseId is required")
    private UUID warehouseId;

    private String customerName;
    private String customerMobile;

    private BigDecimal discountAmount = BigDecimal.ZERO;
    private BigDecimal taxPercent = BigDecimal.ZERO;
    private String paymentMode = "CASH";

    @NotEmpty(message = "Cart items cannot be empty")
    @Valid
    private List<CartItemDto> items;

    @Data
    public static class CartItemDto {
        @NotNull(message = "itemId is required")
        private UUID itemId;

        @NotNull(message = "quantity is required")
        @Min(value = 1, message = "quantity must be at least 1")
        private Integer quantity;

        private boolean wasReserved = false;
    }
}
