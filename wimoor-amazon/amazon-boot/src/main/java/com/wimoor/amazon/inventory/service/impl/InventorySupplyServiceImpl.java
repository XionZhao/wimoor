package com.wimoor.amazon.inventory.service.impl;

import cn.hutool.core.util.StrUtil;
import com.amazon.spapi.SellingPartnerAPIAA.LWAException;
import com.amazon.spapi.api.FbaInventoryApi;
import com.amazon.spapi.client.ApiException;
import com.amazon.spapi.model.fbainventory.*;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wimoor.amazon.auth.pojo.entity.AmazonAuthority;
import com.wimoor.amazon.auth.pojo.entity.AmzAuthApiTimelimit;
import com.wimoor.amazon.auth.pojo.entity.Marketplace;
import com.wimoor.amazon.auth.service.IAmzAuthApiTimelimitService;
import com.wimoor.amazon.auth.service.IMarketplaceService;
import com.wimoor.amazon.auth.service.impl.ApiBuildService;
import com.wimoor.amazon.inventory.mapper.AmzInventoryCountryReportMapper;
import com.wimoor.amazon.inventory.mapper.InventoryReportMapper;
import com.wimoor.amazon.inventory.mapper.InventoryReservedReportMapper;
import com.wimoor.amazon.inventory.pojo.entity.AmzInventoryCountryReport;
import com.wimoor.amazon.inventory.pojo.entity.InventoryReport;
import com.wimoor.amazon.inventory.pojo.entity.InventoryReservedReport;
import com.wimoor.amazon.inventory.pojo.vo.ProductInventoryVo;
import com.wimoor.amazon.inventory.service.IInventorySupplyService;
import com.wimoor.amazon.product.pojo.entity.ProductInfo;
import com.wimoor.amazon.product.service.IProductInOptService;
import com.wimoor.amazon.product.service.IProductInfoService;
import com.wimoor.amazon.util.AmzDateUtils;
import com.wimoor.common.GeneralUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigInteger;
import java.util.*;
import java.util.Map.Entry;

