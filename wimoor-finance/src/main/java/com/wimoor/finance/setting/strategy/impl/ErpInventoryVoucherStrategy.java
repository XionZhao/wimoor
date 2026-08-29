package com.wimoor.finance.setting.strategy.impl;

import cn.hutool.core.util.StrUtil;
import com.wimoor.common.GeneralUtil;
import com.wimoor.common.mvc.BizException;
import com.wimoor.common.result.Result;
import com.wimoor.common.user.UserInfo;
import com.wimoor.finance.api.RemoteERPService;
import com.wimoor.finance.setting.domain.FinMappingVouchersSource;
import com.wimoor.finance.setting.service.IFinMappingVouchersSourceService;
import com.wimoor.finance.setting.domain.FinMappingVouchers;
import com.wimoor.finance.setting.service.IFinMappingVouchersService;
import com.wimoor.finance.setting.domain.FinAccountingPeriods;
import com.wimoor.finance.setting.domain.FinMappingErpInventory;
import com.wimoor.finance.setting.service.IFinAccountingPeriodsService;
import com.wimoor.finance.setting.service.IFinMappingErpInventoryService;
import com.wimoor.finance.setting.strategy.IErpVoucherStrategy;
import com.wimoor.finance.voucher.domain.FinVoucherEntries;
import com.wimoor.finance.voucher.domain.FinVouchers;
import com.wimoor.finance.voucher.service.IFinVoucherEntriesService;
import com.wimoor.finance.voucher.service.IFinVouchersService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * ERP库存凭证转换策略
 * <p>
 * 每晚定时执行，基于 fin_mapping_erp_inventory 配置，将采购订单的付款和入库数据
 * 转换为库存凭证。支持两个阶段：
 * </p>
 * <ul>
 *   <li>阶段1（在途确认）：付款完成时触发，借：在途物资，贷：预付账款/应付暂估</li>
 *   <li>阶段2（入库验收）：收货完成时触发，借：库存商品，贷：在途物资</li>
 * </ul>
 * <p>
 * 映射规则：按 warehouse_type + stage 查找 fin_mapping_erp_inventory 的借贷科目
 * </p>
 *
 * @author wimoor
 * @date 2026-08-07
 */
@Service
@Slf4j
public class ErpInventoryVoucherStrategy implements IErpVoucherStrategy {

    private static final String FTYPE = "erpinventory";
    private static final String VOUCHER_TYPE = "转"; // 转账凭证类型
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private static final int SYNC_PENDING = 0;
    private static final int SYNC_DONE = 1;
    private static final int SYNC_CHANGED = 2;

    /** 凭证类型常量 */
    private static final String VOUCHER_TYPE_TRANSIT = "inventory_transit";
    private static final String VOUCHER_TYPE_INBOUND = "inventory_inbound";

    /** 阶段常量 */
    private static final int STAGE_TRANSIT = 1;
    private static final int STAGE_INBOUND = 2;

    @Resource
    IFinMappingVouchersService finMappingVouchersService;
    @Resource
    IFinVouchersService iFinVouchersService;
    @Resource
    IFinVoucherEntriesService iFinVoucherEntriesService;
    @Resource
    IFinAccountingPeriodsService iFinAccountingPeriodsService;
    @Resource
    IFinMappingErpInventoryService finMappingErpInventoryService;
    @Resource
    IFinMappingVouchersSourceService finMappingVouchersSourceService;
    @Resource
    RemoteERPService remoteERPService;

    @Override
    public String getFtype() {
        return FTYPE;
    }

