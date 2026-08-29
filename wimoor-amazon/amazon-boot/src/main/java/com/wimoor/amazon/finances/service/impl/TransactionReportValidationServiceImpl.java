package com.wimoor.amazon.finances.service.impl;

import com.wimoor.amazon.finances.mapper.TransactionReportMapper;
import com.wimoor.amazon.finances.pojo.entity.TransactionReportValidation;
import com.wimoor.amazon.finances.service.TransactionReportValidationService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 交易报告校验服务实现类
 */
@Service
public class TransactionReportValidationServiceImpl implements TransactionReportValidationService {
    
    @Resource
    private TransactionReportMapper transactionReportMapper;
    
    @Override
    public Map<String, Object> validateTransactionReport(String amazonauthid, String marketplaceid, TransactionReportValidation javaValidation) {
        Map<String, Object> result = new HashMap<>();
        
        if (javaValidation == null || javaValidation.getMinDateTime() == null || javaValidation.getMaxDateTime() == null) {
            result.put("success", false);
            result.put("message", "Java校验结果为空或时间范围无效");
            return result;
        }
        
        // 格式化日期
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        String startDate = sdf.format(javaValidation.getMinDateTime());
        String endDate = sdf.format(javaValidation.getMaxDateTime());
        
        // 从数据库获取统计数据
        List<Map<String, Object>> dbDailySummary = getDailyFeeSummaryFromDB(amazonauthid, marketplaceid, startDate, endDate);
        Map<String, Object> dbMinMaxDateTime = getMinMaxDateTimeFromDB(amazonauthid, marketplaceid, startDate, endDate);
        long dbTotalCount = getCountFromDB(amazonauthid, marketplaceid, startDate, endDate);
        
        // 比较结果
        Map<String, Object> compareResult = compareResults(javaValidation, dbDailySummary, dbMinMaxDateTime, dbTotalCount);
        
        result.put("success", compareResult.get("success"));
        result.put("message", compareResult.get("message"));
        result.put("details", compareResult);
        result.put("javaSummary", javaValidation);
        result.put("dbDailySummary", dbDailySummary);
        result.put("dbMinMaxDateTime", dbMinMaxDateTime);
        result.put("dbTotalCount", dbTotalCount);
        
        return result;
    }
    
    @Override
    public List<Map<String, Object>> getDailyFeeSummaryFromDB(String amazonauthid, String marketplaceid, String startDate, String endDate) {
        return transactionReportMapper.selectDailyFeeSummary(amazonauthid, marketplaceid, startDate, endDate);
    }
    
    @Override
    public Map<String, Object> getMinMaxDateTimeFromDB(String amazonauthid, String marketplaceid, String startDate, String endDate) {
        return transactionReportMapper.selectMinMaxDateTime(amazonauthid, marketplaceid, startDate, endDate);
    }
    
    @Override
    public long getCountFromDB(String amazonauthid, String marketplaceid, String startDate, String endDate) {
        return transactionReportMapper.selectCountByDateRange(amazonauthid, marketplaceid, startDate, endDate);
    }
    
