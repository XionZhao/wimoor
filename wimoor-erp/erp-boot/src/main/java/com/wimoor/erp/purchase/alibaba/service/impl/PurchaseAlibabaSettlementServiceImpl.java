package com.wimoor.erp.purchase.alibaba.service.impl;

import com.wimoor.erp.purchase.alibaba.pojo.entity.PurchaseAlibabaSettlement;
import com.wimoor.erp.purchase.alibaba.pojo.vo.SettlementSummaryVO;
import com.wimoor.erp.purchase.pojo.dto.PurchaseSettlementDTO;
import com.wimoor.common.GeneralUtil;
import com.wimoor.erp.purchase.alibaba.mapper.PurchaseAlibabaSettlementMapper;
import com.wimoor.erp.purchase.alibaba.mapper.PurchaseAlibabaSettlementOrderMapper;
import com.wimoor.erp.finance.pojo.entity.FinAccountPeriodRollover;
import com.wimoor.erp.finance.mapper.FinAccountPeriodRolloverMapper;
import com.wimoor.erp.finance.service.IFinAccountPeriodRolloverService;
import com.wimoor.erp.purchase.mapper.PurchaseFormPaymentMapper;
import com.wimoor.erp.purchase.pojo.entity.PurchaseFormPayment;
import com.wimoor.erp.purchase.service.IPurchaseFormPaymentService;
import com.wimoor.erp.purchase.alibaba.service.IPurchaseAlibabaSettlementService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.extern.slf4j.Slf4j;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author wimoor team
 * @since 2023-11-01
 */
@Slf4j
@Service
public class PurchaseAlibabaSettlementServiceImpl extends ServiceImpl<PurchaseAlibabaSettlementMapper, PurchaseAlibabaSettlement> implements IPurchaseAlibabaSettlementService {

	@Autowired
	PurchaseAlibabaSettlementOrderMapper purchaseAlibabaSettlementOrderMapper;
	@Autowired
	PurchaseFormPaymentMapper purchaseFormPaymentMapper;
	@Autowired
	IPurchaseFormPaymentService purchaseFormPaymentService;
	@Autowired
	IFinAccountPeriodRolloverService finAccountPeriodRolloverService;
	@Autowired
	FinAccountPeriodRolloverMapper finAccountPeriodRolloverMapper;

