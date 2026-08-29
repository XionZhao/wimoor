package com.wimoor.finance.voucher.service.impl;

import com.wimoor.finance.voucher.domain.FinVoucherSummary;
import com.wimoor.finance.voucher.mapper.FinVoucherSummaryMapper;
import com.wimoor.finance.voucher.service.IFinVoucherSummaryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

/**
 * 凭证摘要Service业务层处理
 */
@Service
public class FinVoucherSummaryServiceImpl implements IFinVoucherSummaryService
{
    @Autowired
    private FinVoucherSummaryMapper finVoucherSummaryMapper;

    /**
     * 查询凭证摘要
     *
     * @param id 凭证摘要主键
     * @return 凭证摘要
     */
    @Override
    public FinVoucherSummary selectFinVoucherSummaryById(Long id)
    {
        return finVoucherSummaryMapper.selectFinVoucherSummaryById(id);
    }

    /**
     * 查询凭证摘要列表
     *
     * @param finVoucherSummary 凭证摘要
     * @return 凭证摘要集合
     */
    @Override
    public List<FinVoucherSummary> selectFinVoucherSummaryList(FinVoucherSummary finVoucherSummary)
    {
        return finVoucherSummaryMapper.selectFinVoucherSummaryList(finVoucherSummary);
    }

    /**
     * 新增凭证摘要
     *
     * @param finVoucherSummary 凭证摘要
     * @return 结果
     */
    @Override
    public int insertFinVoucherSummary(FinVoucherSummary finVoucherSummary)
    {
        finVoucherSummary.setCreatedTime(new Date());
        return finVoucherSummaryMapper.insertFinVoucherSummary(finVoucherSummary);
    }

    /**
     * 修改凭证摘要
     *
     * @param finVoucherSummary 凭证摘要
     * @return 结果
     */
    @Override
    public int updateFinVoucherSummary(FinVoucherSummary finVoucherSummary)
    {
        return finVoucherSummaryMapper.updateFinVoucherSummary(finVoucherSummary);
    }

    /**
     * 批量删除凭证摘要
     *
     * @param ids 需要删除的凭证摘要主键集合
     * @return 结果
     */
    @Override
    public int deleteFinVoucherSummaryByIds(Long[] ids)
    {
        return finVoucherSummaryMapper.deleteFinVoucherSummaryByIds(ids);
    }

    /**
     * 删除凭证摘要信息
     *
     * @param id 凭证摘要主键
     * @return 结果
     */
    @Override
    public int deleteFinVoucherSummaryById(Long id)
    {
        return finVoucherSummaryMapper.deleteFinVoucherSummaryById(id);
    }
}
