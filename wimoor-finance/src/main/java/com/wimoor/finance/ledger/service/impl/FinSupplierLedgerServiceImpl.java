package com.wimoor.finance.ledger.service.impl;

import com.wimoor.common.result.Result;
import com.wimoor.finance.api.RemoteERPService;
import com.wimoor.finance.ledger.domain.FinSupplierReconcileRecord;
import com.wimoor.finance.ledger.domain.dto.SupplierLedgerQueryDTO;
import com.wimoor.finance.ledger.mapper.FinSupplierLedgerMapper;
import com.wimoor.finance.ledger.service.IFinSupplierLedgerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 供应商台账Service业务层处理
 * 采购相关数据通过Feign调用ERP模块，发票/对账数据使用本地Mapper
 *
 * @author wimoor
 */
@Slf4j
@Service
public class FinSupplierLedgerServiceImpl implements IFinSupplierLedgerService {

    @Autowired
    private RemoteERPService remoteERPService;

    @Autowired
    private FinSupplierLedgerMapper finSupplierLedgerMapper;

    /**
     * 查询供应商台账汇总（按供应商维度），并补充发票数据
     */
    @Override
    public List<Map<String, Object>> getSupplierLedgerSummary(SupplierLedgerQueryDTO query) {
        Map<String, Object> params = buildParams(query);
        Result<?> result = remoteERPService.getSupplierLedgerSummary(params);
        List<Map<String, Object>> list = new ArrayList<>();
        if (Result.isSuccess(result) && result.getData() instanceof List) {
            list = (List<Map<String, Object>>) result.getData();
        }
        // 补充发票数据
        enrichInvoiceData(list, query);
        // 根据对账状态过滤
        Integer reconcileStatus = query.getReconcileStatus();
        if (reconcileStatus != null && !list.isEmpty()) {
            list.removeIf(row -> {
                Object status = row.get("reconcileStatus");
                return status == null || !status.equals(reconcileStatus);
            });
        }
        return list;
    }

    /**
     * 查询供应商台账统计数据（全局汇总），并补充发票统计
     */
    @Override
    public Map<String, Object> getSupplierLedgerStatistics(SupplierLedgerQueryDTO query) {
        Map<String, Object> params = buildParams(query);
        Result<?> result = remoteERPService.getSupplierLedgerStatistics(params);
        Map<String, Object> statistics = new HashMap<>();
        if (Result.isSuccess(result) && result.getData() instanceof Map) {
            statistics = (Map<String, Object>) result.getData();
        }
        // 补充发票统计数据
        enrichInvoiceStatistics(statistics, query);
        return statistics;
    }

    /**
     * 查询供应商台账合计行（已在ERP模块的summary接口中附带）
     */
    @Override
    public Map<String, Object> getSupplierLedgerSummaryTotal(SupplierLedgerQueryDTO query) {
        return new HashMap<>();
    }

    /**
     * 查询供应商订单明细
     */
    @Override
    public List<Map<String, Object>> getSupplierLedgerOrders(SupplierLedgerQueryDTO query) {
        Map<String, Object> params = buildParams(query);
        Result<?> result = remoteERPService.getSupplierLedgerOrders(params);
        if (Result.isSuccess(result) && result.getData() instanceof List) {
            return (List<Map<String, Object>>) result.getData();
        }
        return new ArrayList<>();
    }

    /**
     * 查询供应商付款明细
     */
    @Override
    public List<Map<String, Object>> getSupplierLedgerPayments(SupplierLedgerQueryDTO query) {
        Map<String, Object> params = buildParams(query);
        Result<?> result = remoteERPService.getSupplierLedgerPayments(params);
        if (Result.isSuccess(result) && result.getData() instanceof List) {
            return (List<Map<String, Object>>) result.getData();
        }
        return new ArrayList<>();
    }

    @Override
    public int getSupplierLedgerOrdersCount(SupplierLedgerQueryDTO query) {
        Map<String, Object> params = buildParams(query);
        Result<?> result = remoteERPService.getSupplierLedgerOrders(params);
        if (result != null && result.getTotal() != null) {
            return result.getTotal();
        }
        return 0;
    }

    @Override
    public int getSupplierLedgerPaymentsCount(SupplierLedgerQueryDTO query) {
        Map<String, Object> params = buildParams(query);
        Result<?> result = remoteERPService.getSupplierLedgerPayments(params);
        if (result != null && result.getTotal() != null) {
            return result.getTotal();
        }
        return 0;
    }