    @Override
    public Map<String, Object> compareResults(TransactionReportValidation javaValidation,
                                             List<Map<String, Object>> dbDailySummary,
                                             Map<String, Object> dbMinMaxDateTime,
                                             long dbTotalCount) {
        Map<String, Object> result = new HashMap<>();
        List<String> errors = new ArrayList<>();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        
        // 1. 比较总记录数
        if (javaValidation.getTotalRecords() != dbTotalCount) {
            errors.add(String.format("总记录数不一致: Java统计=%d, 数据库统计=%d", 
                    javaValidation.getTotalRecords(), dbTotalCount));
        }
        
        // 2. 比较最小/最大时间
        if (dbMinMaxDateTime != null) {
            Object dbMinObj = dbMinMaxDateTime.get("min_datetime");
            Object dbMaxObj = dbMinMaxDateTime.get("max_datetime");
            
            if (dbMinObj instanceof Date) {
                Date dbMinDate = (Date) dbMinObj;
                if (javaValidation.getMinDateTime().compareTo(dbMinDate) != 0) {
                    errors.add(String.format("最小时间不一致: Java统计=%s, 数据库统计=%s", 
                            javaValidation.getMinDateTime(), dbMinDate));
                }
            }
            
            if (dbMaxObj instanceof Date) {
                Date dbMaxDate = (Date) dbMaxObj;
                if (javaValidation.getMaxDateTime().compareTo(dbMaxDate) != 0) {
                    errors.add(String.format("最大时间不一致: Java统计=%s, 数据库统计=%s", 
                            javaValidation.getMaxDateTime(), dbMaxDate));
                }
            }
        }
        
        // 3. 按日期比较各项费用
        Map<String, TransactionReportValidation.FeeSummary> javaDailySummary = javaValidation.getDailySummaryMap();
        
        for (Map<String, Object> dbDaySummary : dbDailySummary) {
            Object dateKeyObj = dbDaySummary.get("date_key");
            String dateKey = dateKeyObj instanceof Date ? sdf.format(dateKeyObj) : String.valueOf(dateKeyObj);
            TransactionReportValidation.FeeSummary javaDaySummary = javaDailySummary.get(dateKey);
            
            if (javaDaySummary == null) {
                errors.add(String.format("日期 %s 在Java统计中不存在", dateKey));
                continue;
            }
            
            // 比较各项费用
            compareBigDecimalField(dateKey, "product_sales", javaDaySummary.getProductSales(), 
                    getBigDecimalFromMap(dbDaySummary, "product_sales"), errors);
            compareBigDecimalField(dateKey, "product_sales_tax", javaDaySummary.getProductSalesTax(), 
                    getBigDecimalFromMap(dbDaySummary, "product_sales_tax"), errors);
            compareBigDecimalField(dateKey, "shipping_credits", javaDaySummary.getShippingCredits(), 
                    getBigDecimalFromMap(dbDaySummary, "shipping_credits"), errors);
            compareBigDecimalField(dateKey, "shipping_credits_tax", javaDaySummary.getShippingCreditsTax(), 
                    getBigDecimalFromMap(dbDaySummary, "shipping_credits_tax"), errors);
            compareBigDecimalField(dateKey, "giftwrap_credits", javaDaySummary.getGiftwrapCredits(), 
                    getBigDecimalFromMap(dbDaySummary, "giftwrap_credits"), errors);
            compareBigDecimalField(dateKey, "giftwrap_credits_tax", javaDaySummary.getGiftwrapCreditsTax(), 
                    getBigDecimalFromMap(dbDaySummary, "giftwrap_credits_tax"), errors);
            compareBigDecimalField(dateKey, "promotional_rebates", javaDaySummary.getPromotionalRebates(), 
                    getBigDecimalFromMap(dbDaySummary, "promotional_rebates"), errors);
            compareBigDecimalField(dateKey, "promotional_rebates_tax", javaDaySummary.getPromotionalRebatesTax(), 
                    getBigDecimalFromMap(dbDaySummary, "promotional_rebates_tax"), errors);
            compareBigDecimalField(dateKey, "marketplace_withheld_tax", javaDaySummary.getMarketplaceWithheldTax(), 
                    getBigDecimalFromMap(dbDaySummary, "marketplace_withheld_tax"), errors);
            compareBigDecimalField(dateKey, "selling_fees", javaDaySummary.getSellingFees(), 
                    getBigDecimalFromMap(dbDaySummary, "selling_fees"), errors);
            compareBigDecimalField(dateKey, "fba_fees", javaDaySummary.getFbaFees(), 
                    getBigDecimalFromMap(dbDaySummary, "fba_fees"), errors);
            compareBigDecimalField(dateKey, "other_transaction_fees", javaDaySummary.getOtherTransactionFees(), 
                    getBigDecimalFromMap(dbDaySummary, "other_transaction_fees"), errors);
            compareBigDecimalField(dateKey, "other", javaDaySummary.getOther(), 
                    getBigDecimalFromMap(dbDaySummary, "other"), errors);
            compareBigDecimalField(dateKey, "total", javaDaySummary.getTotal(), 
                    getBigDecimalFromMap(dbDaySummary, "total"), errors);
        }
        
        // 4. 检查Java中有但数据库中没有的日期
        for (String javaDateKey : javaDailySummary.keySet()) {
            boolean foundInDB = false;
            for (Map<String, Object> dbDaySummary : dbDailySummary) {
                Object dbDateKeyObj = dbDaySummary.get("date_key");
                String dbDateKey = dbDateKeyObj instanceof Date ? sdf.format(dbDateKeyObj) : String.valueOf(dbDateKeyObj);
                if (javaDateKey.equals(dbDateKey)) {
                    foundInDB = true;
                    break;
                }
            }
            if (!foundInDB) {
                errors.add(String.format("日期 %s 在数据库统计中不存在", javaDateKey));
            }
        }
        
        boolean success = errors.isEmpty();
        String message = success ? "校验通过：Java解析结果与数据库统计完全一致" : "校验失败：存在不一致";
        
        result.put("success", success);
        result.put("message", message);
        result.put("errors", errors);
        result.put("errorCount", errors.size());
        
        return result;
    }
    
    private void compareBigDecimalField(String dateKey, String fieldName, BigDecimal javaValue, BigDecimal dbValue, List<String> errors) {
        if (javaValue == null) javaValue = BigDecimal.ZERO;
        if (dbValue == null) dbValue = BigDecimal.ZERO;
        
        if (javaValue.compareTo(dbValue) != 0) {
            errors.add(String.format("日期 %s 的 %s 不一致: Java统计=%s, 数据库统计=%s", 
                    dateKey, fieldName, javaValue.toPlainString(), dbValue.toPlainString()));
        }
    }
    
    private BigDecimal getBigDecimalFromMap(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) {
            return BigDecimal.ZERO;
        }
        if (value instanceof BigDecimal) {
            return (BigDecimal) value;
        }
        try {
            return new BigDecimal(value.toString());
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }
}