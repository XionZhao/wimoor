package com.wimoor.finance.setting.mapper;

import com.wimoor.finance.setting.domain.FinMappingVouchers;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 映射凭证关联Mapper接口
 * 
 * @author wimoor
 * @date 2025-07-09
 */
public interface FinMappingVouchersMapper 
{
    /**
     * 查询映射凭证关联列表
     */
    List<FinMappingVouchers> selectFinMappingVouchersList(FinMappingVouchers finMappingVouchers);

    /**
     * 查询映射凭证关联详情
     */
    FinMappingVouchers selectFinMappingVouchersById(@Param("id") Long id);

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
    int deleteFinMappingVouchersById(@Param("id") Long id);

    /**
     * 批量删除映射凭证关联
     */
    int deleteFinMappingVouchersByIds(@Param("ids") Long[] ids);

    /**
     * 按凭证ID删除映射凭证关联
     */
    int deleteByVoucherId(@Param("voucherId") Long voucherId);

    /**
     * 按凭证ID列表批量删除映射凭证关联
     */
    int deleteByVoucherIds(@Param("voucherIds") List<Long> voucherIds);
}