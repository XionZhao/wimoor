package com.wimoor.finance.voucher.mapper;

import com.wimoor.finance.voucher.domain.FinVoucherTemplateEntries;

import java.util.List;

/**
 * 凭证模版分录Mapper接口
 */
public interface FinVoucherTemplateEntriesMapper
{
    /**
     * 查询凭证模版分录
     *
     * @param entryId 凭证模版分录主键
     * @return 凭证模版分录
     */
    public FinVoucherTemplateEntries selectFinVoucherTemplateEntriesById(Long entryId);

    /**
     * 新增凭证模版分录
     *
     * @param finVoucherTemplateEntries 凭证模版分录
     * @return 结果
     */
    public int insertFinVoucherTemplateEntries(FinVoucherTemplateEntries finVoucherTemplateEntries);

    /**
     * 修改凭证模版分录
     *
     * @param finVoucherTemplateEntries 凭证模版分录
     * @return 结果
     */
    public int updateFinVoucherTemplateEntries(FinVoucherTemplateEntries finVoucherTemplateEntries);

    /**
     * 查询模版的所有分录（含科目信息）
     *
     * @param templateId 模版ID
     * @return 凭证模版分录集合
     */
    public List<FinVoucherTemplateEntries> selectByTemplateId(Long templateId);

    /**
     * 删除凭证模版分录
     *
     * @param entryId 凭证模版分录主键
     * @return 结果
     */
    public int deleteFinVoucherTemplateEntriesById(Long entryId);

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