	public String getString(	Row info ) {
		if(info==null)return null;
		Cell cell = info.getCell(1);
		if(cell==null)return null;
		Object value = cell.getCellTypeEnum().equals(CellType.STRING)? cell.getStringCellValue():cell.getNumericCellValue();
		return value==null?null:value.toString();
	}
	public BigDecimal getDecimal(Row info) {
		if(info==null)return null;
		Cell cell = info.getCell(1);
		if(cell==null)return null;
		 if( cell.getCellTypeEnum().equals(CellType.STRING)) {
				String value=cell.getStringCellValue();
				if(value.contains("-")) {
					return null;
				}
				return value!=null?new BigDecimal(value):null;
		 }else {
			 return new BigDecimal(cell.getNumericCellValue());
		 }
	}
	public Integer getInteger(Row info) {
		if(info==null)return null;
		Cell cell = info.getCell(1);
		if(cell==null)return null;
		 if( cell.getCellTypeEnum().equals(CellType.STRING)) {
			 String value=cell.getStringCellValue();
				if(value.contains("-")) {
					return null;
				}
				return value!=null?  Integer.parseInt(value):null;
		 }else {
			 Double value =  Double.valueOf(cell.getNumericCellValue());
			 return value.intValue();
		 }
	}
	public Date getDate(Row info) {
		if(info==null)return null;
		Cell cell = info.getCell(1);
		if(cell==null)return null;
		return getDate(cell);
	}
	public Date getDate(Cell cell) {
		if(cell==null)return null;
		if(cell.getCellTypeEnum().equals(CellType.STRING)) {
			SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
			String value = cell.getStringCellValue();
			if(value==null)return null;
			else {
				try {
					return fmt.parse(value);
				} catch (ParseException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					return  GeneralUtil.getDatez(value);
				}
			}
			
		}else {
			return cell.getDateCellValue();
		}
	}
	public Boolean setSettlementSheet(Workbook workbook, PurchaseAlibabaSettlement settlement) {
		// TODO Auto-generated method stub
		Sheet sheet = workbook.getSheetAt(0);
		if(sheet.getLastRowNum()>=8) {
			Row info=sheet.getRow(1);
			settlement.setAlibabaAccount(getString(info));
			info=sheet.getRow(2);
			settlement.setAmount(getDecimal(info));
			info=sheet.getRow(3);
			settlement.setQuantity(getInteger(info));
			info=sheet.getRow(4);
			settlement.setPostdate(getDate(info));
			info=sheet.getRow(5);
			settlement.setPaydate(getDate(info));
			info=sheet.getRow(6);
			settlement.setLoaddate(getDate(info));
			info=sheet.getRow(6);
			settlement.setRemark(getString(info));
		}
		sheet = workbook.getSheetAt(1);
		if(sheet!=null&&sheet.getLastRowNum()>=7) {
			Row info=sheet.getRow(2);
			settlement.setPayamount(getDecimal(info));
			info=sheet.getRow(3);
			settlement.setPaytimes(getInteger(info));
		}
		sheet = workbook.getSheetAt(2);
		if(sheet!=null&&sheet.getLastRowNum()>=7) {
			Row info=sheet.getRow(3);
			settlement.setReturnamount(getDecimal(info));
			info=sheet.getRow(4);
			settlement.setReturntimes(getInteger(info));
		}
		sheet = workbook.getSheetAt(3);
		if(sheet!=null&&sheet.getLastRowNum()>=6) {
			Row info=sheet.getRow(2);
			settlement.setPayreturnamount(getDecimal(info));
		}
		settlement.setOpttime(new Date());
	    LambdaQueryWrapper<PurchaseAlibabaSettlement> query=new LambdaQueryWrapper<PurchaseAlibabaSettlement>();
	    query.eq(PurchaseAlibabaSettlement::getAcct, settlement.getAcct());
	    query.eq(PurchaseAlibabaSettlement::getShopid, settlement.getShopid());
	    query.eq(PurchaseAlibabaSettlement::getAlibabaAccount, settlement.getAlibabaAccount());
	    query.eq(PurchaseAlibabaSettlement::getPostdate, settlement.getPostdate());
		PurchaseAlibabaSettlement one = this.baseMapper.selectOne(query);
		if(one!=null) {
			settlement.setId(one.getId());
			this.baseMapper.updateById(settlement);
		}else {
			this.baseMapper.insert(settlement);
		}
		return true;
	}

	@Override
	public SettlementSummaryVO getSummary(PurchaseSettlementDTO dto, String shopid) {
		SettlementSummaryVO vo = new SettlementSummaryVO();
		Map<String, Object> param = new HashMap<>();
		param.put("acct", dto.getAcct());
		param.put("shopid", shopid);
		param.put("fromDate", dto.getFromDate());
		param.put("toDate", dto.getToDate());

		// 已结转汇总
		Map<String, Object> settledMap = purchaseAlibabaSettlementOrderMapper.getSettledSummary(param);
		if (settledMap != null) {
			Object settledCount = settledMap.get("settledCount");
			Object settledAmount = settledMap.get("settledAmount");
			Object settledPaid = settledMap.get("settledPaid");
			vo.setSettledCount(settledCount != null ? ((Number) settledCount).intValue() : 0);
			vo.setSettledAmount(settledAmount != null ? new BigDecimal(settledAmount.toString()) : BigDecimal.ZERO);
			vo.setSettledPaid(settledPaid != null ? new BigDecimal(settledPaid.toString()) : BigDecimal.ZERO);
		}

		// 未结转汇总
		Map<String, Object> unsettledMap = purchaseAlibabaSettlementOrderMapper.getUnsettledSummary(param);
		if (unsettledMap != null) {
			Object unsettledCount = unsettledMap.get("unsettledCount");
			Object unsettledAmount = unsettledMap.get("unsettledAmount");
			vo.setUnsettledCount(unsettledCount != null ? ((Number) unsettledCount).intValue() : 0);
			vo.setUnsettledAmount(unsettledAmount != null ? new BigDecimal(unsettledAmount.toString()) : BigDecimal.ZERO);
		}

		return vo;
	}

