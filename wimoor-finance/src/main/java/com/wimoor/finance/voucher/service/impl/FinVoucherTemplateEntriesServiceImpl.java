package com.wimoor.finance.voucher.service.impl;

import com.wimoor.finance.voucher.domain.FinVoucherTemplateEntries;
import com.wimoor.finance.voucher.mapper.FinVoucherTemplateEntriesMapper;
import com.wimoor.finance.voucher.service.IFinVoucherTemplateEntriesService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 凭证模版分录Service业务层处理
 */
@Service
public class FinVoucherTemplateEntriesServiceImpl implements IFinVoucherTemplateEntriesService
{
    @Autowired
    private FinVoucherTemplateEntriesMapper finVoucherTemplateEntriesMapper;

    /**
     * 查询模版的所有分录（含科目信息）
     *
     * @param templateId 模版ID
     * @return 凭证模版分录集合
     */
    @Override
    public List<FinVoucherTemplateEntries> selectByTemplateId(Long templateId)
    {
        return finVoucherTemplateEntriesMapper.selectByTemplateId(templateId);
    }

    /**
     * 批量新增凭证模版分录
     *
     * @param list 凭证模版分录列表
     * @return 结果
     */
    @Override
    public int insertBatch(List<FinVoucherTemplateEntries> list)
    {
        return finVoucherTemplateEntriesMapper.insertBatch(list);
    }

    /**
     * 删除模版的所有分录
     *
     * @param templateId 模版ID
     * @return 结果
     */
    @Override
    public int deleteByTemplateId(Long templateId)
    {
        return finVoucherTemplateEntriesMapper.deleteByTemplateId(templateId);
    }
}
