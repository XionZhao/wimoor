package com.wimoor.amazon.finances.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wimoor.amazon.finances.pojo.entity.TransactionReport;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface TransactionReportMapper extends BaseMapper<TransactionReport> {

    int insertBatch(@Param("list") List<TransactionReport> list);
    
    /**
     * 按日期汇总统计各项费用
     * @param amazonauthid 亚马逊授权ID
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 按日期汇总的费用列表
     */
    List<Map<String, Object>> selectDailyFeeSummary(@Param("amazonauthid") String amazonauthid, 
                                                    @Param("marketplaceid") String marketplaceid,
                                                    @Param("startDate") String startDate, 
                                                    @Param("endDate") String endDate);
    
    /**
     * 获取指定时间范围内的最小和最大日期时间
     * @param amazonauthid 亚马逊授权ID
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 包含min_datetime和max_datetime的Map
     */
    Map<String, Object> selectMinMaxDateTime(@Param("amazonauthid") String amazonauthid, 
                                            @Param("marketplaceid") String marketplaceid,
                                            @Param("startDate") String startDate, 
                                            @Param("endDate") String endDate);
    
    /**
     * 获取指定时间范围内的总记录数
     * @param amazonauthid 亚马逊授权ID
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 记录数
     */
    long selectCountByDateRange(@Param("amazonauthid") String amazonauthid, 
                               @Param("marketplaceid") String marketplaceid,
                               @Param("startDate") String startDate, 
                               @Param("endDate") String endDate);
    
    /**
     * 按交易类型汇总各项费用（排除订单SKU明细）
     * @param amazonauthid 亚马逊授权ID
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 按交易类型汇总的费用列表
     */
    List<Map<String, Object>> selectFeeSummaryByType(@Param("amazonauthid") String amazonauthid, 
                                                     @Param("startDate") String startDate, 
                                                     @Param("endDate") String endDate);
    
    /**
     * 按日期和交易类型汇总各项费用
     * @param amazonauthid 亚马逊授权ID
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 按日期和交易类型汇总的费用列表
     */
    List<Map<String, Object>> selectDailyFeeByType(@Param("amazonauthid") String amazonauthid, 
                                                   @Param("startDate") String startDate, 
                                                   @Param("endDate") String endDate);
    
    /**
     * 交易报告 LEFT JOIN 结算报告，按日期统计Order类型销量比对
     * @param amazonauthid 亚马逊授权ID
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @param groupType 聚合粒度：day/week/month
     * @return 按日期的销量比对数据
     */
    List<Map<String, Object>> selectSalesCompareByDate(@Param("amazonauthid") String amazonauthid, 
                                                       @Param("startDate") String startDate, 
                                                       @Param("endDate") String endDate,
                                                       @Param("groupType") String groupType);
    
    /**
     * 分页查询交易报告详情
     * @param amazonauthid 亚马逊授权ID
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @param searchType 搜索类型：orderId/sku
     * @param searchValue 搜索值
     * @param offset 偏移量
     * @param limit 每页条数
     * @return 交易报告详情列表
     */
    List<Map<String, Object>> selectDetailPage(@Param("amazonauthid") String amazonauthid,
                                                @Param("startDate") String startDate,
                                                @Param("endDate") String endDate,
                                                @Param("searchType") String searchType,
                                                @Param("searchValue") String searchValue,
                                                @Param("offset") int offset,
                                                @Param("limit") int limit);
    
    /**
     * 查询交易报告详情总数
     * @param amazonauthid 亚马逊授权ID
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @param searchType 搜索类型：orderId/sku
     * @param searchValue 搜索值
     * @return 总记录数
     */
    long selectDetailCount(@Param("amazonauthid") String amazonauthid,
                           @Param("startDate") String startDate,
                           @Param("endDate") String endDate,
                           @Param("searchType") String searchType,
                           @Param("searchValue") String searchValue);
    
    /**
     * 查询交易报告详情（不分页，用于导出）
     */
    List<Map<String, Object>> selectDetailForExport(@Param("amazonauthid") String amazonauthid,
                                                     @Param("startDate") String startDate,
                                                     @Param("endDate") String endDate,
                                                     @Param("searchType") String searchType,
                                                     @Param("searchValue") String searchValue);
}
