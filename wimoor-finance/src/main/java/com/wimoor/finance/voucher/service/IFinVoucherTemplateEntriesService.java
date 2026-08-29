package com.wimoor.finance.voucher.service;

import com.wimoor.finance.voucher.domain.FinVoucherTemplateEntries;

import java.util.List;

/**
 * 凭证模版分录Service接口
 */
public interface IFinVoucherTemplateEntriesService
{
    /**
     * 查询模版的所有分录（含科目信息）
     *
     * @param templateId 模版ID
     * @return 凭证模版分录集合
     */
    public List<FinVoucherTemplateEntries> selectByTemplateId(Long templateId);

    /**
     * 批量新增凭证模版分录
     *
     * @param list 凭证模版分录列表
     * @return 结果
     */
    public int insertBatch(List<FinVoucherTemplateEntries> list);

    /**
     * 删除模版的所有分录
     *
     * @param templateId 模版ID
     * @return 结果
     */
    public int deleteByTemplateId(Long templateId);
}
