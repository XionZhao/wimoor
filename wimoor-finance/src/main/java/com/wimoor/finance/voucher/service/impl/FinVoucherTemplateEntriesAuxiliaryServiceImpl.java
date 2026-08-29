package com.wimoor.finance.voucher.service.impl;

import com.wimoor.finance.voucher.domain.FinVoucherTemplateEntriesAuxiliary;
import com.wimoor.finance.voucher.mapper.FinVoucherTemplateEntriesAuxiliaryMapper;
import com.wimoor.finance.voucher.service.IFinVoucherTemplateEntriesAuxiliaryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 凭证模版分录辅助核算Service业务层处理
 */
@Service
public class FinVoucherTemplateEntriesAuxiliaryServiceImpl implements IFinVoucherTemplateEntriesAuxiliaryService
{
    @Autowired
    private FinVoucherTemplateEntriesAuxiliaryMapper finVoucherTemplateEntriesAuxiliaryMapper;

    /**
     * 根据分录ID列表查询辅助核算（含类型和项目名称）
     *
     * @param entryIds 分录ID列表
     * @return 凭证模版分录辅助核算集合
     */
    @Override
    public List<FinVoucherTemplateEntriesAuxiliary> selectByEntryIds(List<Long> entryIds)
    {
        return finVoucherTemplateEntriesAuxiliaryMapper.selectByEntryIds(entryIds);
    }

    /**
     * 批量新增凭证模版分录辅助核算
     *
     * @param list 凭证模版分录辅助核算列表
     * @return 结果
     */
    @Override
    public int insertBatch(List<FinVoucherTemplateEntriesAuxiliary> list)
    {
        return finVoucherTemplateEntriesAuxiliaryMapper.insertBatch(list);
    }

    /**
     * 删除分录的所有辅助核算
     *
     * @param entryId 分录ID
     * @return 结果
     */
    @Override
    public int deleteByEntryId(Long entryId)
    {
        return finVoucherTemplateEntriesAuxiliaryMapper.deleteByEntryId(entryId);
    }
}