    @Override
    public void generateVoucher(UserInfo userInfo, String groupid, String periodCode) {
        // 确定查询日期
        String targetDate;
        if (StrUtil.isNotBlank(periodCode)) {
            FinAccountingPeriods period = getPeriod(groupid, periodCode);
            LocalDate ld = period.getEndDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            targetDate = ld.format(DATE_FMT);
        } else {
            targetDate = LocalDate.now().minusDays(1).format(DATE_FMT);
        }
        log.info("ERP库存凭证生成：groupid={}, 变更日期={}", groupid, targetDate);

        // 加载库存映射规则
        FinMappingErpInventory queryMapping = new FinMappingErpInventory();
        queryMapping.setGroupid(groupid);
        queryMapping.setIsEnabled(1);
        List<FinMappingErpInventory> mappings = finMappingErpInventoryService.selectFinMappingErpInventoryList(queryMapping);
        if (mappings == null || mappings.isEmpty()) {
            throw new BizException("未配置库存科目映射规则，请先在\"库存凭证\"页签中配置");
        }

        // 按 warehouse_type + stage 构建映射索引
        Map<String, FinMappingErpInventory> mappingIndex = new HashMap<>();
        for (FinMappingErpInventory m : mappings) {
            String key = m.getWarehouseType() + "_" + m.getStage();
            mappingIndex.put(key, m);
        }

        // 阶段1：在途确认（closepaydate变更）
        log.info("开始处理阶段1：在途确认");
        List<Map<String, Object>> transitOrders = queryCompletedOrders(groupid, targetDate);
        processOrders(userInfo, groupid, targetDate, transitOrders,
                mappingIndex, STAGE_TRANSIT, VOUCHER_TYPE_TRANSIT);

        // 阶段2：入库验收（closerecdate变更）
        log.info("开始处理阶段2：入库验收");
        List<Map<String, Object>> inboundOrders = queryCompletedInventory(groupid, targetDate);
        processOrders(userInfo, groupid, targetDate, inboundOrders,
                mappingIndex, STAGE_INBOUND, VOUCHER_TYPE_INBOUND);
    }

    /**
     * 处理订单列表，生成凭证
     */
    private void processOrders(UserInfo userInfo,
                                String groupid, String targetDate,
                                List<Map<String, Object>> orders,
                                Map<String, FinMappingErpInventory> mappingIndex,
                                int stage, String voucherType) {
        if (orders.isEmpty()) {
            log.info("阶段{}无变更订单", stage);
            return;
        }

        // 变更检测
        List<String> orderIds = orders.stream()
                .map(o -> getStringValue(o, "formid"))
                .filter(StrUtil::isNotBlank)
                .collect(Collectors.toList());

        Map<String, FinMappingVouchersSource> existingMap = new HashMap<>();
        if (!orderIds.isEmpty()) {
            List<FinMappingVouchersSource> existingRecords = finMappingVouchersSourceService.selectByOrderIds(groupid, orderIds);
            if (existingRecords != null) {
                for (FinMappingVouchersSource r : existingRecords) {
                    String key = r.getOrderId() + "_" + r.getVoucherType();
                    existingMap.put(key, r);
                }
            }
        }

        String userName = userInfo.getUserName();
        Date now = new Date();
        int successCount = 0;

        for (Map<String, Object> order : orders) {
            String orderId = getStringValue(order, "formid");
            if (StrUtil.isBlank(orderId)) {
                continue;
            }

            String orderNumber = getStringValue(order, "number");
            String warehouseName = getStringValue(order, "warehouseName");

            FinMappingErpInventory mapping = findMapping(mappingIndex, order, stage);
            if (mapping == null) {
                log.warn("订单[{}]仓库[{}]未配置阶段{}映射，跳过", orderNumber, warehouseName, stage);
                continue;
            }

            String hash = computeOrderHash(order);
            String recordKey = orderId + "_" + voucherType;
            FinMappingVouchersSource existing = existingMap.get(recordKey);

            if (existing != null && hash.equals(existing.getDataHash())) {
                continue;
            }

            generateOrderVoucher(order, VOUCHER_TYPE, groupid, targetDate, mapping,
                    voucherType, userName, now, existing, stage);
            successCount++;
        }

        log.info("阶段{}完成：成功{}个订单", stage, successCount);
    }

