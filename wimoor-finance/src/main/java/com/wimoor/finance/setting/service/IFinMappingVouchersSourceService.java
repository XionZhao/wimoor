package com.wimoor.finance.setting.service;

import com.wimoor.finance.setting.domain.FinMappingVouchersSource;

import java.util.List;

/**
 * ERP采购订单凭证同步追踪Service接口
 *
 * @author wimoor
 * @date 2026-08-07
 */
public interface IFinMappingVouchersSourceService
{
    FinMappingVouchersSource selectFinMappingVouchersSourceById(Long id);

    List<FinMappingVouchersSource> selectFinMappingVouchersSourceList(FinMappingVouchersSource record);

    FinMappingVouchersSource selectByOrderId(String groupid, String orderId);

    List<FinMappingVouchersSource> selectByOrderIds(String groupid, List<String> orderIds);

    int insertFinMappingVouchersSource(FinMappingVouchersSource record);

    int updateFinMappingVouchersSource(FinMappingVouchersSource record);

    int deleteFinMappingVouchersSourceById(Long id);

    int deleteFinMappingVouchersSourceByIds(Long[] ids);
}
