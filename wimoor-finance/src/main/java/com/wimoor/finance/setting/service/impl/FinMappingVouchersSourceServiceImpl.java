package com.wimoor.finance.setting.service.impl;

import com.wimoor.finance.ledger.service.IFinInvoiceLedgerService;
import com.wimoor.finance.setting.domain.FinMappingVouchersSource;
import com.wimoor.finance.setting.mapper.FinMappingVouchersSourceMapper;
import com.wimoor.finance.setting.service.IFinMappingVouchersSourceService;
import com.wimoor.finance.setting.mapper.FinMappingVouchersMapper;
import com.wimoor.finance.setting.strategy.ErpVoucherStrategyFactory;
import com.wimoor.finance.setting.strategy.IErpVoucherStrategy;
import com.wimoor.finance.voucher.domain.FinVouchers;
import com.wimoor.finance.voucher.mapper.FinVoucherEntriesAuxiliaryMapper;
import com.wimoor.finance.voucher.mapper.FinVoucherEntriesMapper;
import com.wimoor.finance.voucher.mapper.FinVouchersMapper;
import com.wimoor.finance.voucher.mapper.FinVourchesFileMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 凭证生成记录Service实现
 *
 * @author wimoor
 * @date 2026-08-07
 */
@Service
@Slf4j
public class FinMappingVouchersSourceServiceImpl implements IFinMappingVouchersSourceService
{
    @Resource
    private FinMappingVouchersSourceMapper finMappingVouchersSourceMapper;

    @Resource
    private FinMappingVouchersMapper finMappingVouchersMapper;

    @Resource
    private FinVouchersMapper finVouchersMapper;

    @Resource
    private FinVoucherEntriesMapper finVoucherEntriesMapper;

    @Resource
    private FinVoucherEntriesAuxiliaryMapper finVoucherEntriesAuxiliaryMapper;

    @Resource
    private FinVourchesFileMapper finVourchesFileMapper;

    @Lazy
    @Resource
    private ErpVoucherStrategyFactory erpVoucherStrategyFactory;

    @Resource
    private IFinInvoiceLedgerService finInvoiceLedgerService;

    @Override
    public FinMappingVouchersSource selectFinMappingVouchersSourceById(Long id) {
        return finMappingVouchersSourceMapper.selectFinMappingVouchersSourceById(id);
    }

    @Override
    public List<FinMappingVouchersSource> selectFinMappingVouchersSourceList(FinMappingVouchersSource record) {
        return finMappingVouchersSourceMapper.selectFinMappingVouchersSourceList(record);
    }

    @Override
    public FinMappingVouchersSource selectByOrderId(String groupid, String orderId) {
        return finMappingVouchersSourceMapper.selectByOrderId(groupid, orderId);
    }

    @Override
    public List<FinMappingVouchersSource> selectByOrderIds(String groupid, List<String> orderIds) {
        if (orderIds == null || orderIds.isEmpty()) {
            return null;
        }
        return finMappingVouchersSourceMapper.selectByOrderIds(groupid, orderIds);
    }

    @Override
    public int insertFinMappingVouchersSource(FinMappingVouchersSource record) {
        if (record.getCreatedTime() == null) {
            record.setCreatedTime(new Date());
        }
        if (record.getUpdatedTime() == null) {
            record.setUpdatedTime(new Date());
        }
        return finMappingVouchersSourceMapper.insertFinMappingVouchersSource(record);
    }