	@Override
	public IPage<Map<String, Object>> getMatchedOrders(PurchaseSettlementDTO dto, String shopid) {
		Page<?> page = dto.getPage();
		Map<String, Object> param = new HashMap<>();
		param.put("acct", dto.getAcct());
		param.put("shopid", shopid);
		param.put("fromDate", dto.getFromDate());
		param.put("toDate", dto.getToDate());
		param.put("search", dto.getSearch());
		return purchaseFormPaymentMapper.matchedOrdersPage(page, param);
	}

	@Override
	public IPage<Map<String, Object>> getUnsettledList(PurchaseSettlementDTO dto, String shopid) {
		Page<?> page = dto.getPage();
		Map<String, Object> param = new HashMap<>();
		param.put("acct", dto.getAcct());
		param.put("shopid", shopid);
		param.put("fromDate", dto.getFromDate());
		param.put("toDate", dto.getToDate());
		param.put("search", dto.getSearch());
		return purchaseFormPaymentMapper.unsettledListPage(page, param);
	}

	@Override
	public void exportMatchedOrders(SXSSFWorkbook workbook, PurchaseSettlementDTO dto, String shopid) {
		Map<String, Object> param = new HashMap<>();
		param.put("acct", dto.getAcct());
		param.put("shopid", shopid);
		param.put("fromDate", dto.getFromDate());
		param.put("toDate", dto.getToDate());
		param.put("search", dto.getSearch());
		// 复用付款报表导出格式，使用 matchedOrders 查询
		List<Map<String, Object>> list = purchaseFormPaymentMapper.matchedOrders(param);
		writePaymentReportExcel(workbook, list);
	}

