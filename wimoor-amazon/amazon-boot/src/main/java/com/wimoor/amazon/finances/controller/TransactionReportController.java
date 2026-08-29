package com.wimoor.amazon.finances.controller;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.wimoor.amazon.auth.pojo.entity.AmazonAuthority;
import com.wimoor.amazon.auth.service.IAmazonAuthorityService;
import com.wimoor.amazon.finances.mapper.TransactionReportMapper;
import com.wimoor.common.result.Result;

import io.swagger.annotations.Api;

@Api(tags = "交易报告统计")
@RestController
@RequestMapping("/api/v1/fin/transactionReport")
public class TransactionReportController {

	@Resource
	TransactionReportMapper transactionReportMapper;

	@Resource
	IAmazonAuthorityService amazonAuthorityService;

	@PostMapping("/getFeeSummaryByType")
	public Result<?> getFeeSummaryByType(@RequestBody Map<String, Object> params) {
		String groupid = (String) params.get("groupid");
		String marketplaceid = (String) params.get("marketplaceid");
		String startDate = (String) params.get("startDate");
		String endDate = (String) params.get("endDate");
		AmazonAuthority auth = amazonAuthorityService.selectByGroupAndMarket(groupid, marketplaceid);
		String amazonauthid = auth != null ? auth.getId() : null;
		List<Map<String, Object>> list = transactionReportMapper.selectFeeSummaryByType(amazonauthid, startDate, endDate);
		return Result.success(list);
	}

	@PostMapping("/getDailyFeeByType")
	public Result<?> getDailyFeeByType(@RequestBody Map<String, Object> params) {
		String groupid = (String) params.get("groupid");
		String marketplaceid = (String) params.get("marketplaceid");
		String startDate = (String) params.get("startDate");
		String endDate = (String) params.get("endDate");
		AmazonAuthority auth = amazonAuthorityService.selectByGroupAndMarket(groupid, marketplaceid);
		String amazonauthid = auth != null ? auth.getId() : null;
		List<Map<String, Object>> list = transactionReportMapper.selectDailyFeeByType(amazonauthid, startDate, endDate);
		return Result.success(list);
	}

	@PostMapping("/getSalesCompare")
	public Result<?> getSalesCompare(@RequestParam("amazonauthid") String amazonauthid,
			@RequestParam("startDate") String startDate, @RequestParam("endDate") String endDate,
			@RequestParam("groupType") String groupType) {
		List<Map<String, Object>> list = transactionReportMapper.selectSalesCompareByDate(amazonauthid, startDate,
				endDate, groupType);
		return Result.success(list);
	}

	@PostMapping("/getDetailPage")
	public Result<?> getDetailPage(@RequestBody Map<String, Object> params) {
		String groupid = (String) params.get("groupid");
		String marketplaceid = (String) params.get("marketplaceid");
		String startDate = (String) params.get("startDate");
		String endDate = (String) params.get("endDate");
		String searchType = (String) params.get("searchType");
		String searchValue = (String) params.get("searchValue");
		int page = params.get("page") != null ? ((Number) params.get("page")).intValue() : 1;
		int size = params.get("size") != null ? ((Number) params.get("size")).intValue() : 50;
		// 将 groupid + marketplaceid 转换为 amazonauthid
		AmazonAuthority auth = amazonAuthorityService.selectByGroupAndMarket(groupid, marketplaceid);
		String amazonauthid = auth != null ? auth.getId() : null;
		int offset = (page - 1) * size;
		long total = transactionReportMapper.selectDetailCount(amazonauthid, startDate, endDate, searchType, searchValue);
		List<Map<String, Object>> list = transactionReportMapper.selectDetailPage(amazonauthid, startDate, endDate,
				searchType, searchValue, offset, size);
		Map<String, Object> result = new HashMap<>();
		result.put("list", list);
		result.put("total", total);
		return Result.success(result);
	}

