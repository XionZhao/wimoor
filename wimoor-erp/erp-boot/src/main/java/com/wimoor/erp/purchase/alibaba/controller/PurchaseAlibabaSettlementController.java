package com.wimoor.erp.purchase.alibaba.controller;


import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.wimoor.common.GeneralUtil;
import com.wimoor.common.mvc.BizException;
import com.wimoor.common.result.Result;
import com.wimoor.common.service.impl.SystemControllerLog;
import com.wimoor.common.user.UserInfo;
import com.wimoor.common.user.UserInfoContext;
import com.wimoor.erp.purchase.alibaba.pojo.entity.PurchaseAlibabaSettlement;
import com.wimoor.erp.purchase.alibaba.pojo.entity.PurchaseAlibabaSettlementOrder;
import com.wimoor.erp.purchase.alibaba.pojo.entity.PurchaseAlibabaSettlementOrderReturn;
import com.wimoor.erp.purchase.alibaba.pojo.entity.PurchaseAlibabaSettlementPay;
import com.wimoor.erp.purchase.alibaba.pojo.entity.PurchaseAlibabaSettlementPayReturn;
import com.wimoor.erp.purchase.alibaba.service.IPurchaseAlibabaSettlementOrderReturnService;
import com.wimoor.erp.purchase.alibaba.service.IPurchaseAlibabaSettlementOrderService;
import com.wimoor.erp.purchase.alibaba.service.IPurchaseAlibabaSettlementPayReturnService;
import com.wimoor.erp.purchase.alibaba.service.IPurchaseAlibabaSettlementPayService;
import com.wimoor.erp.purchase.alibaba.service.IPurchaseAlibabaSettlementService;
import com.wimoor.erp.purchase.alibaba.service.IPurchaseFormEntryAlibabaInfoService;
import com.wimoor.erp.purchase.pojo.dto.PaymentReportDTO;
import com.wimoor.erp.purchase.pojo.dto.PurchaseSettlementDTO;
import com.wimoor.erp.purchase.alibaba.pojo.vo.SettlementSummaryVO;

import cn.hutool.core.util.StrUtil;
import io.swagger.annotations.Api;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;

import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author wimoor team
 * @since 2023-11-01
 */
@Api(tags = "1688账期接口")
@RestController
@SystemControllerLog( "1688账期")
@RequestMapping("/api/v1/purchase/alibaba/entry/purchaseAlibabaSettlement")
@RequiredArgsConstructor
@Slf4j
public class PurchaseAlibabaSettlementController {

	@Autowired
	IPurchaseAlibabaSettlementService purchaseAlibabaSettlementService;
    @Autowired
    IPurchaseAlibabaSettlementOrderService purchaseAlibabaSettlementOrderService;
	@Autowired
	IPurchaseAlibabaSettlementPayService purchaseAlibabaSettlementPayService;
    @Autowired
    IPurchaseAlibabaSettlementOrderReturnService purchaseAlibabaSettlementOrderReturnService;
	@Autowired
	IPurchaseAlibabaSettlementPayReturnService purchaseAlibabaSettlementPayReturnService;
	@Autowired
	IPurchaseFormEntryAlibabaInfoService purchaseFormEntryAlibabaInfoService;

