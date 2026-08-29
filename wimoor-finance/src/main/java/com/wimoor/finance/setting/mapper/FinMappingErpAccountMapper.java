package com.wimoor.finance.setting.mapper;

import com.wimoor.finance.setting.domain.FinMappingErpAccount;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 费用类型-科目映射规则Mapper接口
 * 
 * @author wimoor
 * @date 2025-07-07
 */
public interface FinMappingErpAccountMapper 
{
    /**
     * 查询映射规则列表
     */
    List<FinMappingErpAccount> selectFinMappingErpAccountList(FinMappingErpAccount finMappingErpAccount);

    /**
     * 查询映射规则详情
     */
    FinMappingErpAccount selectFinMappingErpAccountById(@Param("id") Long id);

    /**
     * 新增映射规则
     */
    int insertFinMappingErpAccount(FinMappingErpAccount finMappingErpAccount);

    /**
     * 修改映射规则
     */
    int updateFinMappingErpAccount(FinMappingErpAccount finMappingErpAccount);

    /**
     * 删除映射规则
     */
    int deleteFinMappingErpAccountById(@Param("id") Long id);

    /**
     * 批量删除映射规则
     */
    int deleteFinMappingErpAccountByIds(@Param("ids") Long[] ids);
}
