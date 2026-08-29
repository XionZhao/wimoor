package com.wimoor.finance.setting.service.impl;

import com.wimoor.finance.setting.domain.FinMappingVouchersSourcePayment;
import com.wimoor.finance.setting.mapper.FinMappingVouchersSourcePaymentMapper;
import com.wimoor.finance.setting.service.IFinMappingVouchersSourcePaymentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * ERP付款记录Service业务层处理
 *
 * @author wimoor
 * @date 2026-08-11
 */
@Service
public class FinMappingVouchersSourcePaymentServiceImpl implements IFinMappingVouchersSourcePaymentService
{
    private static final Logger log = LoggerFactory.getLogger(FinMappingVouchersSourcePaymentServiceImpl.class);

    @Autowired
    private FinMappingVouchersSourcePaymentMapper finMappingVouchersSourcePaymentMapper;

    @Override
    public List<FinMappingVouchersSourcePayment> selectFinMappingVouchersSourcePaymentList(FinMappingVouchersSourcePayment record) {
        return finMappingVouchersSourcePaymentMapper.selectFinMappingVouchersSourcePaymentList(record);
    }

    @Override
    public FinMappingVouchersSourcePayment selectFinMappingVouchersSourcePaymentById(Long id) {
        return finMappingVouchersSourcePaymentMapper.selectFinMappingVouchersSourcePaymentById(id);
    }

    @Override
    public int insertFinMappingVouchersSourcePayment(FinMappingVouchersSourcePayment record) {
        return finMappingVouchersSourcePaymentMapper.insertFinMappingVouchersSourcePayment(record);
    }

    @Override
    public int updateFinMappingVouchersSourcePayment(FinMappingVouchersSourcePayment record) {
        return finMappingVouchersSourcePaymentMapper.updateFinMappingVouchersSourcePayment(record);
    }

    @Override
    public int batchInsertFinMappingVouchersSourcePayment(List<FinMappingVouchersSourcePayment> list) {
        return finMappingVouchersSourcePaymentMapper.batchInsertFinMappingVouchersSourcePayment(list);
    }

    @Override
    public List<FinMappingVouchersSourcePayment> selectByOrderId(String groupid, String orderId) {
        return finMappingVouchersSourcePaymentMapper.selectByOrderId(groupid, orderId);
    }

    @Override
    public FinMappingVouchersSourcePayment selectByPaymentId(String groupid, String paymentId) {
        return finMappingVouchersSourcePaymentMapper.selectByPaymentId(groupid, paymentId);
    }

    @Override
    public List<FinMappingVouchersSourcePayment> selectNeedSync(String groupid) {
        return finMappingVouchersSourcePaymentMapper.selectNeedSync(groupid);
    }

    @Override
    public int batchUpdateSyncStatus(List<Long> ids, Integer syncStatus, Long voucherId, Date syncTime) {
        return finMappingVouchersSourcePaymentMapper.batchUpdateSyncStatus(ids, syncStatus, voucherId, syncTime);
    }

    @Override
    public int deleteFinMappingVouchersSourcePaymentById(Long id) {
        return finMappingVouchersSourcePaymentMapper.deleteFinMappingVouchersSourcePaymentById(id);
    }

