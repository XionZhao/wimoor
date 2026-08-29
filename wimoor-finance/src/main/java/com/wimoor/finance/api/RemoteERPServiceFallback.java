package com.wimoor.finance.api;

import com.wimoor.common.result.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ERP远程服务调用降级处理
 */
@Component
public class RemoteERPServiceFallback implements FallbackFactory<RemoteERPService> {

    private static final Logger log = LoggerFactory.getLogger(RemoteERPServiceFallback.class);

    @Override
    public RemoteERPService create(Throwable cause) {
        log.error("ERP远程服务调用失败: {}", cause.getMessage());
        return new RemoteERPService() {
            
            @Override
            public Result<List<Map<String, Object>>> getProject() {
                log.error("获取费用类型列表失败: {}", cause.getMessage());
                return Result.success(new ArrayList<>());
            }

            @Override
            public Result<List<Map<String, Object>>> getAccountAll() {
                log.error("获取采购账户列表失败: {}", cause.getMessage());
                return Result.success(new ArrayList<>());
            }

            @Override
            public Result<?> getPurchaseLedgerList(Map<String, Object> params) {
                log.error("获取采购台账列表失败: {}", cause.getMessage());
                return Result.success(new HashMap<>());
            }

            @Override
            public Result<?> getPurchaseLedgerStatistics(Map<String, Object> params) {
                log.error("获取采购台账统计失败: {}", cause.getMessage());
                return Result.success(new HashMap<>());
            }

            @Override
            public Result<?> getPurchaseLedgerAccounts(String groupid) {
                log.error("获取采购账户余额失败: {}", cause.getMessage());
                return Result.success(new ArrayList<>());
            }

            @Override
            public Result<?> getPurchaseLedgerPayments(String entryId) {
                log.error("获取付款明细失败: {}", cause.getMessage());
                return Result.success(new ArrayList<>());
            }

            @Override
            public Result<?> payPurchaseLedger(Map<String, Object> params) {
                log.error("采购台账付款操作失败: {}", cause.getMessage());
                return Result.failed("付款操作失败，请稍后重试");
            }

            @Override
            public Result<?> getSupplierLedgerSummary(Map<String, Object> params) {
                log.error("获取供应商台账汇总失败: {}", cause.getMessage());
                return Result.success(new ArrayList<>());
            }

            @Override
            public Result<?> getSupplierLedgerStatistics(Map<String, Object> params) {
                log.error("获取供应商台账统计失败: {}", cause.getMessage());
                return Result.success(new HashMap<>());
            }

            @Override
            public Result<?> getSupplierLedgerOrders(Map<String, Object> params) {
                log.error("获取供应商订单明细失败: {}", cause.getMessage());
                return Result.success(new ArrayList<>());
            }

            @Override
            public Result<?> getSupplierLedgerPayments(Map<String, Object> params) {
                log.error("获取供应商付款明细失败: {}", cause.getMessage());
                return Result.success(new ArrayList<>());
            }

            @Override
            public Result<?> getInventoryLedgerSummary(Map<String, Object> params) {
                log.error("获取进销存台账汇总失败: {}", cause.getMessage());
                return Result.success(new ArrayList<>());
            }

            @Override
            public Result<?> getInventoryLedgerDetail(Map<String, Object> params) {
                log.error("获取进销存台账明细失败: {}", cause.getMessage());
                return Result.success(new ArrayList<>());
            }

            @Override
            public Result<?> getInventoryLedgerDetailCount(Map<String, Object> params) {
                log.error("获取进销存台账明细总数失败: {}", cause.getMessage());
                return Result.success(0L);
            }

            @Override
            public Result<?> exportInventoryLedger(Map<String, Object> params) {
                log.error("导出进销存台账失败: {}", cause.getMessage());
                return Result.failed("导出失败，请稍后重试");
            }

            @Override
            public Result<?> getSupplierList() {
                log.error("获取供应商列表失败: {}", cause.getMessage());
                return Result.success(new ArrayList<>());
            }

            @Override
            public Result<?> getTransCompanyList() {
                log.error("获取承运商列表失败: {}", cause.getMessage());
                return Result.success(new ArrayList<>());
            }

            @Override
            public Result<?> getWarehouseList() {
                log.error("获取仓库列表失败: {}", cause.getMessage());
                return Result.success(new ArrayList<>());
            }

            @Override
            public Result<?> getCompletedOrdersForVoucher(String groupid, String changedDate) {
                log.error("获取已完成订单失败: {}", cause.getMessage());
                return Result.success(new ArrayList<>());
            }

            @Override
            public Result<?> getCompletedOrdersForInventory(String groupid, String changedDate) {
                log.error("获取已入库订单失败: {}", cause.getMessage());
                return Result.success(new ArrayList<>());
            }

            @Override
            public Result<?> getOrdersByIds(String groupid, String orderIds) {
                log.error("按订单ID查询订单失败: {}", cause.getMessage());
                return Result.success(new ArrayList<>());
            }
        };
    }
}