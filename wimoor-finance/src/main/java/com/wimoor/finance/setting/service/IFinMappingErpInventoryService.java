package com.wimoor.finance.setting.service;

import com.wimoor.finance.setting.domain.FinMappingErpInventory;

import java.util.List;

/**
 * 存货科目映射规则Service接口
 * 
 * @author wimoor
 * @date 2025-07-09
 */
public interface IFinMappingErpInventoryService 
{
    /**
     * 查询存货映射规则列表
     */
    List<FinMappingErpInventory> selectFinMappingErpInventoryList(FinMappingErpInventory finMappingErpInventory);

    /**
     * 查询存货映射规则详情
     */
    FinMappingErpInventory selectFinMappingErpInventoryById(Long id);

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
    int deleteFinMappingErpInventoryById(Long id);

    /**
     * 批量删除存货映射规则
     */
    int deleteFinMappingErpInventoryByIds(Long[] ids);

    /**
     * 批量新增存货映射规则
     */
    int batchInsertFinMappingErpInventory(List<FinMappingErpInventory> list);
}
