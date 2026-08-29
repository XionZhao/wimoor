package com.wimoor.finance.ledger.service.impl;

import com.wimoor.finance.ledger.domain.FinCarrierReconcileRecord;
import com.wimoor.finance.ledger.mapper.FinCarrierLedgerMapper;
import com.wimoor.finance.ledger.service.IFinCarrierLedgerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 承运商台账Service实现类
 *
 * @author wimoor
 */
@Service
public class FinCarrierLedgerServiceImpl implements IFinCarrierLedgerService {

    private static final Logger log = LoggerFactory.getLogger(FinCarrierLedgerServiceImpl.class);

    @Autowired
    private FinCarrierLedgerMapper finCarrierLedgerMapper;

    @Override
    public List<Map<String, Object>> getCarrierLedgerSummary(Map<String, Object> params) {
        String shopid = (String) params.get("shopid");
        String groupid = (String) params.get("groupid");
        String marketplaceid = (String) params.get("marketplaceid");
        String companyid = (String) params.get("companyid");
        String channelid = (String) params.get("channelid");
        String fromDate = (String) params.get("fromDate");
        String toDate = (String) params.get("toDate");

        return finCarrierLedgerMapper.selectCarrierLedgerSummary(
                shopid, groupid, marketplaceid, companyid, channelid, fromDate, toDate);
    }

    @Override
    public Map<String, Object> getCarrierLedgerStatistics(Map<String, Object> params) {
        String shopid = (String) params.get("shopid");
        String groupid = (String) params.get("groupid");
        String marketplaceid = (String) params.get("marketplaceid");
        String companyid = (String) params.get("companyid");
        String channelid = (String) params.get("channelid");
        String fromDate = (String) params.get("fromDate");
        String toDate = (String) params.get("toDate");

        return finCarrierLedgerMapper.selectCarrierLedgerStatistics(
                shopid, groupid, marketplaceid, companyid, channelid, fromDate, toDate);
    }

    @Override
    public boolean reconcileCarrier(String groupid, String carrierId, String operator, Map<String, Object> reconcileData) {
        try {
            String reconcileMonth = reconcileData.get("reconcileMonth") != null ? reconcileData.get("reconcileMonth").toString() : null;
            if (reconcileMonth == null || reconcileMonth.isEmpty()) {
                return false;
            }

            // 检查是否已存在对账记录
            FinCarrierReconcileRecord existingRecord = finCarrierLedgerMapper.selectReconcileRecord(groupid, carrierId, reconcileMonth);

            String carrierName = reconcileData.get("carrierName") != null ? reconcileData.get("carrierName").toString() : "";
            String companyName = reconcileData.get("companyName") != null ? reconcileData.get("companyName").toString() : "";

            if (existingRecord != null) {
                // 更新现有记录
                existingRecord.setCarrierName(carrierName);
                existingRecord.setCompanyName(companyName);
                existingRecord.setTotalPlanQty(reconcileData.get("totalPlanQty") != null ? Integer.parseInt(reconcileData.get("totalPlanQty").toString()) : 0);
                existingRecord.setTotalActualQty(reconcileData.get("totalActualQty") != null ? Integer.parseInt(reconcileData.get("totalActualQty").toString()) : 0);
                existingRecord.setTotalReceivedQty(reconcileData.get("totalReceivedQty") != null ? Integer.parseInt(reconcileData.get("totalReceivedQty").toString()) : 0);
                existingRecord.setTotalShipFee(reconcileData.get("totalShipFee") != null ? new java.math.BigDecimal(reconcileData.get("totalShipFee").toString()) : java.math.BigDecimal.ZERO);
                existingRecord.setTotalOtherFee(reconcileData.get("totalOtherFee") != null ? new java.math.BigDecimal(reconcileData.get("totalOtherFee").toString()) : java.math.BigDecimal.ZERO);
                existingRecord.setTotalWorth(reconcileData.get("totalWorth") != null ? new java.math.BigDecimal(reconcileData.get("totalWorth").toString()) : java.math.BigDecimal.ZERO);
                existingRecord.setTotalShipmentNum(reconcileData.get("totalShipmentNum") != null ? Integer.parseInt(reconcileData.get("totalShipmentNum").toString()) : 0);
                existingRecord.setReconcileBy(operator);
                existingRecord.setReconcileTime(new Date());
                finCarrierLedgerMapper.updateReconcileRecord(existingRecord);
            } else {
                // 插入新记录
                FinCarrierReconcileRecord record = new FinCarrierReconcileRecord();
                record.setGroupid(groupid);
                record.setCarrierId(carrierId);
                record.setCarrierName(carrierName);
                record.setCompanyName(companyName);
                record.setReconcileMonth(reconcileMonth);
                record.setTotalPlanQty(reconcileData.get("totalPlanQty") != null ? Integer.parseInt(reconcileData.get("totalPlanQty").toString()) : 0);
                record.setTotalActualQty(reconcileData.get("totalActualQty") != null ? Integer.parseInt(reconcileData.get("totalActualQty").toString()) : 0);
                record.setTotalReceivedQty(reconcileData.get("totalReceivedQty") != null ? Integer.parseInt(reconcileData.get("totalReceivedQty").toString()) : 0);
                record.setTotalShipFee(reconcileData.get("totalShipFee") != null ? new java.math.BigDecimal(reconcileData.get("totalShipFee").toString()) : java.math.BigDecimal.ZERO);
                record.setTotalOtherFee(reconcileData.get("totalOtherFee") != null ? new java.math.BigDecimal(reconcileData.get("totalOtherFee").toString()) : java.math.BigDecimal.ZERO);
                record.setTotalWorth(reconcileData.get("totalWorth") != null ? new java.math.BigDecimal(reconcileData.get("totalWorth").toString()) : java.math.BigDecimal.ZERO);
                record.setTotalShipmentNum(reconcileData.get("totalShipmentNum") != null ? Integer.parseInt(reconcileData.get("totalShipmentNum").toString()) : 0);
                record.setReconcileBy(operator);
                record.setReconcileTime(new Date());
                finCarrierLedgerMapper.insertReconcileRecord(record);
            }
            return true;
        } catch (Exception e) {
            log.error("承运商对账失败，carrierId={}, reconcileMonth={}", carrierId, reconcileData.get("reconcileMonth"), e);
            return false;
        }
    }

    @Override
    public FinCarrierReconcileRecord getReconcileDetail(String groupid, String carrierId, String reconcileMonth) {
        return finCarrierLedgerMapper.selectReconcileRecord(groupid, carrierId, reconcileMonth);
    }
}
