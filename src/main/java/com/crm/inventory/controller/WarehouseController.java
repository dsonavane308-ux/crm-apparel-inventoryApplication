package com.crm.inventory.controller;

import com.crm.inventory.dto.CreateWarehouseRequest;
import com.crm.inventory.dto.UpdateWarehouseRequest;
import com.crm.inventory.exception.ResourceNotFoundException;
import com.crm.inventory.model.Warehouse;
import com.crm.inventory.repository.WarehouseRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/warehouses")
@RequiredArgsConstructor
@Tag(name = "Warehouse Management", description = "Endpoints for facilities and storage locations")
public class WarehouseController {

    private final WarehouseRepository warehouseRepository;

    @PostMapping
    @Operation(summary = "Register a new warehouse facility")
    public ResponseEntity<Warehouse> createWarehouse(@Valid @RequestBody CreateWarehouseRequest request) {
        if (warehouseRepository.existsByCode(request.getCode())) {
            throw new IllegalStateException("Warehouse code already exists: " + request.getCode());
        }

        Warehouse warehouse = Warehouse.builder()
                .code(request.getCode())
                .name(request.getName())
                .address(request.getAddress())
                .isActive(true)
                .build();

        return ResponseEntity.status(HttpStatus.CREATED).body(warehouseRepository.save(warehouse));
    }

    @GetMapping
    @Operation(summary = "List warehouses")
    public ResponseEntity<List<Warehouse>> listWarehouses(@RequestParam(defaultValue = "true") boolean activeOnly) {
        if (activeOnly) {
            return ResponseEntity.ok(warehouseRepository.findByIsActiveTrue());
        }
        return ResponseEntity.ok(warehouseRepository.findAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get warehouse details by ID")
    public ResponseEntity<Warehouse> getWarehouseById(@PathVariable UUID id) {
        Warehouse warehouse = warehouseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse not found with id: " + id));
        return ResponseEntity.ok(warehouse);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update warehouse details")
    public ResponseEntity<Warehouse> updateWarehouse(@PathVariable UUID id, @Valid @RequestBody UpdateWarehouseRequest request) {
        Warehouse warehouse = warehouseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse not found with id: " + id));

        if (request.getName() != null) warehouse.setName(request.getName());
        if (request.getAddress() != null) warehouse.setAddress(request.getAddress());
        if (request.getIsActive() != null) warehouse.setIsActive(request.getIsActive());

        return ResponseEntity.ok(warehouseRepository.save(warehouse));
    }
}
