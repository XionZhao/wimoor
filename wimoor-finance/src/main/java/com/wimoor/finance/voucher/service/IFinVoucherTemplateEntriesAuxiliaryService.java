package com.wimoor.finance.voucher.service;

import com.wimoor.finance.voucher.domain.FinVoucherTemplateEntriesAuxiliary;

import java.util.List;

/**
 * 凭证模版分录辅助核算Service接口
 */
public interface IFinVoucherTemplateEntriesAuxiliaryService
{
    /**
     * 根据分录ID列表查询辅助核算（含类型和项目名称）
     *
     * @param entryIds 分录ID列表
     * @return 凭证模版分录辅助核算集合
     */
    public List<FinVoucherTemplateEntriesAuxiliary> selectByEntryIds(List<Long> entryIds);

    /**
     * 批量新增凭证模版分录辅助核算
     *
     * @param list 凭证模版分录辅助核算列表
     * @return 结果
     */
    public int insertBatch(List<FinVoucherTemplateEntriesAuxiliary> list);

    /**
     * 删除分录的所有辅助核算
     *
     * @param entryId 分录ID
     * @return 结果
     */
    public int deleteByEntryId(Long entryId);
}
