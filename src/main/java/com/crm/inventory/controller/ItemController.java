package com.crm.inventory.controller;

import com.crm.inventory.dto.CreateItemRequest;
import com.crm.inventory.dto.UpdateItemRequest;
import com.crm.inventory.exception.ResourceNotFoundException;
import com.crm.inventory.model.Item;
import com.crm.inventory.model.ItemStatus;
import com.crm.inventory.repository.ItemRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/items")
@RequiredArgsConstructor
@Tag(name = "Item Management", description = "Apparel SKU & Barcode Catalog")
public class ItemController {

    private final ItemRepository itemRepository;

    @PostMapping
    @Operation(summary = "Create a new apparel item / SKU")
    public ResponseEntity<Item> createItem(@Valid @RequestBody CreateItemRequest request) {
        if (itemRepository.existsBySku(request.getSku())) {
            throw new IllegalStateException("SKU already exists: " + request.getSku());
        }

        Item item = Item.builder()
                .sku(request.getSku())
                .name(request.getName())
                .description(request.getDescription())
                .size(request.getSize())
                .color(request.getColor())
                .brand(request.getBrand())
                .barcode(request.getBarcode() != null && !request.getBarcode().isBlank() ? request.getBarcode() : request.getSku())
                .categoryId(request.getCategoryId())
                .unitPrice(request.getUnitPrice())
                .costPrice(request.getCostPrice())
                .reorderThreshold(request.getReorderThreshold())
                .status(ItemStatus.ACTIVE)
                .build();

        return ResponseEntity.status(HttpStatus.CREATED).body(itemRepository.save(item));
    }

    @GetMapping
    @Operation(summary = "List items with pagination and search by name, SKU, or barcode")
    public ResponseEntity<Page<Item>> listItems(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) String search) {

        PageRequest pageRequest = PageRequest.of(page, size, Sort.by("createdAt").descending());
        if (search != null && !search.isBlank()) {
            return ResponseEntity.ok(itemRepository
                    .findByNameContainingIgnoreCaseOrSkuContainingIgnoreCaseOrBarcodeContainingIgnoreCase(
                            search, search, search, pageRequest));
        }
        return ResponseEntity.ok(itemRepository.findAll(pageRequest));
    }

    @GetMapping("/barcode/{barcode}")
    @Operation(summary = "Fast lookup by barcode or SKU for POS scanner")
    public ResponseEntity<Item> getByBarcode(@PathVariable String barcode) {
        Item item = itemRepository.findByBarcode(barcode)
                .or(() -> itemRepository.findBySku(barcode))
                .orElseThrow(() -> new ResourceNotFoundException("No garment found with barcode/SKU: " + barcode));
        return ResponseEntity.ok(item);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Item> getItemById(@PathVariable UUID id) {
        Item item = itemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Item not found with id: " + id));
        return ResponseEntity.ok(item);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Item> updateItem(@PathVariable UUID id, @Valid @RequestBody UpdateItemRequest request) {
        Item item = itemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Item not found with id: " + id));

        if (request.getName() != null) item.setName(request.getName());
        if (request.getDescription() != null) item.setDescription(request.getDescription());
        if (request.getSize() != null) item.setSize(request.getSize());
        if (request.getColor() != null) item.setColor(request.getColor());
        if (request.getBrand() != null) item.setBrand(request.getBrand());
        if (request.getBarcode() != null) item.setBarcode(request.getBarcode());
        if (request.getCategoryId() != null) item.setCategoryId(request.getCategoryId());
        if (request.getUnitPrice() != null) item.setUnitPrice(request.getUnitPrice());
        if (request.getCostPrice() != null) item.setCostPrice(request.getCostPrice());
        if (request.getReorderThreshold() != null) item.setReorderThreshold(request.getReorderThreshold());
        if (request.getStatus() != null) item.setStatus(request.getStatus());

        return ResponseEntity.ok(itemRepository.save(item));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteItem(@PathVariable UUID id) {
        Item item = itemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Item not found with id: " + id));
        item.setStatus(ItemStatus.INACTIVE);
        itemRepository.save(item);
        return ResponseEntity.noContent().build();
    }
}
