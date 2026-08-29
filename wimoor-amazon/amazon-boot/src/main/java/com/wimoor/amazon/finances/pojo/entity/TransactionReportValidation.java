package com.wimoor.amazon.finances.pojo.entity;

import lombok.Data;
import java.math.BigDecimal;
import java.util.Date;
import java.util.Map;
import java.util.HashMap;

/**
 * 交易报告校验结果类
 * 用于存储解析过程中的统计数据，确保与数据库汇总一致
 */
@Data
public class TransactionReportValidation {
    
    /**
     * 最小开始时间
     */
    private Date minDateTime;
    
    /**
     * 最大结束时间
     */
    private Date maxDateTime;
    
    /**
     * 总记录数
     */
    private long totalRecords;
    
    /**
     * 按日期汇总的各项费用
     * key: 日期字符串 (yyyy-MM-dd)
     * value: 该日期的各项费用汇总
     */
    private Map<String, FeeSummary> dailySummaryMap = new HashMap<>();
    
    /**
     * 总计费用汇总
     */
    private FeeSummary totalSummary = new FeeSummary();
    
    /**
     * 费用汇总内部类
     */
    @Data
    public static class FeeSummary {
        private BigDecimal productSales = BigDecimal.ZERO;
        private BigDecimal productSalesTax = BigDecimal.ZERO;
        private BigDecimal shippingCredits = BigDecimal.ZERO;
        private BigDecimal shippingCreditsTax = BigDecimal.ZERO;
        private BigDecimal giftwrapCredits = BigDecimal.ZERO;
        private BigDecimal giftwrapCreditsTax = BigDecimal.ZERO;
        private BigDecimal promotionalRebates = BigDecimal.ZERO;
        private BigDecimal promotionalRebatesTax = BigDecimal.ZERO;
        private BigDecimal marketplaceWithheldTax = BigDecimal.ZERO;
        private BigDecimal sellingFees = BigDecimal.ZERO;
        private BigDecimal fbaFees = BigDecimal.ZERO;
        private BigDecimal otherTransactionFees = BigDecimal.ZERO;
        private BigDecimal other = BigDecimal.ZERO;
        private BigDecimal total = BigDecimal.ZERO;
        
        /**
         * 累加费用
         */
        public void addFee(TransactionReport report) {
            if (report.getProductSales() != null) {
                productSales = productSales.add(report.getProductSales());
            }
            if (report.getProductSalesTax() != null) {
                productSalesTax = productSalesTax.add(report.getProductSalesTax());
            }
            if (report.getShippingCredits() != null) {
                shippingCredits = shippingCredits.add(report.getShippingCredits());
            }
            if (report.getShippingCreditsTax() != null) {
                shippingCreditsTax = shippingCreditsTax.add(report.getShippingCreditsTax());
            }
            if (report.getGiftwrapCredits() != null) {
                giftwrapCredits = giftwrapCredits.add(report.getGiftwrapCredits());
            }
            if (report.getGiftwrapCreditsTax() != null) {
                giftwrapCreditsTax = giftwrapCreditsTax.add(report.getGiftwrapCreditsTax());
            }
            if (report.getPromotionalRebates() != null) {
                promotionalRebates = promotionalRebates.add(report.getPromotionalRebates());
            }
            if (report.getPromotionalRebatesTax() != null) {
                promotionalRebatesTax = promotionalRebatesTax.add(report.getPromotionalRebatesTax());
            }
            if (report.getMarketplaceWithheldTax() != null) {
                marketplaceWithheldTax = marketplaceWithheldTax.add(report.getMarketplaceWithheldTax());
            }
            if (report.getSellingFees() != null) {
                sellingFees = sellingFees.add(report.getSellingFees());
            }
            if (report.getFbaFees() != null) {
                fbaFees = fbaFees.add(report.getFbaFees());
            }
            if (report.getOtherTransactionFees() != null) {
                otherTransactionFees = otherTransactionFees.add(report.getOtherTransactionFees());
            }
            if (report.getOther() != null) {
                other = other.add(report.getOther());
            }
            if (report.getTotal() != null) {
                total = total.add(report.getTotal());
            }
        }
        
        /**
         * 比较两个费用汇总是否一致
         */
        public boolean equalsFee(FeeSummary target) {
            if (target == null) return false;
            return productSales.compareTo(target.getProductSales()) == 0
                && productSalesTax.compareTo(target.getProductSalesTax()) == 0
                && shippingCredits.compareTo(target.getShippingCredits()) == 0
                && shippingCreditsTax.compareTo(target.getShippingCreditsTax()) == 0
                && giftwrapCredits.compareTo(target.getGiftwrapCredits()) == 0
                && giftwrapCreditsTax.compareTo(target.getGiftwrapCreditsTax()) == 0
                && promotionalRebates.compareTo(target.getPromotionalRebates()) == 0
                && promotionalRebatesTax.compareTo(target.getPromotionalRebatesTax()) == 0
                && marketplaceWithheldTax.compareTo(target.getMarketplaceWithheldTax()) == 0
                && sellingFees.compareTo(target.getSellingFees()) == 0
                && fbaFees.compareTo(target.getFbaFees()) == 0
                && otherTransactionFees.compareTo(target.getOtherTransactionFees()) == 0
                && other.compareTo(target.getOther()) == 0
                && total.compareTo(target.getTotal()) == 0;
        }
    }
    
    /**
     * 添加一条记录到校验结果中
     */
    public void addRecord(TransactionReport report) {
        if (report == null || report.getDateTime() == null) {
            return;
        }
        
        // 更新最小/最大时间
        if (minDateTime == null || report.getDateTime().before(minDateTime)) {
            minDateTime = report.getDateTime();
        }
        if (maxDateTime == null || report.getDateTime().after(maxDateTime)) {
            maxDateTime = report.getDateTime();
        }
        
        // 增加记录数
        totalRecords++;
        
        // 按日期汇总
        String dateKey = new java.text.SimpleDateFormat("yyyy-MM-dd").format(report.getDateTime());
        FeeSummary dailySummary = dailySummaryMap.computeIfAbsent(dateKey, k -> new FeeSummary());
        dailySummary.addFee(report);
        
        // 总计汇总
        totalSummary.addFee(report);
    }
    
    /**
     * 获取所有日期的集合
     */
    public java.util.Set<String> getDateKeys() {
        return dailySummaryMap.keySet();
    }
    
    /**
     * 获取指定日期的费用汇总
     */
    public FeeSummary getDailySummary(String dateKey) {
        return dailySummaryMap.get(dateKey);
    }
}