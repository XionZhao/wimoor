package com.wimoor.finance.setting.service.impl;

import com.wimoor.finance.setting.domain.FinMappingErpInventory;
import com.wimoor.finance.setting.mapper.FinMappingErpInventoryMapper;
import com.wimoor.finance.setting.service.IFinMappingErpInventoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 存货科目映射规则Service业务层处理
 * 
 * @author wimoor
 * @date 2025-07-09
 */
@Service
public class FinMappingErpInventoryServiceImpl implements IFinMappingErpInventoryService 
{
    private static final Logger log = LoggerFactory.getLogger(FinMappingErpInventoryServiceImpl.class);

    @Autowired
    private FinMappingErpInventoryMapper finMappingErpInventoryMapper;

    /**
     * 查询存货映射规则列表
     */
    @Override
    public List<FinMappingErpInventory> selectFinMappingErpInventoryList(FinMappingErpInventory finMappingErpInventory)
    {
        return finMappingErpInventoryMapper.selectFinMappingErpInventoryList(finMappingErpInventory);
    }

    /**
     * 查询存货映射规则详情
     */
    @Override
    public FinMappingErpInventory selectFinMappingErpInventoryById(Long id)
    {
        return finMappingErpInventoryMapper.selectFinMappingErpInventoryById(id);
    }

    /**
     * 新增存货映射规则
     */
    @Override
    public int insertFinMappingErpInventory(FinMappingErpInventory finMappingErpInventory)
    {
        return finMappingErpInventoryMapper.insertFinMappingErpInventory(finMappingErpInventory);
    }

    /**
     * 修改存货映射规则
     */
    @Override
    public int updateFinMappingErpInventory(FinMappingErpInventory finMappingErpInventory)
    {
        return finMappingErpInventoryMapper.updateFinMappingErpInventory(finMappingErpInventory);
    }

    /**
     * 删除存货映射规则
     */
    @Override
    public int deleteFinMappingErpInventoryById(Long id)
    {
        return finMappingErpInventoryMapper.deleteFinMappingErpInventoryById(id);
    }

    /**
     * 批量删除存货映射规则
     */
    @Override
    public int deleteFinMappingErpInventoryByIds(Long[] ids)
    {
        return finMappingErpInventoryMapper.deleteFinMappingErpInventoryByIds(ids);
    }

    /**
     * 批量新增存货映射规则
     */
    @Override
    public int batchInsertFinMappingErpInventory(List<FinMappingErpInventory> list)
    {
        int rows = 0;
        for (FinMappingErpInventory item : list) {
            rows += finMappingErpInventoryMapper.insertFinMappingErpInventory(item);
        }
        return rows;
    }
}
