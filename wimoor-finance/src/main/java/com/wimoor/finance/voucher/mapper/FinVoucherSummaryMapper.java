package com.wimoor.finance.voucher.mapper;

import com.wimoor.finance.voucher.domain.FinVoucherSummary;

import java.util.List;

/**
 * 凭证摘要Mapper接口
 */
public interface FinVoucherSummaryMapper
{
    /**
     * 查询凭证摘要
     *
     * @param id 凭证摘要主键
     * @return 凭证摘要
     */
    public FinVoucherSummary selectFinVoucherSummaryById(Long id);

    /**
     * 查询凭证摘要列表
     *
     * @param finVoucherSummary 凭证摘要
     * @return 凭证摘要集合
     */
    public List<FinVoucherSummary> selectFinVoucherSummaryList(FinVoucherSummary finVoucherSummary);

    /**
     * 新增凭证摘要
     *
     * @param finVoucherSummary 凭证摘要
     * @return 结果
     */
    public int insertFinVoucherSummary(FinVoucherSummary finVoucherSummary);

    /**
     * 修改凭证摘要
     *
     * @param finVoucherSummary 凭证摘要
     * @return 结果
     */
    public int updateFinVoucherSummary(FinVoucherSummary finVoucherSummary);

    /**
     * 删除凭证摘要
     *
     * @param id 凭证摘要主键
     * @return 结果
     */
    public int deleteFinVoucherSummaryById(Long id);

    /**
     * 批量删除凭证摘要
     *
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteFinVoucherSummaryByIds(Long[] ids);
}
