package com.crm.inventory.controller;

import com.crm.inventory.dto.LedgerEntryResponse;
import com.crm.inventory.exception.ResourceNotFoundException;
import com.crm.inventory.model.StockTransaction;
import com.crm.inventory.repository.StockTransactionRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/stock/ledger")
@RequiredArgsConstructor
@Tag(name = "Stock Ledger & Audit", description = "Query immutable stock movement ledger")
public class StockLedgerController {

    private final StockTransactionRepository stockTransactionRepository;

    @GetMapping
    @Operation(summary = "Query immutable stock movement ledger")
    public ResponseEntity<Page<StockTransaction>> getLedgerTransactions(
            @RequestParam(required = false) UUID itemId,
            @RequestParam(required = false) String referenceId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        PageRequest pageRequest = PageRequest.of(page, size, Sort.by("createdAt").descending());

        if (itemId != null) {
            return ResponseEntity.ok(stockTransactionRepository.findByItemId(itemId, pageRequest));
        }
        if (referenceId != null && !referenceId.isBlank()) {
            return ResponseEntity.ok(stockTransactionRepository.findByReferenceId(referenceId, pageRequest));
        }
        return ResponseEntity.ok(stockTransactionRepository.findAll(pageRequest));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Fetch single transaction ledger record by ID")
    public ResponseEntity<LedgerEntryResponse> getLedgerEntryById(@PathVariable UUID id) {
        StockTransaction tx = stockTransactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ledger transaction not found with id: " + id));

        return ResponseEntity.ok(LedgerEntryResponse.builder()
                .id(tx.getId())
                .itemId(tx.getItemId())
                .fromWarehouseId(tx.getFromWarehouseId())
                .toWarehouseId(tx.getToWarehouseId())
                .type(tx.getType())
                .quantity(tx.getQuantity())
                .unitCost(tx.getUnitCost())
                .referenceId(tx.getReferenceId())
                .performedBy(tx.getPerformedBy())
                .notes(tx.getNotes())
                .timestamp(tx.getCreatedAt())
                .build());
    }
}
