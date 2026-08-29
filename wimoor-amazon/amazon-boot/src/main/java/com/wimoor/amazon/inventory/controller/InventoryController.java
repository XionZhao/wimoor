package com.wimoor.amazon.inventory.controller;

import cn.hutool.core.util.StrUtil;
import com.amazon.spapi.model.fbainventory.InventorySummary;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.wimoor.amazon.auth.pojo.entity.AmazonAuthority;
import com.wimoor.amazon.auth.pojo.entity.AmzAuthApiTimelimit;
import com.wimoor.amazon.auth.pojo.entity.Marketplace;
import com.wimoor.amazon.auth.service.IAmazonAuthorityService;
import com.wimoor.amazon.auth.service.IAmazonGroupService;
import com.wimoor.amazon.auth.service.IAmzAuthApiTimelimitService;
import com.wimoor.amazon.auth.service.IMarketplaceService;
import com.wimoor.amazon.feed.mapper.AmzSubmitFeedQueueMapper;
import com.wimoor.amazon.feed.pojo.entity.AmzSubmitFeedQueue;
import com.wimoor.amazon.feed.service.ISubmitfeedService;
import com.wimoor.amazon.inventory.pojo.dto.InventorySizeDTO;
import com.wimoor.amazon.inventory.pojo.entity.AmzInventoryCountryReport;
import com.wimoor.amazon.inventory.pojo.entity.AmzInventoryPlanning;
import com.wimoor.amazon.inventory.pojo.entity.InventoryReport;
import com.wimoor.amazon.inventory.pojo.vo.ProductInventoryVo;
import com.wimoor.amazon.inventory.service.IAmzInventoryPlanningService;
import com.wimoor.amazon.inventory.service.IInventorySupplyService;
import com.wimoor.amazon.product.service.IProductInOptService;
import com.wimoor.common.mvc.BizException;
import com.wimoor.common.result.Result;
import com.wimoor.common.user.UserInfo;
import com.wimoor.common.user.UserInfoContext;
import com.wimoor.common.user.UserLimitDataType;
import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Api(tags = "库存接口")
@Slf4j
@RestController
@RequestMapping("/api/v0/inventry")
public class InventoryController {
	@Autowired
	IMarketplaceService marketplaceService;
	@Autowired
	IAmazonAuthorityService amazonAuthorityService;
	@Autowired
	IInventorySupplyService inventorySupplyService;
	@Autowired
	IProductInOptService productInOptService;
	@Autowired
	ISubmitfeedService submitfeedService;
	@Autowired
	AmzSubmitFeedQueueMapper amzSubmitFeedQueueMapper;
	@Autowired
	IAmzInventoryPlanningService iAmzInventoryPlanningService;
	@Autowired
	IAmzAuthApiTimelimitService amzAuthApiTimelimitService;
	@Autowired
	IAmazonGroupService amazonGroupService;
	
	private volatile boolean inventorySyncRunning = false;
	private final Map<String, Map<String, Object>> inventorySyncTiming = new ConcurrentHashMap<>();
	@GetMapping("/getInventorySupply")
	public Result<Map<String, InventorySummary>> getInventorySupplyAction(String  groupid,String marketplaceid ,String skuStr) {
		 
		List<String> list = null;
		if (StrUtil.isNotEmpty(skuStr)) {
			String[] skuarray = skuStr.split(",");
			if (skuarray.length > 0) {
				list = Arrays.asList(skuarray);
			}
		}
		Map<String, InventorySummary> result = null;
		if (list != null && list.size() > 0) {
			AmazonAuthority amazonAuthority = amazonAuthorityService.selectByGroupAndMarket(groupid, marketplaceid);
			Marketplace marketplace = marketplaceService.selectByPKey(marketplaceid);
			amazonAuthority.setMarketPlace(marketplace);
			result = inventorySupplyService.captureInventorySupplyNew(amazonAuthority, list);
		}
		return Result.success(result);
	}
	
	@GetMapping("/taskInventoryData")
	public Result<?> taskInventoryDataAction() {
		amazonAuthorityService.executTask(inventorySupplyService);
		return Result.success();
	}
	

	
	@GetMapping("/taskInventoryDataByAuth")
	public Result<?> taskInventoryDataByAuthAction(String authid) {
		if (StrUtil.isBlank(authid)) {
			throw new BizException("authid不能为空！");
		}
		AmazonAuthority auth = amazonAuthorityService.getById(authid);
		if (auth == null) {
			throw new BizException("店铺授权不存在！");
		}
		inventorySupplyService.runApi(auth);
		return Result.success();
	}
	
	@GetMapping("/inventorySyncStatus")
	public Result<Map<String, Object>> inventorySyncStatusAction(String authid) {
		if (StrUtil.isBlank(authid)) {
			throw new BizException("authid不能为空！");
		}
		Map<String, Object> result = new HashMap<>();
		AmzAuthApiTimelimit limit = amzAuthApiTimelimitService.getApiLimit(authid, "getInventorySummaries");
		if (limit != null) {
			result.put("nextToken", limit.getNexttoken());
			result.put("pages", limit.getPages());
			result.put("startTime", limit.getStartTime());
			result.put("endTime", limit.getEndTime());
			result.put("lastuptime", limit.getLastuptime());
			result.put("log", limit.getLog());
			result.put("hasPendingSync", StrUtil.isNotBlank(limit.getNexttoken()));
		} else {
			result.put("nextToken", null);
			result.put("pages", 0);
			result.put("hasPendingSync", false);
		}
		return Result.success(result);
	}
	