	@PostMapping("/exportDetail")
	public void exportDetail(@RequestBody Map<String, Object> params,
			HttpServletResponse response) throws IOException {
		String groupid = (String) params.get("groupid");
		String marketplaceid = (String) params.get("marketplaceid");
		String startDate = (String) params.get("startDate");
		String endDate = (String) params.get("endDate");
		String searchType = (String) params.get("searchType");
		String searchValue = (String) params.get("searchValue");
		// 将 groupid + marketplaceid 转换为 amazonauthid
		AmazonAuthority auth = amazonAuthorityService.selectByGroupAndMarket(groupid, marketplaceid);
		String amazonauthid = auth != null ? auth.getId() : null;
		List<Map<String, Object>> list = transactionReportMapper.selectDetailForExport(amazonauthid, startDate, endDate,
				searchType, searchValue);

		Workbook workbook = new XSSFWorkbook();
		Sheet sheet = workbook.createSheet("交易报告明细");

		// 表头
		String[] headers = { "交易日期", "交易类型", "订单号", "结算ID", "SKU", "数量", "商城", "履约方式",
				"城市", "州", "邮编", "征税模型",
				"产品销售", "销售税", "运费抵扣", "运费税",
				"礼品包装", "礼品税", "促销返利", "促销税",
				"代扣税", "销售费用", "FBA费用", "其他交易费", "其他", "合计" };

		CellStyle headerStyle = workbook.createCellStyle();
		Font headerFont = workbook.createFont();
		headerFont.setBold(true);
		headerStyle.setFont(headerFont);

		Row headerRow = sheet.createRow(0);
		for (int i = 0; i < headers.length; i++) {
			Cell cell = headerRow.createCell(i);
			cell.setCellValue(headers[i]);
			cell.setCellStyle(headerStyle);
		}

		int rowIndex = 1;
			for (Map<String, Object> row : list) {
				Row dataRow = sheet.createRow(rowIndex++);
				int col = 0;
				Object dt = row.get("date_time");
				dataRow.createCell(col++).setCellValue(formatDateTime(dt));
				dataRow.createCell(col++).setCellValue(str(row.get("transaction_type")));
			dataRow.createCell(col++).setCellValue(str(row.get("order_id")));
			dataRow.createCell(col++).setCellValue(str(row.get("settlement_id")));
			dataRow.createCell(col++).setCellValue(str(row.get("sku")));
			dataRow.createCell(col++).setCellValue(row.get("quantity") != null ? ((Number) row.get("quantity")).intValue() : 0);
			dataRow.createCell(col++).setCellValue(str(row.get("marketplace")));
			dataRow.createCell(col++).setCellValue(str(row.get("fulfillment")));
			dataRow.createCell(col++).setCellValue(str(row.get("order_city")));
			dataRow.createCell(col++).setCellValue(str(row.get("order_state")));
			dataRow.createCell(col++).setCellValue(str(row.get("order_post_code")));
			dataRow.createCell(col++).setCellValue(str(row.get("tax_collection_model")));
			for (int i = 0; i < 14; i++) {
				String[] feeCols = { "product_sales", "product_sales_tax", "shipping_credits", "shipping_credits_tax",
						"giftwrap_credits", "giftwrap_credits_tax", "promotional_rebates", "promotional_rebates_tax",
						"marketplace_withheld_tax", "selling_fees", "fba_fees", "other_transaction_fees", "other", "total" };
				Object val = row.get(feeCols[i]);
				dataRow.createCell(col++).setCellValue(val != null ? ((Number) val).doubleValue() : 0);
			}
		}

		// 自动列宽
		for (int i = 0; i < headers.length; i++) {
			sheet.autoSizeColumn(i);
		}

		String fileName = URLEncoder.encode("交易报告明细_" + startDate + "_" + endDate, "UTF-8");
		response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
		response.setHeader("Content-Disposition", "attachment;filename=" + fileName + ".xlsx");

		OutputStream out = response.getOutputStream();
		workbook.write(out);
		out.flush();
		out.close();
		workbook.close();
		}

		private String formatDateTime(Object dt) {
			if (dt == null) return "";
			if (dt instanceof LocalDateTime) {
				return ((LocalDateTime) dt).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
			}
			if (dt instanceof Date) {
				return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format((Date) dt);
			}
			return dt.toString();
		}

		private String str(Object obj) {
		return obj != null ? obj.toString() : "";
	}
}
