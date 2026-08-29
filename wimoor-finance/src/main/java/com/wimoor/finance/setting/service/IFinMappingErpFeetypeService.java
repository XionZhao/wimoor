package com.wimoor.finance.setting.service;

import com.wimoor.finance.setting.domain.FinMappingErpFeetype;

import java.util.List;

/**
 * 费用类型级别映射Service接口
 *
 * @author wimoor
 * @date 2025-07-07
 */
public interface IFinMappingErpFeetypeService
{
    /**
     * 查询映射列表
     */
    List<FinMappingErpFeetype> selectFinMappingErpFeetypeList(FinMappingErpFeetype finMappingErpFeetype);

    /**
     * 查询映射详情
     */
    FinMappingErpFeetype selectFinMappingErpFeetypeById(Long id);

    /**
     * 新增映射
     */
    int insertFinMappingErpFeetype(FinMappingErpFeetype finMappingErpFeetype);

    /**
     * 修改映射
     */
    int updateFinMappingErpFeetype(FinMappingErpFeetype finMappingErpFeetype);

    /**
     * 删除映射
     */
    int deleteFinMappingErpFeetypeById(Long id);

    /**
     * 批量删除映射
     */
    int deleteFinMappingErpFeetypeByIds(Long[] ids);

    /**
     * 批量新增映射
     */
    int batchInsertFinMappingErpFeetype(List<FinMappingErpFeetype> list);
}
