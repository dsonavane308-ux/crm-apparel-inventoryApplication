package com.crm.inventory.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class CreateItemRequest {
    @NotBlank(message = "sku is required")
    @Size(min = 3, max = 64)
    private String sku;

    @NotBlank(message = "name is required")
    @Size(min = 2, max = 255)
    private String name;

    private String description;
    private String size;
    private String color;
    private String brand;
    private String barcode;
    private UUID categoryId;

    @NotNull(message = "unitPrice is required")
    @DecimalMin(value = "0.01")
    private BigDecimal unitPrice;

    @NotNull(message = "costPrice is required")
    @DecimalMin(value = "0.00")
    private BigDecimal costPrice;

    @NotNull(message = "reorderThreshold is required")
    @Min(value = 0)
    private Integer reorderThreshold;
}
