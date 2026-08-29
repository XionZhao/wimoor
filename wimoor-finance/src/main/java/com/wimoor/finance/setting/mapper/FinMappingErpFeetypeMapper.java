package com.wimoor.finance.setting.mapper;

import com.wimoor.finance.setting.domain.FinMappingErpFeetype;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 费用类型级别映射Mapper接口
 *
 * @author wimoor
 * @date 2025-07-07
 */
public interface FinMappingErpFeetypeMapper
{
    /**
     * 查询映射列表
     */
    List<FinMappingErpFeetype> selectFinMappingErpFeetypeList(FinMappingErpFeetype finMappingErpFeetype);

    /**
     * 查询映射详情
     */
    FinMappingErpFeetype selectFinMappingErpFeetypeById(@Param("id") Long id);

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
    int deleteFinMappingErpFeetypeById(@Param("id") Long id);

    /**
     * 批量删除映射
     */
    int deleteFinMappingErpFeetypeByIds(@Param("ids") Long[] ids);
}
