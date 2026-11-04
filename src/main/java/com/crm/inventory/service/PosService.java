package com.crm.inventory.service;

import com.crm.inventory.dto.DeductStockRequest;
import com.crm.inventory.dto.PosCheckoutRequest;
import com.crm.inventory.exception.ResourceNotFoundException;
import com.crm.inventory.model.Customer;
import com.crm.inventory.model.Item;
import com.crm.inventory.model.PosInvoice;
import com.crm.inventory.model.PosInvoiceItem;
import com.crm.inventory.repository.CustomerRepository;
import com.crm.inventory.repository.ItemRepository;
import com.crm.inventory.repository.PosInvoiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class PosService {

    private final PosInvoiceRepository posInvoiceRepository;
    private final CustomerRepository customerRepository;
    private final ItemRepository itemRepository;
    private final StockOperationService stockOperationService;

    @Transactional
    public PosInvoice processCheckout(PosCheckoutRequest request) {
        String invoiceNo = "INV-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
                + "-" + ThreadLocalRandom.current().nextInt(1000, 9999);

        PosInvoice invoice = PosInvoice.builder()
                .invoiceNumber(invoiceNo)
                .warehouseId(request.getWarehouseId())
                .customerName(request.getCustomerName() != null && !request.getCustomerName().isBlank()
                        ? request.getCustomerName() : "Walk-in Customer")
                .customerMobile(request.getCustomerMobile())
                .paymentMode(request.getPaymentMode() != null ? request.getPaymentMode() : "CASH")
                .items(new ArrayList<>())
                .build();

        BigDecimal subtotal = BigDecimal.ZERO;

        for (PosCheckoutRequest.CartItemDto cartItem : request.getItems()) {
            Item item = itemRepository.findById(cartItem.getItemId())
                    .orElseThrow(() -> new ResourceNotFoundException("Item not found: " + cartItem.getItemId()));

            // Deduct stock atomically via StockOperationService
            DeductStockRequest deductReq = new DeductStockRequest();
            deductReq.setWarehouseId(request.getWarehouseId());
            deductReq.setItemId(item.getId());
            deductReq.setQuantity(cartItem.getQuantity());
            deductReq.setCrmReferenceId(invoiceNo);
            deductReq.setWasReserved(cartItem.isWasReserved());
            stockOperationService.deductStock(deductReq);

            BigDecimal lineTotal = item.getUnitPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity()));
            BigDecimal lineCostTotal = item.getCostPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity()));
            BigDecimal profitAmount = lineTotal.subtract(lineCostTotal);
            BigDecimal profitPercentage = BigDecimal.ZERO;
            
            if (lineCostTotal.compareTo(BigDecimal.ZERO) > 0) {
                profitPercentage = profitAmount.divide(lineCostTotal, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100));
            }

            subtotal = subtotal.add(lineTotal);

            PosInvoiceItem lineItem = PosInvoiceItem.builder()
                    .invoice(invoice)
                    .itemId(item.getId())
                    .sku(item.getSku())
                    .itemName(item.getName())
                    .size(item.getSize())
                    .color(item.getColor())
                    .quantity(cartItem.getQuantity())
                    .unitPrice(item.getUnitPrice())
                    .lineTotal(lineTotal)
                    .costPrice(item.getCostPrice())
                    .profitAmount(profitAmount)
                    .profitPercentage(profitPercentage)
                    .build();

            invoice.getItems().add(lineItem);
        }

        BigDecimal discount = request.getDiscountAmount() != null ? request.getDiscountAmount() : BigDecimal.ZERO;
        BigDecimal taxable = subtotal.subtract(discount).max(BigDecimal.ZERO);
        BigDecimal taxPct = request.getTaxPercent() != null ? request.getTaxPercent() : BigDecimal.ZERO;
        BigDecimal taxAmount = taxable.multiply(taxPct).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        BigDecimal grandTotal = taxable.add(taxAmount);

        invoice.setSubtotal(subtotal);
        invoice.setDiscountAmount(discount);
        invoice.setTaxAmount(taxAmount);
        invoice.setGrandTotal(grandTotal);

        // Auto-save/update Customer CRM profile
        if (request.getCustomerMobile() != null && !request.getCustomerMobile().isBlank()) {
            String cleanMobile = request.getCustomerMobile().trim();
            Customer customer = customerRepository.findByMobile(cleanMobile)
                    .orElseGet(() -> Customer.builder()
                            .mobile(cleanMobile)
                            .name(invoice.getCustomerName())
                            .totalSpent(BigDecimal.ZERO)
                            .visitCount(0)
                            .build());

            if (request.getCustomerName() != null && !request.getCustomerName().isBlank()) {
                customer.setName(request.getCustomerName().trim());
            }
            customer.setTotalSpent(customer.getTotalSpent().add(grandTotal));
            customer.setVisitCount(customer.getVisitCount() + 1);
            Customer savedCustomer = customerRepository.save(customer);
            invoice.setCustomerId(savedCustomer.getId());
        }

        return posInvoiceRepository.save(invoice);
    }

    public List<PosInvoice> getRecentInvoices() {
        return posInvoiceRepository.findTop50ByOrderByCreatedAtDesc();
    }

    public List<Customer> getAllCustomers() {
        return customerRepository.findAll();
    }
}
