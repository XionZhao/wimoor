package com.wimoor.finance.setting.service.impl;

import com.wimoor.finance.setting.domain.FinMappingVouchers;
import com.wimoor.finance.setting.mapper.FinMappingVouchersMapper;
import com.wimoor.finance.setting.service.IFinMappingVouchersService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 映射凭证关联Service业务层处理
 * 
 * @author wimoor
 * @date 2025-07-09
 */
@Service
public class FinMappingVouchersServiceImpl implements IFinMappingVouchersService 
{
    private static final Logger log = LoggerFactory.getLogger(FinMappingVouchersServiceImpl.class);

    @Autowired
    private FinMappingVouchersMapper finMappingVouchersMapper;

    /**
     * 查询映射凭证关联列表
     */
    @Override
    public List<FinMappingVouchers> selectFinMappingVouchersList(FinMappingVouchers finMappingVouchers)
    {
        return finMappingVouchersMapper.selectFinMappingVouchersList(finMappingVouchers);
    }

    /**
     * 查询映射凭证关联详情
     */
    @Override
    public FinMappingVouchers selectFinMappingVouchersById(Long id)
    {
        return finMappingVouchersMapper.selectFinMappingVouchersById(id);
    }

    /**
     * 新增映射凭证关联
     */
    @Override
    public int insertFinMappingVouchers(FinMappingVouchers finMappingVouchers)
    {
        return finMappingVouchersMapper.insertFinMappingVouchers(finMappingVouchers);
    }

    /**
     * 修改映射凭证关联
     */
    @Override
    public int updateFinMappingVouchers(FinMappingVouchers finMappingVouchers)
    {
        return finMappingVouchersMapper.updateFinMappingVouchers(finMappingVouchers);
    }

    /**
     * 删除映射凭证关联
     */
    @Override
    public int deleteFinMappingVouchersById(Long id)
    {
        return finMappingVouchersMapper.deleteFinMappingVouchersById(id);
    }

    /**
     * 批量删除映射凭证关联
     */
    @Override
    public int deleteFinMappingVouchersByIds(Long[] ids)
    {
        return finMappingVouchersMapper.deleteFinMappingVouchersByIds(ids);
    }
}