    /**
     * 查询供应商发票明细（只显示匹配到供应商的发票）
     */
    @Override
    public List<Map<String, Object>> getSupplierLedgerInvoices(SupplierLedgerQueryDTO query) {
        // 获取groupid
        String groupid = query.getGroupid();
        if (groupid == null || groupid.isEmpty()) {
            log.warn("getSupplierLedgerInvoices: groupid为空，跳过查询");
            return new ArrayList<>();
        }
        
        // 如果有supplierName，直接查询
        if (query.getSupplierName() != null && !query.getSupplierName().isEmpty()) {
            log.info("getSupplierLedgerInvoices: 按supplierName查询，supplierName={}", query.getSupplierName());
            return finSupplierLedgerMapper.selectSupplierLedgerInvoices(query);
        }
        
        // 否则，获取供应商列表，只查询匹配到的发票
        try {
            Result<?> supplierResult = remoteERPService.getSupplierList();
            if (!Result.isSuccess(supplierResult) || !(supplierResult.getData() instanceof List)) {
                log.warn("getSupplierLedgerInvoices: 获取ERP供应商列表失败");
                return new ArrayList<>();
            }
            List<Map<String, Object>> supplierList = (List<Map<String, Object>>) supplierResult.getData();
            log.info("getSupplierLedgerInvoices: 获取ERP供应商数量={}", supplierList.size());
            
            // 收集所有销方名称（供应商名称 + 账户名称）
            List<String> allSellerNames = new ArrayList<>();
            for (Map<String, Object> supplier : supplierList) {
                Object nameObj = supplier.get("name");
                if (nameObj != null) {
                    allSellerNames.add(nameObj.toString().trim());
                }
                Object accountNamesObj = supplier.get("accountNames");
                if (accountNamesObj != null && !accountNamesObj.toString().isEmpty()) {
                    String[] accountNames = accountNamesObj.toString().split(",");
                    for (String accountName : accountNames) {
                        String trimmedName = accountName.trim();
                        if (!trimmedName.isEmpty()) {
                            allSellerNames.add(trimmedName);
                        }
                    }
                }
            }
            
            log.info("getSupplierLedgerInvoices: 收集到销方名称数量={}", allSellerNames.size());
            if (allSellerNames.isEmpty()) {
                log.warn("getSupplierLedgerInvoices: 没有销方名称，返回空");
                return new ArrayList<>();
            }
            
            // 设置sellerNames参数，用于SQL查询
            query.setSellerNames(allSellerNames);
            List<Map<String, Object>> result = finSupplierLedgerMapper.selectSupplierLedgerInvoicesByNames(query);
            log.info("getSupplierLedgerInvoices: 查询到发票数量={}", result.size());
            return result;
        } catch (Exception e) {
            log.error("查询供应商发票明细失败", e);
            return new ArrayList<>();
        }
    }

