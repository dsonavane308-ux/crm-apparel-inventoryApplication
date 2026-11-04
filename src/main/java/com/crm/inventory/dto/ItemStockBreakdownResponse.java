package com.crm.inventory.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
@Builder
public class ItemStockBreakdownResponse {
    private UUID itemId;
    private String sku;
    private String name;
    private Integer totalOnHand;
    private Integer totalReserved;
    private Integer totalAvailable;
    private List<WarehouseStockDetail> warehouseBreakdowns;

    @Data
    @Builder
    public static class WarehouseStockDetail {
        private UUID warehouseId;
        private String warehouseName;
        private Integer quantityOnHand;
        private Integer quantityReserved;
        private Integer quantityAvailable;
    }
}
