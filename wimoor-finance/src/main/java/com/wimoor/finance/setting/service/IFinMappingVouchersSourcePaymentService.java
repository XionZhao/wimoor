package com.wimoor.finance.setting.service;

import com.wimoor.finance.setting.domain.FinMappingVouchersSourcePayment;

import java.util.Date;
import java.util.List;

/**
 * ERP付款记录Service接口
 *
 * @author wimoor
 * @date 2026-08-11
 */
public interface IFinMappingVouchersSourcePaymentService
{
    /**
     * 查询ERP付款记录列表
     */
    List<FinMappingVouchersSourcePayment> selectFinMappingVouchersSourcePaymentList(FinMappingVouchersSourcePayment record);

    /**
     * 查询ERP付款记录详情
     */
    FinMappingVouchersSourcePayment selectFinMappingVouchersSourcePaymentById(Long id);

    /**
     * 新增ERP付款记录
     */
    int insertFinMappingVouchersSourcePayment(FinMappingVouchersSourcePayment record);

    /**
     * 修改ERP付款记录
     */
    int updateFinMappingVouchersSourcePayment(FinMappingVouchersSourcePayment record);

    /**
     * 批量新增
     */
    int batchInsertFinMappingVouchersSourcePayment(List<FinMappingVouchersSourcePayment> list);

    /**
     * 按订单ID查询
     */
    List<FinMappingVouchersSourcePayment> selectByOrderId(String groupid, String orderId);

    /**
     * 按付款ID查询单条记录
     */
    FinMappingVouchersSourcePayment selectByPaymentId(String groupid, String paymentId);

    /**
     * 查询待同步的记录（sync_status = 0 或 2）
     */
    List<FinMappingVouchersSourcePayment> selectNeedSync(String groupid);

    /**
     * 批量更新同步状态
     */
    int batchUpdateSyncStatus(List<Long> ids, Integer syncStatus, Long voucherId, Date syncTime);

    /**
     * 删除ERP付款记录
     */
    int deleteFinMappingVouchersSourcePaymentById(Long id);

    /**
     * 批量落地ERP付款数据（upsert逻辑）
     * <p>
     * 对每个付款记录，检查是否已存在（按 order_id + entry_id + payment_id），
     * 若存在且 data_hash 变更则更新为 sync_status=2（已变更）；
     * 若不存在则新增 sync_status=0（待同步）。
     * </p>
     *
     * @param groupid  租户ID
     * @param records  从ERP获取的付款记录列表
     * @param userName 操作人
     * @return 落地后的记录数
     */
    int batchUpsertPaymentRecords(String groupid, List<FinMappingVouchersSourcePayment> records, String userName);

    /**
     * 清理在ERP中已被删除的付款记录
     * <p>
     * 对比ERP当前返回的付款记录与本地已保存的记录，
     * 删除本地有但ERP已不存在的记录（即付款已被撤销）。
     * </p>
     *
     * @param groupid         租户ID
     * @param currentRecords  当前ERP返回的付款记录（已过滤掉auditstatus!=1的）
     * @param erpOrderIds     ERP返回的订单ID列表（当currentRecords为空时，用于查找本地记录并全部删除）
     * @return 被删除的记录ID列表（用于后续清理凭证）
     */
    List<Long> deleteStalePaymentRecords(String groupid, List<FinMappingVouchersSourcePayment> currentRecords, List<String> erpOrderIds);

    /**
     * 查询已同步的订单ID列表（sync_status=1）
     * 用于检测已同步订单的付款状态是否发生变更（如被撤销）
     */
    List<String> selectSyncedOrderIds(String groupid);

    /**
     * 按订单ID删除付款记录
     * 用于撤销凭证时删除本地付款记录（ERP每晚会重新同步）
     */
    int deleteByOrderId(String groupid, String orderId);
}
