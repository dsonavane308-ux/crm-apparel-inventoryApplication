package com.crm.inventory.service;

import com.crm.inventory.dto.*;
import com.crm.inventory.exception.InsufficientStockException;
import com.crm.inventory.exception.ResourceNotFoundException;
import com.crm.inventory.model.StockLevel;
import com.crm.inventory.model.StockTransaction;
import com.crm.inventory.model.TransactionType;
import com.crm.inventory.repository.ItemRepository;
import com.crm.inventory.repository.StockLevelRepository;
import com.crm.inventory.repository.StockTransactionRepository;
import com.crm.inventory.repository.WarehouseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StockOperationService {

    private final StockLevelRepository stockLevelRepository;
    private final StockTransactionRepository stockTransactionRepository;
    private final ItemRepository itemRepository;
    private final WarehouseRepository warehouseRepository;

    @Transactional
    public StockOperationResult receiveStock(ReceiveStockRequest request) {
        validateItemAndWarehouse(request.getItemId(), request.getWarehouseId());

        StockLevel stockLevel = stockLevelRepository
                .findByItemIdAndWarehouseIdForUpdate(request.getItemId(), request.getWarehouseId())
                .orElseGet(() -> StockLevel.builder()
                        .itemId(request.getItemId())
                        .warehouseId(request.getWarehouseId())
                        .quantityOnHand(0)
                        .quantityReserved(0)
                        .build());

        stockLevel.setQuantityOnHand(stockLevel.getQuantityOnHand() + request.getQuantity());
        StockLevel saved = stockLevelRepository.save(stockLevel);

        StockTransaction tx = stockTransactionRepository.save(StockTransaction.builder()
                .itemId(request.getItemId())
                .toWarehouseId(request.getWarehouseId())
                .type(TransactionType.RECEIVE)
                .quantity(request.getQuantity())
                .unitCost(request.getUnitCost())
                .referenceId(request.getPoReference())
                .notes(request.getNotes())
                .performedBy("SYSTEM")
                .build());

        return buildResult(tx.getId(), saved, TransactionType.RECEIVE, request.getQuantity(), OffsetDateTime.now());
    }

    @Transactional
    public StockOperationResult transferStock(TransferStockRequest request) {
        if (request.getFromWarehouseId().equals(request.getToWarehouseId())) {
            throw new IllegalStateException("Source and destination warehouses cannot be identical");
        }
        validateItemAndWarehouse(request.getItemId(), request.getFromWarehouseId());
        validateItemAndWarehouse(request.getItemId(), request.getToWarehouseId());

        StockLevel origin = stockLevelRepository
                .findByItemIdAndWarehouseIdForUpdate(request.getItemId(), request.getFromWarehouseId())
                .orElseThrow(() -> new ResourceNotFoundException("No inventory found at source warehouse"));

        int availableAtOrigin = origin.getQuantityOnHand() - origin.getQuantityReserved();
        if (availableAtOrigin < request.getQuantity()) {
            throw new InsufficientStockException("Insufficient unreserved stock at origin warehouse to transfer. Available: " + availableAtOrigin);
        }

        StockLevel destination = stockLevelRepository
                .findByItemIdAndWarehouseIdForUpdate(request.getItemId(), request.getToWarehouseId())
                .orElseGet(() -> StockLevel.builder()
                        .itemId(request.getItemId())
                        .warehouseId(request.getToWarehouseId())
                        .quantityOnHand(0)
                        .quantityReserved(0)
                        .build());

        origin.setQuantityOnHand(origin.getQuantityOnHand() - request.getQuantity());
        destination.setQuantityOnHand(destination.getQuantityOnHand() + request.getQuantity());

        stockLevelRepository.save(origin);
        stockLevelRepository.save(destination);

        StockTransaction tx = stockTransactionRepository.save(StockTransaction.builder()
                .itemId(request.getItemId())
                .fromWarehouseId(request.getFromWarehouseId())
                .toWarehouseId(request.getToWarehouseId())
                .type(TransactionType.TRANSFER)
                .quantity(request.getQuantity())
                .notes(request.getReason())
                .performedBy("LOGISTICS_USER")
                .build());

        return buildResult(tx.getId(), origin, TransactionType.TRANSFER, request.getQuantity(), tx.getCreatedAt());
    }

    @Transactional
    public StockOperationResult adjustStock(AdjustStockRequest request) {
        if (request.getQuantityDelta() == 0) {
            throw new IllegalStateException("Adjustment delta cannot be 0");
        }
        validateItemAndWarehouse(request.getItemId(), request.getWarehouseId());

        StockLevel stockLevel = stockLevelRepository
                .findByItemIdAndWarehouseIdForUpdate(request.getItemId(), request.getWarehouseId())
                .orElseGet(() -> StockLevel.builder()
                        .itemId(request.getItemId())
                        .warehouseId(request.getWarehouseId())
                        .quantityOnHand(0)
                        .quantityReserved(0)
                        .build());

        int newOnHand = stockLevel.getQuantityOnHand() + request.getQuantityDelta();
        if (newOnHand < stockLevel.getQuantityReserved()) {
            throw new InsufficientStockException("Cannot adjust stock below currently reserved count (" + stockLevel.getQuantityReserved() + ")");
        }
        if (newOnHand < 0) {
            throw new InsufficientStockException("Adjustment cannot cause physical stock to become negative");
        }

        stockLevel.setQuantityOnHand(newOnHand);
        StockLevel saved = stockLevelRepository.save(stockLevel);

        StockTransaction tx = stockTransactionRepository.save(StockTransaction.builder()
                .itemId(request.getItemId())
                .toWarehouseId(request.getWarehouseId())
                .type(TransactionType.ADJUSTMENT)
                .quantity(request.getQuantityDelta())
                .notes(request.getReason())
                .performedBy("AUDITOR")
                .build());

        return buildResult(tx.getId(), saved, TransactionType.ADJUSTMENT, request.getQuantityDelta(), tx.getCreatedAt());
    }

    @Transactional
    public StockOperationResult reserveStock(ReserveStockRequest request) {
        validateItemAndWarehouse(request.getItemId(), request.getWarehouseId());

        StockLevel stockLevel = stockLevelRepository
                .findByItemIdAndWarehouseIdForUpdate(request.getItemId(), request.getWarehouseId())
                .orElseThrow(() -> new ResourceNotFoundException("No inventory record found for item in this warehouse"));

        int available = stockLevel.getQuantityOnHand() - stockLevel.getQuantityReserved();
        if (available < request.getQuantity()) {
            throw new InsufficientStockException("Insufficient stock to reserve. Available: " + available + ", Requested: " + request.getQuantity());
        }

        stockLevel.setQuantityReserved(stockLevel.getQuantityReserved() + request.getQuantity());
        StockLevel saved = stockLevelRepository.save(stockLevel);

        StockTransaction tx = stockTransactionRepository.save(StockTransaction.builder()
                .itemId(request.getItemId())
                .fromWarehouseId(request.getWarehouseId())
                .type(TransactionType.RESERVE)
                .quantity(request.getQuantity())
                .referenceId(request.getCrmReferenceId())
                .performedBy("CRM_AUTO")
                .build());

        return buildResult(tx.getId(), saved, TransactionType.RESERVE, request.getQuantity(), tx.getCreatedAt());
    }

    @Transactional
    public StockOperationResult releaseStock(ReleaseStockRequest request) {
        validateItemAndWarehouse(request.getItemId(), request.getWarehouseId());

        StockLevel stockLevel = stockLevelRepository
                .findByItemIdAndWarehouseIdForUpdate(request.getItemId(), request.getWarehouseId())
                .orElseThrow(() -> new ResourceNotFoundException("No inventory record found for item in this warehouse"));

        if (stockLevel.getQuantityReserved() < request.getQuantity()) {
            throw new InsufficientStockException("Cannot release more than reserved. Currently reserved: " + stockLevel.getQuantityReserved());
        }

        stockLevel.setQuantityReserved(stockLevel.getQuantityReserved() - request.getQuantity());
        StockLevel saved = stockLevelRepository.save(stockLevel);

        StockTransaction tx = stockTransactionRepository.save(StockTransaction.builder()
                .itemId(request.getItemId())
                .fromWarehouseId(request.getWarehouseId())
                .type(TransactionType.RELEASE)
                .quantity(request.getQuantity())
                .referenceId(request.getCrmReferenceId())
                .performedBy("CRM_AUTO")
                .build());

        return buildResult(tx.getId(), saved, TransactionType.RELEASE, request.getQuantity(), tx.getCreatedAt());
    }

    @Transactional
    public StockOperationResult deductStock(DeductStockRequest request) {
        validateItemAndWarehouse(request.getItemId(), request.getWarehouseId());

        StockLevel stockLevel = stockLevelRepository
                .findByItemIdAndWarehouseIdForUpdate(request.getItemId(), request.getWarehouseId())
                .orElseThrow(() -> new ResourceNotFoundException("No inventory record found for item in this warehouse"));

        if (request.isWasReserved()) {
            if (stockLevel.getQuantityReserved() < request.getQuantity() || stockLevel.getQuantityOnHand() < request.getQuantity()) {
                throw new InsufficientStockException("Deduct failed: reserved quantity or on-hand is insufficient.");
            }
            stockLevel.setQuantityReserved(stockLevel.getQuantityReserved() - request.getQuantity());
        } else {
            int available = stockLevel.getQuantityOnHand() - stockLevel.getQuantityReserved();
            if (available < request.getQuantity()) {
                throw new InsufficientStockException("Deduct failed: unreserved available stock is insufficient.");
            }
        }

        stockLevel.setQuantityOnHand(stockLevel.getQuantityOnHand() - request.getQuantity());
        StockLevel saved = stockLevelRepository.save(stockLevel);

        StockTransaction tx = stockTransactionRepository.save(StockTransaction.builder()
                .itemId(request.getItemId())
                .fromWarehouseId(request.getWarehouseId())
                .type(TransactionType.DEDUCT)
                .quantity(request.getQuantity())
                .referenceId(request.getCrmReferenceId())
                .performedBy("DISPATCH_AGENT")
                .build());

        return buildResult(tx.getId(), saved, TransactionType.DEDUCT, request.getQuantity(), tx.getCreatedAt());
    }

    private void validateItemAndWarehouse(UUID itemId, UUID warehouseId) {
        if (!itemRepository.existsById(itemId)) {
            throw new ResourceNotFoundException("Item not found with id: " + itemId);
        }
        if (!warehouseRepository.existsById(warehouseId)) {
            throw new ResourceNotFoundException("Warehouse not found with id: " + warehouseId);
        }
    }

    private StockOperationResult buildResult(UUID txId, StockLevel level, TransactionType type, int impacted, OffsetDateTime timestamp) {
        return StockOperationResult.builder()
                .transactionId(txId)
                .itemId(level.getItemId())
                .warehouseId(level.getWarehouseId())
                .operationType(type)
                .quantityImpacted(impacted)
                .remainingOnHand(level.getQuantityOnHand())
                .remainingReserved(level.getQuantityReserved())
                .remainingAvailable(level.getQuantityOnHand() - level.getQuantityReserved())
                .timestamp(timestamp)
                .build();
    }
}