    /**
     * 为单个订单生成库存凭证
     */
    private void generateOrderVoucher(Map<String, Object> order, String voucherType,
                                       String groupid, String targetDate,
                                       FinMappingErpInventory mapping,
                                       String orderVoucherType, String userName, Date now,
                                       FinMappingVouchersSource existingRecord, int stage) {
        String orderId = getStringValue(order, "formid");
        String orderNumber = getStringValue(order, "number");
        String warehouseName = getStringValue(order, "warehouseName");
        String supplierName = getSupplierName(order);

        List<FinVoucherEntries> entryList = new ArrayList<>();
        BigDecimal debitTotal = BigDecimal.ZERO;

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> entries = (List<Map<String, Object>>) order.get("entries");
        if (entries != null) {
            long entryNo = 1L;
            for (Map<String, Object> entry : entries) {
                String sku = getStringValue(entry, "sku");
                BigDecimal amount;

                if (stage == STAGE_TRANSIT) {
                    amount = getBigDecimalValue(entry, "totalpay");
                } else {
                    Integer totalin = getIntegerValue(entry, "totalin");
                    BigDecimal itemprice = getBigDecimalValue(entry, "itemprice");
                    if (totalin != null && itemprice != null) {
                        amount = itemprice.multiply(new BigDecimal(totalin));
                    } else {
                        amount = getBigDecimalValue(entry, "totalpay");
                    }
                }

                if (amount == null || amount.compareTo(BigDecimal.ZERO) == 0) {
                    continue;
                }

                String summary = buildSummary(sku, supplierName, orderNumber, stage);

                FinVoucherEntries debitEntry = new FinVoucherEntries();
                debitEntry.setSubjectId(mapping.getDebitSubjectId());
                debitEntry.setDebitAmount(amount);
                debitEntry.setSummary(summary);
                debitEntry.setEntryNo(entryNo++);
                entryList.add(debitEntry);

                FinVoucherEntries creditEntry = new FinVoucherEntries();
                creditEntry.setSubjectId(mapping.getCreditSubjectId());
                creditEntry.setCreditAmount(amount);
                creditEntry.setSummary(summary);
                creditEntry.setEntryNo(entryNo++);
                entryList.add(creditEntry);

                debitTotal = debitTotal.add(amount);
            }
        }

        if (entryList.isEmpty()) {
            log.warn("订单[{}]阶段{}未生成凭证分录，跳过", orderNumber, stage);
            return;
        }

        FinVouchers finVouchers = new FinVouchers();
        finVouchers.setVoucherType(voucherType);
        finVouchers.setGroupid(groupid);
        finVouchers.setVoucherDate(GeneralUtil.getDatez(targetDate));
        finVouchers.setTotalAmount(debitTotal);
        finVouchers.setEntries(entryList);
        finVouchers.setVoucherStatus(3);
        finVouchers.setDataSource(3);
        finVouchers.setRemark(buildVoucherSummary(orderNumber, supplierName, warehouseName, stage, entries));

        Long voucherId;
        if (existingRecord != null && existingRecord.getVoucherId() != null) {
            voucherId = existingRecord.getVoucherId();
            FinVouchers old = iFinVouchersService.selectFinVouchersByVoucherId(voucherId);
            finVouchers.setVoucherNo(old != null ? old.getVoucherNo() : iFinVouchersService.selectNextVoucherNo(finVouchers));
            finVouchers.setVoucherId(voucherId);
            finVouchers.setUpdateBy(userName);
            finVouchers.setUpdatedTime(now);

            iFinVoucherEntriesService.deleteByVoucherId(voucherId);
            for (FinVoucherEntries e : finVouchers.getEntries()) {
                e.setVoucherId(voucherId);
            }
            iFinVouchersService.updateFinVouchers(finVouchers);
            log.info("更新库存凭证：voucherId={}, 订单={}, 阶段={}", voucherId, orderNumber, stage);
        } else {
            finVouchers.setVoucherNo(iFinVouchersService.selectNextVoucherNo(finVouchers));
            finVouchers.setCreateBy(userName);
            finVouchers.setUpdateBy(userName);
            finVouchers.setCreatedTime(now);
            finVouchers.setUpdatedTime(now);
            iFinVouchersService.insertFinVouchers(finVouchers);
            voucherId = finVouchers.getVoucherId();
            log.info("创建库存凭证：voucherId={}, 订单={}, 阶段={}", voucherId, orderNumber, stage);
        }

        // 记录映射凭证关联日志（fin_mapping_vouchers）
        FinMappingVouchers queryMapping = new FinMappingVouchers();
        queryMapping.setGroupid(groupid);
        queryMapping.setVouchersId(voucherId);
        List<FinMappingVouchers> existMappingList = finMappingVouchersService.selectFinMappingVouchersList(queryMapping);

        FinMappingVouchers mappingLog;
        if (existMappingList != null && !existMappingList.isEmpty()) {
            mappingLog = existMappingList.get(0);
            mappingLog.setUpdateBy(userName);
            mappingLog.setUpdatedTime(now);
            finMappingVouchersService.updateFinMappingVouchers(mappingLog);
        } else {
            mappingLog = new FinMappingVouchers();
            mappingLog.setGroupid(groupid);
            mappingLog.setVouchersId(voucherId);
            mappingLog.setVoucherDate(finVouchers.getVoucherDate());
            mappingLog.setCreatedTime(now);
            mappingLog.setUpdatedTime(now);
            mappingLog.setCreateBy(userName);
            mappingLog.setModifyBy(userName);
            finMappingVouchersService.insertFinMappingVouchers(mappingLog);
        }

        BigDecimal totalAmount = calculateTotalAmount(entries);
        saveOrUpdateRecord(order, orderId, orderNumber, warehouseName, supplierName,
                totalAmount, voucherId, orderVoucherType, SYNC_DONE, userName, now, existingRecord);
    }

