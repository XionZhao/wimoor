package com.wimoor.finance.setting.service;

import com.wimoor.finance.setting.domain.FinMappingVouchers;

import java.util.List;

/**
 * 映射凭证关联Service接口
 * 
 * @author wimoor
 * @date 2025-07-09
 */
public interface IFinMappingVouchersService 
{
    /**
     * 查询映射凭证关联列表
     */
    List<FinMappingVouchers> selectFinMappingVouchersList(FinMappingVouchers finMappingVouchers);

    /**
     * 查询映射凭证关联详情
     */
    FinMappingVouchers selectFinMappingVouchersById(Long id);

    /**
     * 新增映射凭证关联
     */
    int insertFinMappingVouchers(FinMappingVouchers finMappingVouchers);

    /**
     * 修改映射凭证关联
     */
    int updateFinMappingVouchers(FinMappingVouchers finMappingVouchers);

    /**
     * 删除映射凭证关联
     */
    int deleteFinMappingVouchersById(Long id);

    /**
     * 批量删除映射凭证关联
     */
    int deleteFinMappingVouchersByIds(Long[] ids);
}