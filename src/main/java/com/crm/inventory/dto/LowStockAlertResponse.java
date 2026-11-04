package com.crm.inventory.dto;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class LowStockAlertResponse {
    private UUID itemId;
    private String sku;
    private String name;
    private Integer totalAvailable;
    private Integer reorderThreshold;
    private Integer shortfall;
}
