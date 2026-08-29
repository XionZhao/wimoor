package com.wimoor.finance.setting.service;

import com.wimoor.finance.setting.domain.FinMappingInvoice;

import java.util.List;

/**
 * 发票凭证映射模版Service接口
 * 
 * @author wimoor
 * @date 2026-07-20
 */
public interface IFinMappingInvoiceService 
{
    /**
     * 查询发票凭证映射模版列表
     */
    List<FinMappingInvoice> selectFinMappingInvoiceList(FinMappingInvoice finMappingInvoice);

    /**
     * 查询发票凭证映射模版详情
     */
    FinMappingInvoice selectFinMappingInvoiceById(String id);

    /**
     * 新增发票凭证映射模版
     */
    int insertFinMappingInvoice(FinMappingInvoice finMappingInvoice);

    /**
     * 修改发票凭证映射模版
     */
    int updateFinMappingInvoice(FinMappingInvoice finMappingInvoice);

    /**
     * 删除发票凭证映射模版
     */
    int deleteFinMappingInvoiceById(String id);

    /**
     * 批量删除发票凭证映射模版
     */
    int deleteFinMappingInvoiceByIds(List<String> ids);
}