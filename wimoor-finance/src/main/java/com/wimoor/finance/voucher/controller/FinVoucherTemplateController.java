package com.wimoor.finance.voucher.controller;

import com.wimoor.common.core.web.controller.BaseController;
import com.wimoor.common.core.web.domain.Result;
import com.wimoor.common.core.web.page.TableDataInfo;
import com.wimoor.common.user.UserInfo;
import com.wimoor.common.user.UserInfoContext;
import com.wimoor.finance.voucher.domain.FinVoucherTemplate;
import com.wimoor.finance.voucher.domain.FinVoucherTemplateEntries;
import com.wimoor.finance.voucher.domain.FinVoucherTemplateEntriesAuxiliary;
import com.wimoor.finance.voucher.service.IFinVoucherTemplateEntriesAuxiliaryService;
import com.wimoor.finance.voucher.service.IFinVoucherTemplateEntriesService;
import com.wimoor.finance.voucher.service.IFinVoucherTemplateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 凭证模版Controller
 */
@RestController
@RequestMapping("/voucher-template")
public class FinVoucherTemplateController extends BaseController
{
    @Autowired
    private IFinVoucherTemplateService finVoucherTemplateService;

    @Autowired
    private IFinVoucherTemplateEntriesService finVoucherTemplateEntriesService;

    @Autowired
    private IFinVoucherTemplateEntriesAuxiliaryService finVoucherTemplateEntriesAuxiliaryService;

    /**
     * 查询凭证模版列表（分页）
     */
    @GetMapping("/list")
    public TableDataInfo list(FinVoucherTemplate finVoucherTemplate)
    {
        startPage();
        List<FinVoucherTemplate> list = finVoucherTemplateService.selectFinVoucherTemplateList(finVoucherTemplate);
        return getDataTable(list);
    }

    /**
     * 获取凭证模版详细信息
     */
    @GetMapping(value = "/{id}")
    public Result getInfo(@PathVariable("id") Long id)
    {
        return success(finVoucherTemplateService.selectFinVoucherTemplateById(id));
    }

    /**
     * 获取凭证模版详细信息（包含分录和辅助核算）
     */
    @GetMapping(value = "/detail/{id}")
    public Result getDetail(@PathVariable("id") Long id)
    {
        FinVoucherTemplate template = finVoucherTemplateService.selectFinVoucherTemplateById(id);
        if (template == null)
        {
            return success(null);
        }
        Map<String, Object> result = new HashMap<>();
        result.put("template", template);

        List<FinVoucherTemplateEntries> entriesList = finVoucherTemplateEntriesService.selectByTemplateId(id);
        if (entriesList != null && !entriesList.isEmpty())
        {
            List<Long> entryIds = new ArrayList<>();
            for (FinVoucherTemplateEntries entry : entriesList)
            {
                entryIds.add(entry.getEntryId());
            }
            List<FinVoucherTemplateEntriesAuxiliary> auxiliaryList = finVoucherTemplateEntriesAuxiliaryService.selectByEntryIds(entryIds);
            if (auxiliaryList != null && !auxiliaryList.isEmpty())
            {
                Map<Long, List<FinVoucherTemplateEntriesAuxiliary>> auxiliaryMap = new HashMap<>();
                for (FinVoucherTemplateEntriesAuxiliary aux : auxiliaryList)
                {
                    auxiliaryMap.computeIfAbsent(aux.getEntryId(), k -> new ArrayList<>()).add(aux);
                }
                for (FinVoucherTemplateEntries entry : entriesList)
                {
                    List<FinVoucherTemplateEntriesAuxiliary> entryAuxList = auxiliaryMap.get(entry.getEntryId());
                    if (entryAuxList == null)
                    {
                        entryAuxList = new ArrayList<>();
                    }
                    entry.setAuxiliaryList(entryAuxList);
                }
            }
        }
        else
        {
            entriesList = new ArrayList<>();
        }
        result.put("entries", entriesList);
        return success(result);
    }

