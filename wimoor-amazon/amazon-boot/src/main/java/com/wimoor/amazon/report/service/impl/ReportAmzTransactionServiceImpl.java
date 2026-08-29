package com.wimoor.amazon.report.service.impl;

import com.amazon.spapi.SellingPartnerAPIAA.LWAException;
import com.amazon.spapi.api.ReportsApi;
import com.amazon.spapi.client.ApiCallback;
import com.amazon.spapi.client.ApiException;
import com.amazon.spapi.model.reports.GetReportsResponse;
import com.wimoor.amazon.auth.pojo.entity.AmazonAuthority;
import com.wimoor.amazon.auth.pojo.entity.Marketplace;
import com.wimoor.amazon.finances.mapper.TransactionReportMapper;
import com.wimoor.amazon.finances.mapper.AmzTransactionReportColumnMappingMapper;
import com.wimoor.amazon.finances.pojo.entity.TransactionReport;
import com.wimoor.amazon.finances.pojo.entity.TransactionReportValidation;
import com.wimoor.amazon.finances.pojo.entity.AmzTransactionReportColumnMapping;
import com.wimoor.amazon.finances.service.TransactionReportValidationService;
import com.wimoor.amazon.util.AmzDateUtils;
import com.wimoor.common.GeneralUtil;
import org.springframework.stereotype.Service;
import org.threeten.bp.OffsetDateTime;

import javax.annotation.Resource;
import java.io.BufferedReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


@Service("reportAmzTransactionService")
public class ReportAmzTransactionServiceImpl extends ReportServiceImpl {
	@Resource
	private TransactionReportMapper transactionReportMapper;
	
	@Resource
	private TransactionReportValidationService transactionReportValidationService;
	
	@Resource
	private AmzTransactionReportColumnMappingMapper columnMappingMapper;
	
	/** 列名映射缓存: marketplaceid -> (CSV列名 -> 实体字段名) */
	private final Map<String, Map<String, String>> columnMappingCache = new HashMap<>();

