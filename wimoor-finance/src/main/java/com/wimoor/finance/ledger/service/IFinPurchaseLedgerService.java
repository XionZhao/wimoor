package com.wimoor.finance.ledger.service;

import com.wimoor.finance.ledger.domain.dto.FinPurchaseLedgerQuery;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 采购账户台账Service接口
 *
 * @author wimoor
 */
public interface IFinPurchaseLedgerService {

    /**
     * 查询采购订单列表
     */
    List<Map<String, Object>> selectPurchaseLedgerList(FinPurchaseLedgerQuery query);

    /**
     * 统计订单总额、已付总额、未付总额
     */
    Map<String, Object> selectPurchaseLedgerStatistics(FinPurchaseLedgerQuery query);

    /**
     * 查询所有采购账户及余额
     */
    List<Map<String, Object>> selectAccounts(String groupid);

    /**
     * 查询某个订单的付款明细
     */
    List<Map<String, Object>> selectPayments(String entryId);

    /**
     * 付款操作（创建付款记录，更新订单付款状态和账户余额）
     */
    void pay(String entryId, String acct, String projectid, BigDecimal payprice,
             String remark, String operator, Integer paymentMethod);
}
