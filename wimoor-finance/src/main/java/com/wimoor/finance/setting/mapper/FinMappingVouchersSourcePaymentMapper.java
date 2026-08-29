package com.wimoor.finance.setting.mapper;

import com.wimoor.finance.setting.domain.FinMappingVouchersSourcePayment;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * ERP付款记录Mapper接口
 *
 * @author wimoor
 * @date 2026-08-11
 */
public interface FinMappingVouchersSourcePaymentMapper
{
    /**
     * 查询ERP付款记录列表
     */
    List<FinMappingVouchersSourcePayment> selectFinMappingVouchersSourcePaymentList(FinMappingVouchersSourcePayment record);

    /**
     * 查询ERP付款记录详情
     */
    FinMappingVouchersSourcePayment selectFinMappingVouchersSourcePaymentById(@Param("id") Long id);

    /**
     * 新增ERP付款记录
     */
    int insertFinMappingVouchersSourcePayment(FinMappingVouchersSourcePayment record);

    /**
     * 修改ERP付款记录
     */
    int updateFinMappingVouchersSourcePayment(FinMappingVouchersSourcePayment record);

    /**
     * 批量新增ERP付款记录
     */
    int batchInsertFinMappingVouchersSourcePayment(@Param("list") List<FinMappingVouchersSourcePayment> list);

    /**
     * 按订单ID查询
     */
    List<FinMappingVouchersSourcePayment> selectByOrderId(@Param("groupid") String groupid, @Param("orderId") String orderId);

    /**
     * 按付款ID查询单条记录
     */
    FinMappingVouchersSourcePayment selectByPaymentId(@Param("groupid") String groupid, @Param("paymentId") String paymentId);

    /**
     * 查询待同步的记录
     */
    List<FinMappingVouchersSourcePayment> selectNeedSync(@Param("groupid") String groupid);

    /**
     * 批量更新同步状态
     */
    int batchUpdateSyncStatus(@Param("ids") List<Long> ids, @Param("syncStatus") Integer syncStatus,
                               @Param("voucherId") Long voucherId, @Param("syncTime") java.util.Date syncTime);

    /**
     * 删除ERP付款记录
     */
    int deleteFinMappingVouchersSourcePaymentById(@Param("id") Long id);

    /**
     * 批量删除ERP付款记录
     */
    int deleteFinMappingVouchersSourcePaymentByIds(@Param("ids") List<Long> ids);

    /**
     * 按订单ID列表查询
     */
    List<FinMappingVouchersSourcePayment> selectByOrderIds(@Param("groupid") String groupid, @Param("orderIds") List<String> orderIds);

    /**
     * 查询已同步的订单ID列表（sync_status=1）
     */
    List<String> selectSyncedOrderIds(@Param("groupid") String groupid);

    /**
     * 按订单ID删除付款记录
     */
    int deleteByOrderId(@Param("groupid") String groupid, @Param("orderId") String orderId);

    /**
     * 按订单ID列表批量删除付款记录
     */
    int deleteByOrderIds(@Param("groupid") String groupid, @Param("orderIds") List<String> orderIds);
}
