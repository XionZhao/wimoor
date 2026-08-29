package com.wimoor.finance.voucher.mapper;

import com.wimoor.finance.voucher.domain.FinVoucherTemplate;

import java.util.List;

/**
 * 凭证模版Mapper接口
 */
public interface FinVoucherTemplateMapper
{
    /**
     * 查询凭证模版
     *
     * @param id 凭证模版主键
     * @return 凭证模版
     */
    public FinVoucherTemplate selectFinVoucherTemplateById(Long id);

    /**
     * 查询凭证模版列表
     *
     * @param finVoucherTemplate 凭证模版
     * @return 凭证模版集合
     */
    public List<FinVoucherTemplate> selectFinVoucherTemplateList(FinVoucherTemplate finVoucherTemplate);

    /**
     * 新增凭证模版
     *
     * @param finVoucherTemplate 凭证模版
     * @return 结果
     */
    public int insertFinVoucherTemplate(FinVoucherTemplate finVoucherTemplate);

    /**
     * 修改凭证模版
     *
     * @param finVoucherTemplate 凭证模版
     * @return 结果
     */
    public int updateFinVoucherTemplate(FinVoucherTemplate finVoucherTemplate);

    /**
     * 删除凭证模版
     *
     * @param id 凭证模版主键
     * @return 结果
     */
    public int deleteFinVoucherTemplateById(Long id);

    /**
     * 批量删除凭证模版
     *
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteFinVoucherTemplateByIds(Long[] ids);
}