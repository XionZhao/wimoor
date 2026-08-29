package com.wimoor.amazon.inventory.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wimoor.amazon.inventory.mapper.AmzInventoryPlanningMapper;
import com.wimoor.amazon.inventory.pojo.entity.AmzInventoryPlanning;
import com.wimoor.amazon.inventory.service.IAmzInventoryPlanningService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author wimoor team
 * @since 2022-12-07
 */
@Service
public class AmzInventoryPlanningServiceImpl extends ServiceImpl<AmzInventoryPlanningMapper, AmzInventoryPlanning> implements IAmzInventoryPlanningService {

	@Override
	public Map<String, AmzInventoryPlanning> listBatch(Set<String> amazonauthidSet, Set<String> skuSet, Set<String> countrySet) {
		Map<String, AmzInventoryPlanning> result = new HashMap<>();
		if (amazonauthidSet == null || amazonauthidSet.isEmpty() || skuSet == null || skuSet.isEmpty()) {
			return result;
		}
		LambdaQueryWrapper<AmzInventoryPlanning> query = new LambdaQueryWrapper<>();
		query.in(AmzInventoryPlanning::getAmazonauthid, amazonauthidSet);
		query.in(AmzInventoryPlanning::getSku, skuSet);
		query.and(wrapper -> {
			wrapper.eq(AmzInventoryPlanning::getCondition, "new")
				.or().eq(AmzInventoryPlanning::getCondition, "New")
				.or().eq(AmzInventoryPlanning::getCondition, "");
		});
		if (countrySet != null && !countrySet.isEmpty()) {
			query.in(AmzInventoryPlanning::getCountrycode, countrySet);
		}
		List<AmzInventoryPlanning> list = this.list(query);
		for (AmzInventoryPlanning item : list) {
			String key = item.getAmazonauthid() + "_" + item.getSku() + "_" + item.getCountrycode();
			// 优先保留 New 状态的数据
			if (!result.containsKey(key) || "New".equals(item.getCondition())) {
				result.put(key, item);
			}
		}
		return result;
	}
	
}
