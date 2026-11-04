package com.crm.inventory.controller;

import com.crm.inventory.dto.PosCheckoutRequest;
import com.crm.inventory.model.Customer;
import com.crm.inventory.model.PosInvoice;
import com.crm.inventory.service.PosService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/pos")
@RequiredArgsConstructor
@Tag(name = "Clothing Shop POS & CRM", description = "Multi-item retail billing counter & customer loyalty")
public class PosController {

    private final PosService posService;

    @PostMapping("/checkout")
    @Operation(summary = "Process multi-item retail POS bill, deduct stock, and update customer CRM")
    public ResponseEntity<PosInvoice> checkout(@Valid @RequestBody PosCheckoutRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(posService.processCheckout(request));
    }

    @GetMapping("/invoices")
    @Operation(summary = "List recent POS counter bills")
    public ResponseEntity<List<PosInvoice>> listInvoices() {
        return ResponseEntity.ok(posService.getRecentInvoices());
    }

    @GetMapping("/customers")
    @Operation(summary = "List all retail customers with total spend and visit count")
    public ResponseEntity<List<Customer>> listCustomers() {
        return ResponseEntity.ok(posService.getAllCustomers());
    }
}
