package com.wimoor.finance.setting.mapper;

import com.wimoor.finance.setting.domain.FinMappingInvoice;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 发票凭证映射模版Mapper接口
 * 
 * @author wimoor
 * @date 2026-07-20
 */
public interface FinMappingInvoiceMapper 
{
    /**
     * 查询发票凭证映射模版列表
     */
    List<FinMappingInvoice> selectFinMappingInvoiceList(FinMappingInvoice finMappingInvoice);

    /**
     * 查询发票凭证映射模版详情
     */
    FinMappingInvoice selectFinMappingInvoiceById(@Param("id") String id);

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
    int deleteFinMappingInvoiceById(@Param("id") String id);

    /**
     * 批量删除发票凭证映射模版
     */
    int deleteFinMappingInvoiceByIds(@Param("ids") List<String> ids);
}