package com.wimoor.finance.api;

import com.wimoor.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * ERP远程服务调用接口
 * 用于调用ERP模块的费用类型、采购账户、台账等接口
 */
@Component
@FeignClient(value = "wimoor-erp", fallbackFactory = RemoteERPServiceFallback.class)
public interface RemoteERPService {

    // ==================== 费用类型和采购账户 ====================
    
    @GetMapping("/erp/api/v1/ledger/project/list")
    Result<List<Map<String, Object>>> getProject();

    @GetMapping("/erp/api/v1/ledger/account/all")
    Result<List<Map<String, Object>>> getAccountAll();

    // ==================== 采购账户台账 ====================
    
    @PostMapping("/erp/api/v1/ledger/purchase/list")
    Result<?> getPurchaseLedgerList(@RequestBody Map<String, Object> params);

    @GetMapping("/erp/api/v1/ledger/purchase/statistics")
    Result<?> getPurchaseLedgerStatistics(@RequestParam Map<String, Object> params);

    @GetMapping("/erp/api/v1/ledger/purchase/accounts")
    Result<?> getPurchaseLedgerAccounts(@RequestParam("groupid") String groupid);

    @GetMapping("/erp/api/v1/ledger/purchase/payments")
    Result<?> getPurchaseLedgerPayments(@RequestParam("entryId") String entryId);

    @PostMapping("/erp/api/v1/ledger/purchase/pay")
    Result<?> payPurchaseLedger(@RequestBody Map<String, Object> params);

    // ==================== 供应商台账 ====================
    
    @GetMapping("/erp/api/v1/ledger/supplier/summary")
    Result<?> getSupplierLedgerSummary(@RequestParam Map<String, Object> params);

    @GetMapping("/erp/api/v1/ledger/supplier/statistics")
    Result<?> getSupplierLedgerStatistics(@RequestParam Map<String, Object> params);

    @GetMapping("/erp/api/v1/ledger/supplier/orders")
    Result<?> getSupplierLedgerOrders(@RequestParam Map<String, Object> params);

    @GetMapping("/erp/api/v1/ledger/supplier/payments")
    Result<?> getSupplierLedgerPayments(@RequestParam Map<String, Object> params);

    // ==================== 进销存台账 ====================
    
    @GetMapping("/erp/api/v1/ledger/inventory/summary")
    Result<?> getInventoryLedgerSummary(@RequestParam Map<String, Object> params);

    @GetMapping("/erp/api/v1/ledger/inventory/detail")
    Result<?> getInventoryLedgerDetail(@RequestParam Map<String, Object> params);

    @GetMapping("/erp/api/v1/ledger/inventory/detail/count")
    Result<?> getInventoryLedgerDetailCount(@RequestParam Map<String, Object> params);

    @GetMapping("/erp/api/v1/ledger/inventory/export")
    Result<?> exportInventoryLedger(@RequestParam Map<String, Object> params);

    // ==================== 供应商查询 ====================
    
    @GetMapping("/erp/api/v1/ledger/supplier/list")
    Result<?> getSupplierList();

    // ==================== 承运商查询 ====================
    
    @GetMapping("/erp/api/v1/shipTransCompany/getCompanyList")
    Result<?> getTransCompanyList();

    // ==================== 仓库查询 ====================
    
    @GetMapping("/erp/api/v1/warehouse/list")
    Result<?> getWarehouseList();

    // ==================== 财务模块凭证生成专用接口 ====================

    @GetMapping("/erp/api/v1/ledger/purchase/completed-orders")
    Result<?> getCompletedOrdersForVoucher(@RequestParam("groupid") String groupid,
                                           @RequestParam("changedDate") String changedDate);

    @GetMapping("/erp/api/v1/ledger/purchase/completed-inventory")
    Result<?> getCompletedOrdersForInventory(@RequestParam("groupid") String groupid,
                                            @RequestParam("changedDate") String changedDate);

    @GetMapping("/erp/api/v1/ledger/purchase/orders-by-ids")
    Result<?> getOrdersByIds(@RequestParam("groupid") String groupid,
                              @RequestParam("orderIds") String orderIds);
}