    /**
     * 新增凭证模版（包含分录和辅助核算）
     */
    @PostMapping
    @Transactional
    public Result add(@RequestBody FinVoucherTemplate finVoucherTemplate)
    {
        UserInfo user = UserInfoContext.get();
        finVoucherTemplate.setCreateBy(user.getUserName());
        finVoucherTemplate.setCreatedTime(new Date());
        finVoucherTemplate.setModifyBy(user.getUserName());
        finVoucherTemplate.setUpdatedTime(new Date());
        finVoucherTemplateService.insertFinVoucherTemplate(finVoucherTemplate);

        List<FinVoucherTemplateEntries> entriesList = finVoucherTemplate.getEntries();
        if (entriesList != null && !entriesList.isEmpty())
        {
            for (FinVoucherTemplateEntries entry : entriesList)
            {
                entry.setGroupid(user.getGroupid());
                entry.setTemplateId(finVoucherTemplate.getId());
                entry.setCreatedTime(new Date());
            }
            finVoucherTemplateEntriesService.insertBatch(entriesList);

            List<FinVoucherTemplateEntriesAuxiliary> allAuxiliary = new ArrayList<>();
            for (FinVoucherTemplateEntries entry : entriesList)
            {
                if (entry.getAuxiliaryList() != null && !entry.getAuxiliaryList().isEmpty())
                {
                    for (FinVoucherTemplateEntriesAuxiliary aux : entry.getAuxiliaryList())
                    {
                        aux.setEntryId(entry.getEntryId());
                        aux.setGroupid(user.getGroupid());
                        allAuxiliary.add(aux);
                    }
                }
            }
            if (!allAuxiliary.isEmpty())
            {
                finVoucherTemplateEntriesAuxiliaryService.insertBatch(allAuxiliary);
            }
        }
        return toResult(1);
    }

    /**
     * 修改凭证模版（包含分录和辅助核算）
     */
    @PutMapping
    @Transactional
    public Result edit(@RequestBody FinVoucherTemplate finVoucherTemplate)
    {
        UserInfo user = UserInfoContext.get();
        finVoucherTemplate.setModifyBy(user.getUserName());
        finVoucherTemplate.setUpdatedTime(new Date());
        finVoucherTemplateService.updateFinVoucherTemplate(finVoucherTemplate);

        // 删除旧分录及其辅助核算
        List<FinVoucherTemplateEntries> oldEntries = finVoucherTemplateEntriesService.selectByTemplateId(finVoucherTemplate.getId());
        if (oldEntries != null && !oldEntries.isEmpty())
        {
            for (FinVoucherTemplateEntries oldEntry : oldEntries)
            {
                finVoucherTemplateEntriesAuxiliaryService.deleteByEntryId(oldEntry.getEntryId());
            }
            finVoucherTemplateEntriesService.deleteByTemplateId(finVoucherTemplate.getId());
        }

        // 新增分录和辅助核算
        List<FinVoucherTemplateEntries> entriesList = finVoucherTemplate.getEntries();
        if (entriesList != null && !entriesList.isEmpty())
        {
            for (FinVoucherTemplateEntries entry : entriesList)
            {
                entry.setGroupid(user.getGroupid());
                entry.setTemplateId(finVoucherTemplate.getId());
                entry.setCreatedTime(new Date());
            }
            finVoucherTemplateEntriesService.insertBatch(entriesList);

            List<FinVoucherTemplateEntriesAuxiliary> allAuxiliary = new ArrayList<>();
            for (FinVoucherTemplateEntries entry : entriesList)
            {
                if (entry.getAuxiliaryList() != null && !entry.getAuxiliaryList().isEmpty())
                {
                    for (FinVoucherTemplateEntriesAuxiliary aux : entry.getAuxiliaryList())
                    {
                        aux.setEntryId(entry.getEntryId());
                        aux.setGroupid(user.getGroupid());
                        allAuxiliary.add(aux);
                    }
                }
            }
            if (!allAuxiliary.isEmpty())
            {
                finVoucherTemplateEntriesAuxiliaryService.insertBatch(allAuxiliary);
            }
        }
        return toResult(1);
    }

    /**
     * 删除凭证模版
     */
    @DeleteMapping("/{ids}")
    @Transactional
    public Result remove(@PathVariable Long[] ids)
    {
        for (Long id : ids)
        {
            List<FinVoucherTemplateEntries> entriesList = finVoucherTemplateEntriesService.selectByTemplateId(id);
            if (entriesList != null && !entriesList.isEmpty())
            {
                for (FinVoucherTemplateEntries entry : entriesList)
                {
                    finVoucherTemplateEntriesAuxiliaryService.deleteByEntryId(entry.getEntryId());
                }
                finVoucherTemplateEntriesService.deleteByTemplateId(id);
            }
        }
        return toResult(finVoucherTemplateService.deleteFinVoucherTemplateByIds(ids));
    }
}
