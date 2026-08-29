package com.wimoor.erp.purchase.alibaba.mapper;

import java.util.Map;

import com.wimoor.erp.purchase.alibaba.pojo.entity.PurchaseAlibabaSettlementOrder;

import org.apache.ibatis.annotations.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author wimoor team
 * @since 2023-11-01
 */
@Mapper
public interface PurchaseAlibabaSettlementOrderMapper extends BaseMapper<PurchaseAlibabaSettlementOrder> {

	// 已结转汇总（订单数、入账金额、已付金额）
	Map<String, Object> getSettledSummary(Map<String, Object> param);

	// 未结转汇总（订单数、付款金额）
	Map<String, Object> getUnsettledSummary(Map<String, Object> param);

	// 1688账单明细汇总
	Map<String, Object> getOrderSummary(Map<String, Object> param);
}