    @Override
    public int updateFinMappingVouchersSource(FinMappingVouchersSource record) {
        record.setUpdatedTime(new Date());
        return finMappingVouchersSourceMapper.updateFinMappingVouchersSource(record);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteFinMappingVouchersSourceById(Long id) {
        // 查询记录获取关联信息
        FinMappingVouchersSource record = finMappingVouchersSourceMapper.selectFinMappingVouchersSourceById(id);
        if (record == null) {
            return 0;
        }

        Long voucherId = record.getVoucherId();
        String groupid = record.getGroupid();
        String orderId = record.getOrderId();
        String voucherType = record.getVoucherType();

        // 第一步：删除凭证及其关联数据（凭证分录、辅助核算、附件）
        if (voucherId != null) {
            deleteVoucherAndRelatedData(voucherId);
        }

        // 第二步：还原原始单据状态
        revokeOriginalDocumentStatus(groupid, orderId, voucherId, voucherType);

        // 第三步：删除追溯信息（凭证映射记录 + 凭证生成记录）
        if (voucherId != null) {
            finMappingVouchersMapper.deleteByVoucherId(voucherId);
        }
        return finMappingVouchersSourceMapper.deleteFinMappingVouchersSourceById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteFinMappingVouchersSourceByIds(Long[] ids) {
        if (ids == null || ids.length == 0) {
            return 0;
        }

        // 查询所有要删除的记录
        List<FinMappingVouchersSource> records = new ArrayList<>();
        for (Long id : ids) {
            FinMappingVouchersSource record = finMappingVouchersSourceMapper.selectFinMappingVouchersSourceById(id);
            if (record != null) {
                records.add(record);
            }
        }

        if (records.isEmpty()) {
            return 0;
        }

        // 第一步：批量删除凭证及其关联数据
        List<Long> voucherIds = records.stream()
                .filter(v -> v.getVoucherId() != null)
                .map(FinMappingVouchersSource::getVoucherId)
                .distinct()
                .collect(Collectors.toList());
        if (!voucherIds.isEmpty()) {
            for (Long voucherId : voucherIds) {
                deleteVoucherAndRelatedData(voucherId);
            }
        }

        // 第二步：逐条还原原始单据状态（不同类型策略不同）
        for (FinMappingVouchersSource record : records) {
            revokeOriginalDocumentStatus(record.getGroupid(), record.getOrderId(),
                    record.getVoucherId(), record.getVoucherType());
        }

        // 第三步：批量删除追溯信息
        if (!voucherIds.isEmpty()) {
            finMappingVouchersMapper.deleteByVoucherIds(voucherIds);
        }
        int rows = 0;
        for (Long id : ids) {
            rows += finMappingVouchersSourceMapper.deleteFinMappingVouchersSourceById(id);
        }
        return rows;
    }

    /**
     * 删除凭证及其关联数据（分录、辅助核算、附件）
     */
    private void deleteVoucherAndRelatedData(Long voucherId) {
        // 删除凭证附件
        FinVouchers finVouchers = new FinVouchers();
        finVouchers.setVoucherId(voucherId);
        finVourchesFileMapper.deleteFinVourchesFileByVoucherId(finVouchers);

        // 删除凭证辅助核算
        finVoucherEntriesAuxiliaryMapper.deleteByVoucherId(voucherId);

        // 删除凭证分录
        finVoucherEntriesMapper.deleteByVoucherId(voucherId);

        // 删除凭证
        finVouchersMapper.deleteFinVouchersByVoucherId(voucherId);
    }

    /**
     * 根据凭证类型还原原始单据状态
     * <ul>
     *   <li>payment/inventory_transit/inventory_inbound：委托给对应的ERP策略</li>
     *   <li>invoice/invoice_carrier：还原发票的 posting_status 和 voucher_id</li>
     * </ul>
     */
    private void revokeOriginalDocumentStatus(String groupid, String orderId,
                                               Long voucherId, String voucherType) {
        if (orderId == null || groupid == null || voucherType == null) {
            return;
        }

        switch (voucherType) {
            case "invoice":
            case "invoice_carrier":
                // 发票凭证：还原发票的入账状态
                try {
                    Long invoiceId = Long.valueOf(orderId);
                    finInvoiceLedgerService.batchUpdatePostingStatus(
                            Collections.singletonList(invoiceId), 0, null, "system");
                    log.info("撤销发票凭证：已还原发票[{}]的入账状态", orderId);
                } catch (NumberFormatException e) {
                    log.warn("撤销发票凭证：orderId[{}]不是有效的发票ID", orderId);
                } catch (Exception e) {
                    log.error("撤销发票凭证：还原发票[{}]入账状态失败", orderId, e);
                }
                break;
            case "payment":
            case "inventory_transit":
            case "inventory_inbound":
                // ERP凭证类型：委托给对应的策略处理
                try {
                    String ftype = mapVoucherTypeToStrategy(voucherType);
                    if (ftype != null && erpVoucherStrategyFactory.hasStrategy(ftype)) {
                        IErpVoucherStrategy strategy = erpVoucherStrategyFactory.getStrategy(ftype);
                        strategy.revokeOriginalDocumentStatus(groupid, orderId, voucherId);
                    }
                } catch (Exception e) {
                    log.error("撤销ERP凭证：还原原始单据状态失败，voucherType={}, orderId={}", voucherType, orderId, e);
                }
                break;
            default:
                log.info("撤销凭证：未知的凭证类型[{}]，跳过还原原始单据状态", voucherType);
                break;
        }
    }

    /**
     * 将 fin_mapping_vouchers_source.voucherType 映射到 IErpVoucherStrategy.getFtype()
     */
    private String mapVoucherTypeToStrategy(String voucherType) {
        switch (voucherType) {
            case "payment":
                return "erppayment";
            case "inventory_transit":
            case "inventory_inbound":
                return "erpinventory";
            default:
                return null;
        }
    }
}
