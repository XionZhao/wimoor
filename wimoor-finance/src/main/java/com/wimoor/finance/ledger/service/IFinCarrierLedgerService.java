package com.wimoor.finance.ledger.service;

import com.wimoor.finance.ledger.domain.FinCarrierReconcileRecord;

import java.util.List;
import java.util.Map;

/**
 * 承运商台账Service接口
 *
 * @author wimoor
 */
public interface IFinCarrierLedgerService {

    /**
     * 查询承运商台账汇总（按承运商维度）
     */
    List<Map<String, Object>> getCarrierLedgerSummary(Map<String, Object> params);

    /**
     * 查询承运商台账统计数据（全局汇总）
     */
    Map<String, Object> getCarrierLedgerStatistics(Map<String, Object> params);

    /**
     * 承运商对账操作
     */
    boolean reconcileCarrier(String groupid, String carrierId, String operator, Map<String, Object> reconcileData);

    /**
     * 获取承运商对账详情
     */
    FinCarrierReconcileRecord getReconcileDetail(String groupid, String carrierId, String reconcileMonth);
}
