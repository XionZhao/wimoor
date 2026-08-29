package com.wimoor.finance.setting.mapper;

import com.wimoor.finance.setting.domain.FinMappingErpInventory;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 存货科目映射规则Mapper接口
 * 
 * @author wimoor
 * @date 2025-07-09
 */
public interface FinMappingErpInventoryMapper 
{
    /**
     * 查询存货映射规则列表
     */
    List<FinMappingErpInventory> selectFinMappingErpInventoryList(FinMappingErpInventory finMappingErpInventory);

    /**
     * 查询存货映射规则详情
     */
    FinMappingErpInventory selectFinMappingErpInventoryById(@Param("id") Long id);

    /**
     * 新增存货映射规则
     */
    int insertFinMappingErpInventory(FinMappingErpInventory finMappingErpInventory);

    /**
     * 修改存货映射规则
     */
    int updateFinMappingErpInventory(FinMappingErpInventory finMappingErpInventory);

    /**
     * 删除存货映射规则
     */
    int deleteFinMappingErpInventoryById(@Param("id") Long id);

    /**
     * 批量删除存货映射规则
     */
    int deleteFinMappingErpInventoryByIds(@Param("ids") Long[] ids);
}