	@GetMapping("/resetInventorySync")
	public Result<?> resetInventorySyncAction(String authid) {
		if (StrUtil.isBlank(authid)) {
			throw new BizException("authid不能为空！");
		}
		AmzAuthApiTimelimit limit = amzAuthApiTimelimitService.getApiLimit(authid, "getInventorySummaries");
		if (limit != null) {
			limit.setNexttoken(null);
			limit.setPages(0);
			limit.setStartTime(null);
			limit.setEndTime(null);
			amzAuthApiTimelimitService.update(limit);
		}
		return Result.success();
	}

	@GetMapping("/syncInventorySupply")
	public Result<InventoryReport> syncInventorySupplyAction(String  groupid,String marketplaceid ,String skus) {
		List<String> list = null;
		if (StrUtil.isNotEmpty(skus)) {
			String[] skuarray = skus.split(",");
			if (skuarray.length > 0) {
				list = Arrays.asList(skuarray);
			}
		}
		InventoryReport result = null;
		if(marketplaceid!=null&&marketplaceid.equals("EU")) {
			List<Map<String, Object>> markets = marketplaceService.findEUMarketPriorityByGroup(groupid);
			if(markets!=null&&markets.size()>0) {
				Map<String, Object> market = markets.get(0);
				marketplaceid=market!=null?market.get("marketplaceId").toString():null;
			}
		}
		if (list != null && list.size() > 0) {
			AmazonAuthority amazonAuthority = amazonAuthorityService.selectByGroupAndMarket(groupid, marketplaceid);
			if(amazonAuthority!=null) {
				Marketplace marketplace = marketplaceService.selectByPKey(marketplaceid);
				amazonAuthority.setMarketPlace(marketplace);
				result=inventorySupplyService.syncInventorySupply(amazonAuthority, list);
			}else {
				throw new BizException("店铺参数异常！");
			}
		}
		return Result.success(result);
	}
	
	@GetMapping("/findFBA")
	public Result<Map<String,Object>> findFBAAction(String  groupid,String marketplaceid ,String sku) {
    	UserInfo userinfo = UserInfoContext.get();
    	Marketplace market = marketplaceService.findMapByMarketplaceId().get(marketplaceid);
    	String region=market.getRegion();
    	Map<String,Object> result=new HashMap<String,Object>();
    	AmazonAuthority auth = amazonAuthorityService.selectByGroupAndMarket(groupid, marketplaceid);
    	LambdaQueryWrapper<AmzInventoryPlanning> query=new LambdaQueryWrapper<AmzInventoryPlanning>();
		query.eq(AmzInventoryPlanning::getAmazonauthid,auth.getId());
		query.eq(AmzInventoryPlanning::getSku,sku);
		query.and(wrapper -> {
			wrapper.eq(AmzInventoryPlanning::getCondition,"new")
			.or().eq(AmzInventoryPlanning::getCondition,"New");
		});
		query.eq(AmzInventoryPlanning::getCountrycode,market.getMarket());
		AmzInventoryPlanning plandata = iAmzInventoryPlanningService.getOne(query);
		result.put("invplandata", plandata);
    	if(region.equals("EU")) {
    		marketplaceid="EU";
    	}
    	List<ProductInventoryVo> list = inventorySupplyService.findFBA(groupid, marketplaceid, sku, null, userinfo.getCompanyid());
    	if(list!=null&&list.size()>0) {
    		result.put("fbainv",list.get(0));
    	}
		return Result.success(result);
	}
	
	@PostMapping("/getSizePro")
	public Result<IPage<Map<String, Object>>> getSizeProAction(@RequestBody InventorySizeDTO dto) {
		UserInfo userinfo = UserInfoContext.get();
		Map<String, Object> param = new HashMap<String, Object>();
		String skuname = dto.getSearch();
		if (StrUtil.isNotEmpty(skuname)) {
			param.put("skuname", "%" + skuname + "%");
		} else {
			param.put("skuname", null);
		}
		param.put("shopid", userinfo.getCompanyid());
		String groupid = dto.getGroupid();
		param.put("groupid", groupid);
		String marketplaceid = dto.getMarketplaceid();
		param.put("marketplaceid", marketplaceid);
		String country = dto.getCountry();
		param.put("country", country);
		String isgtself = dto.getIsgtself();
		param.put("isgtself", isgtself);
		String sizetype = dto.getSizetype();
		param.put("sizetype", sizetype);
		String searchtype = dto.getSearchtype();
		param.put("searchtype", searchtype);
		if(userinfo.isLimit(UserLimitDataType.operations)) {
			param.put("owner",userinfo.getId());

		} 
		try {
			List<Map<String, Object>> list = productInOptService.findMaterialSizeByCondition(param);
			IPage<Map<String, Object>> pagelist= dto.getListPage(list);
			return Result.success(pagelist);
		}catch(Exception e) {
			e.printStackTrace();
		}
		return Result.success();
	}
	
	@GetMapping("/findEUFBA")
	public Result<List<AmzInventoryCountryReport>> findEUFBAAction(String  authid ,String sku) {
		if(StrUtil.isNotEmpty(authid)) {
			return Result.success(inventorySupplyService.findEUFBA(authid, sku));
		}else {
			return Result.success(null);
		}
	}
	
	@GetMapping("/callqueue")
	public void findEUFBAAction2(String authid,String marketplaceid,String queueid) {
		AmazonAuthority auth = amazonAuthorityService.getById(authid);
		Marketplace marketplace = marketplaceService.getById(marketplaceid);
		AmzSubmitFeedQueue queue = amzSubmitFeedQueueMapper.selectById(queueid);
		submitfeedService.callSubmitFeed(auth, marketplace, queue);
	}
	
	
}
