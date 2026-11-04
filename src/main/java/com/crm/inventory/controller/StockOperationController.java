package com.crm.inventory.controller;

import com.crm.inventory.dto.*;
import com.crm.inventory.service.StockOperationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/stock/operations")
@RequiredArgsConstructor
@Tag(name = "Stock Operations", description = "Transactional inventory movement operations")
public class StockOperationController {

    private final StockOperationService stockOperationService;

    @PostMapping("/receive")
    @Operation(summary = "Inward stock from supplier or purchase order")
    public ResponseEntity<StockOperationResult> receiveStock(@Valid @RequestBody ReceiveStockRequest request) {
        return ResponseEntity.ok(stockOperationService.receiveStock(request));
    }

    @PostMapping("/transfer")
    @Operation(summary = "Transfer inventory between two warehouses")
    public ResponseEntity<StockOperationResult> transferStock(@Valid @RequestBody TransferStockRequest request) {
        return ResponseEntity.ok(stockOperationService.transferStock(request));
    }

    @PostMapping("/adjust")
    @Operation(summary = "Manual cycle-count inventory adjustment")
    public ResponseEntity<StockOperationResult> adjustStock(@Valid @RequestBody AdjustStockRequest request) {
        return ResponseEntity.ok(stockOperationService.adjustStock(request));
    }

    @PostMapping("/reserve")
    @Operation(summary = "Reserve inventory for CRM Deal / Quotation")
    public ResponseEntity<StockOperationResult> reserveStock(@Valid @RequestBody ReserveStockRequest request) {
        return ResponseEntity.ok(stockOperationService.reserveStock(request));
    }

    @PostMapping("/release")
    @Operation(summary = "Release locked stock back to available pool")
    public ResponseEntity<StockOperationResult> releaseStock(@Valid @RequestBody ReleaseStockRequest request) {
        return ResponseEntity.ok(stockOperationService.releaseStock(request));
    }

    @PostMapping("/deduct")
    @Operation(summary = "Finalize dispatch and deduct physical stock")
    public ResponseEntity<StockOperationResult> deductStock(@Valid @RequestBody DeductStockRequest request) {
        return ResponseEntity.ok(stockOperationService.deductStock(request));
    }
}
