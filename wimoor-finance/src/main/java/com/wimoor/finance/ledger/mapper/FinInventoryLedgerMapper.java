package com.wimoor.finance.ledger.mapper;

import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 进销存台账Mapper接口
 *
 * @author wimoor
 * @date 2026-07-10
 */
public interface FinInventoryLedgerMapper {

    /**
     * 查询汇总账（按SKU+仓库维度）
     *
     * @param shopid      公司ID
     * @param warehouseid 仓库ID
     * @param materialid  产品ID
     * @param sku         SKU模糊搜索
     * @param name        产品名称模糊搜索
     * @param startDate   开始日期
     * @param endDate     结束日期
     * @return 汇总数据列表
     */
    List<Map<String, Object>> selectSummary(@Param("shopid") String shopid,
                                            @Param("warehouseid") String warehouseid,
                                            @Param("materialid") String materialid,
                                            @Param("sku") String sku,
                                            @Param("name") String name,
                                            @Param("startDate") Date startDate,
                                            @Param("endDate") Date endDate);

    /**
     * 查询明细账（按SKU查询变动记录）
     *
     * @param shopid      公司ID
     * @param warehouseid 仓库ID
     * @param materialid  产品ID
     * @param formtype    单据类型
     * @param startDate   开始日期
     * @param endDate     结束日期
     * @return 明细数据列表
     */
    List<Map<String, Object>> selectDetail(@Param("shopid") String shopid,
                                           @Param("warehouseid") String warehouseid,
                                           @Param("materialid") String materialid,
                                           @Param("formtype") String formtype,
                                           @Param("startDate") Date startDate,
                                           @Param("endDate") Date endDate);

    /**
     * 勾稽校验（验证库存金额与凭证金额是否一致）
     *
     * @param shopid      公司ID
     * @param warehouseid 仓库ID
     * @param startDate   开始日期
     * @param endDate     结束日期
     * @return 校验结果列表
     */
    List<Map<String, Object>> selectCheckResult(@Param("shopid") String shopid,
                                                @Param("warehouseid") String warehouseid,
                                                @Param("startDate") Date startDate,
                                                @Param("endDate") Date endDate);

    /**
     * 库存趋势图数据（按日期聚合库存金额变化）
     *
     * @param shopid      公司ID
     * @param warehouseid 仓库ID
     * @param materialid  产品ID
     * @param startDate   开始日期
     * @param endDate     结束日期
     * @return 趋势数据列表
     */
    List<Map<String, Object>> selectChartTrend(@Param("shopid") String shopid,
                                               @Param("warehouseid") String warehouseid,
                                               @Param("materialid") String materialid,
                                               @Param("startDate") Date startDate,
                                               @Param("endDate") Date endDate);

    /**
     * 查询变动记录详情（用于生成凭证）
     *
     * @param recordIds 变动记录ID列表
     * @param shopid    公司ID
     * @return 变动记录列表
     */
    List<Map<String, Object>> selectRecordsByIds(@Param("recordIds") List<Long> recordIds,
                                                 @Param("shopid") String shopid);

    /**
     * 更新库存余额表的金额和单位成本字段
     *
     * @param shopid     公司ID
     * @param materialid 产品ID
     * @param warehouseid 仓库ID
     * @param status     库存状态
     * @param currentAmount 库存金额
     * @param unitCost   单位成本
     * @return 影响行数
     */
    int updateInventoryAmount(@Param("shopid") String shopid,
                              @Param("materialid") String materialid,
                              @Param("warehouseid") String warehouseid,
                              @Param("status") String status,
                              @Param("currentAmount") BigDecimal currentAmount,
                              @Param("unitCost") BigDecimal unitCost);

    /**
     * 更新变动记录的金额、单价和凭证ID
     *
     * @param id           记录ID
     * @param amountChange 金额变动
     * @param unitCost     单位成本
     * @param voucherId    凭证ID
     * @return 影响行数
     */
    int updateRecordFinance(@Param("id") Long id,
                            @Param("amountChange") BigDecimal amountChange,
                            @Param("unitCost") BigDecimal unitCost,
                            @Param("voucherId") Long voucherId);

    /**
     * 查询需要计算成本的变动记录（按时间顺序）
     *
     * @param shopid      公司ID
     * @param materialid  产品ID
     * @param warehouseid 仓库ID
     * @param startDate   开始日期
     * @return 变动记录列表
     */
    List<Map<String, Object>> selectRecordsForCostCalculation(@Param("shopid") String shopid,
                                                              @Param("materialid") String materialid,
                                                              @Param("warehouseid") String warehouseid,
                                                              @Param("startDate") Date startDate);

    /**
     * 获取当前库存余额信息
     *
     * @param shopid      公司ID
     * @param materialid  产品ID
     * @param warehouseid 仓库ID
     * @param status      库存状态
     * @return 库存余额信息
     */
    Map<String, Object> selectInventoryBalance(@Param("shopid") String shopid,
                                               @Param("materialid") String materialid,
                                               @Param("warehouseid") String warehouseid,
                                               @Param("status") String status);

    /**
     * 查询导出数据
     *
     * @param shopid      公司ID
     * @param warehouseid 仓库ID
     * @param materialid  产品ID
     * @param startDate   开始日期
     * @param endDate     结束日期
     * @return 导出数据列表
     */
    List<Map<String, Object>> selectExportData(@Param("shopid") String shopid,
                                               @Param("warehouseid") String warehouseid,
                                               @Param("materialid") String materialid,
                                               @Param("startDate") Date startDate,
                                               @Param("endDate") Date endDate);
}