    private FinMappingErpInventory findMapping(Map<String, FinMappingErpInventory> mappingIndex,
                                                Map<String, Object> order, int stage) {
        for (int type = 1; type <= 3; type++) {
            String key = type + "_" + stage;
            FinMappingErpInventory mapping = mappingIndex.get(key);
            if (mapping != null) {
                return mapping;
            }
        }
        return mappingIndex.get("1_" + stage);
    }

    private void saveOrUpdateRecord(Map<String, Object> order, String orderId, String orderNumber,
                                     String warehouseName, String supplierName, BigDecimal totalAmount,
                                     Long voucherId, String voucherType, int status,
                                     String userName, Date now, FinMappingVouchersSource existing) {
        if (existing != null) {
            existing.setOrderNumber(orderNumber);
            existing.setWarehouseName(warehouseName);
            existing.setSupplierName(supplierName);
            existing.setTotalAmount(totalAmount);
            existing.setVoucherType(voucherType);
            existing.setSyncStatus(status);
            existing.setDataHash(computeOrderHash(order));
            existing.setSyncTime(now);
            existing.setUpdateBy(userName);
            finMappingVouchersSourceService.updateFinMappingVouchersSource(existing);
        } else {
            FinMappingVouchersSource record = new FinMappingVouchersSource();
            record.setGroupid(getStringValue(order, "groupid"));
            record.setOrderId(orderId);
            record.setOrderNumber(orderNumber);
            record.setWarehouseName(warehouseName);
            record.setSupplierName(supplierName);
            record.setTotalAmount(totalAmount);
            record.setVoucherType(voucherType);
            record.setVoucherId(voucherId);
            record.setSyncStatus(status);
            record.setDataHash(computeOrderHash(order));
            record.setSyncTime(now);
            record.setCreatedTime(now);
            record.setUpdatedTime(now);
            record.setCreateBy(userName);
            record.setUpdateBy(userName);
            finMappingVouchersSourceService.insertFinMappingVouchersSource(record);
        }
    }

    // ==================== ERP API 调用 ====================

    private List<Map<String, Object>> queryCompletedOrders(String groupid, String changedDate) {
        Result<?> result = remoteERPService.getCompletedOrdersForVoucher(groupid, changedDate);
        return parseList(result);
    }

    private List<Map<String, Object>> queryCompletedInventory(String groupid, String changedDate) {
        Result<?> result = remoteERPService.getCompletedOrdersForInventory(groupid, changedDate);
        return parseList(result);
    }

    // ==================== 工具方法 ====================

    private FinAccountingPeriods getPeriod(String groupid, String periodCode) {
        FinAccountingPeriods period = null;
        if (StrUtil.isNotBlank(periodCode)) {
            period = iFinAccountingPeriodsService.selectByPeriod(groupid, periodCode);
            if (period == null) {
                String periodDate = periodCode + "01";
                try {
                    period = iFinAccountingPeriodsService.selectFinAccountingPeriodsByDate(groupid,
                            new SimpleDateFormat("yyyyMMdd").parse(periodDate));
                } catch (ParseException e) {
                    throw new RuntimeException(e);
                }
            }
        } else {
            period = iFinAccountingPeriodsService.getCurrentPeriod(groupid);
        }
        if (period == null) throw new BizException("未找到会计期间");
        if (period.getPeriodStatus() == 3) throw new BizException("会计期间已关闭");
        return period;
    }

