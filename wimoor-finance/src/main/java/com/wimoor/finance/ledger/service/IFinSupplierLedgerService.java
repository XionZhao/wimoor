package com.wimoor.finance.ledger.service;

import com.wimoor.finance.ledger.domain.FinSupplierReconcileRecord;
import com.wimoor.finance.ledger.domain.dto.SupplierLedgerQueryDTO;

import java.util.List;
import java.util.Map;

/**
 * 供应商台账Service接口
 * 采购相关数据通过Feign调用ERP模块，发票/对账数据使用本地
 *
 * @author wimoor
 */
public interface IFinSupplierLedgerService {

    /**
     * 查询供应商台账汇总（按供应商维度）
     */
    List<Map<String, Object>> getSupplierLedgerSummary(SupplierLedgerQueryDTO query);

    /**
     * 查询供应商台账统计数据（全局汇总）
     */
    Map<String, Object> getSupplierLedgerStatistics(SupplierLedgerQueryDTO query);

    /**
     * 查询供应商台账合计行（已在ERP模块的summary接口中附带）
     */
    Map<String, Object> getSupplierLedgerSummaryTotal(SupplierLedgerQueryDTO query);

    /**
     * 查询供应商订单明细
     */
    List<Map<String, Object>> getSupplierLedgerOrders(SupplierLedgerQueryDTO query);

    /**
     * 获取供应商订单总数
     */
    int getSupplierLedgerOrdersCount(SupplierLedgerQueryDTO query);

    /**
     * 查询供应商付款明细
     */
    List<Map<String, Object>> getSupplierLedgerPayments(SupplierLedgerQueryDTO query);

    /**
     * 获取供应商付款总数
     */
    int getSupplierLedgerPaymentsCount(SupplierLedgerQueryDTO query);

    /**
     * 查询供应商发票明细
     */
    List<Map<String, Object>> getSupplierLedgerInvoices(SupplierLedgerQueryDTO query);

    /**
     * 对账操作（标记供应商已对账，保存对账记录）
     */
    boolean reconcileSupplier(String groupid, String supplierId, String operator,
                               Map<String, Object> reconcileData);

    /**
     * 查询对账详情（点击最后对账日期弹窗展示）
     */
    FinSupplierReconcileRecord getReconcileDetail(String groupid, String supplierId, String reconcileMonth);

    /**
     * 查询未开票订单（用于导出）
     */
    List<Map<String, Object>> getUninvoicedOrders(SupplierLedgerQueryDTO query);
}
