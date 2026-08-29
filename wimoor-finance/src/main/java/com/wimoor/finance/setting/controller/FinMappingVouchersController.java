package com.wimoor.finance.setting.controller;

import com.wimoor.common.core.utils.poi.ExcelUtil;
import com.wimoor.common.core.web.controller.BaseController;
import com.wimoor.common.core.web.domain.Result;
import com.wimoor.finance.setting.domain.FinMappingVouchers;
import com.wimoor.finance.setting.service.IFinMappingVouchersService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.util.Date;
import java.util.List;

/**
 * 映射凭证关联Controller
 * 
 * @author wimoor
 * @date 2025-07-09
 */
@RestController
@RequestMapping("/mappingVouchers")
public class FinMappingVouchersController extends BaseController
{
    @Autowired
    private IFinMappingVouchersService finMappingVouchersService;

    /**
     * 查询映射凭证关联列表
     */
    @GetMapping("/list")
    public Result list(FinMappingVouchers finMappingVouchers)
    {
        List<FinMappingVouchers> list = finMappingVouchersService.selectFinMappingVouchersList(finMappingVouchers);
        return success(list);
    }

    /**
     * 查询映射凭证关联详情
     */
    @GetMapping(value = "/{id}")
    public Result getInfo(@PathVariable("id") Long id)
    {
        return success(finMappingVouchersService.selectFinMappingVouchersById(id));
    }

    /**
     * 新增映射凭证关联
     */
    @PostMapping
    public Result add(@RequestBody FinMappingVouchers finMappingVouchers)
    {
        finMappingVouchers.setCreatedTime(new Date());
        finMappingVouchers.setUpdatedTime(new Date());
        return toResult(finMappingVouchersService.insertFinMappingVouchers(finMappingVouchers));
    }

    /**
     * 修改映射凭证关联
     */
    @PutMapping
    public Result edit(@RequestBody FinMappingVouchers finMappingVouchers)
    {
        finMappingVouchers.setUpdatedTime(new Date());
        return toResult(finMappingVouchersService.updateFinMappingVouchers(finMappingVouchers));
    }

    /**
     * 删除映射凭证关联
     */
    @DeleteMapping("/{ids}")
    public Result remove(@PathVariable Long[] ids)
    {
        return toResult(finMappingVouchersService.deleteFinMappingVouchersByIds(ids));
    }

    /**
     * 导出映射凭证关联列表
     */
    @PostMapping("/export")
    public void export(HttpServletResponse response, FinMappingVouchers finMappingVouchers)
    {
        List<FinMappingVouchers> list = finMappingVouchersService.selectFinMappingVouchersList(finMappingVouchers);
        ExcelUtil<FinMappingVouchers> util = new ExcelUtil<FinMappingVouchers>(FinMappingVouchers.class);
        util.exportExcel(response, list, "映射凭证关联数据");
    }
}