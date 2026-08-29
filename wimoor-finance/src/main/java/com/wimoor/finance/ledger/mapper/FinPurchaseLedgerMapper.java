package com.wimoor.finance.ledger.mapper;

import com.wimoor.finance.ledger.domain.dto.FinPurchaseLedgerQuery;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 采购账户台账Mapper接口
 *
 * @author wimoor
 */
public interface FinPurchaseLedgerMapper {

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
    List<Map<String, Object>> selectAccounts(@Param("groupid") String groupid);

    /**
     * 查询某个订单的付款明细
     */
    List<Map<String, Object>> selectPayments(@Param("entryId") String entryId);

    /**
     * 新增付款记录
     */
    int insertPayment(Map<String, Object> payment);

    /**
     * 更新采购订单的已付总额和付款状态
     */
    int updateEntryPayStatus(@Param("entryId") String entryId);

    /**
     * 更新账户余额（扣减）
     */
    int updateAccountBalance(@Param("acct") String acct, @Param("payprice") BigDecimal payprice);
}
