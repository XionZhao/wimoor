package com.wimoor.finance.voucher.controller;

import com.wimoor.common.core.web.controller.BaseController;
import com.wimoor.common.core.web.domain.Result;
import com.wimoor.common.core.web.page.TableDataInfo;
import com.wimoor.common.user.UserInfo;
import com.wimoor.common.user.UserInfoContext;
import com.wimoor.finance.voucher.domain.FinVoucherSummary;
import com.wimoor.finance.voucher.service.IFinVoucherSummaryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 凭证摘要Controller
 */
@RestController
@RequestMapping("/summary")
public class FinVoucherSummaryController extends BaseController
{
    @Autowired
    private IFinVoucherSummaryService finVoucherSummaryService;

    /**
     * 查询凭证摘要列表
     */
    @GetMapping("/list")
    public TableDataInfo list(FinVoucherSummary finVoucherSummary)
    {
        startPage();
        List<FinVoucherSummary> list = finVoucherSummaryService.selectFinVoucherSummaryList(finVoucherSummary);
        return getDataTable(list);
    }

    /**
     * 获取凭证摘要详细信息
     */
    @GetMapping(value = "/{id}")
    public Result getInfo(@PathVariable("id") Long id)
    {
        return success(finVoucherSummaryService.selectFinVoucherSummaryById(id));
    }

    /**
     * 新增凭证摘要
     */
    @PostMapping
    public Result add(@RequestBody FinVoucherSummary finVoucherSummary)
    {
        UserInfo user= UserInfoContext.get();
        finVoucherSummary.setCreateBy(user.getUserName());
        return toResult(finVoucherSummaryService.insertFinVoucherSummary(finVoucherSummary));
    }

    /**
     * 修改凭证摘要
     */
    @PutMapping
    public Result edit(@RequestBody FinVoucherSummary finVoucherSummary)
    {
        return toResult(finVoucherSummaryService.updateFinVoucherSummary(finVoucherSummary));
    }

    /**
     * 删除凭证摘要
     */
    @DeleteMapping("/{ids}")
    public Result remove(@PathVariable Long[] ids)
    {
        return toResult(finVoucherSummaryService.deleteFinVoucherSummaryByIds(ids));
    }
}
