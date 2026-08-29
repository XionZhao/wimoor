package com.wimoor.finance.voucher.service.impl;

import com.wimoor.finance.voucher.domain.FinVoucherTemplate;
import com.wimoor.finance.voucher.mapper.FinVoucherTemplateMapper;
import com.wimoor.finance.voucher.service.IFinVoucherTemplateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

/**
 * 凭证模版Service业务层处理
 */
@Service
public class FinVoucherTemplateServiceImpl implements IFinVoucherTemplateService
{
    @Autowired
    private FinVoucherTemplateMapper finVoucherTemplateMapper;

    /**
     * 查询凭证模版
     *
     * @param id 凭证模版主键
     * @return 凭证模版
     */
    @Override
    public FinVoucherTemplate selectFinVoucherTemplateById(Long id)
    {
        return finVoucherTemplateMapper.selectFinVoucherTemplateById(id);
    }

    /**
     * 查询凭证模版列表
     *
     * @param finVoucherTemplate 凭证模版
     * @return 凭证模版集合
     */
    @Override
    public List<FinVoucherTemplate> selectFinVoucherTemplateList(FinVoucherTemplate finVoucherTemplate)
    {
        return finVoucherTemplateMapper.selectFinVoucherTemplateList(finVoucherTemplate);
    }

    /**
     * 新增凭证模版
     *
     * @param finVoucherTemplate 凭证模版
     * @return 结果
     */
    @Override
    public int insertFinVoucherTemplate(FinVoucherTemplate finVoucherTemplate)
    {
        finVoucherTemplate.setCreatedTime(new Date());
        return finVoucherTemplateMapper.insertFinVoucherTemplate(finVoucherTemplate);
    }

    /**
     * 修改凭证模版
     *
     * @param finVoucherTemplate 凭证模版
     * @return 结果
     */
    @Override
    public int updateFinVoucherTemplate(FinVoucherTemplate finVoucherTemplate)
    {
        return finVoucherTemplateMapper.updateFinVoucherTemplate(finVoucherTemplate);
    }

    /**
     * 删除凭证模版
     *
     * @param id 凭证模版主键
     * @return 结果
     */
    @Override
    public int deleteFinVoucherTemplateById(Long id)
    {
        return finVoucherTemplateMapper.deleteFinVoucherTemplateById(id);
    }

    /**
     * 批量删除凭证模版
     *
     * @param ids 需要删除的凭证模版主键集合
     * @return 结果
     */
    @Override
    public int deleteFinVoucherTemplateByIds(Long[] ids)
    {
        return finVoucherTemplateMapper.deleteFinVoucherTemplateByIds(ids);
    }
}