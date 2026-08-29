package com.wimoor.amazon.finances.service;

import com.wimoor.amazon.finances.pojo.entity.TransactionReportValidation;
import java.util.List;
import java.util.Map;

/**
 * 交易报告校验服务接口
 */
public interface TransactionReportValidationService {
    
    /**
     * 校验交易报告数据
     * @param amazonauthid 亚马逊授权ID
     * @param javaValidation Java代码解析的校验结果
     * @return 校验结果详情
     */
    Map<String, Object> validateTransactionReport(String amazonauthid, String marketplaceid, TransactionReportValidation javaValidation);
    
    /**
     * 从数据库获取按日期汇总的统计数据
     * @param amazonauthid 亚马逊授权ID
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 按日期汇总的费用列表
     */
    List<Map<String, Object>> getDailyFeeSummaryFromDB(String amazonauthid, String marketplaceid, String startDate, String endDate);
    
    /**
     * 从数据库获取最小和最大日期时间
     * @param amazonauthid 亚马逊授权ID
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 包含min_datetime和max_datetime的Map
     */
    Map<String, Object> getMinMaxDateTimeFromDB(String amazonauthid, String marketplaceid, String startDate, String endDate);
    
    /**
     * 从数据库获取总记录数
     * @param amazonauthid 亚马逊授权ID
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 记录数
     */
    long getCountFromDB(String amazonauthid, String marketplaceid, String startDate, String endDate);
    
    /**
     * 比较Java解析结果与数据库统计结果
     * @param javaValidation Java代码解析的校验结果
     * @param dbDailySummary 数据库按日期汇总的结果
     * @param dbMinMaxDateTime 数据库最小最大时间
     * @param dbTotalCount 数据库总记录数
     * @return 比较结果
     */
    Map<String, Object> compareResults(TransactionReportValidation javaValidation,
                                      List<Map<String, Object>> dbDailySummary,
                                      Map<String, Object> dbMinMaxDateTime,
                                      long dbTotalCount);
}