@Slf4j
@Service
public class InventorySupplyServiceImpl implements IInventorySupplyService{
	@Autowired
	ApiBuildService apiBuildService;
	@Resource
	private IProductInfoService iProductInfoService;
	@Resource
	private InventoryReportMapper inventoryReportMapper;
	@Resource
	private IProductInOptService iProductInOptService;
	@Resource
	AmzInventoryCountryReportMapper amzInventoryCountryReportMapper;
    @Resource
    InventoryReservedReportMapper inventoryReservedReportMapper;
    @Resource
    IMarketplaceService marketplaceService;
    @Resource
    IAmzAuthApiTimelimitService amzAuthApiTimelimitService;
	@Override
	public Map<String, InventorySummary> captureInventorySupplyNew(AmazonAuthority amazonAuthority, Date date) {
		// TODO Auto-generated method stub
		amazonAuthority.setUseApi("getInventorySummaries");
		FbaInventoryApi api = apiBuildService.getFbaInventoryApi(amazonAuthority);
		Marketplace market = amazonAuthority.getMarketPlace();
		Map<String,InventorySummary> result=new HashMap<String,InventorySummary>();
		try {
			Calendar c=Calendar.getInstance();
			c.setTime(date);
			GetInventorySummariesResponse response = api.getInventorySummaries("Marketplace", market.getMarketplaceid(), Arrays.asList(market.getMarketplaceid()), true, AmzDateUtils.getOffsetDateTimeUTC(c.getTime()), null,null, null);
			if(response==null) {
				return result;
			}
			GetInventorySummariesResult payload = response.getPayload();
			if(payload==null) {
				return result;
			}
			Pagination page = response.getPagination();
			String nexttoken = page!=null ? page.getNextToken() : null;
			if(payload.getInventorySummaries()!=null && payload.getInventorySummaries().size()>0) {
				Map<String,InventorySummary> itemlist=handlerFbaInventory(amazonAuthority,market,payload.getInventorySummaries());
				result.putAll(itemlist);
			}
			while(nexttoken!=null) {
				     response = api.getInventorySummaries("Marketplace", market.getMarketplaceid(),Arrays.asList(market.getMarketplaceid()), true, null, null,null, nexttoken);
				     if(response!=null) {
						 payload = response.getPayload();
						 if(response.getPagination()!=null) {
							 page = response.getPagination();
							 if(page!=null) {
								 nexttoken = page.getNextToken();
							 }else {
								 nexttoken=null;
							 }
						 }else {
							 nexttoken=null;
						 }
						 if(payload!=null && payload.getInventorySummaries()!=null) {
							 Map<String,InventorySummary> itemlist=handlerFbaInventory(amazonAuthority,market,payload.getInventorySummaries());
							 result.putAll(itemlist);
						 }
						
					 }
			}
		} catch (ApiException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (LWAException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}
	
	String handlerInventorySummariesResult(AmazonAuthority amazonAuthority,Marketplace market,GetInventorySummariesResponse response,Map<String,InventorySummary> result){
		 String nexttoken=null;
		 if(response!=null) {
			 GetInventorySummariesResult payload = response.getPayload();
			 if(payload==null) {
				 return null;
			 }
			 if(response.getPagination()!=null) {
				 Pagination page = response.getPagination();
				 if(page!=null) {
					 nexttoken = page.getNextToken();
				 }else {
					 nexttoken=null;
				 }
			 }else {
				 nexttoken=null;
			 }
			 if(payload.getInventorySummaries()!=null && payload.getInventorySummaries().size()>0) {
				 Map<String,InventorySummary> itemlist=handlerFbaInventory(amazonAuthority,market,payload.getInventorySummaries());
				 result.putAll(itemlist);
			 }
		 }
		 return nexttoken;
	}
	
	public Map<String,InventorySummary> captureInventorySupply(AmazonAuthority amazonAuthority,List<String> skulist) {
		Map<String,InventorySummary> result=new HashMap<String,InventorySummary>();
		int totalRows = skulist.size();
		if (totalRows > 50) {
			int totalPages = totalRows / 50;
			if (totalRows % 50 != 0) {
				totalPages = totalPages + 1;
			}
			for (int page = 1; page <= totalPages; page++) {
				List<String> tempskulist = GeneralUtil.getListWithLimit(skulist, page, 50);
				Map<String,InventorySummary>  pageResult=captureInventorySupplyNew(amazonAuthority, tempskulist);
				result.putAll(pageResult);
			}
		} else {
			Map<String,InventorySummary>  pageResult=captureInventorySupplyNew(amazonAuthority, skulist);
			result.putAll(pageResult);
		}
		return result;
	}
	
	@Override
	public Map<String, InventorySummary> captureInventorySupplyNew(AmazonAuthority amazonAuthority, List<String> list) {
		// TODO Auto-generated method stub
		amazonAuthority.setUseApi("getInventorySummaries");
		FbaInventoryApi api = apiBuildService.getFbaInventoryApi(amazonAuthority);
		Marketplace market = amazonAuthority.getMarketPlace();
		Map<String,InventorySummary> result=new HashMap<String,InventorySummary>();
		
		try {
			GetInventorySummariesResponse response = api.getInventorySummaries("Marketplace", market.getMarketplaceid(), Arrays.asList(market.getMarketplaceid()), true, null, list,null, null);
			 
			String nexttoken = handlerInventorySummariesResult(amazonAuthority,market,response,result);
  
			while(nexttoken!=null) {
				     response = api.getInventorySummaries("Marketplace", market.getMarketplaceid(), null, true, null, null,null, nexttoken);
				     nexttoken = handlerInventorySummariesResult(amazonAuthority,market,response,result);
			}
		} catch (ApiException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (LWAException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}
	private Map<String, InventorySummary> handlerFbaInventory(AmazonAuthority amazonAuthority, Marketplace market,
			InventorySummaries inventorySummaries) {
		// TODO Auto-generated method stub
		Map<String,InventorySummary> result=new HashMap<String,InventorySummary>();
		for(int i=0;i<inventorySummaries.size();i++) {
			InventorySummary item = inventorySummaries.get(i);
			result.put(item.getSellerSku(), item);
		}
		
		return result;
	}

	
	    public List<ProductInventoryVo> findFBA(String groupid,
										 String marketplaceid,
										 String sku,
										 String myself,
										 String shopid){
	    	HashMap<String, Object> param = new HashMap<String,Object>();
	    	param.put("groupid", groupid);
	    	param.put("marketplaceid", marketplaceid);
	    	param.put("sku", sku);
	    	param.put("myself", myself);
	    	param.put("shopid", shopid);
		return inventoryReportMapper.findFBA(param);
	}
	    
	@Override
	public InventoryReport syncInventorySupply(AmazonAuthority amazonAuthority, List<String> list) {
		if(amazonAuthority!=null) {
			 Map<String, InventorySummary> result = captureInventorySupplyNew(amazonAuthority, list);
			 return saveInventoryData(amazonAuthority, result);
		}else {
			 return null;
		}
	}
	
	private InventoryReport saveInventoryData(AmazonAuthority amazonAuthority, Map<String, InventorySummary> result) {
		InventoryReport report = null;
		for(Entry<String, InventorySummary> entry:result.entrySet()) {
			 InventorySummary inv=entry.getValue();
			 InventoryDetails invdetail = inv.getInventoryDetails();
			 if(invdetail==null) {
				 continue;
			 }
			 List<ProductInfo> infolist = iProductInfoService.selectBySku(inv.getSellerSku(),amazonAuthority.getMarketPlace().getMarketplaceid() , amazonAuthority.getId());
			 if(infolist==null||infolist.size()==0) {
				 continue;
			 }
			 ProductInfo info = infolist.get(0);
			 String mkplaceid = amazonAuthority.getMarketPlace().getRegion().equals("EU") ? "EU" : amazonAuthority.getMarketPlace().getMarketplaceid();
			 LambdaQueryWrapper<InventoryReport> query=new LambdaQueryWrapper<InventoryReport>();
			 query.eq(InventoryReport::getMarketplaceid, mkplaceid);
			 query.eq(InventoryReport::getAmazonAuthId, amazonAuthority.getId());
			 query.eq(InventoryReport::getAsin, info.getAsin());
			 query.eq(InventoryReport::getSku, inv.getSellerSku());
			 report = inventoryReportMapper.selectOne(query);
			 if(report==null) {
				 report= new InventoryReport();
				 report.setSku(inv.getSellerSku());
				 report.setMarketplaceid(mkplaceid);
				 report.setAsin(info.getAsin());
				 report.setIsnewest(true);
				 report.setAmazonAuthId(amazonAuthority.getId());
				 report.setByday(new Date());
			 }
			 report.setAfnFulfillableQuantity(invdetail.getFulfillableQuantity());
			 if(invdetail.getUnfulfillableQuantity()!=null) {
				 report.setAfnUnsellableQuantity(invdetail.getUnfulfillableQuantity().getTotalUnfulfillableQuantity());
			 }
			 report.setAfnInboundReceivingQuantity(invdetail.getInboundReceivingQuantity());
			 report.setAfnInboundShippedQuantity(invdetail.getInboundShippedQuantity());
			 report.setAfnInboundWorkingQuantity(invdetail.getInboundWorkingQuantity());
			 if(invdetail.getReservedQuantity()!=null) {
				 ReservedQuantity resqty = invdetail.getReservedQuantity();
				 report.setAfnReservedQuantity(resqty.getTotalReservedQuantity());
				 saveReservedReport(report, resqty);
			 } else {
				 // API返回null说明无预留库存，清零防止残留旧数据
				 report.setAfnReservedQuantity(0);
				 clearReservedReport(report);
			 }
			 if(invdetail.getResearchingQuantity()!=null) {
				 report.setAfnResearchingQuantity(invdetail.getResearchingQuantity().getTotalResearchingQuantity());
			 }
			 // 亚马逊仓库库存 = 可用库存 + 不可用库存 + 预留库存
			 int fulfillable = report.getAfnFulfillableQuantity() != null ? report.getAfnFulfillableQuantity() : 0;
			 int unsellable = report.getAfnUnsellableQuantity() != null ? report.getAfnUnsellableQuantity() : 0;
			 int reserved = report.getAfnReservedQuantity() != null ? report.getAfnReservedQuantity() : 0;
			 report.setAfnWarehouseQuantity(fulfillable + unsellable + reserved);
			 report.setAfnTotalQuantity(inv.getTotalQuantity());
			 report.setIsnewest(true);
			 report.setByday(new Date());
			 if(report.idIsNULL()) {
				 try {
					 inventoryReportMapper.insert(report);
				 } catch (org.springframework.dao.DuplicateKeyException e) {
					 // 并发插入导致重复，查出已有记录后更新
					 InventoryReport existing = inventoryReportMapper.selectOne(query);
					 if (existing != null) {
						 report.setId(existing.getId());
						 inventoryReportMapper.updateById(report);
					 }
				 }
			 }else {
				 inventoryReportMapper.updateById(report);
			 }
		}
		return report;
	}
	
	private void saveReservedReport(InventoryReport report, ReservedQuantity resqty) {
		 InventoryReservedReport resdetail=new InventoryReservedReport();
		 resdetail.setAmazonAuthId(report.getAmazonAuthId());
		 resdetail.setAsin(report.getAsin());
		 resdetail.setSku(report.getSku());
		 resdetail.setFnsku(report.getFnsku());
		 resdetail.setMarketplaceid(report.getMarketplaceid());
		 resdetail.setByday(new Date());
		 resdetail.setReservedQty(resqty.getTotalReservedQuantity());
		 resdetail.setReservedCustomerorders(resqty.getPendingCustomerOrderQuantity());
		 resdetail.setReservedFcProcessing(resqty.getFcProcessingQuantity());
		 resdetail.setReservedFcTransfers(resqty.getPendingTransshipmentQuantity());
		 
		 LambdaQueryWrapper<InventoryReservedReport> queryRes=new LambdaQueryWrapper<InventoryReservedReport>();
		 queryRes.eq(InventoryReservedReport::getSku, resdetail.getSku());
		 queryRes.eq(InventoryReservedReport::getAmazonAuthId, resdetail.getAmazonAuthId());
		 queryRes.eq(InventoryReservedReport::getMarketplaceid, resdetail.getMarketplaceid());
		 InventoryReservedReport oldres = inventoryReservedReportMapper.selectOne(queryRes);
		 if(oldres!=null) {
			 resdetail.setId(oldres.getId());
			 inventoryReservedReportMapper.updateById(resdetail);
		 }else {
			 try {
				 inventoryReservedReportMapper.insert(resdetail);
			 } catch (org.springframework.dao.DuplicateKeyException e) {
				 InventoryReservedReport existing = inventoryReservedReportMapper.selectOne(queryRes);
				 if (existing != null) {
					 resdetail.setId(existing.getId());
					 inventoryReservedReportMapper.updateById(resdetail);
				 }
			 }
		 }
		 report.setResdetail(resdetail);
	}
	
	private void clearReservedReport(InventoryReport report) {
		LambdaQueryWrapper<InventoryReservedReport> queryRes = new LambdaQueryWrapper<>();
		queryRes.eq(InventoryReservedReport::getSku, report.getSku());
		queryRes.eq(InventoryReservedReport::getAmazonAuthId, report.getAmazonAuthId());
		queryRes.eq(InventoryReservedReport::getMarketplaceid, report.getMarketplaceid());
		InventoryReservedReport oldres = inventoryReservedReportMapper.selectOne(queryRes);
		if (oldres != null) {
			oldres.setReservedQty(0);
			oldres.setReservedCustomerorders(0);
			oldres.setReservedFcProcessing(0);
			oldres.setReservedFcTransfers(0);
			oldres.setByday(new Date());
			inventoryReservedReportMapper.updateById(oldres);
		}
	}

	@Override
	public List<AmzInventoryCountryReport> findEUFBA(String authid, String sku) {
		LambdaQueryWrapper<AmzInventoryCountryReport> queryWrapper=new LambdaQueryWrapper<AmzInventoryCountryReport>();
		queryWrapper.eq(AmzInventoryCountryReport::getAuthid, new BigInteger(authid));
		queryWrapper.eq(AmzInventoryCountryReport::getSku, sku);
		List<AmzInventoryCountryReport> list = amzInventoryCountryReportMapper.selectList(queryWrapper);
		return list;
	}

	@Override
	public void runApi(AmazonAuthority amazonAuthority) {
		amazonAuthority.setUseApi("getInventorySummaries");
		try {
			AmzAuthApiTimelimit limit = amazonAuthority.getApiRateLimit();
			// 跨天自动重置：如果上次更新是昨天或更早，重置状态开始新一轮同步
			if (limit!=null&&limit.getLastuptime() != null && GeneralUtil.distanceOfHour(limit.getLastuptime(), new Date()) >= 5) {
				limit.setProgress(null);
				limit.setNexttoken(null);
				limit.setPages(0);
				amzAuthApiTimelimitService.update(limit);
			}
			// 已完成本轮同步，跳过
			if (limit!=null&&"done".equals(limit.getProgress())&&GeneralUtil.distanceOfHour(limit.getLastuptime(), new Date())<5) {
				return;
			}
			if (limit!=null&&!limit.apiNotRateLimit()) {
				return;
			}
			// 获取去重后的站点列表（EU只保留第一个）
			List<Marketplace> marketlist = getMarketplacesForSync(amazonAuthority);
			if (marketlist.isEmpty()) {
				return;
			}
			// 从progress字段读取当前同步到的站点索引
			int marketIndex = 0;
			if (StrUtil.isNotBlank(limit.getProgress()) && limit.getProgress().startsWith("marketIndex:")) {
				marketIndex = Integer.parseInt(limit.getProgress().substring("marketIndex:".length()));
			}
			// 所有站点已同步完成，标记done
			if (marketIndex >= marketlist.size()) {
				limit.setNexttoken(null);
				limit.setProgress("done");
				limit.setLastuptime(new Date());
				amzAuthApiTimelimitService.update(limit);
				log.info("库存同步全部完成, authId={}, totalPages={}", amazonAuthority.getId(), limit.getPages());
				return;
			}
			Marketplace market = marketlist.get(marketIndex);
			amazonAuthority.setMarketPlace(market);
			
			// 提前保存当前marketIndex，防止API异常导致进度丢失
			limit.setProgress("marketIndex:" + marketIndex);
			limit.setLastuptime(new Date());
			amzAuthApiTimelimitService.update(limit);
			
			String nextToken = StrUtil.isNotBlank(limit.getNexttoken()) ? limit.getNexttoken() : null;
			// nextToken有效期仅30秒，超过则失效
			if (nextToken != null && limit.getLastuptime() != null) {
				if (GeneralUtil.distanceOfHour(limit.getLastuptime(),new Date()) > 5) {
					nextToken = null;
				}
			}
			// 获取一页数据
			FbaInventoryApi api = apiBuildService.getFbaInventoryApi(amazonAuthority);
			GetInventorySummariesResponse response;
			if (nextToken == null) {
				// 首页：全量获取，details=true
				response = api.getInventorySummaries("Marketplace", market.getMarketplaceid(),
						Arrays.asList(market.getMarketplaceid()), true, null, null, null, null);
			} else {
				try {
					// 翻页：用保存的nextToken继续，details=true确保返回完整库存数据
				response = api.getInventorySummaries("Marketplace", market.getMarketplaceid(),
						Arrays.asList(market.getMarketplaceid()), true, null, null, null, nextToken);
				} catch (ApiException e) {
					// nextToken已过期，重新从首页开始
					log.warn("nextToken失效，重新全量同步, authId={}, marketplaceId={}",
							amazonAuthority.getId(), market.getMarketplaceid());
					response = api.getInventorySummaries("Marketplace", market.getMarketplaceid(),
							Arrays.asList(market.getMarketplaceid()), true, null, null, null, null);
				}
			}
			if (response == null || response.getPayload() == null) {
				amazonAuthority.setApiRateLimit(null, (String) null);
				return;
			}
			// 处理当前页数据
			Map<String, InventorySummary> pageResult = new HashMap<>();
			if (response.getPayload().getInventorySummaries() != null) {
				pageResult = handlerFbaInventory(amazonAuthority, market, response.getPayload().getInventorySummaries());
			}
			if (pageResult.size() > 0) {
				saveInventoryData(amazonAuthority, pageResult);
			}
			// 获取下一页token
			String newNextToken = null;
			if (response.getPagination() != null && response.getPagination().getNextToken() != null) {
				newNextToken = response.getPagination().getNextToken();
			}
			// 保存状态：用progress字段记录当前站点索引
			if (newNextToken != null) {
				// 当前站点还有下一页
				limit.setNexttoken(newNextToken);
				limit.setProgress("marketIndex:" + marketIndex);
			} else {
				// 当前站点完成，移到下一个站点
				limit.setNexttoken(null);
				limit.setProgress("marketIndex:" + (marketIndex + 1));
			}
			limit.setLastuptime(new Date());
			limit.setPages(limit.getPages() != null ? limit.getPages() + 1 : 1);
			amzAuthApiTimelimitService.update(limit);
			log.info("库存同步一页完成, authId={}, marketplaceId={}, siteProgress={}/{}, skuCount={}, hasMore={}",
					amazonAuthority.getId(), market.getMarketplaceid(),
					marketIndex + 1, marketlist.size(), pageResult.size(), newNextToken != null);
		} catch (ApiException e) {
			log.error("同步库存API异常, authId={}, marketplaceId={}", amazonAuthority.getId(),
					amazonAuthority.getMarketPlace() != null ? amazonAuthority.getMarketPlace().getMarketplaceid() : "unknown", e);
			// 记录异常到apilimit，progress字段不会被setApiRateLimit覆盖
			amazonAuthority.setApiRateLimit(e.getResponseHeaders(), e);
		} catch (LWAException e) {
			log.error("同步库存授权异常, authId={}", amazonAuthority.getId(), e);
			amazonAuthority.setApiRateLimit(e);
		} catch (Exception e) {
			log.error("同步库存失败, authId={}", amazonAuthority.getId(), e);
			amazonAuthority.setApiRateLimit(e);
		}
	}
	
	private List<Marketplace> getMarketplacesForSync(AmazonAuthority amazonAuthority) {
		List<Marketplace> allMarkets = marketplaceService.findbyauth(amazonAuthority.getId());
		List<Marketplace> result = new ArrayList<>();
		boolean euProcessed = false;
		for (Marketplace market : allMarkets) {
			if ("EU".equals(market.getRegion())) {
				if (euProcessed) {
					continue;
				}
				euProcessed = true;
			}
			result.add(market);
		}
		return result;
	}
}
