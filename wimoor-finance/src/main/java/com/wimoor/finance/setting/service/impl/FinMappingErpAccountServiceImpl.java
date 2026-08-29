package com.wimoor.finance.setting.service.impl;

import com.wimoor.common.result.Result;
import com.wimoor.finance.api.RemoteERPService;
import com.wimoor.finance.setting.domain.FinMappingErpAccount;
import com.wimoor.finance.setting.mapper.FinMappingErpAccountMapper;
import com.wimoor.finance.setting.service.IFinMappingErpAccountService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 费用类型-科目映射规则Service业务层处理
 * 
 * @author wimoor
 * @date 2025-07-07
 */
@Service
public class FinMappingErpAccountServiceImpl implements IFinMappingErpAccountService 
{
    private static final Logger log = LoggerFactory.getLogger(FinMappingErpAccountServiceImpl.class);

    @Autowired
    private FinMappingErpAccountMapper finMappingErpAccountMapper;

    @Autowired
    private RemoteERPService remoteERPService;

    /**
     * 查询映射规则列表
     */
    @Override
    public List<FinMappingErpAccount> selectFinMappingErpAccountList(FinMappingErpAccount finMappingErpAccount)
    {
        List<FinMappingErpAccount> list = finMappingErpAccountMapper.selectFinMappingErpAccountList(finMappingErpAccount);
        // 补充费用类型名称
        fillProjectNames(list);
        return list;
    }

    /**
     * 填充费用类型名称
     */
    private void fillProjectNames(List<FinMappingErpAccount> list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        try {
            // 获取费用类型列表
            Result<List<Map<String, Object>>> projectResult = remoteERPService.getProject();
            Map<String, String> projectMap = projectResult.getData().stream()
                .collect(Collectors.toMap(
                    p -> p.get("id").toString(),
                    p -> p.get("name").toString(),
                    (v1, v2) -> v1
                ));

            // 填充名称
            for (FinMappingErpAccount rule : list) {
                if (rule.getFeeTypeId() != null) {
                    rule.setFeeTypeName(projectMap.getOrDefault(rule.getFeeTypeId(), String.valueOf(rule.getFeeTypeId())));
                }
            }
        } catch (Exception e) {
            log.error("获取费用类型名称失败: {}", e.getMessage());
            // 降级处理：使用ID作为名称
            for (FinMappingErpAccount rule : list) {
                if (rule.getFeeTypeId() != null && rule.getFeeTypeName() == null) {
                    rule.setFeeTypeName(String.valueOf(rule.getFeeTypeId()));
                }
            }
        }
    }

    /**
     * 查询映射规则详情
     */
    @Override
    public FinMappingErpAccount selectFinMappingErpAccountById(Long id)
    {
        return finMappingErpAccountMapper.selectFinMappingErpAccountById(id);
    }

    /**
     * 新增映射规则
     */
    @Override
    public int insertFinMappingErpAccount(FinMappingErpAccount finMappingErpAccount)
    {
        return finMappingErpAccountMapper.insertFinMappingErpAccount(finMappingErpAccount);
    }

    /**
     * 修改映射规则
     */
    @Override
    public int updateFinMappingErpAccount(FinMappingErpAccount finMappingErpAccount)
    {
        return finMappingErpAccountMapper.updateFinMappingErpAccount(finMappingErpAccount);
    }

    /**
     * 删除映射规则
     */
    @Override
    public int deleteFinMappingErpAccountById(Long id)
    {
        return finMappingErpAccountMapper.deleteFinMappingErpAccountById(id);
    }

    /**
     * 批量删除映射规则
     */
    @Override
    public int deleteFinMappingErpAccountByIds(Long[] ids)
    {
        return finMappingErpAccountMapper.deleteFinMappingErpAccountByIds(ids);
    }

    /**
     * 批量新增映射规则
     */
    @Override
    public int batchInsertFinMappingErpAccount(List<FinMappingErpAccount> list)
    {
        int rows = 0;
        for (FinMappingErpAccount rule : list) {
            rows += finMappingErpAccountMapper.insertFinMappingErpAccount(rule);
        }
        return rows;
    }
}
