package com.wimoor.finance.setting.controller;

import com.wimoor.common.core.utils.poi.ExcelUtil;
import com.wimoor.common.core.web.controller.BaseController;
import com.wimoor.common.core.web.domain.Result;
import com.wimoor.finance.setting.domain.FinMappingErpInventory;
import com.wimoor.finance.setting.service.IFinMappingErpInventoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.util.Date;
import java.util.List;

/**
 * 存货科目映射规则Controller
 * 
 * @author wimoor
 * @date 2025-07-09
 */
@RestController
@RequestMapping("/mappingErpInventory")
public class FinMappingErpInventoryController extends BaseController
{
    @Autowired
    private IFinMappingErpInventoryService finMappingErpInventoryService;

    /**
     * 查询存货映射规则列表
     */
    @GetMapping("/list")
    public Result list(FinMappingErpInventory finMappingErpInventory)
    {
        List<FinMappingErpInventory> list = finMappingErpInventoryService.selectFinMappingErpInventoryList(finMappingErpInventory);
        return success(list);
    }

    /**
     * 查询存货映射规则详情
     */
    @GetMapping(value = "/{id}")
    public Result getInfo(@PathVariable("id") Long id)
    {
        return success(finMappingErpInventoryService.selectFinMappingErpInventoryById(id));
    }

    /**
     * 新增存货映射规则
     */
    @PostMapping
    public Result add(@RequestBody FinMappingErpInventory finMappingErpInventory)
    {
        finMappingErpInventory.setCreatedTime(new Date());
        finMappingErpInventory.setUpdatedTime(new Date());
        return toResult(finMappingErpInventoryService.insertFinMappingErpInventory(finMappingErpInventory));
    }

    /**
     * 修改存货映射规则
     */
    @PutMapping
    public Result edit(@RequestBody FinMappingErpInventory finMappingErpInventory)
    {
        finMappingErpInventory.setUpdatedTime(new Date());
        return toResult(finMappingErpInventoryService.updateFinMappingErpInventory(finMappingErpInventory));
    }

    /**
     * 删除存货映射规则
     */
    @DeleteMapping("/{ids}")
    public Result remove(@PathVariable Long[] ids)
    {
        return toResult(finMappingErpInventoryService.deleteFinMappingErpInventoryByIds(ids));
    }

    /**
     * 导出存货映射规则列表
     */
    @PostMapping("/export")
    public void export(HttpServletResponse response, FinMappingErpInventory finMappingErpInventory)
    {
        List<FinMappingErpInventory> list = finMappingErpInventoryService.selectFinMappingErpInventoryList(finMappingErpInventory);
        ExcelUtil<FinMappingErpInventory> util = new ExcelUtil<FinMappingErpInventory>(FinMappingErpInventory.class);
        util.exportExcel(response, list, "存货映射规则数据");
    }
}
