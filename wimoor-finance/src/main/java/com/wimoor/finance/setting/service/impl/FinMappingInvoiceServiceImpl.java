package com.wimoor.finance.setting.service.impl;

import com.wimoor.finance.setting.domain.FinMappingInvoice;
import com.wimoor.finance.setting.mapper.FinMappingInvoiceMapper;
import com.wimoor.finance.setting.service.IFinMappingInvoiceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 发票凭证映射模版Service业务层处理
 * 
 * @author wimoor
 * @date 2026-07-20
 */
@Service
public class FinMappingInvoiceServiceImpl implements IFinMappingInvoiceService 
{
    private static final Logger log = LoggerFactory.getLogger(FinMappingInvoiceServiceImpl.class);

    @Autowired
    private FinMappingInvoiceMapper finMappingInvoiceMapper;

    /**
     * 查询发票凭证映射模版列表
     */
    @Override
    public List<FinMappingInvoice> selectFinMappingInvoiceList(FinMappingInvoice finMappingInvoice)
    {
        return finMappingInvoiceMapper.selectFinMappingInvoiceList(finMappingInvoice);
    }

    /**
     * 查询发票凭证映射模版详情
     */
    @Override
    public FinMappingInvoice selectFinMappingInvoiceById(String id)
    {
        return finMappingInvoiceMapper.selectFinMappingInvoiceById(id);
    }

    /**
     * 新增发票凭证映射模版
     */
    @Override
    public int insertFinMappingInvoice(FinMappingInvoice finMappingInvoice)
    {
        return finMappingInvoiceMapper.insertFinMappingInvoice(finMappingInvoice);
    }

    /**
     * 修改发票凭证映射模版
     */
    @Override
    public int updateFinMappingInvoice(FinMappingInvoice finMappingInvoice)
    {
        return finMappingInvoiceMapper.updateFinMappingInvoice(finMappingInvoice);
    }

    /**
     * 删除发票凭证映射模版
     */
    @Override
    public int deleteFinMappingInvoiceById(String id)
    {
        return finMappingInvoiceMapper.deleteFinMappingInvoiceById(id);
    }

    /**
     * 批量删除发票凭证映射模版
     */
    @Override
    public int deleteFinMappingInvoiceByIds(List<String> ids)
    {
        return finMappingInvoiceMapper.deleteFinMappingInvoiceByIds(ids);
    }
}