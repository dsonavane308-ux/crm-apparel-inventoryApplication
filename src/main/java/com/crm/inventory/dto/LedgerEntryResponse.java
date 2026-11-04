package com.crm.inventory.dto;

import com.crm.inventory.model.TransactionType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
public class LedgerEntryResponse {
    private UUID id;
    private UUID itemId;
    private UUID fromWarehouseId;
    private UUID toWarehouseId;
    private TransactionType type;
    private Integer quantity;
    private BigDecimal unitCost;
    private String referenceId;
    private String performedBy;
    private String notes;
    private OffsetDateTime timestamp;
}