	@Override
	public void   requestReport(AmazonAuthority amazonAuthority,Calendar cstart,Calendar cend,Boolean ignore) {
		    ReportsApi api = apiBuildService.getReportsApi(amazonAuthority);
		    List<Marketplace> marketlist = marketplaceService.findbyauth(amazonAuthority.getId());
		    boolean doneEU = false;
		    List<String> marketplaceIds=new ArrayList<String>();
			for(Marketplace market:marketlist) {
				// 过滤无效的商城ID：必须非空且符合亚马逊商城ID格式（字母数字，通常13-14位）
				String marketId = market.getMarketplaceid();
				if(marketId != null && !marketId.isEmpty() && marketId.matches("^[A-Z0-9]{5,20}$")) {
					marketplaceIds.add(marketId);
				} else {
					System.out.println("[TransactionReport] Skipping invalid marketplaceId: " + marketId + " for seller=" + amazonAuthority.getSellerid());
				}
			}
			List<String> reportTypes=new LinkedList<String>();
			reportTypes.add(this.myReportType());
			List<String> processingStatuses=new LinkedList<String>();
			processingStatuses.add("DONE");
			processingStatuses.add("CANCELLED");
			processingStatuses.add("FATAL");
			processingStatuses.add("IN_PROGRESS");
			processingStatuses.add("IN_QUEUE");
			OffsetDateTime createdSince=AmzDateUtils.getOffsetDateTimeUTC(cstart);
			OffsetDateTime createdUntil=AmzDateUtils.getOffsetDateTimeUTC(cend);
			System.out.println("[TransactionReport] Requesting reports for seller=" + amazonAuthority.getSellerid()
			    + ", createdSince=" + createdSince + ", createdUntil=" + createdUntil
			    + ", marketplaceIds=" + marketplaceIds);
			String nextToken=null;
			int pageSize=100;
			final ApiCallback<GetReportsResponse> callback=new ApiCallbackGetReports(this,amazonAuthority);
			try {
				// 不传marketplaceIds，让API自动匹配卖家有权访问的商城
				api.getReportsAsync(reportTypes,processingStatuses,null,pageSize,createdSince,createdUntil,nextToken,callback);
			} catch (ApiException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (LWAException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}





	public static boolean isNumericzidai(String str) {
		if (str == null)
			return false;
		Pattern pattern = Pattern.compile("-?[0-9]+\\.?[0-9]*");
		Matcher isNum = pattern.matcher(str);
		if (!isNum.matches()) {
			return false;
		}
		return true;
	}

	@Override
	public String treatResponse(AmazonAuthority amazonAuthority, BufferedReader br) {
		int lineNumber = 0;
		String line;
		String mlog="";
		List<TransactionReport> list=new ArrayList<TransactionReport>();
		boolean headerFound = false;
		int dataStartLine = 0;
		// 用Set记录已经删除过的日期+站点组合，避免反复进行数据库删除操作
		// key格式: "yyyy-MM-dd|marketplaceid"，确保多站点同日期数据都能正确删除
		Set<String> deletedDates = new HashSet<>();
		
		// 创建校验对象，用于收集统计数据
		TransactionReportValidation validation = new TransactionReportValidation();

		Map<String, Integer> columnIndexMap = new HashMap<>(); // 列名到索引的映射
		try {
			while ((line = br.readLine()) != null) {
				lineNumber++;
				// 查找列名行：包含足够多列的行（通常是20列以上）
				if(!headerFound) {
					// 列名行通常有20+列，而标题行通常只有1-2列
					// 使用简单的分割来检查列数
					String[] checkInfo = line.split(",");
					if(checkInfo.length >= 20) {
						System.out.println("[TransactionReport] 找到列名在第" + lineNumber + "行，列数: " + checkInfo.length);
						System.out.println("[TransactionReport] 列名: " + line);
						headerFound = true;
						dataStartLine = lineNumber;
						// 解析列名，建立列名到索引的映射
						List<String> headerFields = parseCsvLine(line);
						for(int i = 0; i < headerFields.size(); i++) {
							String colName = headerFields.get(i).trim().toLowerCase()
								.replace("\"", "").replace("'", "");
							columnIndexMap.put(colName, i);
						}
						System.out.println("[TransactionReport] 列名映射: " + columnIndexMap);
					} else {
						System.out.println("[TransactionReport] 跳过标题行" + lineNumber + " (列数=" + checkInfo.length + "): " + line);
					}
					continue;
				}
				// 数据行 - 使用CSV解析处理引号内的逗号
				List<String> fields = parseCsvLine(line);
				
				// 调试：打印前3行数据的字段
				if(lineNumber <= dataStartLine + 3) {
					System.out.println("[TransactionReport] 数据行" + lineNumber + " 字段数: " + fields.size());
					for(int i = 0; i < Math.min(fields.size(), 30); i++) {
						System.out.println("  [" + i + "] = " + fields.get(i));
					}
				}
				
				if(fields.size() >= 10) { // 确保数据行有足够的列
					TransactionReport report = new TransactionReport();
					report.setAmazonauthid(amazonAuthority.getId());
					if(amazonAuthority.getMarketPlace() != null) {
						report.setMarketplaceid(amazonAuthority.getMarketPlace().getMarketplaceid());
					}
					// 动态映射字段（根据列名特征）
				mapFieldsByColumnNames(fields, columnIndexMap, report, amazonAuthority);
				report.setCreatetime(new Date());
				report.setOpttime(new Date());

				// 按天+站点去重：解析日期和站点，对当天+站点的数据进行删除并保存到集合中避免反复删除
					if(report.getDateTime() != null) {
						String dateKey = GeneralUtil.formatDate(report.getDateTime(), "yyyy-MM-dd");
						String deleteKey = dateKey + "|" + (report.getMarketplaceid() != null ? report.getMarketplaceid() : "");
						if(!deletedDates.contains(deleteKey)) {
							// 使用时间范围查询替代DATE()函数，确保索引可用
							Calendar dayStart = Calendar.getInstance();
							dayStart.setTime(report.getDateTime());
							dayStart.set(Calendar.HOUR_OF_DAY, 0);
							dayStart.set(Calendar.MINUTE, 0);
							dayStart.set(Calendar.SECOND, 0);
							dayStart.set(Calendar.MILLISECOND, 0);
							
							Calendar dayEnd = Calendar.getInstance();
							dayEnd.setTime(dayStart.getTime());
							dayEnd.add(Calendar.DAY_OF_MONTH, 1);
							
							com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<TransactionReport> deleteQuery =
							new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
						deleteQuery.eq("amazonauthid", amazonAuthority.getId());
						deleteQuery.eq("marketplaceid", report.getMarketplaceid());
						deleteQuery.ge("date_time", dayStart.getTime());
						deleteQuery.lt("date_time", dayEnd.getTime());
						transactionReportMapper.delete(deleteQuery);
						deletedDates.add(deleteKey);
						System.out.println("[TransactionReport] 已删除日期 " + dateKey + " marketplace=" + report.getMarketplaceid() + " 的旧数据, seller=" + amazonAuthority.getSellerid());
						}
					}
					
					// 收集统计数据用于校验
					validation.addRecord(report);
					
					list.add(report);
				} else {
					System.out.println("[TransactionReport] 数据行列数不足: " + fields.size() + ", 行: " + line);
				}
				if(list.size()>200) {
					transactionReportMapper.insertBatch(list);
					list.clear();
				}
			}
			if(list.size()>0) {
				transactionReportMapper.insertBatch(list);
			}
			
			// 数据入库完成后，进行校验
			if(validation.getTotalRecords() > 0) {
				System.out.println("[TransactionReport] 开始校验数据，Java统计记录数: " + validation.getTotalRecords());
				Map<String, Object> validationResult = transactionReportValidationService.validateTransactionReport(
							amazonAuthority.getId(), amazonAuthority.getMarketPlace().getMarketplaceid(), validation);
				
				Boolean success = (Boolean) validationResult.get("success");
				String message = (String) validationResult.get("message");
				
				if (success) {
					System.out.println("[TransactionReport] 校验通过: " + message);
					mlog = mlog + "校验通过: " + message;
				} else {
					System.out.println("[TransactionReport] 校验失败: " + message);
					@SuppressWarnings("unchecked")
					Map<String, Object> details = (Map<String, Object>) validationResult.get("details");
					if (details != null) {
						@SuppressWarnings("unchecked")
						List<String> errors = (List<String>) details.get("errors");
						if (errors != null) {
							for (String error : errors) {
								System.out.println("[TransactionReport] 校验错误: " + error);
								mlog = mlog + "\n校验错误: " + error;
							}
						}
					}
					// 校验失败，设置错误状态
					mlog = "校验失败: " + message + "\n" + mlog;
				}
			}
		} catch (NumberFormatException e) {
			e.printStackTrace();
			mlog=mlog+e.getMessage();
		} catch (IOException e) {
			e.printStackTrace();
			mlog=mlog+e.getMessage();
		} finally {
		if(list!=null) {
				list.clear();
			}
			if(br!=null) {
				try {
					br.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return mlog;
	}
	
	// 动态映射字段（优先从数据库映射表加载，fallback 到硬编码逻辑）
	private void mapFieldsByColumnNames(List<String> fields, Map<String, Integer> columnIndexMap, TransactionReport report, AmazonAuthority amazonAuthority) {
		String marketplaceid = amazonAuthority.getMarketPlace().getMarketplaceid();
		Map<String, String> colToField = getColumnMappingMap(marketplaceid);
		
		if (!colToField.isEmpty()) {
			// 数据库驱动映射
			for (Map.Entry<String, Integer> entry : columnIndexMap.entrySet()) {
				String colName = entry.getKey();
				int index = entry.getValue();
				if (index >= fields.size()) continue;
				String value = fields.get(index).trim();
				if (value.startsWith("\"") && value.endsWith("\"")) {
					value = value.substring(1, value.length() - 1);
				}
				String fieldName = colToField.get(colName);
				if (fieldName != null) {
					applyFieldMapping(fieldName, colName, value, report, amazonAuthority);
				}
			}
		} else {
			// Fallback: 硬编码逻辑（兼容未配置映射表的站点）
			mapFieldsByColumnNamesFallback(fields, columnIndexMap, report, amazonAuthority);
		}
		
		// 如果marketplace为空，使用默认值
		if (report.getMarketplace() == null || report.getMarketplace().isEmpty()) {
			report.setMarketplace(amazonAuthority.getMarketPlace().getPointName().toLowerCase());
		}
		// Order/Refund类型的description是商品名称，不存储
		String type = report.getTransactionType();
		if (type != null && (type.equals("Order") || type.equals("Refund") || type.contains("Pedido") || type.contains("Bestellung") || type.contains("Ordine") || type.contains("Bestelling") || type.contains("Commande") || type.contains("Reembolso") || type.contains("Erstattung") || type.contains("Rimborso") || type.contains("Terugbetaling") || type.contains("Remboursement"))) {
			report.setDescription(null);
		}
	}
	
	/**
	 * 从数据库加载列名映射，缓存到内存
	 */
	private Map<String, String> getColumnMappingMap(String marketplaceid) {
		Map<String, String> map = columnMappingCache.get(marketplaceid);
		if (map != null) return map;
		
		List<AmzTransactionReportColumnMapping> dbMappings = columnMappingMapper.selectByMarketplaceId(marketplaceid);
		if (dbMappings == null || dbMappings.isEmpty()) {
			// 查不到时 fallback 到 default
			dbMappings = columnMappingMapper.selectByMarketplaceId("default");
		}
		if (dbMappings == null || dbMappings.isEmpty()) {
			map = Collections.emptyMap();
		} else {
			map = new HashMap<>();
			for (AmzTransactionReportColumnMapping m : dbMappings) {
				map.put(m.getColumnName(), m.getFieldName());
			}
		}
		columnMappingCache.put(marketplaceid, map);
		return map;
	}
	
	/**
	 * 根据字段名分发到对应的 setter
	 */
	private void applyFieldMapping(String fieldName, String colName, String value,
	                                TransactionReport report, AmazonAuthority amazonAuthority) {
		switch (fieldName) {
			case "dateTime":
				if (report.getDateTime() == null && value != null && !value.isEmpty()) {
					try { report.setDateTime(getDatez(value, amazonAuthority.getMarketPlace())); } catch (Exception e) {}
				}
				break;
			case "settlementId":       report.setSettlementId(value); break;
			case "transactionType":    report.setTransactionType(value); break;
			case "description":        report.setDescription(value); break;
			case "orderId":            report.setOrderId(value); break;
			case "sku":                report.setSku(value); break;
			case "quantity":
				if (isNumericzidai(value)) report.setQuantity(Integer.parseInt(value));
				break;
			case "marketplace":
				report.setMarketplace(value != null && !value.isEmpty() ? value
					: amazonAuthority.getMarketPlace().getPointName().toLowerCase());
				break;
			case "fulfillment":         report.setFulfillment(value); break;
			case "orderCity":           report.setOrderCity(value); break;
			case "orderState":          report.setOrderState(value); break;
			case "orderPostCode":       report.setOrderPostCode(value); break;
			case "taxCollectionModel":  report.setTaxCollectionModel(value); break;
			case "productSales":        report.setProductSales(parseBigDecimal(value)); break;
			case "productSalesTax":     report.setProductSalesTax(parseBigDecimal(value)); break;
			case "shippingCredits":     report.setShippingCredits(parseBigDecimal(value)); break;
			case "shippingCreditsTax":  report.setShippingCreditsTax(parseBigDecimal(value)); break;
			case "giftwrapCredits":     report.setGiftwrapCredits(parseBigDecimal(value)); break;
			case "giftwrapCreditsTax":  report.setGiftwrapCreditsTax(parseBigDecimal(value)); break;
			case "promotionalRebates":  report.setPromotionalRebates(parseBigDecimal(value)); break;
			case "promotionalRebatesTax": report.setPromotionalRebatesTax(parseBigDecimal(value)); break;
			case "marketplaceWithheldTax": report.setMarketplaceWithheldTax(parseBigDecimal(value)); break;
			case "sellingFees":         report.setSellingFees(parseBigDecimal(value)); break;
			case "fbaFees":             report.setFbaFees(parseBigDecimal(value)); break;
			case "otherTransactionFees": report.setOtherTransactionFees(parseBigDecimal(value)); break;
			case "other":               report.setOther(parseBigDecimal(value)); break;
				case "total":               report.setTotal(parseBigDecimal(value)); break;
				case "transactionStatus":       report.setTransactionStatus(value); break;
				case "transactonReleaseDate":
					if (report.getTransactonReleaseDate() == null && value != null && !value.isEmpty()) {
						try { report.setTransactonReleaseDate(getDatez(value, amazonAuthority.getMarketPlace())); } catch (Exception e) {}
					}
					break;
			}
	}
	
	/**
	 * Fallback: 硬编码列名映射（兼容未配置映射表的站点）
	 */
	private void mapFieldsByColumnNamesFallback(List<String> fields, Map<String, Integer> columnIndexMap, TransactionReport report, AmazonAuthority amazonAuthority) {
		// 遍历列名，根据特征映射到对应字段
		for(Map.Entry<String, Integer> entry : columnIndexMap.entrySet()) {
			String colName = entry.getKey();
			int index = entry.getValue();
			if(index >= fields.size()) continue;
			String value = fields.get(index).trim();
			if(value.startsWith("\"") && value.endsWith("\"")) {
				value = value.substring(1, value.length() - 1);
			}
			
			// 根据列名特征映射字段（使用精确匹配或优先匹配短列名）
			if(colName.equals("date/time") || colName.equals("datum/zeit") || colName.equals("date/heure") || colName.contains("日付/時刻")) {
				if(report.getDateTime() == null && value != null && !value.isEmpty()) {
					try {
						report.setDateTime(getDatez(value, amazonAuthority.getMarketPlace()));
					} catch (Exception e) {}
				}
			} else if(colName.equals("settlement id") || colName.contains("決済") || colName.contains("abrechnung") || colName.contains("liquidación") || colName.contains("liquidazione") || colName.contains("identifiant du paiement") || colName.contains("identificador de pago") || colName.contains("identificatie van de afrekening")) {
				report.setSettlementId(value);
			} else if(colName.equals("type") || colName.equals("typ") || colName.contains("タイプ") || colName.equals("tipo")) {
				report.setTransactionType(value);
			} else if(colName.equals("description") || colName.equals("beschreibung") || colName.equals("descripción") || colName.equals("descrizione") || colName.contains("説明") || colName.contains("omschrijving")) {
				report.setDescription(value);
			} else if(colName.equals("order id") || colName.contains("注文ID") || colName.contains("bestellnummer") || colName.contains("id del pedido") || colName.contains("id ordine") || colName.contains("numéro de la commande") || colName.contains("número de pedido") || colName.equals("bestelnummer")) {
				report.setOrderId(value);
			} else if(colName.equals("sku")) {
				report.setSku(value);
			} else if(colName.equals("quantity") || colName.contains("数量") || colName.equals("menge") || colName.equals("cantidad") || colName.equals("quantità") || colName.equals("quantité") || colName.equals("hoeveelheid")) {
				if(isNumericzidai(value)) {
					report.setQuantity(Integer.parseInt(value));
				}
			} else if(colName.equals("marketplace") || colName.contains("マーケットプレイス") || colName.equals("verkaufsplattform") || colName.equals("place de marché") || colName.equals("site de vente") || colName.contains("web de amazon") || colName.contains("amazon-website")) {
				report.setMarketplace(value != null && !value.isEmpty() ? value : amazonAuthority.getMarketPlace().getPointName().toLowerCase());
			} else if(colName.equals("account type") || colName.equals("fulfillment") || colName.equals("fulfilment") || colName.contains("出荷") || colName.contains("versand") || colName.contains("cumplimiento") || colName.contains("logistica") || colName.equals("expédition") || colName.contains("gestión logística") || colName.equals("verzending")) {
				report.setFulfillment(value);
			} else if(colName.equals("order city") || colName.contains("市区") || colName.contains("stadt") || colName.contains("ville") || colName.contains("ciudad") || colName.contains("città") || colName.contains("ville de la commande") || colName.contains("ciudad de procedencia") || colName.equals("woonplaats")) {
				report.setOrderCity(value);
			} else if(colName.equals("order state") || colName.contains("都道府") || colName.contains("bundesland") || colName.contains("état") || colName.contains("estado") || colName.contains("stato") || colName.contains("état de la commande") || colName.contains("comunidad autónoma") || colName.equals("provincie")) {
				report.setOrderState(value);
			} else if(colName.equals("order postal") || colName.contains("郵便") || colName.contains("postleitzahl") || colName.contains("codice postale") || colName.contains("commande postale") || colName.contains("código postal") || colName.equals("postcode")) {
				report.setOrderPostCode(value);
			} else if(colName.equals("tax collection model") || colName.contains("税金徴収") || colName.contains("steuererhebung") || colName.contains("recaudación") || colName.contains("riscossione") || colName.contains("formulario de recaudación")) {
					report.setTaxCollectionModel(value);
				} else if(colName.equals("transaction status") || colName.contains("status transakcji") || colName.contains("transaktionsstatus")) {
					report.setTransactionStatus(value);
				} else if(colName.equals("transaction release date") || colName.contains("data zrealizowania") || colName.contains("transaktionens utgivningsdatum")) {
					if(value != null && !value.isEmpty()) {
						try { report.setTransactonReleaseDate(getDatez(value, amazonAuthority.getMarketPlace())); } catch (Exception e) {}
					}
				} else if(isNumericColumn(colName)) {
				BigDecimal numValue = parseBigDecimal(value);
				mapNumericField(colName, numValue, report);
			}
		}
	}
		
		// Fallback: 根据列名特征映射数值字段
		private void mapNumericField(String colName, BigDecimal value, TransactionReport report) {
			if(value == null) return;
			String lower = colName.toLowerCase();
			
			if(lower.contains("product") && lower.contains("sales") && !lower.contains("tax") || lower.contains("ventas de productos") || lower.contains("verkoop van producten")) {
				report.setProductSales(value);
			} else if(lower.contains("product") && lower.contains("sales") && lower.contains("tax") || lower.contains("sales tax collected") || lower.contains("sales tax liable") || lower.contains("impuesto de ventas de productos") || lower.contains("belasting op productverkoop")) {
				report.setProductSalesTax(value);
			} else if(lower.contains("shipping") && lower.contains("credits") && !lower.contains("tax") || lower.contains("abonos de envío") || lower.contains("verzendkredieten") || lower.contains("crédits d'expédition") || lower.contains("versandgutschriften") || lower.contains("クレジット送料") || lower.contains("postage credits")) {
				report.setShippingCredits(value);
			} else if(lower.contains("shipping") && lower.contains("credits") && lower.contains("tax") || lower.contains("impuestos por abonos de envío") || lower.contains("belasting op verzendkredieten") || lower.contains("taxe sur les crédits d'expédition") || lower.contains("versandgutschrift-steuer") || lower.contains("送料クレジット税")) {
				report.setShippingCreditsTax(value);
			} else if((lower.contains("gift") || lower.contains("geschenk") || lower.contains("ギフト") || lower.contains("envoltorio") || lower.contains("confezione") || lower.contains("abonos de envoltorio") || lower.contains("cadeaubonnen voor inpakken") || lower.contains("crédits d'emballage") || lower.contains("geschenkverpackungsgutschriften") || lower.contains("ギフト包装クレジット")) && !lower.contains("tax") && !lower.contains("impuesto") && !lower.contains("belasting") && !lower.contains("taxe") && !lower.contains("steuer") && !lower.contains("税")) {
				report.setGiftwrapCredits(value);
			} else if((lower.contains("gift") || lower.contains("geschenk") || lower.contains("ギフト") || lower.contains("envoltorio") || lower.contains("confezione") || lower.contains("abonos de envoltorio") || lower.contains("cadeaubonnen voor inpakken") || lower.contains("crédits d'emballage") || lower.contains("geschenkverpackungsgutschriften") || lower.contains("ギフト包装クレジット")) && (lower.contains("tax") || lower.contains("impuesto") || lower.contains("belasting") || lower.contains("taxe") || lower.contains("steuer") || lower.contains("税"))) {
				report.setGiftwrapCreditsTax(value);
			} else if((lower.contains("promotional") || lower.contains("werbe") || lower.contains("プロモーション") || lower.contains("promocionales") || lower.contains("promozionali") || lower.contains("devoluciones promocionales") || lower.contains("promotiekortingen") || lower.contains("remises promotionnelles") || lower.contains("werbegutschriften") || lower.contains("プロモーションリベート")) && !lower.contains("tax") && !lower.contains("impuesto") && !lower.contains("belasting") && !lower.contains("taxe") && !lower.contains("steuer") && !lower.contains("税")) {
				report.setPromotionalRebates(value);
			} else if((lower.contains("promotional") || lower.contains("werbe") || lower.contains("プロモーション") || lower.contains("promocionales") || lower.contains("promozionali") || lower.contains("devoluciones promocionales") || lower.contains("promotiekortingen") || lower.contains("remises promotionnelles") || lower.contains("werbegutschriften") || lower.contains("プロモーションリベート")) && (lower.contains("tax") || lower.contains("impuesto") || lower.contains("belasting") || lower.contains("taxe") || lower.contains("steuer") || lower.contains("税"))) {
				report.setPromotionalRebatesTax(value);
			} else if(lower.contains("marketplace") && lower.contains("withheld") || lower.contains("marktplatz") || lower.contains("マーケットプレイス徴収") || lower.contains("retenue") || lower.contains("trattenuta") || lower.contains("impuesto retenido") || lower.contains("ingehouden belasting") || lower.contains("taxe retenue par la place de marché") || lower.contains("einbehaltene marktplatzsteuer") || lower.contains("マーケットプレイス徴収税")) {
				report.setMarketplaceWithheldTax(value);
			} else if(lower.contains("selling") && lower.contains("fees") || lower.contains("verkaufsgebühr") || lower.contains("販売手数料") || lower.contains("tarifas de venta") || lower.contains("commissioni di vendita") || lower.contains("verkoopkosten") || lower.contains("frais de vente") || lower.contains("verkaufsgebühren") || lower.contains("販売手数料")) {
				report.setSellingFees(value);
			} else if(lower.contains("fba") && lower.contains("fees") || lower.contains("fba-gebühr") || lower.contains("fba手数料") || lower.contains("tarifas fba") || lower.contains("commissioni fba") || lower.contains("tarifas de logística de amazon") || lower.contains("frais pour le service expédié par amazon") || lower.contains("fba-kosten") || lower.contains("frais fba") || lower.contains("fba-gebühren") || lower.contains("fba手数料")) {
				report.setFbaFees(value);
			} else if(lower.contains("other") && lower.contains("transaction") || lower.contains("sonstige") && lower.contains("transaktion") || lower.contains("その他の取引") || lower.contains("otras tarifas") || lower.contains("altre commissioni") || lower.contains("tarifas de otras transacciones") || lower.contains("autres frais de transaction") || lower.contains("andere transaktionsgebühren") || lower.contains("その他の取引手数料") || lower.contains("andere transactiekosten") || lower.contains("autres frais de transaction") || lower.contains("andere transaktionsgebühren") || lower.contains("その他の取引手数料")) {
				report.setOtherTransactionFees(value);
			} else if(lower.equals("other") || lower.equals("sonstiges") || lower.equals("その他") || lower.equals("otros") || lower.equals("altro") || lower.equals("autres") || lower.equals("andere") || lower.equals("overig")) {
				report.setOther(value);
			} else if(lower.equals("total") || lower.equals("totaal") || lower.contains("gesamt") || lower.contains("合計")) {
				report.setTotal(value);
			}
		}
		
		// 解析BigDecimal（支持多语言数字格式）
	private BigDecimal parseBigDecimal(String value) {
		if(value == null || value.isEmpty() || value.equals("-") || value.equals("N/A")) {
			return BigDecimal.ZERO;
		}
		// 去除货币符号和空格
		value = value.replaceAll("[$€£¥\\s]", "").trim();
		// 处理括号表示的负数
		if(value.startsWith("(") && value.endsWith(")")) {
			value = "-" + value.substring(1, value.length() - 1);
		}
		
		// 处理多语言数字格式：
		// 英语: 1,234.56 或 1234.56
		// 西班牙语/法语/德语: 1.234,56 或 1234,56
		try {
			// 先尝试直接解析（英语格式）
			return new BigDecimal(value);
		} catch (NumberFormatException e1) {
			try {
				// 如果失败，尝试将逗号替换为点（处理欧洲格式）
				// 判断格式：
				// 1. 如果同时有逗号和点：
				//    - 如果逗号后只有1-2位，逗号是小数点：1.234,56 -> 1234.56
				//    - 如果逗号后有3位，逗号是千位分隔符：1,159.19 -> 1159.19
				// 2. 如果只有逗号，逗号是小数点：1234,56 -> 1234.56
				if(value.contains(",") && value.contains(".")) {
					int lastComma = value.lastIndexOf(",");
					int lastDot = value.lastIndexOf(".");
					// 逗号在点后面，且逗号后只有1-2位 -> 逗号是小数点
					if(lastComma > lastDot && value.length() - lastComma <= 3) {
						value = value.replace(".", "").replace(",", ".");
					} else {
						// 逗号是千位分隔符，点是小数点
						value = value.replace(",", "");
					}
				} else if(value.contains(",")) {
					// 格式: 1234,56 -> 1234.56
					value = value.replace(",", ".");
				}
				return new BigDecimal(value);
			} catch (NumberFormatException e2) {
				return BigDecimal.ZERO;
			}
		}
	}
	
	// 数值列特征关键词（不区分语言，通过关键词匹配）
	private static final String[] NUMERIC_PATTERNS = {
		"sales", "tax", "credits", "fees", "rebates", "rebat", "withheld", "total", "other",
		"tcs", "tds", "gst"  // 印度站点特有的税种列
	};
	
	// 判断列名是否为数值列（通过特征匹配）
	private boolean isNumericColumn(String colName) {
		if(colName == null) return false;
		String lower = colName.trim().toLowerCase();
		for(String pattern : NUMERIC_PATTERNS) {
			if(lower.contains(pattern)) {
				return true;
			}
		}
		return false;
	}
	
	// 解析CSV行，处理引号内的逗号
	private List<String> parseCsvLine(String line) {
		List<String> fields = new ArrayList<>();
		StringBuilder current = new StringBuilder();
		boolean inQuotes = false;
		
		for(int i = 0; i < line.length(); i++) {
			char c = line.charAt(i);
			if(c == '"') {
				inQuotes = !inQuotes;
			} else if(c == ',' && !inQuotes) {
				fields.add(current.toString().trim());
				current = new StringBuilder();
			} else {
				current.append(c);
			}
		}
		fields.add(current.toString().trim());
		
		return fields;
	}
	
	// 生成数据哈希值用于去重
	private String generateDataHash(String... fields) {
		try {
			StringBuilder sb = new StringBuilder();
			for (String field : fields) {
				sb.append(field != null ? field : "").append("|");
			}
			java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
			byte[] hash = md.digest(sb.toString().getBytes("UTF-8"));
			StringBuilder hexString = new StringBuilder();
			for (byte b : hash) {
				String hex = Integer.toHexString(0xff & b);
				if (hex.length() == 1) hexString.append('0');
				hexString.append(hex);
			}
			return hexString.toString();
		} catch (Exception e) {
			return null;
		}
	}
	
	// 根据位置获取BigDecimal值
	private BigDecimal getBigDecimalField(List<String> fields, int index) {
		if(index < fields.size()) {
			String value = fields.get(index).trim();
			// 去除引号
			if(value.startsWith("\"") && value.endsWith("\"")) {
				value = value.substring(1, value.length() - 1);
			}
			// 去除货币符号和空格
			value = value.replaceAll("[$€£¥\\s]", "").trim();
			// 处理空值
			if(value.isEmpty() || value.equals("-") || value.equals("N/A")) {
				return BigDecimal.ZERO;
			}
			// 处理括号表示的负数: (10.00) = -10.00
			if(value.startsWith("(") && value.endsWith(")")) {
				value = "-" + value.substring(1, value.length() - 1);
			}
			try {
				return new BigDecimal(value);
			} catch (NumberFormatException e) {
				System.out.println("[TransactionReport] 解析数值失败: '" + value + "', 索引: " + index);
				return BigDecimal.ZERO;
			}
		}
		return BigDecimal.ZERO;
	}
	public static Locale createLocale(String languageCode, String countryCode) {
		// 将 "UK" 映射为 "GB"
		if ("UK".equalsIgnoreCase(countryCode)) {
			countryCode = "GB";
		}
		// 根据标准处理大小写：language 小写，country 大写
		return new Locale(languageCode.toLowerCase(), countryCode.toUpperCase());
	}
    public Date getDatez(String datetime, Marketplace marketPlace) {
		if (datetime == null || datetime.isEmpty()) {
			return null;
		}
		
		// 标准化日期格式：替换 a.m./p.m./am/pm 为 AM/PM
		String normalizedDate = datetime.trim()
			.replace("a.m.", "AM")
			.replace("p.m.", "PM")
			.replace("A.M.", "AM")
			.replace("P.M.", "PM")
			.replace(" am", " AM")
			.replace(" pm", " PM")
			.replaceAll("\\s+", " ");
		
		// 定义多种日期格式
		String[] patterns = {
			"MMM d, yyyy h:mm:ss a z",      // Apr 1, 2026 10:47:48 AM PDT
			"MMM d, yyyy hh:mm:ss a z",     // Apr 1, 2026 10:47:48 AM PDT (两位小时)
			"MMM dd, yyyy h:mm:ss a z",     // Apr 29, 2026 10:47:48 AM PDT
			"MMM dd, yyyy hh:mm:ss a z",    // Apr 29, 2026 10:47:48 AM PDT (两位小时)
			"MMM d, yyyy HH:mm:ss z",       // 24小时制
			"MMM dd, yyyy HH:mm:ss z",
			"dd MMM yyyy HH:mm:ss z",       // 26 Jun 2026 20:53:15 UTC (日-月-年格式)
			"d MMM yyyy HH:mm:ss z",        // 6 Jun 2026 20:53:15 UTC
			"dd MMM yyyy h:mm:ss a z",      // 26 Jun 2026 8:53:15 PM UTC
			"d MMM yyyy h:mm:ss a z",       // 6 Jun 2026 8:53:15 PM UTC
			"d MMM. yyyy HH:mm:ss z",       // 4 avr. 2026 16:34:52 UTC (法语格式，带点)
			"dd MMM. yyyy HH:mm:ss z",      // 14 avr. 2026 16:34:52 UTC
			"d MMM. yyyy h:mm:ss a z",      // 4 avr. 2026 4:34:52 PM UTC
			"dd MMM. yyyy h:mm:ss a z",     // 14 avr. 2026 4:34:52 PM UTC
			"dd.MM.yyyy HH:mm:ss z",        // 31.08.2023 22:14:55 UTC (德国格式)
			"d.MM.yyyy HH:mm:ss z",         // 1.08.2023 22:14:55 UTC
			"dd.MM.yyyy HH:mm:ss",          // 31.08.2023 22:14:55 (无时区)
			"yyyy-MM-dd'T'HH:mm:ssz",       // ISO格式
			"yyyy-MM-dd'T'HH:mm:ss'Z'"
		};
		
		// 尝试多个 Locale（支持多语言日期）
		Locale[] locales = {
			createLocale(marketPlace.getLanguage(), marketPlace.getMarket()),  // 主 Locale
			Locale.FRENCH,     // 法语
			Locale.GERMAN,     // 德语
			Locale.JAPANESE,   // 日语
			Locale.ITALIAN,    // 意大利语
			new Locale("es"),  // 西班牙语
			new Locale("nl"),  // 荷兰语
			new Locale("pl"),  // 波兰语
			new Locale("sv"),  // 瑞典语
			new Locale("tr"),  // 土耳其语
			new Locale("pt"),  // 葡萄牙语
			new Locale("ar"),  // 阿拉伯语
			Locale.ENGLISH     // 英语（默认）
		};
		
		for (Locale locale : locales) {
			for (String pattern : patterns) {
				try {
					DateTimeFormatter parseFormatter = DateTimeFormatter.ofPattern(pattern, locale);
					ZonedDateTime zdt = ZonedDateTime.parse(normalizedDate, parseFormatter);
					
					// 转换为目标格式
					DateTimeFormatter targetFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
					String target = zdt.format(targetFormatter);
					
					return GeneralUtil.getDatez(target);
				} catch (Exception e) {
					// 继续尝试下一个格式
				}
			}
		}
		
		System.out.println("[TransactionReport] 无法解析日期: " + datetime + ", 标准化后: " + normalizedDate);
		return null;
	}
	@Override
	public String myReportType() {
		return "GET_DATE_RANGE_FINANCIAL_TRANSACTION_DATA";
	}

}
