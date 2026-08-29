package com.wimoor.amazon.inventory.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.wimoor.amazon.inventory.pojo.entity.AmzInventoryPlanning;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author wimoor team
 * @since 2022-12-07
 */
public interface IAmzInventoryPlanningService extends IService<AmzInventoryPlanning> {
	
	/**
	 * 批量查询库存规划数据
	 * @param amazonauthidSet
	 * @param skuSet
	 * @param countrySet
	 * @return key: amazonauthid_sku_country, value: AmzInventoryPlanning
	 */
	public Map<String, AmzInventoryPlanning> listBatch(Set<String> amazonauthidSet, Set<String> skuSet, Set<String> countrySet);
	
}