    /**
     * 对账操作（标记供应商已对账，按销方名称匹配，保存对账记录）
     */
    @Override
    public boolean reconcileSupplier(String groupid, String supplierId, String operator,
                                      Map<String, Object> reconcileData) {
        if (groupid == null || supplierId == null) {
            log.warn("对账参数不完整：groupid={}, supplierId={}", groupid, supplierId);
            return false;
        }
        
        log.info("开始对账：groupid={}, supplierId={}", groupid, supplierId);
        
        // 从ERP获取供应商列表，找到对应供应商的名称和账户名称
        try {
            Result<?> supplierResult = remoteERPService.getSupplierList();
            if (!Result.isSuccess(supplierResult) || !(supplierResult.getData() instanceof List)) {
                log.warn("获取ERP供应商列表失败");
                return false;
            }
            List<Map<String, Object>> supplierList = (List<Map<String, Object>>) supplierResult.getData();
            log.info("获取ERP供应商数量：{}", supplierList.size());
            
            // 找到目标供应商，收集其名称和账户名称
            List<String> sellerNames = new ArrayList<>();
            String supplierName = null;
            String companyName = null;
            for (Map<String, Object> supplier : supplierList) {
                Object idObj = supplier.get("id");
                if (idObj != null && idObj.toString().equals(supplierId)) {
                    Object nameObj = supplier.get("name");
                    if (nameObj != null) {
                        supplierName = nameObj.toString().trim();
                        sellerNames.add(supplierName);
                    }
                    // 获取公司名
                    Object companyObj = supplier.get("company");
                    if (companyObj == null) {
                        companyObj = supplier.get("companyName");
                    }
                    if (companyObj != null) {
                        companyName = companyObj.toString().trim();
                    }
                    Object accountNamesObj = supplier.get("accountNames");
                    if (accountNamesObj != null && !accountNamesObj.toString().isEmpty()) {
                        String[] accountNames = accountNamesObj.toString().split(",");
                        for (String accountName : accountNames) {
                            String trimmedName = accountName.trim();
                            if (!trimmedName.isEmpty()) {
                                sellerNames.add(trimmedName);
                            }
                        }
                    }
                    break;
                }
            }
            
            log.info("找到供应商的销方名称：{}，公司名：{}", sellerNames, companyName);
            if (sellerNames.isEmpty()) {
                log.warn("未找到供应商的销方名称，supplierId={}", supplierId);
                return false;
            }
            
            // 更新发票对账状态
            int rows = finSupplierLedgerMapper.updateReconcileStatusByNames(groupid, sellerNames, operator);
            log.info("对账更新行数：{}", rows);
            
            // 保存对账记录到数据库
            if (reconcileData != null) {
                String reconcileMonth = reconcileData.get("reconcileMonth") != null ? reconcileData.get("reconcileMonth").toString() : null;
                
                // 先查询是否已存在该月份的对账记录
                FinSupplierReconcileRecord existingRecord = null;
                if (reconcileMonth != null && !reconcileMonth.isEmpty()) {
                    existingRecord = finSupplierLedgerMapper.selectReconcileRecord(groupid, supplierId, reconcileMonth);
                }
                
                FinSupplierReconcileRecord record = existingRecord != null ? existingRecord : new FinSupplierReconcileRecord();
                record.setGroupid(groupid);
                record.setSupplierId(supplierId);
                record.setSupplierName(supplierName);
                // 优先使用前端传入的公司名，如果没有则使用ERP中的公司名
                String finalCompanyName = reconcileData.get("companyName") != null && !reconcileData.get("companyName").toString().isEmpty() 
                    ? reconcileData.get("companyName").toString() : companyName;
                record.setCompanyName(finalCompanyName);
                record.setReconcileMonth(reconcileMonth);
                record.setOrderCount(reconcileData.get("orderCount") != null ? Integer.parseInt(reconcileData.get("orderCount").toString()) : 0);
                record.setTotalOrderAmount(reconcileData.get("totalOrderAmount") != null ? new BigDecimal(reconcileData.get("totalOrderAmount").toString()) : BigDecimal.ZERO);
                record.setTotalReceived(reconcileData.get("totalReceived") != null ? Integer.parseInt(reconcileData.get("totalReceived").toString()) : 0);
                record.setTotalPaidAmount(reconcileData.get("totalPaidAmount") != null ? new BigDecimal(reconcileData.get("totalPaidAmount").toString()) : BigDecimal.ZERO);
                record.setTotalUnpaidAmount(reconcileData.get("totalUnpaidAmount") != null ? new BigDecimal(reconcileData.get("totalUnpaidAmount").toString()) : BigDecimal.ZERO);
                record.setTotalInvoicedAmount(reconcileData.get("totalInvoicedAmount") != null ? new BigDecimal(reconcileData.get("totalInvoicedAmount").toString()) : BigDecimal.ZERO);
                record.setTotalUninvoicedAmount(reconcileData.get("totalUninvoicedAmount") != null ? new BigDecimal(reconcileData.get("totalUninvoicedAmount").toString()) : BigDecimal.ZERO);
                record.setReconcileBy(operator);
                record.setReconcileTime(new Date());
                
                if (existingRecord != null) {
                    // 更新已有记录（支持反复对账覆盖）
                    log.info("更新对账记录，recordId={}, 新时间={}", record.getId(), record.getReconcileTime());
                    int updateRows = finSupplierLedgerMapper.updateReconcileRecord(record);
                    log.info("对账记录更新影响行数：{}", updateRows);
                    if (updateRows <= 0) {
                        log.error("对账记录更新失败，recordId={}，影响行数为0", record.getId());
                        return false;
                    }
                } else {
                    // 插入新记录
                    int insertRows = finSupplierLedgerMapper.insertReconcileRecord(record);
                    log.info("对账记录插入结果，recordId={}, 影响行数={}", record.getId(), insertRows);
                    if (insertRows <= 0) {
                        log.error("对账记录插入失败，supplierId={}, reconcileMonth={}", supplierId, reconcileMonth);
                        return false;
                    }
                }
                return true;
            }
            
            return rows > 0;
        } catch (Exception e) {
            log.error("对账操作失败", e);
            return false;
        }
    }

