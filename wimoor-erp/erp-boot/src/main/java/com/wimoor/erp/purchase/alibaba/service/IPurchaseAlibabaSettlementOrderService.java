package com.wimoor.erp.purchase.alibaba.service;

import java.util.Map;

import com.wimoor.erp.purchase.alibaba.pojo.entity.PurchaseAlibabaSettlement;
import com.wimoor.erp.purchase.alibaba.pojo.entity.PurchaseAlibabaSettlementOrder;

import org.apache.poi.ss.usermodel.Workbook;

import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author wimoor team
 * @since 2023-11-01
 */
public interface IPurchaseAlibabaSettlementOrderService extends IService<PurchaseAlibabaSettlementOrder> {
	public Boolean paySettlementSheetOrder(Workbook workbook, PurchaseAlibabaSettlement settlement);
	// 1688账单明细汇总
	Map<String, Object> getOrderSummary(Map<String, Object> param);
}