    /**
     * 批量落地ERP付款数据
     * <p>
     * 逐条对比：按 order_id + payment_id 匹配已有记录，
     * 若 data_hash 变化则标记为 sync_status=2（已变更），
     * 若不存在则新增 sync_status=0（待同步）。
     * </p>
     */
    @Override
    public int batchUpsertPaymentRecords(String groupid, List<FinMappingVouchersSourcePayment> records, String userName) {
        if (records == null || records.isEmpty()) {
            return 0;
        }

        Date now = new Date();
        List<FinMappingVouchersSourcePayment> toInsert = new ArrayList<>();
        List<FinMappingVouchersSourcePayment> toUpdate = new ArrayList<>();
        int updatedCount = 0;
        int insertedCount = 0;

        // 第一步：收集所有涉及的订单ID，预加载已有记录
        Set<String> allOrderIds = records.stream()
                .map(FinMappingVouchersSourcePayment::getOrderId)
                .collect(Collectors.toSet());

        // orderId -> (paymentId -> existingRecord)
        Map<String, Map<String, FinMappingVouchersSourcePayment>> existingByOrder = new HashMap<>();
        for (String orderId : allOrderIds) {
            List<FinMappingVouchersSourcePayment> existingList = selectByOrderId(groupid, orderId);
            Map<String, FinMappingVouchersSourcePayment> existingMap = new HashMap<>();
            if (existingList != null) {
                for (FinMappingVouchersSourcePayment e : existingList) {
                    existingMap.put(e.getPaymentId(), e);
                }
            }
            existingByOrder.put(orderId, existingMap);
        }

        // 记录当前ERP数据中出现的 orderId+paymentId 组合
        Set<String> currentPaymentKeys = new HashSet<>();

        for (FinMappingVouchersSourcePayment record : records) {
            String key = record.getOrderId() + ":" + record.getPaymentId();
            currentPaymentKeys.add(key);

            Map<String, FinMappingVouchersSourcePayment> existingMap = existingByOrder.get(record.getOrderId());
            FinMappingVouchersSourcePayment existing = existingMap != null ? existingMap.get(record.getPaymentId()) : null;

            if (existing != null) {
                // 已有记录，检查是否变更
                if (!Objects.equals(existing.getDataHash(), record.getDataHash())) {
                    existing.setAmount(record.getAmount());
                    existing.setPaymentDate(record.getPaymentDate());
                    existing.setSupplierName(record.getSupplierName());
                    existing.setOrderNumber(record.getOrderNumber());
                    existing.setWarehouseName(record.getWarehouseName());
                    existing.setFeeTypeId(record.getFeeTypeId());
                    existing.setFeeTypeName(record.getFeeTypeName());
                    existing.setAccountId(record.getAccountId());
                    existing.setAccountName(record.getAccountName());
                    existing.setEntryIds(record.getEntryIds());
                    existing.setPaymentStatus(record.getPaymentStatus());
                    existing.setDataHash(record.getDataHash());
                    existing.setSyncStatus(2); // 已变更，需重新同步
                    existing.setUpdatedTime(now);
                    existing.setModifyBy(userName);
                    toUpdate.add(existing);
                }
                // hash 相同则跳过
            } else {
                // 新记录
                record.setSyncStatus(0); // 待同步
                record.setCreatedTime(now);
                record.setUpdatedTime(now);
                record.setCreateBy(userName);
                record.setModifyBy(userName);
                toInsert.add(record);
            }
        }

        // 第二步：检查已同步但不在当前ERP数据中的记录（可能已撤销），标记为"已变更"
        for (Map.Entry<String, Map<String, FinMappingVouchersSourcePayment>> orderEntry : existingByOrder.entrySet()) {
            String orderId = orderEntry.getKey();
            for (Map.Entry<String, FinMappingVouchersSourcePayment> paymentEntry : orderEntry.getValue().entrySet()) {
                String paymentId = paymentEntry.getKey();
                String key = orderId + ":" + paymentId;
                if (!currentPaymentKeys.contains(key)) {
                    FinMappingVouchersSourcePayment stale = paymentEntry.getValue();
                    // 只处理已同步过的记录（sync_status=1）且未被删除（sync_status!=3）
                    if (stale.getSyncStatus() != null && stale.getSyncStatus() == 1) {
                        stale.setSyncStatus(2); // 标记为已变更，让 selectNeedSync 重新处理
                        stale.setUpdatedTime(now);
                        stale.setModifyBy(userName);
                        toUpdate.add(stale);
                        log.info("付款记录已撤销（不在当前ERP数据中）：orderId={}, paymentId={}", orderId, paymentId);
                    }
                }
            }
        }

        // 批量执行
        if (!toUpdate.isEmpty()) {
            for (FinMappingVouchersSourcePayment record : toUpdate) {
                updatedCount += updateFinMappingVouchersSourcePayment(record);
            }
            log.info("ERP付款记录更新：{} 条变更", updatedCount);
        }

        if (!toInsert.isEmpty()) {
            // 分批插入，每批500条
            int batchSize = 500;
            for (int i = 0; i < toInsert.size(); i += batchSize) {
                int end = Math.min(i + batchSize, toInsert.size());
                List<FinMappingVouchersSourcePayment> batch = toInsert.subList(i, end);
                insertedCount += batchInsertFinMappingVouchersSourcePayment(batch);
            }
            log.info("ERP付款记录新增：{} 条", insertedCount);
        }

        return updatedCount + insertedCount;
    }

    @Override
    public List<Long> deleteStalePaymentRecords(String groupid, List<FinMappingVouchersSourcePayment> currentRecords, List<String> erpOrderIds) {
        // 收集需要检查的订单ID：优先从currentRecords获取，如果为空则使用ERP返回的订单ID
        Set<String> orderIdSet = new HashSet<>();
        if (currentRecords != null && !currentRecords.isEmpty()) {
            for (FinMappingVouchersSourcePayment record : currentRecords) {
                if (record.getOrderId() != null && !record.getOrderId().isEmpty()) {
                    orderIdSet.add(record.getOrderId());
                }
            }
        } else if (erpOrderIds != null && !erpOrderIds.isEmpty()) {
            // currentRecords为空（全部付款被撤销），使用ERP返回的订单ID
            orderIdSet.addAll(erpOrderIds);
        }
        if (orderIdSet.isEmpty()) {
            return new ArrayList<>();
        }

        // 查询本地已有的付款记录
        List<String> orderIds = new ArrayList<>(orderIdSet);
        List<FinMappingVouchersSourcePayment> existingRecords = finMappingVouchersSourcePaymentMapper.selectByOrderIds(groupid, orderIds);
        if (existingRecords == null || existingRecords.isEmpty()) {
            return new ArrayList<>();
        }

        // 构建当前ERP数据的paymentId集合
        Set<String> currentPaymentIds = new HashSet<>();
        if (currentRecords != null) {
            for (FinMappingVouchersSourcePayment record : currentRecords) {
                if (record.getPaymentId() != null && !record.getPaymentId().isEmpty()) {
                    currentPaymentIds.add(record.getPaymentId());
                }
            }
        }

        // 找出本地有但ERP当前数据中已不存在的记录
        List<Long> staleIds = new ArrayList<>();
        for (FinMappingVouchersSourcePayment existing : existingRecords) {
            if (!currentPaymentIds.contains(existing.getPaymentId())) {
                staleIds.add(existing.getId());
            }
        }

        if (!staleIds.isEmpty()) {
            finMappingVouchersSourcePaymentMapper.deleteFinMappingVouchersSourcePaymentByIds(staleIds);
            log.info("清理过期付款记录：{} 条", staleIds.size());
        }

        return staleIds;
    }

    @Override
    public List<String> selectSyncedOrderIds(String groupid) {
        return finMappingVouchersSourcePaymentMapper.selectSyncedOrderIds(groupid);
    }

    @Override
    public int deleteByOrderId(String groupid, String orderId) {
        return finMappingVouchersSourcePaymentMapper.deleteByOrderId(groupid, orderId);
    }
}