    /**
     * 查询对账详情（点击最后对账日期弹窗展示）
     */
    @Override
    public FinSupplierReconcileRecord getReconcileDetail(String groupid, String supplierId, String reconcileMonth) {
        if (groupid == null || supplierId == null) {
            log.warn("查询对账详情参数不完整：groupid={}, supplierId={}", groupid, supplierId);
            return null;
        }
        try {
            FinSupplierReconcileRecord record = null;
            if (reconcileMonth != null && !reconcileMonth.isEmpty()) {
                record = finSupplierLedgerMapper.selectReconcileRecord(groupid, supplierId, reconcileMonth);
            }
            // 如果指定月份没查到记录，回退到查询最新记录
            if (record == null) {
                record = finSupplierLedgerMapper.selectLatestReconcileRecord(groupid, supplierId);
            }
            
            // 如果记录存在但没有公司名，尝试从ERP获取
            if (record != null && (record.getCompanyName() == null || record.getCompanyName().isEmpty())) {
                try {
                    Result<?> supplierResult = remoteERPService.getSupplierList();
                    if (Result.isSuccess(supplierResult) && supplierResult.getData() instanceof List) {
                        List<Map<String, Object>> supplierList = (List<Map<String, Object>>) supplierResult.getData();
                        for (Map<String, Object> supplier : supplierList) {
                            Object idObj = supplier.get("id");
                            if (idObj != null && idObj.toString().equals(supplierId)) {
                                Object companyObj = supplier.get("company");
                                if (companyObj == null) {
                                    companyObj = supplier.get("companyName");
                                }
                                if (companyObj != null) {
                                    record.setCompanyName(companyObj.toString().trim());
                                }
                                break;
                            }
                        }
                    }
                } catch (Exception e) {
                    log.warn("从ERP获取公司名失败", e);
                }
            }
            
            return record;
        } catch (Exception e) {
            log.error("查询对账详情失败", e);
            return null;
        }
    }

    /**
     * 查询未开票订单（用于导出，暂返回空列表）
     */
    @Override
    public List<Map<String, Object>> getUninvoicedOrders(SupplierLedgerQueryDTO query) {
        // TODO: 实现未开票订单查询
        return new ArrayList<>();
    }

