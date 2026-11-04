package com.crm.inventory.dto;

import com.crm.inventory.model.ItemStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class UpdateItemRequest {
    private String name;
    private String description;
    private String size;
    private String color;
    private String brand;
    private String barcode;
    private UUID categoryId;

    @DecimalMin(value = "0.01")
    private BigDecimal unitPrice;

    @DecimalMin(value = "0.00")
    private BigDecimal costPrice;

    @Min(value = 0)
    private Integer reorderThreshold;

    private ItemStatus status;
}