	@GetMapping(value = "/downExcelTemp")
	public void downExcelTempAction(HttpServletResponse response) {
		try {
			SXSSFWorkbook workbook = new SXSSFWorkbook();
			response.setContentType("application/force-download");
			response.addHeader("Content-Disposition", "attachment;fileName=PurchaseAlibabaSettlement.xlsx");
			ServletOutputStream fOut = response.getOutputStream();
			Sheet sheet = workbook.createSheet("导入模板");
			// 列宽设置
			sheet.setColumnWidth(0, 25 * 256);  // 订单编码
			sheet.setColumnWidth(1, 20 * 256);  // SKU
			// 必填项标题样式（灰色背景）
			CellStyle requiredStyle = workbook.createCellStyle();
			requiredStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
			requiredStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
			// 表头行
			Row titleRow = sheet.createRow(0);
			titleRow.setHeight((short) (25 * 15));
			String[] titles = {"订单编码", "SKU"};
			for (int i = 0; i < titles.length; i++) {
				Cell cell = titleRow.createCell(i);
				cell.setCellValue(titles[i]);
				cell.setCellStyle(requiredStyle);
			}
			workbook.write(fOut);
			workbook.close();
			fOut.flush();
			fOut.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@PostMapping(value = "/uploadPayDate",consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public Result<String> uploadPayDateAction(@RequestParam("file")MultipartFile file,@RequestParam String acct, HttpServletResponse response)  {
			if (file != null) {
				try {
					UserInfo userinfo = UserInfoContext.get();
					InputStream inputStream = file.getInputStream();
					Workbook workbook = WorkbookFactory.create(inputStream);
					PurchaseAlibabaSettlement settlement=new PurchaseAlibabaSettlement();
					settlement.setShopid(userinfo.getCompanyid());
					settlement.setAcct(acct);
					Boolean haserror=purchaseAlibabaSettlementService.setSettlementSheet(workbook,settlement);
				    Boolean ordererror=purchaseAlibabaSettlementOrderService.paySettlementSheetOrder(workbook, settlement);
				    Boolean payerror=purchaseAlibabaSettlementPayService.uploadSettlementSheet(workbook, settlement);
					Boolean orderReturnError=purchaseAlibabaSettlementOrderReturnService.paySettlementSheetOrder(workbook, settlement);
					Boolean payReturnError=purchaseAlibabaSettlementPayReturnService.uploadSettlementSheet(workbook, settlement);
				    if(haserror||ordererror||payerror||orderReturnError||payReturnError) {
						ServletOutputStream fOut = null;
						try {
							response.setContentType("application/force-download");// 设置强制下载不打开
							response.addHeader("Content-Disposition", "attachment;fileName=error.xlsx");// 设置文件名
							fOut = response.getOutputStream();
							workbook.write(fOut);
						} catch (Exception e2) {
							e2.printStackTrace();
						}finally {
							try {
								if(fOut != null) {
									fOut.flush();
									fOut.close();
								}
								if(workbook != null) {
									workbook.close();
								}
							} catch (IOException e3) {
								e3.printStackTrace();
							}
						}
					}
					workbook.close();
					return null;
				} catch (IOException e) {
					e.printStackTrace();
					return Result.failed();
				} catch (EncryptedDocumentException e) {
					e.printStackTrace();
				}  catch (Exception e) {
					e.printStackTrace();
				}
			}
		return Result.success("ok");
	}

	
	@PostMapping(value = "/list" )
	public Result<?> listAction(@RequestBody PurchaseSettlementDTO dto)  {
		UserInfo userinfo = UserInfoContext.get();
		List<PurchaseAlibabaSettlement> list = purchaseAlibabaSettlementService
												.lambdaQuery()
												.ge(PurchaseAlibabaSettlement::getPostdate,dto.getFromDate())
										        .le(PurchaseAlibabaSettlement::getPostdate, dto.getToDate())
										        .eq(PurchaseAlibabaSettlement::getAcct, dto.getAcct())
										        .eq(PurchaseAlibabaSettlement::getShopid, userinfo.getCompanyid())
										        .orderByDesc(PurchaseAlibabaSettlement::getPostdate)
										        .list();
		return Result.success(list);
	}
	
	@PostMapping(value = "/orderSummary" )
	public Result<?> orderSummaryAction(@RequestBody PurchaseSettlementDTO dto)  {
		UserInfo userinfo = UserInfoContext.get();
		Map<String, Object> param = new HashMap<>();
		param.put("acct", dto.getAcct());
		param.put("settlementid", dto.getSettlementid());
		Map<String, Object> summary = purchaseAlibabaSettlementOrderService.getOrderSummary(param);
		return Result.success(summary);
	}

	@PostMapping(value = "/orderList" )
	public Result<?> orderListAction(@RequestBody PurchaseSettlementDTO dto)  {
		LambdaQueryChainWrapper<PurchaseAlibabaSettlementOrder> query = purchaseAlibabaSettlementOrderService.lambdaQuery();
		if(StrUtil.isNotBlank(dto.getSearch())) {
			query.eq(PurchaseAlibabaSettlementOrder::getOrderid, dto.getSearch().trim());
		}
		PurchaseAlibabaSettlement settlment = purchaseAlibabaSettlementService.getById(dto.getSettlementid());
		List<PurchaseAlibabaSettlementOrder> list = query.eq(PurchaseAlibabaSettlementOrder::getSettlementid, dto.getSettlementid())
										        .list();
		List<PurchaseAlibabaSettlementOrder> resultlist=new LinkedList<PurchaseAlibabaSettlementOrder>();
		if(dto.getIscheck()!=null&&dto.getIscheck()==true) {
		   for(PurchaseAlibabaSettlementOrder item:list) {
			   try {
					BigDecimal price = item.getConfirmamount();
					if(item.getReturnamount()!=null) {
						price=price.subtract(item.getReturnamount());
					}
					purchaseFormEntryAlibabaInfoService.checkPay(new BigInteger(item.getOrderid().toString()),price,settlment.getAcct());
				}catch(BizException e) {
					 item.setRemark(e.getMessage());
					 resultlist.add(item);
				}
		   }
			IPage<PurchaseAlibabaSettlementOrder> page = dto.getListPage(resultlist);
			return Result.success(page);
		}else {
			IPage<PurchaseAlibabaSettlementOrder> page = dto.getListPage(list);
			for(PurchaseAlibabaSettlementOrder item:page.getRecords()) {
				try {
					BigDecimal price = item.getConfirmamount();
					if(item.getReturnamount()!=null) {
						price=price.subtract(item.getReturnamount());
					}
					purchaseFormEntryAlibabaInfoService.checkPay(new BigInteger(item.getOrderid().toString()),price,settlment.getAcct());
				}catch(BizException e) {
					 item.setRemark(e.getMessage());
				}
			}
			return Result.success(page);
		}
		
	}
	
	@PostMapping(value = "/payList" )
	public Result<?> payListAction(@RequestBody PurchaseSettlementDTO dto)  {
		List<PurchaseAlibabaSettlementPay> list = purchaseAlibabaSettlementPayService
												.lambdaQuery()
										        .eq(PurchaseAlibabaSettlementPay::getSettlementid, dto.getSettlementid())
										        .list();
		IPage<PurchaseAlibabaSettlementPay> page = dto.getListPage(list);
		return Result.success(page);
	}
	
	@PostMapping(value = "/returnPayList" )
	public Result<?> returnPayListAction(@RequestBody PurchaseSettlementDTO dto)  {
		List<PurchaseAlibabaSettlementPayReturn> list = purchaseAlibabaSettlementPayReturnService
												.lambdaQuery()
										        .eq(PurchaseAlibabaSettlementPayReturn::getSettlementid, dto.getSettlementid())
										        .list();
		IPage<PurchaseAlibabaSettlementPayReturn> page = dto.getListPage(list);
		return Result.success(page);
	}
	
	@PostMapping(value = "/orderReturnList" )
	public Result<?> orderReturnListAction(@RequestBody PurchaseSettlementDTO dto)  {
		List<PurchaseAlibabaSettlementOrderReturn> list = purchaseAlibabaSettlementOrderReturnService
												.lambdaQuery()
										        .eq(PurchaseAlibabaSettlementOrderReturn::getSettlementid, dto.getSettlementid())
										        .list();
		IPage<PurchaseAlibabaSettlementOrderReturn> page = dto.getListPage(list);
		return Result.success(page);
	}
	
	@PostMapping(value = "/delete" )
	public Result<?> deleteAction(@RequestBody PurchaseSettlementDTO dto)  {
		 LambdaQueryWrapper<PurchaseAlibabaSettlementPay> payquery = new LambdaQueryWrapper<PurchaseAlibabaSettlementPay>();
		 payquery.eq(PurchaseAlibabaSettlementPay::getSettlementid, dto.getSettlementid());
		 purchaseAlibabaSettlementPayService.remove(payquery);
		  
		 LambdaQueryWrapper<PurchaseAlibabaSettlementOrder> orderquery = new LambdaQueryWrapper<PurchaseAlibabaSettlementOrder>();
		 orderquery.eq(PurchaseAlibabaSettlementOrder::getSettlementid, dto.getSettlementid());
		 purchaseAlibabaSettlementOrderService.remove(orderquery);
		
		 LambdaQueryWrapper<PurchaseAlibabaSettlementOrderReturn> orderReturnQuery = new LambdaQueryWrapper<PurchaseAlibabaSettlementOrderReturn>();
		 orderReturnQuery.eq(PurchaseAlibabaSettlementOrderReturn::getSettlementid, dto.getSettlementid());
		 purchaseAlibabaSettlementOrderReturnService.remove(orderReturnQuery);
		
		 LambdaQueryWrapper<PurchaseAlibabaSettlementPayReturn> payReturnQuery = new LambdaQueryWrapper<PurchaseAlibabaSettlementPayReturn>();
		 payReturnQuery.eq(PurchaseAlibabaSettlementPayReturn::getSettlementid, dto.getSettlementid());
		 purchaseAlibabaSettlementPayReturnService.remove(payReturnQuery);
		
		 purchaseAlibabaSettlementService.removeById(dto.getSettlementid());
		 return Result.success("success");
	}

	@PostMapping(value = "/summary" )
	public Result<?> summaryAction(@RequestBody PurchaseSettlementDTO dto)  {
		UserInfo userinfo = UserInfoContext.get();
		SettlementSummaryVO vo = purchaseAlibabaSettlementService.getSummary(dto, userinfo.getCompanyid());
		return Result.success(vo);
	}

	@PostMapping(value = "/matchedOrders" )
	public Result<?> matchedOrdersAction(@RequestBody PurchaseSettlementDTO dto)  {
		UserInfo userinfo = UserInfoContext.get();
		IPage<Map<String, Object>> page = purchaseAlibabaSettlementService.getMatchedOrders(dto, userinfo.getCompanyid());
		return Result.success(page);
	}

	@PostMapping(value = "/unsettledList" )
	public Result<?> unsettledListAction(@RequestBody PurchaseSettlementDTO dto)  {
		UserInfo userinfo = UserInfoContext.get();
		IPage<Map<String, Object>> page = purchaseAlibabaSettlementService.getUnsettledList(dto, userinfo.getCompanyid());
		return Result.success(page);
	}

	@PostMapping(value = "/exportMatchedOrders" )
	public void exportMatchedOrdersAction(@RequestBody PurchaseSettlementDTO dto, HttpServletResponse response)  {
		UserInfo userinfo = UserInfoContext.get();
		SXSSFWorkbook workbook = new SXSSFWorkbook();
		purchaseAlibabaSettlementService.exportMatchedOrders(workbook, dto, userinfo.getCompanyid());
		ServletOutputStream fOut = null;
		try {
			response.setContentType("application/force-download");
			response.addHeader("Content-Disposition", "attachment;fileName=matchedOrders.xlsx");
			fOut = response.getOutputStream();
			workbook.write(fOut);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				if(fOut != null) {
					fOut.flush();
					fOut.close();
				}
				workbook.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	@PostMapping(value = "/exportUnsettledList" )
	public void exportUnsettledListAction(@RequestBody PurchaseSettlementDTO dto, HttpServletResponse response)  {
		UserInfo userinfo = UserInfoContext.get();
		SXSSFWorkbook workbook = new SXSSFWorkbook();
		purchaseAlibabaSettlementService.exportUnsettledList(workbook, dto, userinfo.getCompanyid());
		ServletOutputStream fOut = null;
		try {
			response.setContentType("application/force-download");
			response.addHeader("Content-Disposition", "attachment;fileName=unsettledOrders.xlsx");
			fOut = response.getOutputStream();
			workbook.write(fOut);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				if(fOut != null) {
					fOut.flush();
					fOut.close();
				}
				workbook.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	@PostMapping(value = "/settle" )
	public Result<?> settleAction(@RequestBody Map<String, Object> body)  {
		UserInfo userinfo = UserInfoContext.get();
		@SuppressWarnings("unchecked")
		List<String> ids = (List<String>) body.get("ids");
		String acct = (String) body.get("acct");
		if (ids == null || ids.isEmpty()) {
			return Result.failed("请选择要结转的付款记录");
		}
		purchaseAlibabaSettlementService.settle(ids, acct, userinfo.getId());
		return Result.success("结转成功");
	}

	@PostMapping(value = "/importSettle", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public Result<String> importSettleAction(@RequestParam("file") MultipartFile file, @RequestParam String acct, HttpServletResponse response) {
		if (file != null) {
			try {
				UserInfo userinfo = UserInfoContext.get();
				InputStream inputStream = file.getInputStream();
				Workbook workbook = WorkbookFactory.create(inputStream);
				// 读取Excel中的订单编码和SKU
				Sheet sheet = workbook.getSheetAt(0);
				List<Map<String, String>> importData = new LinkedList<>();
				for (int i = 1; i <= sheet.getLastRowNum(); i++) {
					Row row = sheet.getRow(i);
					if (row == null) continue;
					Cell orderCell = row.getCell(0);
					Cell skuCell = row.getCell(1);
					if (orderCell != null && skuCell != null) {
						String orderId = orderCell.toString().trim();
						String sku = skuCell.toString().trim();
						if (StrUtil.isNotBlank(orderId) && StrUtil.isNotBlank(sku)) {
							Map<String, String> data = new HashMap<>();
							data.put("orderId", orderId);
							data.put("sku", sku);
							importData.add(data);
						}
					}
				}
				workbook.close();
				// 调用服务处理导入数据
				List<String> settledIds = purchaseAlibabaSettlementService.importSettle(importData, acct, userinfo.getId(), userinfo.getCompanyid());
				if (settledIds.isEmpty()) {
					return Result.failed("未找到匹配的付款记录");
				}
				return Result.success("成功导入" + settledIds.size() + "条结转记录");
			} catch (IOException e) {
				e.printStackTrace();
				return Result.failed("文件读取失败");
			} catch (EncryptedDocumentException e) {
				e.printStackTrace();
				return Result.failed("文件格式错误");
			} catch (Exception e) {
				e.printStackTrace();
				return Result.failed("导入失败: " + e.getMessage());
			}
		}
		return Result.failed("请选择文件");
	}

	@PostMapping(value = "/rolloverList" )
	public Result<?> rolloverListAction(@RequestBody PurchaseSettlementDTO dto)  {
		UserInfo userinfo = UserInfoContext.get();
		IPage<Map<String, Object>> page = purchaseAlibabaSettlementService.getRolloverList(dto, userinfo.getCompanyid());
		return Result.success(page);
	}

	@PostMapping(value = "/rolloverDetail" )
	public Result<?> rolloverDetailAction(@RequestBody Map<String, Object> body)  {
		String rolloverId = (String) body.get("rolloverId");
		List<Map<String, Object>> list = purchaseAlibabaSettlementService.getRolloverDetail(rolloverId);
		return Result.success(list);
	}

	@PostMapping(value = "/cancelRollover" )
	public Result<?> cancelRolloverAction(@RequestBody Map<String, Object> body)  {
		UserInfo userinfo = UserInfoContext.get();
		String rolloverId = (String) body.get("rolloverId");
		purchaseAlibabaSettlementService.cancelRollover(rolloverId, userinfo.getId());
		return Result.success("撤销成功");
	}

	@PostMapping(value = "/getAllUnsettledIds" )
	public Result<?> getAllUnsettledIdsAction(@RequestBody PaymentReportDTO condition)  {
		UserInfo userinfo = UserInfoContext.get();
		Map<String, Object> param = buildPaymentQueryParam(userinfo, condition);
		log.info("[全部结转] 统计参数: {}", param);
		int count = purchaseAlibabaSettlementService.getUnsettledCount(param);
		log.info("[全部结转] 统计结果: {}条", count);
		return Result.success(count);
	}

	@PostMapping(value = "/settleAll" )
	public Result<?> settleAllAction(@RequestBody PaymentReportDTO condition)  {
		UserInfo userinfo = UserInfoContext.get();
		Map<String, Object> param = buildPaymentQueryParam(userinfo, condition);
		String acct = condition.getAcct();
		purchaseAlibabaSettlementService.settleAll(param, userinfo.getId(), acct);
		return Result.success("全部结转成功");
	}

	/**
	 * 构建付款查询参数（与getPaymentReport完全一致的参数构建逻辑）
	 */
	private Map<String, Object> buildPaymentQueryParam(UserInfo userinfo, PaymentReportDTO condition) {
		Map<String, Object> param = new HashMap<>();
		param.put("shopid", userinfo.getCompanyid());
		if (StrUtil.isNotBlank(condition.getSearch())) {
			param.put("search", condition.getSearch().trim() + "%");
		} else {
			param.put("search", null);
		}
		if (condition.getDatetype() != null) {
			param.put("datetype", condition.getDatetype());
		} else {
			param.put("datetype", "paydate");
		}
		// 注意：前端Datepicker已经将toDate拼接了" 23:59:59"，所以这里不再重复拼接
		if (StrUtil.isNotBlank(condition.getFromDate())) {
			param.put("fromDate", condition.getFromDate().trim());
		} else {
			param.put("fromDate", null);
		}
		if (StrUtil.isNotBlank(condition.getToDate())) {
			param.put("endDate", condition.getToDate().trim());
		} else {
			param.put("endDate", null);
		}
		if (StrUtil.isNotBlank(condition.getSearchtype())) {
			param.put("searchtype", condition.getSearchtype());
		} else {
			param.put("searchtype", "sku");
		}
		param.put("warehouseid", StrUtil.isNotBlank(condition.getWarehouseid()) ? condition.getWarehouseid() : null);
		param.put("settlementid", StrUtil.isNotBlank(condition.getSettlementid()) ? condition.getSettlementid() : null);
		param.put("supplierid", StrUtil.isNotBlank(condition.getSupplierid()) ? condition.getSupplierid() : null);
		param.put("paymethod", StrUtil.isNotBlank(condition.getPaymethod()) ? condition.getPaymethod() : null);
		param.put("projectid", StrUtil.isNotBlank(condition.getProjectid()) ? condition.getProjectid() : null);
		param.put("groupid", StrUtil.isNotBlank(condition.getGroupid()) ? condition.getGroupid() : null);
		return param;
	}

}

