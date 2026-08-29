package com.wimoor.erp.purchase.alibaba.service;

import java.util.List;
import java.util.Map;

import com.wimoor.erp.purchase.alibaba.pojo.entity.PurchaseAlibabaSettlement;
import com.wimoor.erp.purchase.alibaba.pojo.vo.SettlementSummaryVO;
import com.wimoor.erp.purchase.pojo.dto.PurchaseSettlementDTO;

import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author wimoor team
 * @since 2023-11-01
 */
public interface IPurchaseAlibabaSettlementService extends IService<PurchaseAlibabaSettlement> {
	public Boolean setSettlementSheet(Workbook workbook, PurchaseAlibabaSettlement settlement);

	// 结算汇总（已结转+未结转）
	SettlementSummaryVO getSummary(PurchaseSettlementDTO dto, String shopid);

	// 已结转订单分页列表
	IPage<Map<String, Object>> getMatchedOrders(PurchaseSettlementDTO dto, String shopid);

	// 未结转订单分页列表
	IPage<Map<String, Object>> getUnsettledList(PurchaseSettlementDTO dto, String shopid);

	// 导出已结转订单
	void exportMatchedOrders(SXSSFWorkbook workbook, PurchaseSettlementDTO dto, String shopid);

	// 导出未结转订单
	void exportUnsettledList(SXSSFWorkbook workbook, PurchaseSettlementDTO dto, String shopid);

	// 结转选中的付款记录
	void settle(List<String> ids, String acct, String operator);

	// 查询结转记录列表
	IPage<Map<String, Object>> getRolloverList(PurchaseSettlementDTO dto, String shopid);

	// 查询结转详情（关联的付款明细）
	List<Map<String, Object>> getRolloverDetail(String rolloverId);

	// 撤销结转
	void cancelRollover(String rolloverId, String operator);

	// 获取所有未结转记录的ID
	List<String> getAllUnsettledIds(Map<String, Object> param);

	// 获取未结转记录数量
	int getUnsettledCount(Map<String, Object> param);

	// 全部结转
	void settleAll(Map<String, Object> param, String operator, String acct);

	// 导入结转
	List<String> importSettle(List<Map<String, String>> importData, String acct, String operator, String shopid);
}