	@Override
	public void exportUnsettledList(SXSSFWorkbook workbook, PurchaseSettlementDTO dto, String shopid) {
		Map<String, Object> param = new HashMap<>();
		param.put("acct", dto.getAcct());
		param.put("shopid", shopid);
		param.put("fromDate", dto.getFromDate());
		param.put("toDate", dto.getToDate());
		param.put("search", dto.getSearch());
		// 复用付款报表导出格式，使用 unsettledList 查询
		List<Map<String, Object>> list = purchaseFormPaymentMapper.unsettledList(param);
		writePaymentReportExcel(workbook, list);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void settle(List<String> ids, String acct, String operator) {
		if (ids == null || ids.isEmpty()) {
			return;
		}
		// 查询选中的付款记录，计算总金额
		BigDecimal totalAmount = BigDecimal.ZERO;
		List<PurchaseFormPayment> payments = purchaseFormPaymentService.listByIds(ids);
		for (PurchaseFormPayment payment : payments) {
			if (payment.getPayprice() != null) {
				totalAmount = totalAmount.add(payment.getPayprice());
			}
		}
		// 创建结转记录
		FinAccountPeriodRollover rollover = new FinAccountPeriodRollover();
		rollover.setAcct(acct);
		rollover.setTotalAmount(totalAmount);
		rollover.setOperator(operator);
		rollover.setCreator(operator);
		rollover.setOpttime(new Date());
		rollover.setCreatetime(new Date());
		finAccountPeriodRolloverService.save(rollover);
		String rolloverId = rollover.getId();
		// 更新付款记录的period_rollover_id
		for (PurchaseFormPayment payment : payments) {
			payment.setPeriodRolloverId(rolloverId);
			payment.setOperator(operator);
			payment.setOpttime(new Date());
		}
		purchaseFormPaymentService.updateBatchById(payments);
	}

	@Override
	public IPage<Map<String, Object>> getRolloverList(PurchaseSettlementDTO dto, String shopid) {
		Page<?> page = dto.getPage();
		Map<String, Object> param = new HashMap<>();
		param.put("acct", dto.getAcct());
		param.put("fromDate", dto.getFromDate());
		param.put("toDate", dto.getToDate());
		return finAccountPeriodRolloverMapper.selectRolloverList(page, param);
	}

	@Override
	public List<Map<String, Object>> getRolloverDetail(String rolloverId) {
		return finAccountPeriodRolloverMapper.selectRolloverDetail(rolloverId);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void cancelRollover(String rolloverId, String operator) {
		// 还原付款明细的period_rollover_id（使用UpdateWrapper显式设置NULL）
		purchaseFormPaymentService.update(
			new LambdaUpdateWrapper<PurchaseFormPayment>()
				.eq(PurchaseFormPayment::getPeriodRolloverId, rolloverId)
				.set(PurchaseFormPayment::getPeriodRolloverId, null)
				.set(PurchaseFormPayment::getOperator, operator)
				.set(PurchaseFormPayment::getOpttime, new Date())
		);
		// 删除主表记录
		finAccountPeriodRolloverService.removeById(rolloverId);
	}

	@Override
	public List<String> getAllUnsettledIds(Map<String, Object> param) {
		return purchaseFormPaymentMapper.getAllUnsettledIds(param);
	}

	@Override
	public int getUnsettledCount(Map<String, Object> param) {
		return purchaseFormPaymentMapper.countUnsettledByCondition(param);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void settleAll(Map<String, Object> param, String operator, String acct) {
		List<String> ids = purchaseFormPaymentMapper.getUnsettledIdsByCondition(param);
		if (ids == null || ids.isEmpty()) {
			return;
		}
		settle(ids, acct, operator);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public List<String> importSettle(List<Map<String, String>> importData, String acct, String operator, String shopid) {
		// 使用Set去重
		java.util.Set<String> settledIdSet = new java.util.HashSet<>();
		// 收集所有匹配的付款记录ID
		for (Map<String, String> data : importData) {
			String orderId = data.get("orderId");
			String sku = data.get("sku");
			Map<String, Object> param = new HashMap<>();
			param.put("shopid", shopid);
			param.put("acct", acct);
			param.put("orderId", orderId);
			param.put("sku", sku);
			List<String> ids = purchaseFormPaymentMapper.getUnsettledIdsByOrderAndSku(param);
			log.info("查询条件: 订单号={}, SKU={}, 账户={}, 匹配到{}条记录", orderId, sku, acct, ids != null ? ids.size() : 0);
			if (ids != null && !ids.isEmpty()) {
				settledIdSet.addAll(ids);
			}
		}
		List<String> allSettledIds = new java.util.ArrayList<>(settledIdSet);
		log.info("总共匹配到{}条去重后的付款记录", allSettledIds.size());
		// 将所有匹配的记录合并到一个结转记录中
		if (!allSettledIds.isEmpty()) {
			settle(allSettledIds, acct, operator);
		}
		return allSettledIds;
	}

	private void writePaymentReportExcel(SXSSFWorkbook workbook, List<Map<String, Object>> list) {
		Map<String, Object> titlemap = new java.util.LinkedHashMap<>();
		titlemap.put("number", "订单编码");
		titlemap.put("createdate", "创建日期");
		titlemap.put("cname", "供应商");
		titlemap.put("sku", "SKU");
		titlemap.put("groupname", "店铺名称");
		titlemap.put("mname", "产品名称");
		titlemap.put("paystatus", "付款状态");
		titlemap.put("purchases", "订单采购量");
		titlemap.put("totalin", "订单已入库");
		titlemap.put("orderprice", "订单采购金额");
		titlemap.put("totalpay", "订单已付款");
		titlemap.put("payment_method", "付款方式");
		titlemap.put("fee_type", "费用类型");
		titlemap.put("payprice", "付款金额");
		titlemap.put("name", "操作人");
		titlemap.put("opttime", "付款日期");
		titlemap.put("remark", "备注");
		titlemap.put("wname", "仓库");

		org.apache.poi.ss.usermodel.Sheet sheet = workbook.createSheet("sheet1");
		org.apache.poi.ss.usermodel.Row trow = sheet.createRow(0);
		Object[] titlearray = titlemap.keySet().toArray();
		for (int i = 0; i < titlearray.length; i++) {
			org.apache.poi.ss.usermodel.Cell cell = trow.createCell(i);
			Object value = titlemap.get(titlearray[i].toString());
			cell.setCellValue(value.toString());
		}
		for (int i = 0; i < list.size(); i++) {
			org.apache.poi.ss.usermodel.Row row = sheet.createRow(i + 1);
			Map<String, Object> item = list.get(i);
			for (int j = 0; j < titlearray.length; j++) {
				org.apache.poi.ss.usermodel.Cell cell = row.createCell(j);
				Object val = item.get(titlearray[j].toString());
				if (val != null) {
					cell.setCellValue(val.toString());
				}
			}
		}
	}
}
