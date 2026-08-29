package com.wimoor.finance.voucher.service;

import com.wimoor.finance.voucher.domain.FinVoucherTemplateType;

import java.util.List;

/**
 * 凭证模版分类Service接口
 */
public interface IFinVoucherTemplateTypeService
{
    /**
     * 查询凭证模版分类
     *
     * @param id 凭证模版分类主键
     * @return 凭证模版分类
     */
    public FinVoucherTemplateType selectFinVoucherTemplateTypeById(Long id);

    /**
     * 查询所有凭证模版分类
     *
     * @param groupid 租户ID
     * @return 凭证模版分类集合
     */
    public List<FinVoucherTemplateType> selectFinVoucherTemplateTypeAll(String groupid);

    /**
     * 查询凭证模版分类列表（分页）
     *
     * @param finVoucherTemplateType 凭证模版分类
     * @return 凭证模版分类集合
     */
    public List<FinVoucherTemplateType> selectFinVoucherTemplateTypeList(FinVoucherTemplateType finVoucherTemplateType);

    /**
     * 新增凭证模版分类
     *
     * @param finVoucherTemplateType 凭证模版分类
     * @return 结果
     */
    public int insertFinVoucherTemplateType(FinVoucherTemplateType finVoucherTemplateType);

    /**
     * 修改凭证模版分类
     *
     * @param finVoucherTemplateType 凭证模版分类
     * @return 结果
     */
    public int updateFinVoucherTemplateType(FinVoucherTemplateType finVoucherTemplateType);

    /**
     * 删除凭证模版分类
     *
     * @param id 凭证模版分类主键
     * @return 结果
     */
    public int deleteFinVoucherTemplateTypeById(Long id);

    /**
     * 批量删除凭证模版分类
     *
     * @param ids 需要删除的凭证模版分类主键集合
     * @return 结果
     */
    public int deleteFinVoucherTemplateTypeByIds(Long[] ids);
}