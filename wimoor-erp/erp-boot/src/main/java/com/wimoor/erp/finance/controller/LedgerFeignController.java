package com.wimoor.erp.finance.controller;

import com.wimoor.common.result.Result;
import com.wimoor.common.user.UserInfo;
import com.wimoor.common.user.UserInfoContext;
import com.wimoor.erp.finance.service.IFaccountService;
import com.wimoor.erp.finance.service.IFinanceProjectService;
import com.wimoor.erp.purchase.service.IPurchaseFormPaymentService;
import com.wimoor.erp.purchase.service.IPurchaseFormService;
import com.wimoor.erp.ship.service.IShipTransCompanyService;
import io.swagger.annotations.Api;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 台账Feign接口
 * 供Finance模块通过Feign调用
 */
@Api(tags = "台账Feign接口")
@RestController
@RequestMapping("/api/v1/ledger")
@RequiredArgsConstructor
public class LedgerFeignController {

    final IFaccountService faccountService;
    final IFinanceProjectService financeProjectService;
    final IPurchaseFormService purchaseFormService;
    final IPurchaseFormPaymentService purchaseFormPaymentService;
    final IShipTransCompanyService shipTransCompanyService;

    // ==================== 采购账户台账 ====================
    
    /**
     * 采购订单列表（带统计字段）
     */
    @PostMapping("/purchase/list")
    public Result<?> getPurchaseLedgerList(@RequestBody Map<String, Object> params) {
        UserInfo userinfo = UserInfoContext.get();
        params.put("shopid", userinfo.getCompanyid());
        // 复用现有的采购订单查询
        return Result.success(purchaseFormService.getLedgerList(params));
    }

    /**
     * 采购订单统计（订单总额、已付总额、未付总额）
     */
    @GetMapping("/purchase/statistics")
    public Result<?> getPurchaseLedgerStatistics(@RequestParam Map<String, Object> params) {
        UserInfo userinfo = UserInfoContext.get();
        params.put("shopid", userinfo.getCompanyid());
        return Result.success(purchaseFormService.getLedgerStatistics(params));
    }

    /**
     * 采购账户余额列表
     */
    @GetMapping("/purchase/accounts")
    public Result<?> getPurchaseLedgerAccounts(@RequestParam("groupid") String groupid) {
        return Result.success(faccountService.findAccountAll(groupid));
    }

    /**
     * 订单付款明细
     */
    @GetMapping("/purchase/payments")
    public Result<?> getPurchaseLedgerPayments(@RequestParam("entryId") String entryId) {
        return Result.success(purchaseFormPaymentService.getPaymentsByEntryId(entryId));
    }

    /**
     * 采购台账付款操作
     */
    @PostMapping("/purchase/pay")
    public Result<?> payPurchaseLedger(@RequestBody Map<String, Object> params) {
        // 委托给采购服务处理付款
        try {
            purchaseFormService.payPurchaseOrder(params);
            return Result.success();
        } catch (Exception e) {
            return Result.failed("付款操作失败: " + e.getMessage());
        }
    }

    // ==================== 供应商台账 ====================
    
    /**
     * 供应商台账汇总（按供应商维度，参照采购统计补全付款/入库信息）
     * shopid为主账号隔离，groupid为公司隔离，两者同时生效
     */
    @GetMapping("/supplier/summary")
    public Result<?> getSupplierLedgerSummary(@RequestParam Map<String, Object> params) {
        UserInfo userinfo = UserInfoContext.get();
        params.put("shopid", userinfo.getCompanyid());
        List<Map<String, Object>> list = purchaseFormService.getSupplierLedgerSummary(params);
        // 附带合计行数据
        if (list != null && !list.isEmpty()) {
            Map<String, Object> total = purchaseFormService.getSupplierLedgerSummaryTotal(params);
            list.get(0).put("summary", total);
        }
        return Result.success(list);
    }

    /**
     * 供应商台账统计（全局汇总）
     * shopid为主账号隔离，groupid为公司隔离，两者同时生效
     */
    @GetMapping("/supplier/statistics")
    public Result<?> getSupplierLedgerStatistics(@RequestParam Map<String, Object> params) {
        UserInfo userinfo = UserInfoContext.get();
        params.put("shopid", userinfo.getCompanyid());
        return Result.success(purchaseFormService.getSupplierLedgerStatistics(params));
    }

    /**
     * 供应商订单明细
     * shopid为主账号隔离，groupid为公司隔离，两者同时生效
     */
    @GetMapping("/supplier/orders")
    public Result<?> getSupplierLedgerOrders(@RequestParam Map<String, Object> params) {
        UserInfo userinfo = UserInfoContext.get();
        params.put("shopid", userinfo.getCompanyid());
        parsePagingParams(params);
        List<Map<String, Object>> list = purchaseFormService.getSupplierOrders(params);
        int total = purchaseFormService.getSupplierOrdersCount(params);
        Result result = Result.success(list);
        result.setTotal(total);
        return result;
    }

