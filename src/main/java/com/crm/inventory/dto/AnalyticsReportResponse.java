package com.crm.inventory.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class AnalyticsReportResponse {
    private List<ProductVelocityMetric> productVelocities;
    private List<BrandProfitMetric> brandProfits;
    private List<CustomerSegmentMetric> topCustomers;
    private List<MonthlyTrendMetric> monthlyTrends;

    @Data
    @Builder
    public static class ProductVelocityMetric {
        private String sku;
        private String itemName;
        private String size;
        private String color;
        private long totalUnitsSold;
        private BigDecimal totalRevenue;
        private BigDecimal averageProfitMargin;
        private String velocityStatus; // FAST-SELLING, MODERATE, SLOW-SELLING
    }

    @Data
    @Builder
    public static class BrandProfitMetric {
        private String brand;
        private long unitsSold;
        private BigDecimal revenue;
        private BigDecimal totalProfit;
        private BigDecimal profitMarginPercentage;
    }

    @Data
    @Builder
    public static class CustomerSegmentMetric {
        private String customerName;
        private String mobile;
        private int visitCount;
        private BigDecimal totalSpent;
        private String customerTier; // VIP, REGULAR, NEW
    }

    @Data
    @Builder
    public static class MonthlyTrendMetric {
        private String yearMonth; // e.g. "2026-11"
        private long totalBills;
        private BigDecimal totalRevenue;
        private BigDecimal totalProfit;
    }
}