    /**
     * 补充发票数据到供应商台账汇总（按销方名称+groupid直接查询）
     */
    private void enrichInvoiceData(List<Map<String, Object>> list, SupplierLedgerQueryDTO query) {
        if (list == null || list.isEmpty()) {
            return;
        }
        // groupid必须提供（财务数据隔离）
        String groupid = query.getGroupid();
        if (groupid == null || groupid.isEmpty()) {
            log.warn("未提供groupid，跳过发票数据补充");
            return;
        }
        
        // 日期必须提供
        if (query.getStartDate() == null || query.getEndDate() == null) {
            log.warn("未提供查询日期，跳过发票数据补充");
            return;
        }
        String startDate = new SimpleDateFormat("yyyy-MM-dd").format(query.getStartDate());
        String endDate = new SimpleDateFormat("yyyy-MM-dd").format(query.getEndDate());
        
        try {
            // 从ERP获取供应商列表（包含账户名称）
            Result<?> supplierResult = remoteERPService.getSupplierList();
            if (!Result.isSuccess(supplierResult) || !(supplierResult.getData() instanceof List)) {
                log.warn("获取供应商列表失败，跳过发票数据补充");
                return;
            }
            List<Map<String, Object>> supplierList = (List<Map<String, Object>>) supplierResult.getData();
            log.info("ERP供应商数量：{}", supplierList.size());
            
            // 构建映射：sellerName -> supplierId，以及收集所有销方名称
            Map<String, String> nameToSupplierIdMap = new HashMap<>();
            List<String> allSellerNames = new ArrayList<>();
            
            for (Map<String, Object> supplier : supplierList) {
                Object idObj = supplier.get("id");
                Object nameObj = supplier.get("name");
                if (idObj == null || nameObj == null) continue;
                
                String supplierId = idObj.toString();
                String supplierName = nameObj.toString().trim();
                
                // 映射供应商名称
                nameToSupplierIdMap.put(supplierName, supplierId);
                allSellerNames.add(supplierName);
                
                // 映射账户名称
                Object accountNamesObj = supplier.get("accountNames");
                if (accountNamesObj != null && !accountNamesObj.toString().isEmpty()) {
                    String[] accountNames = accountNamesObj.toString().split(",");
                    for (String accountName : accountNames) {
                        String trimmedName = accountName.trim();
                        if (!trimmedName.isEmpty()) {
                            nameToSupplierIdMap.put(trimmedName, supplierId);
                            allSellerNames.add(trimmedName);
                        }
                    }
                }
            }
            log.info("销方名称总数：{}，供应商数量：{}", allSellerNames.size(), supplierList.size());
            
            if (allSellerNames.isEmpty()) {
                log.warn("无销方名称，跳过发票查询");
                return;
            }
            
            // 直接按名称+groupid+日期查询发票汇总
            List<Map<String, Object>> invoiceData = finSupplierLedgerMapper.selectInvoiceAmountByNames(
                    groupid, allSellerNames, startDate, endDate);
            log.info("发票汇总数据数量：{}", invoiceData.size());
            
            // 构建 supplierId -> 发票金额 的映射
            Map<String, Double> supplierInvoiceMap = new HashMap<>();
            for (Map<String, Object> row : invoiceData) {
                Object sellerName = row.get("sellerName");
                Object amt = row.get("totalInvoicedAmount");
                if (sellerName != null) {
                    String sellerNameStr = sellerName.toString().trim();
                    String supplierId = nameToSupplierIdMap.get(sellerNameStr);
                    if (supplierId != null) {
                        double amount = amt != null ? ((Number) amt).doubleValue() : 0;
                        supplierInvoiceMap.merge(supplierId, amount, Double::sum);
                    }
                }
            }
            log.info("匹配到的供应商发票数量：{}", supplierInvoiceMap.size());
            
            // 计算当前查询月份（用于判断对账状态）
            String queryMonth = startDate.substring(0, 7); // yyyy-MM
            
            // 补充到汇总数据中
            for (Map<String, Object> row : list) {
                Object sidObj = row.get("supplierId");
                if (sidObj != null) {
                    String supplierId = sidObj.toString();
                    // 补充发票金额
                    double invoiced = supplierInvoiceMap.getOrDefault(supplierId, 0.0);
                    row.put("totalInvoicedAmount", invoiced);
                    Object orderPrice = row.get("totalorderprice");
                    double orderAmount = orderPrice != null ? ((Number) orderPrice).doubleValue() : 0;
                    row.put("totalUninvoicedAmount", Math.max(orderAmount - invoiced, 0));
                    
                    // 查询该供应商的最新对账记录
                    try {
                        FinSupplierReconcileRecord latestRecord = finSupplierLedgerMapper.selectLatestReconcileRecord(groupid, supplierId);
                        if (latestRecord != null) {
                            row.put("lastReconcileDate", latestRecord.getReconcileTime() != null ? 
                                new SimpleDateFormat("yyyy-MM-dd").format(latestRecord.getReconcileTime()) : null);
                            // 判断当前查询月份是否已对账
                            if (queryMonth.equals(latestRecord.getReconcileMonth())) {
                                row.put("reconcileStatus", 1); // 已对账
                            } else {
                                row.put("reconcileStatus", 0); // 未对账
                            }
                        } else {
                            row.put("reconcileStatus", 0); // 未对账
                        }
                    } catch (Exception e) {
                        log.warn("查询对账记录失败，supplierId={}", supplierId, e);
                        row.put("reconcileStatus", 0);
                    }
                }
            }
        } catch (Exception e) {
            log.error("补充发票数据失败", e);
        }
    }

