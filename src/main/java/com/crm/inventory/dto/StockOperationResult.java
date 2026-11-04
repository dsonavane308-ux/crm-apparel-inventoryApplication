package com.crm.inventory.dto;

import com.crm.inventory.model.TransactionType;
import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
public class StockOperationResult {
    private UUID transactionId;
    private UUID itemId;
    private UUID warehouseId;
    private TransactionType operationType;
    private Integer quantityImpacted;
    private Integer remainingOnHand;
    private Integer remainingReserved;
    private Integer remainingAvailable;
    private OffsetDateTime timestamp;
}