    private String computeOrderHash(Map<String, Object> order) {
        try {
            StringBuilder sb = new StringBuilder();
            sb.append(getStringValue(order, "formid")).append("|");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> entries = (List<Map<String, Object>>) order.get("entries");
            if (entries != null) {
                for (Map<String, Object> e : entries) {
                    sb.append(getStringValue(e, "entryid")).append("|");
                    sb.append(getStringValue(e, "closepaydate")).append("|");
                    sb.append(getStringValue(e, "closerecdate")).append("|");
                    sb.append(getBigDecimalValue(e, "totalpay")).append("|");
                    sb.append(getIntegerValue(e, "totalin")).append("|");
                }
            }
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(sb.toString().getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : digest) hex.append(String.format("%02x", b));
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            return String.valueOf(order.hashCode());
        }
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> parseList(Result<?> result) {
        if (result == null || result.getData() == null) return new ArrayList<>();
        Object data = result.getData();
        if (data instanceof List) return (List<Map<String, Object>>) data;
        if (data instanceof Map) {
            for (String key : new String[]{"rows", "data", "records"}) {
                Object inner = ((Map<String, Object>) data).get(key);
                if (inner instanceof List) return (List<Map<String, Object>>) inner;
            }
        }
        return new ArrayList<>();
    }

    private String getStringValue(Map<String, Object> map, String key) {
        Object v = map.get(key);
        return v != null ? v.toString() : null;
    }

    private BigDecimal getBigDecimalValue(Map<String, Object> map, String key) {
        Object v = map.get(key);
        if (v == null) return null;
        if (v instanceof BigDecimal) return (BigDecimal) v;
        try { return new BigDecimal(v.toString()); } catch (NumberFormatException e) { return null; }
    }

    private Integer getIntegerValue(Map<String, Object> map, String key) {
        Object v = map.get(key);
        if (v == null) return null;
        if (v instanceof Integer) return (Integer) v;
        try { return Integer.parseInt(v.toString()); } catch (NumberFormatException e) { return null; }
    }

    private String getSupplierName(Map<String, Object> order) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> entries = (List<Map<String, Object>>) order.get("entries");
        if (entries != null && !entries.isEmpty()) return getStringValue(entries.get(0), "supplierName");
        return null;
    }

    private BigDecimal calculateTotalAmount(List<Map<String, Object>> entries) {
        BigDecimal total = BigDecimal.ZERO;
        if (entries != null) {
            for (Map<String, Object> e : entries) {
                BigDecimal t = getBigDecimalValue(e, "totalpay");
                if (t != null) total = total.add(t);
            }
        }
        return total;
    }

    private String buildSummary(String sku, String supplierName, String orderNumber, int stage) {
        StringBuilder sb = new StringBuilder();
        if (StrUtil.isNotBlank(sku)) sb.append("SKU:").append(sku);
        if (StrUtil.isNotBlank(supplierName)) {
            if (sb.length() > 0) sb.append("，");
            sb.append("供应商:").append(supplierName);
        }
        if (StrUtil.isNotBlank(orderNumber)) {
            if (sb.length() > 0) sb.append("，");
            sb.append("订单:").append(orderNumber);
        }
        sb.append("（");
        sb.append(stage == STAGE_TRANSIT ? "在途确认" : "入库验收");
        sb.append("）");
        return sb.toString();
    }

    private String buildVoucherSummary(String orderNumber, String supplierName, String warehouseName,
                                        int stage, List<Map<String, Object>> entries) {
        int entryCount = entries != null ? entries.size() : 0;
        String stageName = stage == STAGE_TRANSIT ? "在途确认" : "入库验收";
        return String.format("采购订单[%s]%s 供应商:%s 仓库:%s 共%d个SKU",
                orderNumber, stageName, supplierName, warehouseName, entryCount);
    }

    @Override
    public Map<String, Object> getCalculationDetail(String groupid, String periodCode) {
        Map<String, Object> result = new HashMap<>();

        result.put("templateName", "ERP库存凭证");
        result.put("formula", "每晚定时从ERP获取closepaydate/closerecdate变更的订单，按仓库类型+阶段映射生成库存凭证");
        result.put("dataSource", "ERP采购订单（按订单维度，在途确认+入库验收两个阶段）");

        FinMappingErpInventory query = new FinMappingErpInventory();
        query.setGroupid(groupid);
        query.setIsEnabled(1);
        List<FinMappingErpInventory> mappings = finMappingErpInventoryService.selectFinMappingErpInventoryList(query);
        result.put("mappings", mappings != null ? mappings : new ArrayList<>());

        return result;
    }
}