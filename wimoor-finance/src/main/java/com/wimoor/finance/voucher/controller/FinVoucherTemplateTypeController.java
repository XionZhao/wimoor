package com.wimoor.finance.voucher.controller;

import com.wimoor.common.core.web.controller.BaseController;
import com.wimoor.common.core.web.domain.Result;
import com.wimoor.common.core.web.page.TableDataInfo;
import com.wimoor.common.user.UserInfo;
import com.wimoor.common.user.UserInfoContext;
import com.wimoor.finance.voucher.domain.FinVoucherTemplateType;
import com.wimoor.finance.voucher.service.IFinVoucherTemplateTypeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;

/**
 * 凭证模版分类Controller
 */
@RestController
@RequestMapping("/voucher-template-type")
public class FinVoucherTemplateTypeController extends BaseController
{
    @Autowired
    private IFinVoucherTemplateTypeService finVoucherTemplateTypeService;

    /**
     * 查询凭证模版分类列表（分页）
     */
    @GetMapping("/list")
    public TableDataInfo list(FinVoucherTemplateType finVoucherTemplateType)
    {
        startPage();
        List<FinVoucherTemplateType> list = finVoucherTemplateTypeService.selectFinVoucherTemplateTypeList(finVoucherTemplateType);
        return getDataTable(list);
    }

    /**
     * 查询所有凭证模版分类（不分页）
     */
    @GetMapping("/all")
    public Result all()
    {
        UserInfo user = UserInfoContext.get();
        List<FinVoucherTemplateType> list = finVoucherTemplateTypeService.selectFinVoucherTemplateTypeAll(user.getGroupid());
        return success(list);
    }

    /**
     * 获取凭证模版分类详细信息
     */
    @GetMapping(value = "/{id}")
    public Result getInfo(@PathVariable("id") Long id)
    {
        return success(finVoucherTemplateTypeService.selectFinVoucherTemplateTypeById(id));
    }

    /**
     * 新增凭证模版分类
     */
    @PostMapping
    public Result add(@RequestBody FinVoucherTemplateType finVoucherTemplateType)
    {
        UserInfo user = UserInfoContext.get();
        finVoucherTemplateType.setGroupid(user.getGroupid());
        finVoucherTemplateType.setCreateBy(user.getUserName());
        finVoucherTemplateType.setCreatedTime(new Date());
        finVoucherTemplateType.setModifyBy(user.getUserName());
        finVoucherTemplateType.setUpdatedTime(new Date());
        return toResult(finVoucherTemplateTypeService.insertFinVoucherTemplateType(finVoucherTemplateType));
    }

    /**
     * 修改凭证模版分类
     */
    @PutMapping
    public Result edit(@RequestBody FinVoucherTemplateType finVoucherTemplateType)
    {
        UserInfo user = UserInfoContext.get();
        finVoucherTemplateType.setModifyBy(user.getUserName());
        finVoucherTemplateType.setUpdatedTime(new Date());
        return toResult(finVoucherTemplateTypeService.updateFinVoucherTemplateType(finVoucherTemplateType));
    }

    /**
     * 删除凭证模版分类
     */
    @DeleteMapping("/{ids}")
    public Result remove(@PathVariable Long[] ids)
    {
        return toResult(finVoucherTemplateTypeService.deleteFinVoucherTemplateTypeByIds(ids));
    }
}
