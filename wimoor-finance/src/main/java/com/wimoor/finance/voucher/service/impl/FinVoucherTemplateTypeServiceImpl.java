package com.wimoor.finance.voucher.service.impl;

import com.wimoor.finance.voucher.domain.FinVoucherTemplateType;
import com.wimoor.finance.voucher.mapper.FinVoucherTemplateTypeMapper;
import com.wimoor.finance.voucher.service.IFinVoucherTemplateTypeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

/**
 * 凭证模版分类Service业务层处理
 */
@Service
public class FinVoucherTemplateTypeServiceImpl implements IFinVoucherTemplateTypeService
{
    @Autowired
    private FinVoucherTemplateTypeMapper finVoucherTemplateTypeMapper;

    /**
     * 查询凭证模版分类
     *
     * @param id 凭证模版分类主键
     * @return 凭证模版分类
     */
    @Override
    public FinVoucherTemplateType selectFinVoucherTemplateTypeById(Long id)
    {
        return finVoucherTemplateTypeMapper.selectFinVoucherTemplateTypeById(id);
    }

    /**
     * 查询所有凭证模版分类
     *
     * @param groupid 租户ID
     * @return 凭证模版分类集合
     */
    @Override
    public List<FinVoucherTemplateType> selectFinVoucherTemplateTypeAll(String groupid)
    {
        return finVoucherTemplateTypeMapper.selectFinVoucherTemplateTypeAll(groupid);
    }

    /**
     * 查询凭证模版分类列表（分页）
     *
     * @param finVoucherTemplateType 凭证模版分类
     * @return 凭证模版分类集合
     */
    @Override
    public List<FinVoucherTemplateType> selectFinVoucherTemplateTypeList(FinVoucherTemplateType finVoucherTemplateType)
    {
        return finVoucherTemplateTypeMapper.selectFinVoucherTemplateTypeList(finVoucherTemplateType);
    }

    /**
     * 新增凭证模版分类
     *
     * @param finVoucherTemplateType 凭证模版分类
     * @return 结果
     */
    @Override
    public int insertFinVoucherTemplateType(FinVoucherTemplateType finVoucherTemplateType)
    {
        finVoucherTemplateType.setCreatedTime(new Date());
        return finVoucherTemplateTypeMapper.insertFinVoucherTemplateType(finVoucherTemplateType);
    }

    /**
     * 修改凭证模版分类
     *
     * @param finVoucherTemplateType 凭证模版分类
     * @return 结果
     */
    @Override
    public int updateFinVoucherTemplateType(FinVoucherTemplateType finVoucherTemplateType)
    {
        return finVoucherTemplateTypeMapper.updateFinVoucherTemplateType(finVoucherTemplateType);
    }

    /**
     * 删除凭证模版分类
     *
     * @param id 凭证模版分类主键
     * @return 结果
     */
    @Override
    public int deleteFinVoucherTemplateTypeById(Long id)
    {
        return finVoucherTemplateTypeMapper.deleteFinVoucherTemplateTypeById(id);
    }

    /**
     * 批量删除凭证模版分类
     *
     * @param ids 需要删除的凭证模版分类主键集合
     * @return 结果
     */
    @Override
    public int deleteFinVoucherTemplateTypeByIds(Long[] ids)
    {
        return finVoucherTemplateTypeMapper.deleteFinVoucherTemplateTypeByIds(ids);
    }
}