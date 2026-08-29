package com.wimoor.finance.setting.service.impl;

import com.wimoor.common.result.Result;
import com.wimoor.finance.api.RemoteERPService;
import com.wimoor.finance.setting.domain.FinMappingErpFeetype;
import com.wimoor.finance.setting.mapper.FinMappingErpFeetypeMapper;
import com.wimoor.finance.setting.service.IFinMappingErpFeetypeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 费用类型级别映射Service业务层处理
 *
 * @author wimoor
 * @date 2025-07-07
 */
@Service
public class FinMappingErpFeetypeServiceImpl implements IFinMappingErpFeetypeService
{
    private static final Logger log = LoggerFactory.getLogger(FinMappingErpFeetypeServiceImpl.class);

    @Autowired
    private FinMappingErpFeetypeMapper finMappingErpFeetypeMapper;

    @Autowired
    private RemoteERPService remoteERPService;

    /**
     * 查询映射列表
     */
    @Override
    public List<FinMappingErpFeetype> selectFinMappingErpFeetypeList(FinMappingErpFeetype finMappingErpFeetype)
    {
        List<FinMappingErpFeetype> list = finMappingErpFeetypeMapper.selectFinMappingErpFeetypeList(finMappingErpFeetype);
        // 补充采购账户名称
        fillAccountNames(list);
        return list;
    }

    /**
     * 填充采购账户名称
     */
    private void fillAccountNames(List<FinMappingErpFeetype> list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        try {
            // 获取采购账户列表
            Result<List<Map<String, Object>>> accountResult = remoteERPService.getAccountAll();
            Map<String, String> accountMap = accountResult.getData().stream()
                .collect(Collectors.toMap(
                    a -> a.get("id").toString(),
                    a -> a.get("name").toString(),
                    (v1, v2) -> v1
                ));

            // 填充名称
            for (FinMappingErpFeetype rule : list) {
                if (rule.getAccountId() != null) {
                    rule.setAccountName(accountMap.getOrDefault(rule.getAccountId(), String.valueOf(rule.getAccountId())));
                }
            }
        } catch (Exception e) {
            log.error("获取采购账户名称失败: {}", e.getMessage());
            // 降级处理：使用ID作为名称
            for (FinMappingErpFeetype rule : list) {
                if (rule.getAccountId() != null && rule.getAccountName() == null) {
                    rule.setAccountName(String.valueOf(rule.getAccountId()));
                }
            }
        }
    }

    /**
     * 查询映射详情
     */
    @Override
    public FinMappingErpFeetype selectFinMappingErpFeetypeById(Long id)
    {
        return finMappingErpFeetypeMapper.selectFinMappingErpFeetypeById(id);
    }

    /**
     * 新增映射
     */
    @Override
    public int insertFinMappingErpFeetype(FinMappingErpFeetype finMappingErpFeetype)
    {
        return finMappingErpFeetypeMapper.insertFinMappingErpFeetype(finMappingErpFeetype);
    }

    /**
     * 修改映射
     */
    @Override
    public int updateFinMappingErpFeetype(FinMappingErpFeetype finMappingErpFeetype)
    {
        return finMappingErpFeetypeMapper.updateFinMappingErpFeetype(finMappingErpFeetype);
    }

    /**
     * 删除映射
     */
    @Override
    public int deleteFinMappingErpFeetypeById(Long id)
    {
        return finMappingErpFeetypeMapper.deleteFinMappingErpFeetypeById(id);
    }

    /**
     * 批量删除映射
     */
    @Override
    public int deleteFinMappingErpFeetypeByIds(Long[] ids)
    {
        return finMappingErpFeetypeMapper.deleteFinMappingErpFeetypeByIds(ids);
    }

    /**
     * 批量新增映射
     */
    @Override
    public int batchInsertFinMappingErpFeetype(List<FinMappingErpFeetype> list)
    {
        int rows = 0;
        for (FinMappingErpFeetype rule : list) {
            rows += finMappingErpFeetypeMapper.insertFinMappingErpFeetype(rule);
        }
        return rows;
    }
}
