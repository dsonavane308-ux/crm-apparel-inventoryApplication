package com.crm.inventory.controller;

import com.crm.inventory.dto.AnalyticsReportResponse;
import com.crm.inventory.model.Customer;
import com.crm.inventory.model.PosInvoice;
import com.crm.inventory.model.PosInvoiceItem;
import com.crm.inventory.repository.CustomerRepository;
import com.crm.inventory.repository.PosInvoiceRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
@Tag(name = "Shop Business Intelligence & Analytics", description = "Fast/Slow selling, brand profit margins, seasonal trends, and customer RFM analytics")
public class AnalyticsController {

    private final PosInvoiceRepository invoiceRepository;
    private final CustomerRepository customerRepository;

    @GetMapping("/dashboard")
    @Operation(summary = "Generate comprehensive retail analytics report for day, month, year, and brand comparisons")
    public ResponseEntity<AnalyticsReportResponse> getAnalyticsReport() {
        List<PosInvoice> invoices = invoiceRepository.findAll();
        List<Customer> customers = customerRepository.findAll();

        // 1. Flatten all invoice line items
        List<PosInvoiceItem> allItems = invoices.stream()
                .flatMap(inv -> inv.getItems().stream())
                .collect(Collectors.toList());

        // 2. Product Velocity & Profitability (Group by SKU)
        Map<String, List<PosInvoiceItem>> itemGroups = allItems.stream()
                .collect(Collectors.groupingBy(PosInvoiceItem::getSku));

        List<AnalyticsReportResponse.ProductVelocityMetric> productVelocities = new ArrayList<>();
        itemGroups.forEach((sku, lineItems) -> {
            String name = lineItems.get(0).getItemName();
            String size = lineItems.get(0).getSize();
            String color = lineItems.get(0).getColor();
            long units = lineItems.stream().mapToInt(PosInvoiceItem::getQuantity).sum();
            BigDecimal rev = lineItems.stream().map(PosInvoiceItem::getLineTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
            
            BigDecimal avgMargin = lineItems.stream()
                    .map(PosInvoiceItem::getProfitPercentage)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .divide(BigDecimal.valueOf(Math.max(1, lineItems.size())), 2, RoundingMode.HALF_UP);

            String status = units > 15 ? "FAST-SELLING" : (units > 5 ? "MODERATE" : "SLOW-SELLING");

            productVelocities.add(AnalyticsReportResponse.ProductVelocityMetric.builder()
                    .sku(sku)
                    .itemName(name)
                    .size(size)
                    .color(color)
                    .totalUnitsSold(units)
                    .totalRevenue(rev)
                    .averageProfitMargin(avgMargin)
                    .velocityStatus(status)
                    .build());
        });

        productVelocities.sort((a, b) -> Long.compare(b.getTotalUnitsSold(), a.getTotalUnitsSold()));

        // 3. Customer Segments (VIP, Regular, New)
        List<AnalyticsReportResponse.CustomerSegmentMetric> topCustomers = customers.stream()
                .sorted((a, b) -> b.getTotalSpent().compareTo(a.getTotalSpent()))
                .map(c -> {
                    String tier = c.getVisitCount() >= 5 ? "VIP LOYALIST" : (c.getVisitCount() >= 2 ? "REGULAR" : "NEW CUSTOMER");
                    return AnalyticsReportResponse.CustomerSegmentMetric.builder()
                            .customerName(c.getName())
                            .mobile(c.getMobile())
                            .visitCount(c.getVisitCount())
                            .totalSpent(c.getTotalSpent())
                            .customerTier(tier)
                            .build();
                })
                .collect(Collectors.toList());

        // 4. Monthly Trends
        Map<String, List<PosInvoice>> monthlyGroups = invoices.stream()
                .collect(Collectors.groupingBy(inv -> inv.getCreatedAt() != null ?
                        inv.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM")) : "2026-11"));

        List<AnalyticsReportResponse.MonthlyTrendMetric> monthlyTrends = new ArrayList<>();
        monthlyGroups.forEach((ym, invs) -> {
            long bills = invs.size();
            BigDecimal rev = invs.stream().map(PosInvoice::getGrandTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal profit = invs.stream()
                    .flatMap(i -> i.getItems().stream())
                    .map(PosInvoiceItem::getProfitAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            monthlyTrends.add(AnalyticsReportResponse.MonthlyTrendMetric.builder()
                    .yearMonth(ym)
                    .totalBills(bills)
                    .totalRevenue(rev)
                    .totalProfit(profit)
                    .build());
        });

        monthlyTrends.sort(Comparator.comparing(AnalyticsReportResponse.MonthlyTrendMetric::getYearMonth));

        return ResponseEntity.ok(AnalyticsReportResponse.builder()
                .productVelocities(productVelocities)
                .topCustomers(topCustomers)
                .monthlyTrends(monthlyTrends)
                .brandProfits(new ArrayList<>()) // Placeholder for brand comparison
                .build());
    }
}
