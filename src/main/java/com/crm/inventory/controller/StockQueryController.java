package com.crm.inventory.controller;

import com.crm.inventory.dto.ItemStockBreakdownResponse;
import com.crm.inventory.dto.LowStockAlertResponse;
import com.crm.inventory.exception.ResourceNotFoundException;
import com.crm.inventory.model.Item;
import com.crm.inventory.model.StockLevel;
import com.crm.inventory.model.Warehouse;
import com.crm.inventory.repository.ItemRepository;
import com.crm.inventory.repository.StockLevelRepository;
import com.crm.inventory.repository.WarehouseRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/stock")
@RequiredArgsConstructor
@Tag(name = "Stock State & Alerts", description = "Query stock levels, distribution, and low-stock warnings")
public class StockQueryController {

    private final StockLevelRepository stockLevelRepository;
    private final ItemRepository itemRepository;
    private final WarehouseRepository warehouseRepository;

    @GetMapping
    @Operation(summary = "Query stock levels across facilities")
    public ResponseEntity<Page<StockLevel>> getStockLevels(
            @RequestParam(required = false) UUID warehouseId,
            @RequestParam(required = false) UUID itemId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(stockLevelRepository.findAll(PageRequest.of(page, size)));
    }

    @GetMapping("/items/{itemId}")
    @Operation(summary = "Get inventory breakdown across all warehouses for one SKU")
    public ResponseEntity<ItemStockBreakdownResponse> getStockByItem(@PathVariable UUID itemId) {
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Item not found with id: " + itemId));

        List<StockLevel> levels = stockLevelRepository.findByItemId(itemId);
        Map<UUID, Warehouse> warehouseMap = warehouseRepository.findAll().stream()
                .collect(Collectors.toMap(Warehouse::getId, w -> w));

        int totalOnHand = levels.stream().mapToInt(StockLevel::getQuantityOnHand).sum();
        int totalReserved = levels.stream().mapToInt(StockLevel::getQuantityReserved).sum();

        List<ItemStockBreakdownResponse.WarehouseStockDetail> details = levels.stream().map(sl -> {
            Warehouse wh = warehouseMap.get(sl.getWarehouseId());
            return ItemStockBreakdownResponse.WarehouseStockDetail.builder()
                    .warehouseId(sl.getWarehouseId())
                    .warehouseName(wh != null ? wh.getName() : "Unknown")
                    .quantityOnHand(sl.getQuantityOnHand())
                    .quantityReserved(sl.getQuantityReserved())
                    .quantityAvailable(sl.getQuantityOnHand() - sl.getQuantityReserved())
                    .build();
        }).collect(Collectors.toList());

        return ResponseEntity.ok(ItemStockBreakdownResponse.builder()
                .itemId(item.getId())
                .sku(item.getSku())
                .name(item.getName())
                .totalOnHand(totalOnHand)
                .totalReserved(totalReserved)
                .totalAvailable(totalOnHand - totalReserved)
                .warehouseBreakdowns(details)
                .build());
    }

    @GetMapping("/low-stock-alerts")
    @Operation(summary = "Retrieve items falling below their reorder threshold")
    public ResponseEntity<List<LowStockAlertResponse>> getLowStockAlerts() {
        List<Item> lowStockItems = itemRepository.findLowStockItems();
        List<LowStockAlertResponse> alerts = lowStockItems.stream().map(item -> {
            int available = stockLevelRepository.findByItemId(item.getId()).stream()
                    .mapToInt(sl -> sl.getQuantityOnHand() - sl.getQuantityReserved())
                    .sum();
            return LowStockAlertResponse.builder()
                    .itemId(item.getId())
                    .sku(item.getSku())
                    .name(item.getName())
                    .totalAvailable(available)
                    .reorderThreshold(item.getReorderThreshold())
                    .shortfall(Math.max(0, item.getReorderThreshold() - available))
                    .build();
        }).collect(Collectors.toList());

        return ResponseEntity.ok(alerts);
    }
}
