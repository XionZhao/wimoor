package com.wimoor.finance.ledger.controller;

import com.wimoor.common.core.utils.poi.ExcelUtil;
import com.wimoor.common.core.web.controller.BaseController;
import com.wimoor.common.result.Result;
import com.wimoor.common.user.UserInfo;
import com.wimoor.common.user.UserInfoContext;
import com.wimoor.finance.ledger.domain.FinSupplierReconcileRecord;
import com.wimoor.finance.ledger.domain.dto.SupplierLedgerQueryDTO;
import com.wimoor.finance.ledger.service.IFinSupplierLedgerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 供应商台账Controller
 * 采购相关数据通过Feign调用ERP模块，发票/对账数据使用本地
 *
 * @author wimoor
 */
@RestController
@RequestMapping("/api/v1/supplier/ledger")
public class FinSupplierLedgerController extends BaseController {

    @Autowired
    private IFinSupplierLedgerService finSupplierLedgerService;

    /**
     * 查询供应商台账汇总（按供应商维度，参照采购统计补全付款/入库信息）
     */
    @GetMapping("/summary")
    public Result summary(SupplierLedgerQueryDTO query) {
        UserInfo userInfo = UserInfoContext.get();
        setDefaultQueryParams(query, userInfo);
        List<Map<String, Object>> list = finSupplierLedgerService.getSupplierLedgerSummary(query);
        Result result = Result.success(list);
        result.setTotal(list != null ? list.size() : 0);
        return result;
    }

    /**
     * 查询供应商台账统计数据（全局汇总）
     */
    @GetMapping("/statistics")
    public Result statistics(SupplierLedgerQueryDTO query) {
        UserInfo userInfo = UserInfoContext.get();
        setDefaultQueryParams(query, userInfo);
        Map<String, Object> statistics = finSupplierLedgerService.getSupplierLedgerStatistics(query);
        return Result.success(statistics);
    }

    /**
     * 查询供应商订单明细
     */
    @GetMapping("/orders")
    public Result orders(SupplierLedgerQueryDTO query) {
        UserInfo userInfo = UserInfoContext.get();
        setDefaultQueryParams(query, userInfo);
        List<Map<String, Object>> list = finSupplierLedgerService.getSupplierLedgerOrders(query);
        int total = finSupplierLedgerService.getSupplierLedgerOrdersCount(query);
        Result result = Result.success(list);
        result.setTotal(total);
        return result;
    }

    /**
     * 查询供应商付款明细
     */
    @GetMapping("/payments")
    public Result payments(SupplierLedgerQueryDTO query) {
        UserInfo userInfo = UserInfoContext.get();
        setDefaultQueryParams(query, userInfo);
        List<Map<String, Object>> list = finSupplierLedgerService.getSupplierLedgerPayments(query);
        int total = finSupplierLedgerService.getSupplierLedgerPaymentsCount(query);
        Result result = Result.success(list);
        result.setTotal(total);
        return result;
    }

    /**
     * 查询供应商发票明细
     */
    @GetMapping("/invoices")
    public Result invoices(SupplierLedgerQueryDTO query) {
        UserInfo userInfo = UserInfoContext.get();
        setDefaultQueryParams(query, userInfo);
        List<Map<String, Object>> list = finSupplierLedgerService.getSupplierLedgerInvoices(query);
        Result result = Result.success(list);
        result.setTotal(list != null ? list.size() : 0);
        return result;
    }

    /**
     * 对账操作（标记供应商已对账，保存对账记录）
     */
    @SuppressWarnings("unchecked")
    @PostMapping("/reconcile")
    public Result reconcile(@RequestBody Map<String, Object> params) {
        UserInfo userInfo = UserInfoContext.get();
        String groupid = params.get("groupid") != null ? params.get("groupid").toString() : null;
        String supplierId = params.get("supplierId") != null ? params.get("supplierId").toString() : null;
        if (groupid == null || supplierId == null) {
            return Result.failed("参数不完整，groupid和supplierId不能为空");
        }
        // 提取对账汇总数据
        Map<String, Object> reconcileData = new HashMap<>();
        reconcileData.put("companyName", params.get("companyName"));
        reconcileData.put("reconcileMonth", params.get("reconcileMonth"));
        reconcileData.put("orderCount", params.get("orderCount"));
        reconcileData.put("totalOrderAmount", params.get("totalOrderAmount"));
        reconcileData.put("totalReceived", params.get("totalReceived"));
        reconcileData.put("totalPaidAmount", params.get("totalPaidAmount"));
        reconcileData.put("totalUnpaidAmount", params.get("totalUnpaidAmount"));
        reconcileData.put("totalInvoicedAmount", params.get("totalInvoicedAmount"));
        reconcileData.put("totalUninvoicedAmount", params.get("totalUninvoicedAmount"));
        boolean result = finSupplierLedgerService.reconcileSupplier(groupid, supplierId, userInfo.getId(), reconcileData);
        return result ? Result.success("对账成功") : Result.failed("对账失败");
    }

    /**
     * 查询对账详情（点击最后对账日期弹窗展示）
     */
    @GetMapping("/reconcile/detail")
    public Result reconcileDetail(@RequestParam String groupid,
                                   @RequestParam String supplierId,
                                   @RequestParam(required = false) String reconcileMonth) {
        FinSupplierReconcileRecord record = finSupplierLedgerService.getReconcileDetail(groupid, supplierId, reconcileMonth);
        return Result.success(record);
    }

    /**
     * 导出未开票订单
     */
    @GetMapping("/export")
    public void export(HttpServletResponse response, SupplierLedgerQueryDTO query) {
        UserInfo userInfo = UserInfoContext.get();
        setDefaultQueryParams(query, userInfo);
        List<Map<String, Object>> list = finSupplierLedgerService.getUninvoicedOrders(query);
        ExcelUtil util = new ExcelUtil(Map.class);
        util.exportExcel(response, list, "未开票订单数据");
    }

    /**
     * 设置默认查询参数（groupid完全由前端传入，财务模块的groupid是公司/租户概念）
     */
    private void setDefaultQueryParams(SupplierLedgerQueryDTO query, UserInfo userInfo) {
        // groupid完全由前端传入，这里不做任何处理
        // 财务模块的groupid是公司/租户概念，和ERP的店铺概念不同
    }
}