    /**
     * 供应商付款明细
     * shopid为主账号隔离，groupid为公司隔离，两者同时生效
     */
    @GetMapping("/supplier/payments")
    public Result<?> getSupplierLedgerPayments(@RequestParam Map<String, Object> params) {
        UserInfo userinfo = UserInfoContext.get();
        params.put("shopid", userinfo.getCompanyid());
        parsePagingParams(params);
        List<Map<String, Object>> list = purchaseFormService.getSupplierPayments(params);
        int total = purchaseFormService.getSupplierPaymentsCount(params);
        Result result = Result.success(list);
        result.setTotal(total);
        return result;
    }

    // ==================== 进销存台账 ====================
    
    /**
     * 进销存台账汇总（按SKU+仓库维度）
     */
    @GetMapping("/inventory/summary")
    public Result<?> getInventoryLedgerSummary(@RequestParam Map<String, Object> params) {
        UserInfo userinfo = UserInfoContext.get();
        params.put("shopid", userinfo.getCompanyid());
        // 调用现有的库存查询
        return Result.success(faccountService.getInventoryLedgerSummary(params));
    }

    /**
     * 进销存台账明细（库存变动记录）
     */
    @GetMapping("/inventory/detail")
    public Result<?> getInventoryLedgerDetail(@RequestParam Map<String, Object> params) {
        UserInfo userinfo = UserInfoContext.get();
        params.put("shopid", userinfo.getCompanyid());
        // 将分页参数转为整数，避免MySQL LIMIT语法错误
        if (params.containsKey("pageSize")) {
            params.put("pageSize", Integer.parseInt(params.get("pageSize").toString()));
        }
        if (params.containsKey("offset")) {
            params.put("offset", Integer.parseInt(params.get("offset").toString()));
        }
        return Result.success(faccountService.getInventoryLedgerDetail(params));
    }

    /**
     * 进销存台账明细总数
     */
    @GetMapping("/inventory/detail/count")
    public Result<?> getInventoryLedgerDetailCount(@RequestParam Map<String, Object> params) {
        UserInfo userinfo = UserInfoContext.get();
        params.put("shopid", userinfo.getCompanyid());
        return Result.success(faccountService.getInventoryLedgerDetailCount(params));
    }

    /**
     * 进销存台账导出
     */
    @GetMapping("/inventory/export")
    public Result<?> exportInventoryLedger(@RequestParam Map<String, Object> params) {
        UserInfo userinfo = UserInfoContext.get();
        params.put("shopid", userinfo.getCompanyid());
        return Result.success(faccountService.getInventoryLedgerDetail(params));
    }

    // ==================== 费用类型和采购账户 ====================
    
    /**
     * 获取费用类型列表
     */
    @GetMapping("/project/list")
    public Result<?> getProject() {
        UserInfo userinfo = UserInfoContext.get();
        return Result.success(financeProjectService.findProject(userinfo.getCompanyid()));
    }

    /**
     * 获取采购账户列表
     */
    @GetMapping("/account/all")
    public Result<?> getAccountAll() {
        UserInfo userinfo = UserInfoContext.get();
        return Result.success(faccountService.findAccountAll(userinfo.getCompanyid()));
    }

    /**
     * 获取所有供应商列表
     */
    @GetMapping("/supplier/list")
    public Result<?> getSupplierList() {
        UserInfo userinfo = UserInfoContext.get();
        return Result.success(purchaseFormService.getSupplierList(userinfo.getCompanyid()));
    }

    // ==================== 财务模块凭证生成专用接口 ====================

    /**
     * 查询已完成的采购订单（用于财务模块凭证生成）
     * 每晚定时调用，获取closepaydate在当天发生变更的已完成订单
     * @param groupid 租户ID（账簿）
     * @param changedDate 变更日期（yyyy-MM-dd）
     */
    @GetMapping("/purchase/completed-orders")
    public Result<?> getCompletedOrdersForVoucher(@RequestParam("groupid") String groupid,
                                                   @RequestParam("changedDate") String changedDate) {
        return Result.success(purchaseFormService.getCompletedOrdersForVoucher(groupid, changedDate));
    }

    /**
     * 查询已入库的采购订单（用于库存凭证生成-入库验收）
     * @param groupid 租户ID（账簿）
     * @param changedDate 变更日期（yyyy-MM-dd），查询closerecdate在当天变更的订单
     */
    @GetMapping("/purchase/completed-inventory")
    public Result<?> getCompletedOrdersForInventory(@RequestParam("groupid") String groupid,
                                                     @RequestParam("changedDate") String changedDate) {
        return Result.success(purchaseFormService.getCompletedOrdersForInventory(groupid, changedDate));
    }

    /**
     * 将分页参数从String转为Integer，避免MySQL LIMIT/OFFSET语法错误
     */
    private void parsePagingParams(Map<String, Object> params) {
        if (params.containsKey("pageSize")) {
            params.put("pageSize", Integer.parseInt(params.get("pageSize").toString()));
        }
        if (params.containsKey("offset")) {
            params.put("offset", Integer.parseInt(params.get("offset").toString()));
        }
    }
}
