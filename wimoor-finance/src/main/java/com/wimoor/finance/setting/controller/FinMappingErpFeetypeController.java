package com.wimoor.finance.setting.controller;

import com.wimoor.common.core.utils.poi.ExcelUtil;
import com.wimoor.common.core.web.controller.BaseController;
import com.wimoor.common.core.web.domain.Result;
import com.wimoor.finance.api.RemoteERPService;
import com.wimoor.finance.setting.domain.FinAccountingSubjects;
import com.wimoor.finance.setting.domain.FinMappingErpFeetype;
import com.wimoor.finance.setting.domain.FinMappingErpFeetypeImport;
import com.wimoor.finance.setting.mapper.FinAccountingSubjectsMapper;
import com.wimoor.finance.setting.service.IFinMappingErpFeetypeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.util.*;

/**
 * 费用类型级别映射Controller
 *
 * @author wimoor
 * @date 2025-07-07
 */
@RestController
@RequestMapping("/mappingErpFeetype")
public class FinMappingErpFeetypeController extends BaseController
{
    @Autowired
    private IFinMappingErpFeetypeService finMappingErpFeetypeService;

    @Autowired
    private RemoteERPService remoteERPService;

    @Autowired
    private FinAccountingSubjectsMapper finAccountingSubjectsMapper;

    /**
     * 查询映射列表
     */
    @GetMapping("/list")
    public Result list(FinMappingErpFeetype finMappingErpFeetype)
    {
        List<FinMappingErpFeetype> list = finMappingErpFeetypeService.selectFinMappingErpFeetypeList(finMappingErpFeetype);
        return success(list);
    }

    /**
     * 查询映射详情
     */
    @GetMapping(value = "/{id}")
    public Result getInfo(@PathVariable("id") Long id)
    {
        return success(finMappingErpFeetypeService.selectFinMappingErpFeetypeById(id));
    }

    /**
     * 新增映射
     */
    @PostMapping
    public Result add(@RequestBody FinMappingErpFeetype finMappingErpFeetype)
    {
        return toResult(finMappingErpFeetypeService.insertFinMappingErpFeetype(finMappingErpFeetype));
    }

    /**
     * 修改映射
     */
    @PutMapping
    public Result edit(@RequestBody FinMappingErpFeetype finMappingErpFeetype)
    {
        return toResult(finMappingErpFeetypeService.updateFinMappingErpFeetype(finMappingErpFeetype));
    }

    /**
     * 删除映射
     */
    @DeleteMapping("/{ids}")
    public Result remove(@PathVariable Long[] ids)
    {
        return toResult(finMappingErpFeetypeService.deleteFinMappingErpFeetypeByIds(ids));
    }

    /**
     * 导出映射列表
     */
    @PostMapping("/export")
    public void export(HttpServletResponse response, FinMappingErpFeetype finMappingErpFeetype)
    {
        List<FinMappingErpFeetype> list = finMappingErpFeetypeService.selectFinMappingErpFeetypeList(finMappingErpFeetype);
        ExcelUtil<FinMappingErpFeetype> util = new ExcelUtil<FinMappingErpFeetype>(FinMappingErpFeetype.class);
        util.exportExcel(response, list, "费用类型映射数据");
    }

    /**
     * 导入模板下载
     */
    @PostMapping("/importTemplate")
    public void importTemplate(HttpServletResponse response)
    {
        ExcelUtil<FinMappingErpFeetypeImport> util = new ExcelUtil<FinMappingErpFeetypeImport>(FinMappingErpFeetypeImport.class);
        util.importTemplateExcel(response, "费用类型映射数据");
    }

    /**
     * 导入映射数据
     */
    @PostMapping("/importData")
    public Result importData(MultipartFile file, String groupid) throws Exception
    {
        // 使用导入DTO读取Excel
        ExcelUtil<FinMappingErpFeetypeImport> util = new ExcelUtil<FinMappingErpFeetypeImport>(FinMappingErpFeetypeImport.class);
        List<FinMappingErpFeetypeImport> importList = util.importExcel(file.getInputStream());

        if (importList == null || importList.isEmpty()) {
            return Result.error("导入数据为空");
        }

        // 获取采购账户列表（通过feign）
        com.wimoor.common.result.Result<List<Map<String, Object>>> accountResult = remoteERPService.getAccountAll();
        Map<String, String> accountMap = new HashMap<>();
        if (accountResult.getData() != null) {
            for (Map<String, Object> item : accountResult.getData()) {
                accountMap.put(item.get("name").toString(), item.get("id").toString());
            }
        }

        // 转换为实体对象
        List<FinMappingErpFeetype> list = new ArrayList<>();
        List<String> errorMessages = new ArrayList<>();

        for (int i = 0; i < importList.size(); i++) {
            FinMappingErpFeetypeImport importItem = importList.get(i);
            FinMappingErpFeetype rule = new FinMappingErpFeetype();

            // 解析采购账户
            if (importItem.getAccountName() != null && !importItem.getAccountName().isEmpty()) {
                String accountId = accountMap.get(importItem.getAccountName().trim());
                if (accountId != null) {
                    rule.setAccountId(accountId);
                } else {
                    errorMessages.add("第" + (i + 1) + "行：采购账户'" + importItem.getAccountName() + "'不存在");
                    continue;
                }
            } else {
                errorMessages.add("第" + (i + 1) + "行：采购账户不能为空");
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
        int rows = finMappingErpFeetypeService.batchInsertFinMappingErpFeetype(list);
        return Result.success("导入成功，共导入 " + rows + " 条数据");
    }
}