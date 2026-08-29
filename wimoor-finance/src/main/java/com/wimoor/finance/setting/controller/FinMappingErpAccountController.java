package com.wimoor.finance.setting.controller;

import com.wimoor.common.core.utils.poi.ExcelUtil;
import com.wimoor.common.core.web.controller.BaseController;
import com.wimoor.common.core.web.domain.Result;
import com.wimoor.finance.api.RemoteERPService;
import com.wimoor.finance.setting.domain.FinAccountingSubjects;
import com.wimoor.finance.setting.domain.FinMappingErpAccount;
import com.wimoor.finance.setting.domain.FinMappingErpAccountImport;
import com.wimoor.finance.setting.mapper.FinAccountingSubjectsMapper;
import com.wimoor.finance.setting.service.IFinMappingErpAccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.util.*;

/**
 * 费用类型-科目映射规则Controller
 * 
 * @author wimoor
 * @date 2025-07-07
 */
@RestController
@RequestMapping("/mappingErpAccount")
public class FinMappingErpAccountController extends BaseController
{
    @Autowired
    private IFinMappingErpAccountService finMappingErpAccountService;

    @Autowired
    private RemoteERPService remoteERPService;

    @Autowired
    private FinAccountingSubjectsMapper finAccountingSubjectsMapper;

    /**
     * 查询映射规则列表
     */
    @GetMapping("/list")
    public Result list(FinMappingErpAccount finMappingErpAccount)
    {
        List<FinMappingErpAccount> list = finMappingErpAccountService.selectFinMappingErpAccountList(finMappingErpAccount);
        return success(list);
    }

    /**
     * 查询映射规则详情
     */
    @GetMapping(value = "/{id}")
    public Result getInfo(@PathVariable("id") Long id)
    {
        return success(finMappingErpAccountService.selectFinMappingErpAccountById(id));
    }

    /**
     * 新增映射规则
     */
    @PostMapping
    public Result add(@RequestBody FinMappingErpAccount finMappingErpAccount)
    {
        return toResult(finMappingErpAccountService.insertFinMappingErpAccount(finMappingErpAccount));
    }

    /**
     * 修改映射规则
     */
    @PutMapping
    public Result edit(@RequestBody FinMappingErpAccount finMappingErpAccount)
    {
        return toResult(finMappingErpAccountService.updateFinMappingErpAccount(finMappingErpAccount));
    }

    /**
     * 删除映射规则
     */
    @DeleteMapping("/{ids}")
    public Result remove(@PathVariable Long[] ids)
    {
        return toResult(finMappingErpAccountService.deleteFinMappingErpAccountByIds(ids));
    }

    /**
     * 导出映射规则列表
     */
    @PostMapping("/export")
    public void export(HttpServletResponse response, FinMappingErpAccount finMappingErpAccount)
    {
        List<FinMappingErpAccount> list = finMappingErpAccountService.selectFinMappingErpAccountList(finMappingErpAccount);
        ExcelUtil<FinMappingErpAccount> util = new ExcelUtil<FinMappingErpAccount>(FinMappingErpAccount.class);
        util.exportExcel(response, list, "映射规则数据");
    }

    /**
     * 导入模板下载
     */
    @PostMapping("/importTemplate")
    public void importTemplate(HttpServletResponse response)
    {
        ExcelUtil<FinMappingErpAccountImport> util = new ExcelUtil<FinMappingErpAccountImport>(FinMappingErpAccountImport.class);
        util.importTemplateExcel(response, "映射规则数据");
    }

    /**
     * 导入映射规则数据
     */
    @PostMapping("/importData")
    public Result importData(MultipartFile file, String groupid) throws Exception
    {
        // 使用导入DTO读取Excel
        ExcelUtil<FinMappingErpAccountImport> util = new ExcelUtil<FinMappingErpAccountImport>(FinMappingErpAccountImport.class);
        List<FinMappingErpAccountImport> importList = util.importExcel(file.getInputStream());
        
        if (importList == null || importList.isEmpty()) {
            return Result.error("导入数据为空");
        }

        // 获取费用类型列表（通过feign）
        com.wimoor.common.result.Result<List<Map<String, Object>>> projectResult = remoteERPService.getProject();
        Map<String, String> projectMap = new HashMap<>();
        if (projectResult.getData() != null) {
            for (Map<String, Object> item : projectResult.getData()) {
                projectMap.put(item.get("name").toString(), item.get("id").toString());
            }
        }

        // 转换为实体对象
        List<FinMappingErpAccount> list = new ArrayList<>();
        List<String> errorMessages = new ArrayList<>();
        
        for (int i = 0; i < importList.size(); i++) {
            FinMappingErpAccountImport importItem = importList.get(i);
            FinMappingErpAccount rule = new FinMappingErpAccount();

            // 解析费用类型
            if (importItem.getFeeTypeName() != null && !importItem.getFeeTypeName().isEmpty()) {
                String feeTypeId = projectMap.get(importItem.getFeeTypeName().trim());
                if (feeTypeId != null) {
                    rule.setFeeTypeId(feeTypeId);
                } else {
                    errorMessages.add("第" + (i + 1) + "行：费用类型'" + importItem.getFeeTypeName() + "'不存在");
                    continue;
                }
            } else {
                errorMessages.add("第" + (i + 1) + "行：费用类型不能为空");
                continue;
            }

            // 解析科目（通过编码）
            if (importItem.getSubjectCode() != null && !importItem.getSubjectCode().isEmpty()) {
                FinAccountingSubjects subject = finAccountingSubjectsMapper.selectBySubjectCode(groupid, importItem.getSubjectCode().trim());
                if (subject != null) {
                    rule.setSubjectId(String.valueOf(subject.getSubjectId()));
                } else {
                    errorMessages.add("第" + (i + 1) + "行：科目编码'" + importItem.getSubjectCode() + "'不存在");
                    continue;
                }
            } else {
                errorMessages.add("第" + (i + 1) + "行：科目编码不能为空");
                continue;
            }

            // 设置其他字段
            rule.setGroupid(groupid);

            list.add(rule);
        }

        // 如果有错误信息，返回错误
        if (!errorMessages.isEmpty()) {
            return Result.error("导入失败：\n" + String.join("\n", errorMessages));
        }

        // 批量插入
        int rows = finMappingErpAccountService.batchInsertFinMappingErpAccount(list);
        return Result.success("导入成功，共导入 " + rows + " 条数据");
    }
}