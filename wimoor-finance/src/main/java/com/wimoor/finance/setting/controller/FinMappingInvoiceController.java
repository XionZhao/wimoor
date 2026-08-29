package com.wimoor.finance.setting.controller;

import com.wimoor.common.core.web.controller.BaseController;
import com.wimoor.common.core.web.domain.Result;
import com.wimoor.common.core.web.page.TableDataInfo;
import com.wimoor.common.mvc.BizException;
import com.wimoor.common.user.UserInfo;
import com.wimoor.common.user.UserInfoContext;
import com.wimoor.finance.setting.domain.FinMappingInvoice;
import com.wimoor.finance.setting.domain.FinAccountingSubjects;
import com.wimoor.finance.setting.service.IFinAccountingSubjectsService;
import com.wimoor.finance.setting.service.IFinMappingInvoiceService;
import com.wimoor.finance.setting.strategy.IInvoiceVoucherStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 发票凭证映射模版Controller
 *
 * @author wimoor
 * @date 2026-07-20
 */
@RestController
@RequestMapping("/closing_template_invoice")
public class FinMappingInvoiceController extends BaseController
{
    private static final Logger log = LoggerFactory.getLogger(FinMappingInvoiceController.class);

    @Autowired
    private IFinMappingInvoiceService finMappingInvoiceService;
    @Autowired
    private IFinAccountingSubjectsService finAccountingSubjectsService;
    @Autowired
    private IInvoiceVoucherStrategy invoiceVoucherStrategy;

    /**
     * 查询发票凭证映射模版列表
     */
    @GetMapping("/list")
    public TableDataInfo list(FinMappingInvoice query)
    {
        UserInfo user = UserInfoContext.get();
        if (query.getGroupid() == null || query.getGroupid().isEmpty()) {
            query.setGroupid(user.getCompanyid());
        }
        startPage();
        List<FinMappingInvoice> list = finMappingInvoiceService.selectFinMappingInvoiceList(query);
        // 填充科目名称
        for (FinMappingInvoice item : list) {
            fillSubjectNames(item);
        }
        return getDataTable(list);
    }

    /**
     * 查询发票凭证映射模版详情
     */
    @GetMapping("/{id}")
    public Result getInfo(@PathVariable("id") String id)
    {
        FinMappingInvoice record = finMappingInvoiceService.selectFinMappingInvoiceById(id);
        if (record != null) {
            fillSubjectNames(record);
        }
        return success(record);
    }

    /**
     * 新增发票凭证映射模版
     */
    @PostMapping
    public Result add(@RequestBody FinMappingInvoice record)
    {
        UserInfo user = UserInfoContext.get();
        if (record.getGroupid() == null || record.getGroupid().isEmpty()) {
            record.setGroupid(user.getCompanyid());
        }
        record.setCreateBy(user.getUserName());
        record.setCreatedTime(new Date());
        record.setUpdateBy(user.getUserName());
        record.setUpdatedTime(new Date());
        return toResult(finMappingInvoiceService.insertFinMappingInvoice(record));
    }

    /**
     * 修改发票凭证映射模版
     */
    @PutMapping
    public Result edit(@RequestBody FinMappingInvoice record)
    {
        UserInfo user = UserInfoContext.get();
        record.setUpdateBy(user.getUserName());
        record.setUpdatedTime(new Date());
        return toResult(finMappingInvoiceService.updateFinMappingInvoice(record));
    }

    /**
     * 删除发票凭证映射模版
     */
    @DeleteMapping("/{ids}")
    public Result remove(@PathVariable String[] ids)
    {
        List<String> idList = Arrays.asList(ids);
        return toResult(finMappingInvoiceService.deleteFinMappingInvoiceByIds(idList));
    }

    /**
     * 发票生成凭证（核心接口）
     * 委托给 IInvoiceVoucherStrategy 处理
     */
    @PostMapping("/generateVoucher")
    @Transactional
    public Result generateVoucher(@RequestBody Map<String, Object> params)
    {
        UserInfo user = UserInfoContext.get();
        String groupid = params.get("groupid") != null ? params.get("groupid").toString() : user.getCompanyid();

        // 解析发票ID列表
        @SuppressWarnings("unchecked")
        List<Object> invoiceIdObjects = (List<Object>) params.get("invoiceIds");
        if (invoiceIdObjects == null || invoiceIdObjects.isEmpty()) {
            return error("请选择需要生成凭证的发票");
        }
        List<Long> invoiceIds = new ArrayList<>();
        for (Object obj : invoiceIdObjects) {
            if (obj instanceof Number) {
                invoiceIds.add(((Number) obj).longValue());
            } else {
                invoiceIds.add(Long.parseLong(obj.toString()));
            }
        }

        String voucherType = (String) params.getOrDefault("voucherType", "1");
        String voucherDateStr = (String) params.get("voucherDate");
        String summary = (String) params.getOrDefault("summary", "发票入账");
        Integer voucherStatus = params.get("voucherStatus") != null ?
                Integer.parseInt(params.get("voucherStatus").toString()) : 1;
        Integer invoiceType = params.get("invoiceType") != null ?
                Integer.parseInt(params.get("invoiceType").toString()) : null;

        // 解析凭证日期
        Date voucherDate;
        if (voucherDateStr != null && !voucherDateStr.isEmpty()) {
            try {
                voucherDate = new SimpleDateFormat("yyyy-MM-dd").parse(voucherDateStr);
            } catch (Exception e) {
                return error("凭证日期格式不正确，请使用yyyy-MM-dd格式");
            }
        } else {
            voucherDate = new Date();
        }

        try {
            Map<String, Object> result = invoiceVoucherStrategy.generateVoucher(
                    user, groupid, invoiceIds, voucherType, voucherDate, summary, voucherStatus, invoiceType);

            Integer successCount = (Integer) result.get("successCount");
            if (successCount == 0) {
                @SuppressWarnings("unchecked")
                List<String> errors = (List<String>) result.get("errors");
                return error("生成凭证失败：" + (errors == null || errors.isEmpty() ? "未知错误" : String.join("；", errors)));
            }
            return success(result);
        } catch (BizException e) {
            return error(e.getMessage());
        }
    }

    /**
     * 填充科目名称
     */
    private void fillSubjectNames(FinMappingInvoice item)
    {
        if (item.getDebitSubjectId() != null && !item.getDebitSubjectId().isEmpty()) {
            try {
                FinAccountingSubjects subject = finAccountingSubjectsService.selectFinAccountingSubjectsBySubjectId(
                        item.getDebitSubjectId());
                if (subject != null) {
                    item.setDebitSubjectName(subject.getSubjectCode() + " " + subject.getSubjectName());
                }
            } catch (Exception e) {
                // 忽略
            }
        }
        if (item.getCreditSubjectId() != null && !item.getCreditSubjectId().isEmpty()) {
            try {
                FinAccountingSubjects subject = finAccountingSubjectsService.selectFinAccountingSubjectsBySubjectId(
                        item.getCreditSubjectId());
                if (subject != null) {
                    item.setCreditSubjectName(subject.getSubjectCode() + " " + subject.getSubjectName());
                }
            } catch (Exception e) {
                // 忽略
            }
        }
    }
}