    /**
     * 补充发票统计数据（按销方名称+groupid直接查询）
     */
    private void enrichInvoiceStatistics(Map<String, Object> statistics, SupplierLedgerQueryDTO query) {
        if (statistics == null) {
            return;
        }
        // groupid必须提供（财务数据隔离）
        String groupid = query.getGroupid();
        if (groupid == null || groupid.isEmpty()) {
            statistics.putIfAbsent("totalInvoicedAmount", 0);
            statistics.putIfAbsent("totalUninvoicedAmount", 0);
            return;
        }
        
        // 日期必须提供
        if (query.getStartDate() == null || query.getEndDate() == null) {
            statistics.putIfAbsent("totalInvoicedAmount", 0);
            statistics.putIfAbsent("totalUninvoicedAmount", 0);
            return;
        }
        String startDate = new SimpleDateFormat("yyyy-MM-dd").format(query.getStartDate());
        String endDate = new SimpleDateFormat("yyyy-MM-dd").format(query.getEndDate());
        
        try {
            // 从ERP获取供应商列表
            Result<?> supplierResult = remoteERPService.getSupplierList();
            if (!Result.isSuccess(supplierResult) || !(supplierResult.getData() instanceof List)) {
                statistics.putIfAbsent("totalInvoicedAmount", 0);
                statistics.putIfAbsent("totalUninvoicedAmount", 0);
                return;
            }
            List<Map<String, Object>> supplierList = (List<Map<String, Object>>) supplierResult.getData();
            
            // 收集所有销方名称
            List<String> allSellerNames = new ArrayList<>();
            for (Map<String, Object> supplier : supplierList) {
                Object nameObj = supplier.get("name");
                if (nameObj != null) {
                    allSellerNames.add(nameObj.toString().trim());
                }
                Object accountNamesObj = supplier.get("accountNames");
                if (accountNamesObj != null && !accountNamesObj.toString().isEmpty()) {
                    String[] accountNames = accountNamesObj.toString().split(",");
                    for (String accountName : accountNames) {
                        String trimmedName = accountName.trim();
                        if (!trimmedName.isEmpty()) {
                            allSellerNames.add(trimmedName);
                        }
                    }
                }
            }
            
            if (allSellerNames.isEmpty()) {
                statistics.putIfAbsent("totalInvoicedAmount", 0);
                statistics.putIfAbsent("totalUninvoicedAmount", 0);
                return;
            }
            
            // 查询发票汇总
            List<Map<String, Object>> invoiceData = finSupplierLedgerMapper.selectInvoiceAmountByNames(
                    groupid, allSellerNames, startDate, endDate);
            double totalInvoiced = 0;
            for (Map<String, Object> row : invoiceData) {
                Object amt = row.get("totalInvoicedAmount");
                if (amt != null) {
                    totalInvoiced += ((Number) amt).doubleValue();
                }
            }
            statistics.put("totalInvoicedAmount", totalInvoiced);
            Object orderAmount = statistics.get("totalOrderAmount");
            double orderTotal = orderAmount != null ? ((Number) orderAmount).doubleValue() : 0;
            statistics.put("totalUninvoicedAmount", Math.max(orderTotal - totalInvoiced, 0));
        } catch (Exception e) {
            statistics.putIfAbsent("totalInvoicedAmount", 0);
            statistics.putIfAbsent("totalUninvoicedAmount", 0);
        }
    }

    /**
     * 构建查询参数
     */
    private Map<String, Object> buildParams(SupplierLedgerQueryDTO query) {
        Map<String, Object> params = new HashMap<>();
        // 只使用单个groupid（财务数据隔离）
        if (query.getGroupid() != null) {
            params.put("groupid", query.getGroupid());
        }
        if (query.getSupplierId() != null) {
            params.put("supplierId", String.valueOf(query.getSupplierId()));
        }
        if (query.getSupplierName() != null) {
            params.put("supplierName", query.getSupplierName());
        }
        if (query.getStartDate() != null) {
            String startDate = new SimpleDateFormat("yyyy-MM-dd").format(query.getStartDate());
            params.put("startDate", startDate);
            params.put("fromDate", startDate);
        }
        if (query.getEndDate() != null) {
            String endDate = new SimpleDateFormat("yyyy-MM-dd").format(query.getEndDate());
            params.put("endDate", endDate);
            params.put("toDate", endDate);
        }
        if (query.getSearch() != null) {
            params.put("search", query.getSearch());
        }
        if (query.getWarehouseid() != null) {
            params.put("warehouseid", query.getWarehouseid());
        }
        if (query.getContactPerson() != null) {
            params.put("contactPerson", query.getContactPerson());
        }
        if (query.getReconcileStatus() != null) {
            params.put("reconcileStatus", query.getReconcileStatus());
        }
        // 分页参数
        if (query.getPageNum() != null) {
            params.put("pageNum", query.getPageNum());
        }
        if (query.getPageSize() != null) {
            params.put("pageSize", query.getPageSize());
            // 计算offset供SQL的LIMIT/OFFSET使用
            int pageNum = query.getPageNum() != null ? query.getPageNum() : 1;
            int offset = (pageNum - 1) * query.getPageSize();
            params.put("offset", offset);
        }
        return params;
    }
}
