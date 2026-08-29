package com.wimoor.finance.ledger.controller;

import com.wimoor.common.core.web.controller.BaseController;
import com.wimoor.common.core.web.domain.Result;
import com.wimoor.common.core.web.page.TableDataInfo;
import com.wimoor.common.user.UserInfo;
import com.wimoor.common.user.UserInfoContext;
import com.wimoor.finance.ledger.domain.FinInvoice;
import com.wimoor.finance.ledger.domain.FinInvoiceDetail;
import com.wimoor.finance.ledger.domain.FinInvoiceExtension;
import com.wimoor.finance.ledger.service.IFinInvoiceLedgerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 发票台账Controller
 *
 * @author wimoor
 * @date 2025-11-04
 */
@RestController
@RequestMapping("/api/v1/invoice")
public class FinInvoiceLedgerController extends BaseController
{
    private static final Logger log = LoggerFactory.getLogger(FinInvoiceLedgerController.class);

    @Autowired
    private IFinInvoiceLedgerService finInvoiceLedgerService;

    /**
     * 查询发票列表
     */
    @GetMapping("/list")
    public TableDataInfo list(FinInvoice finInvoice)
    {
        startPage();
        List<FinInvoice> list = finInvoiceLedgerService.selectFinInvoiceList(finInvoice);
        return getDataTable(list);
    }

    /**
     * 统计发票信息
     */
    @GetMapping("/statistics")
    public Result statistics(FinInvoice finInvoice)
    {
        Map<String, Object> statistics = finInvoiceLedgerService.selectInvoiceStatistics(finInvoice);
        return success(statistics);
    }

    /**
     * 获取发票详情（含商品明细和扩展信息）
     */
    @GetMapping("/detail")
    public Result detail(@RequestParam("id") Long id)
    {
        FinInvoice invoice = finInvoiceLedgerService.selectFinInvoiceById(id);
        if (invoice == null) {
            return error("发票不存在");
        }
        Map<String, Object> result = new HashMap<>();
        result.put("invoice", invoice);
        result.put("details", finInvoiceLedgerService.selectInvoiceDetails(id));
        // 将扩展信息转换为Map格式，方便前端使用
        List<FinInvoiceExtension> extensions = finInvoiceLedgerService.selectInvoiceExtensions(id);
        Map<String, String> extensionMap = new HashMap<>();
        for (FinInvoiceExtension ext : extensions) {
            extensionMap.put(ext.getAttrKey(), ext.getAttrValue());
        }
        result.put("extensions", extensionMap);
        // 查询关联凭证信息
        if (invoice.getVoucherId() != null) {
            Map<String, Object> voucherInfo = finInvoiceLedgerService.selectVoucherInfo(invoice.getVoucherId());
            result.put("voucherInfo", voucherInfo);
        }
        return success(result);
    }

    /**
     * 查询发票关联的采购订单和付款记录
     */
    @GetMapping("/relations")
    public Result relations(@RequestParam("id") Long id)
    {
        return success(finInvoiceLedgerService.selectInvoiceRelations(id));
    }

    /**
     * 从税局API同步发票
     */
    @PostMapping("/sync")
    public Result sync(@RequestBody Map<String, Object> params)
    {
        UserInfo user = UserInfoContext.get();
        params.put("groupid", user.getCompanyid());
        int count = finInvoiceLedgerService.syncInvoices(params);
        return success("同步成功，共同步 " + count + " 条发票");
    }

    /**
     * 手动导入发票
     */
    @PostMapping("/import")
    public Result importInvoices(@RequestParam("file") MultipartFile file)
    {
        UserInfo user = UserInfoContext.get();
        String groupid = user.getCompanyid();
        int count = finInvoiceLedgerService.importInvoices(file, groupid);
        return success("导入成功，共导入 " + count + " 条发票");
    }

    /**
     * JSON批量导入发票（前端解析税控文件后调用）
     */
    @PostMapping("/importJson")
    public Result importInvoicesFromJson(@RequestBody Map<String, Object> requestData)
    {
        UserInfo user = UserInfoContext.get();
        // 从前端获取groupid
        String groupid = requestData.get("groupid") != null ? requestData.get("groupid").toString() : null;
        if (groupid == null || groupid.isEmpty()) {
            return error("账套ID不能为空");
        }
        // 获取按页签分组的发票数据
        Object sheetsObj = requestData.get("sheets");
        if (sheetsObj == null) {
            return error("发票数据不能为空");
        }
        @SuppressWarnings("unchecked")
        Map<String, List<Map<String, Object>>> sheets = (Map<String, List<Map<String, Object>>>) sheetsObj;
        if (sheets.isEmpty()) {
            return error("发票数据不能为空");
        }
        String msg = finInvoiceLedgerService.importInvoicesFromJson(sheets, groupid, user.getUserName());
        return success(msg);
    }

    /**
     * 发票入账（生成凭证）
     */
    @PostMapping("/posting")
    public Result posting(@RequestBody List<Long> ids)
    {
        if (ids == null || ids.isEmpty()) {
            return error("请选择需要入账的发票");
        }
        int count = finInvoiceLedgerService.postingInvoices(ids);
        return success("入账成功，共处理 " + count + " 条发票");
    }

    /**
     * 新增发票
     */
    @PostMapping("/create")
    public Result create(@RequestBody FinInvoice finInvoice)
    {
        UserInfo user = UserInfoContext.get();
        finInvoice.setCreatedBy(user.getUserName());
        finInvoice.setSource("MANUAL");
        finInvoice.setPostingStatus(0);
        int count = finInvoiceLedgerService.insertFinInvoice(finInvoice);
        if (count > 0) {
            return success("新增成功");
        }
        return error("新增失败");
    }

    /**
     * 新增发票明细行
     */
    @PostMapping("/detail/create")
    public Result createDetail(@RequestBody FinInvoiceDetail detail)
    {
        if (detail.getInvoiceId() == null) {
            return error("发票ID不能为空");
        }
        int count = finInvoiceLedgerService.insertInvoiceDetail(detail);
        if (count > 0) {
            return success("新增成功");
        }
        return error("新增失败");
    }

    /**
     * 批量匹配发票的supplier_id（修复历史数据：通过seller_name匹配供应商）
     */
    @PostMapping("/matchSupplier")
    public Result matchSupplier()
    {
        Map<String, Object> result = finInvoiceLedgerService.batchMatchSupplierId();
        String msg = result.get("msg") != null ? result.get("msg").toString() : "匹配完成";
        return Result.success(msg, result);
    }

    /**
     * 批量匹配发票的carrier_id（修复历史数据：通过seller_name匹配承运商）
     */
    @PostMapping("/matchCarrier")
    public Result matchCarrier()
    {
        Map<String, Object> result = finInvoiceLedgerService.batchMatchCarrierId();
        String msg = result.get("msg") != null ? result.get("msg").toString() : "匹配完成";
        return Result.success(msg, result);
    }
}
