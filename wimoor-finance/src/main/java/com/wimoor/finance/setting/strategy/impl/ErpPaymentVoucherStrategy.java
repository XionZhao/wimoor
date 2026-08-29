package com.wimoor.finance.setting.strategy.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.wimoor.common.GeneralUtil;
import com.wimoor.common.mvc.BizException;
import com.wimoor.common.result.Result;
import com.wimoor.common.user.UserInfo;
import com.wimoor.finance.api.RemoteERPService;
import com.wimoor.finance.setting.domain.FinMappingVouchersSource;
import com.wimoor.finance.setting.service.IFinMappingVouchersSourceService;
import com.wimoor.finance.setting.domain.FinMappingVouchers;
import com.wimoor.finance.setting.domain.FinMappingVouchersSourcePayment;
import com.wimoor.finance.setting.service.IFinMappingVouchersService;
import com.wimoor.finance.setting.service.IFinMappingVouchersSourcePaymentService;
import com.wimoor.finance.setting.domain.FinAccountingPeriods;
import com.wimoor.finance.setting.domain.FinMappingErpAccount;
import com.wimoor.finance.setting.domain.FinMappingErpFeetype;
import com.wimoor.finance.setting.service.IFinAccountingPeriodsService;
import com.wimoor.finance.setting.service.IFinAccountingSubjectsService;
import com.wimoor.finance.setting.service.IFinMappingErpAccountService;
import com.wimoor.finance.setting.service.IFinMappingErpFeetypeService;
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
 * ERP付款记录 → 凭证转换策略
 * <p>
 * 每晚定时执行，从ERP获取昨天closepaydate发生变更且全部SKU已完成付款的采购订单，
 * 根据 fin_mapping_erp_account（费用类型→借方科目）和
 * fin_mapping_erp_feetype（采购账户→贷方科目）的映射配置，
 * 每个订单生成一个凭证，每个付款批次生成一对借贷分录。
 * </p>
 * <p>
 * 变更追踪：通过 fin_mapping_vouchers_source 表以订单维度追踪同步状态，
 * 当订单closepaydate发生变更时，自动更新对应的凭证。
 * </p>
 *
 * @author wimoor
 * @date 2026-08-07
 */
@Service
@Slf4j
public class ErpPaymentVoucherStrategy implements IErpVoucherStrategy {

    private static final String FTYPE = "erppayment";
    private static final String VOUCHER_TYPE = "付"; // 付款凭证类型
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private static final int SYNC_PENDING = 0;
    private static final int SYNC_DONE = 1;
    private static final int SYNC_CHANGED = 2;

    @Resource
    IFinMappingVouchersService finMappingVouchersService;
    @Resource
    IFinVouchersService iFinVouchersService;
    @Resource
    IFinVoucherEntriesService iFinVoucherEntriesService;
    @Resource
    IFinAccountingPeriodsService iFinAccountingPeriodsService;
    @Resource
    IFinAccountingSubjectsService finAccountingSubjectsService;
    @Resource
    IFinMappingErpAccountService finMappingErpAccountService;
    @Resource
    IFinMappingErpFeetypeService finMappingErpFeetypeService;
    @Resource
    IFinMappingVouchersSourceService finMappingVouchersSourceService;
    @Resource
    IFinMappingVouchersSourcePaymentService finMappingVouchersSourcePaymentService;
    @Resource
    RemoteERPService remoteERPService;
    @Resource
    ErpInventoryVoucherStrategy erpInventoryVoucherStrategy;

    @Override
    public String getFtype() {
        return FTYPE;
    }

    private FinAccountingPeriods getPeriod(String groupid, String periodCode) {
        FinAccountingPeriods period = null;
        if (StrUtil.isNotBlank(periodCode)) {
            period = iFinAccountingPeriodsService.selectByPeriod(groupid, periodCode);
            if (period == null) {
                String periodDate = periodCode + "01";
                SimpleDateFormat FMT_YMD = new SimpleDateFormat("yyyyMMdd");
                try {
                    period = iFinAccountingPeriodsService.selectFinAccountingPeriodsByDate(groupid, FMT_YMD.parse(periodDate));
                } catch (ParseException e) {
                    throw new RuntimeException(e);
                }
            }
        } else {
            period = iFinAccountingPeriodsService.getCurrentPeriod(groupid);
        }
        if (period == null) {
            throw new BizException("未找到指定的会计期间");
        }
        if (period.getPeriodStatus() == 3) {
            throw new BizException("会计期间已关闭，无法生成凭证");
        }
        return period;
    }

    /**
     * 核心入口：每晚将昨天变更的已完成订单转换为凭证
     * <p>
     * 流程：
     * 1. 通过 groupid 反查模板
     * 2. 确定查询日期（默认昨天）
     * 3. 加载映射规则
     * 4. 调用ERP接口获取已完成订单
     * 5. 将ERP数据落地到 fin_erp_payment_record（upsert，检测变更）
     * 6. 查询待同步的付款记录
     * 7. 按订单维度分组，每个订单生成一个凭证
     * 8. 批量更新同步状态
     * 9. 更新 fin_erp_order_voucher 追踪记录
     * </p>
     */
    @Override
    public void generateVoucher(UserInfo userInfo, String groupid, String periodCode) {
        // 确定查询日期（默认昨天）
        String targetDate;
        if (StrUtil.isNotBlank(periodCode)) {
            FinAccountingPeriods period = getPeriod(groupid, periodCode);
            LocalDate ld = period.getEndDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            targetDate = ld.format(DATE_FMT);
        } else {
            targetDate = LocalDate.now().minusDays(1).format(DATE_FMT);
        }
        log.info("ERP付款凭证生成：groupid={}, 变更日期={}", groupid, targetDate);

        // 加载映射规则
        Map<String, String> feeTypeSubjectMap = loadAccountMapping(groupid);
        Map<String, String> accountSubjectMap = loadFeetypeMapping(groupid);

        if (feeTypeSubjectMap.isEmpty() && accountSubjectMap.isEmpty()) {
            throw new BizException("未配置任何费用类型映射或采购账户映射，请先在\"付款凭证\"页签中配置");
        }

        // 调用ERP接口获取已完成订单
        List<Map<String, Object>> completedOrders = queryCompletedOrders(groupid, targetDate);

        if (completedOrders.isEmpty()) {
            log.info("当天无已完成订单变更，跳过凭证生成");
            return;
        }
        log.info("获取到{}个已完成订单", completedOrders.size());

        // 将ERP数据转换为 FinMappingVouchersSourcePayment 并落地到本地表
        String userName = userInfo.getUserName();
        List<FinMappingVouchersSourcePayment> paymentRecords = convertToPaymentRecords(completedOrders, groupid);
        int persistedCount = finMappingVouchersSourcePaymentService.batchUpsertPaymentRecords(groupid, paymentRecords, userName);
        log.info("ERP付款记录落地：{} 条（含新增和变更）", persistedCount);

        // 查询待同步的付款记录（含新增、变更和撤销的）
        List<FinMappingVouchersSourcePayment> needSyncRecords = finMappingVouchersSourcePaymentService.selectNeedSync(groupid);
        if (needSyncRecords.isEmpty()) {
            log.info("无待同步的付款记录，跳过凭证生成");
            return;
        }

        log.info("待同步付款记录：{} 条", needSyncRecords.size());

        // 每笔付款记录生成一张凭证
        int successCount = 0;
        Date now = new Date();

        for (FinMappingVouchersSourcePayment paymentRecord : needSyncRecords) {
            String paymentId = paymentRecord.getPaymentId();
            String orderId = paymentRecord.getOrderId();

            // 从单条付款记录构建订单数据结构
            List<FinMappingVouchersSourcePayment> singleList = Collections.singletonList(paymentRecord);
            Map<String, Object> orderMap = buildOrderMap(singleList);

            // 查找已有的 fin_mapping_vouchers_source 记录（按 paymentId 维度）
            FinMappingVouchersSource existingRecord = null;
            List<FinMappingVouchersSource> existingOrders = finMappingVouchersSourceService.selectByOrderIds(groupid, Collections.singletonList(orderId));
            if (existingOrders != null && !existingOrders.isEmpty()) {
                existingRecord = existingOrders.stream()
                        .filter(o -> "payment".equals(o.getVoucherType()))
                        .findFirst().orElse(null);
            }

            Long voucherId = generateOrderVoucher(orderMap, VOUCHER_TYPE, groupid, targetDate,
                    feeTypeSubjectMap, accountSubjectMap, userName, now, existingRecord);

            if (voucherId != null) {
                // 更新单条付款记录的同步状态（凭证关联存储在 fin_mapping_vouchers_source_payment）
                finMappingVouchersSourcePaymentService.batchUpdateSyncStatus(
                        Collections.singletonList(paymentRecord.getId()), SYNC_DONE, voucherId, now);
                // 更新 fin_mapping_vouchers_source 订单信息（不存储 voucher_id）
                if (existingRecord != null) {
                    existingRecord.setSyncStatus(SYNC_DONE);
                    existingRecord.setUpdatedTime(now);
                    finMappingVouchersSourceService.updateFinMappingVouchersSource(existingRecord);
                } else {
                    // 创建订单记录（不带 voucher_id）
                    saveOrUpdateSyncRecord(orderId,
                            paymentRecord.getOrderNumber(),
                            paymentRecord.getWarehouseName(),
                            paymentRecord.getSupplierName(),
                            paymentRecord.getAmount(),
                            null, "payment", SYNC_DONE, orderMap, paymentRecord.getSku(),
                            userName, now, null);
                }
                successCount++;
            } else {
                // 付款记录被撤销，从 fin_mapping_vouchers_source_payment 获取凭证ID删除
                FinMappingVouchersSourcePayment existingPayment = finMappingVouchersSourcePaymentService.selectByPaymentId(groupid, paymentId);
                if (existingPayment != null && existingPayment.getVoucherId() != null) {
                    try {
                        iFinVouchersService.deleteFinVouchersByVoucherId(existingPayment.getVoucherId());
                        log.info("付款 [{}] 撤销，已删除凭证: voucherId={}", paymentId, existingPayment.getVoucherId());
                    } catch (Exception e) {
                        log.error("删除凭证失败: voucherId={}, error={}", existingPayment.getVoucherId(), e.getMessage());
                    }
                }
                // 标记已撤销的记录为已处理
                finMappingVouchersSourcePaymentService.batchUpdateSyncStatus(
                        Collections.singletonList(paymentRecord.getId()), SYNC_DONE, null, now);
            }
        }

        log.info("凭证生成完成：成功{}笔付款", successCount);

        // 同步生成在途确认库存凭证（阶段1：付款完成时触发）
        try {
            erpInventoryVoucherStrategy.generateVoucher(userInfo, groupid, periodCode);
        } catch (Exception e) {
            log.error("在途确认库存凭证生成失败：{}", e.getMessage(), e);
        }
    }

    /**
     * 按指定日期生成凭证（手动触发）
     * <p>
     * 直接使用指定日期从ERP获取完成付款的订单，落地到本地表后生成凭证。
     * 已生成过的凭证会自动更新。
     * </p>
     *
     * @param userInfo 当前用户
     * @param groupid  租户ID
     * @param date     日期 yyyy-MM-dd
     * @return 生成结果摘要
     */
    @Override
    public Map<String, Object> generateVoucherByDate(UserInfo userInfo, String groupid, String date) {
        Map<String, Object> result = new HashMap<>();
        result.put("date", date);

        // 校验日期是否在开放的会计期间内
        FinAccountingPeriods period;
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            period = iFinAccountingPeriodsService.selectFinAccountingPeriodsByDate(groupid, sdf.parse(date));
        } catch (ParseException e) {
            throw new BizException("日期格式错误：" + date + "，请使用 yyyy-MM-dd 格式");
        }
        if (period == null) {
            throw new BizException("日期 [" + date + "] 不在任何会计期间内");
        }
        if (period.getPeriodStatus() == 3) {
            throw new BizException("会计期间 [" + period.getPeriodName() + "] 已关闭，无法生成凭证");
        }

        log.info("按日期生成凭证：groupid={}, 日期={}", groupid, date);

        // 加载映射规则
        Map<String, String> feeTypeSubjectMap = loadAccountMapping(groupid);
        Map<String, String> accountSubjectMap = loadFeetypeMapping(groupid);

        if (feeTypeSubjectMap.isEmpty() && accountSubjectMap.isEmpty()) {
            throw new BizException("未配置任何费用类型映射或采购账户映射");
        }

        // 调用ERP接口获取指定日期完成付款的订单
        List<Map<String, Object>> completedOrders = queryCompletedOrders(groupid, date);

        if (completedOrders.isEmpty()) {
            log.info("日期 [{}] 无已完成订单变更", date);
            result.put("orderCount", 0);
            result.put("voucherCount", 0);
            result.put("message", "该日期无已完成付款的订单");
            return result;
        }

        result.put("orderCount", completedOrders.size());
        log.info("日期 [{}] 获取到 {} 个已完成订单", date, completedOrders.size());

        // 将ERP数据转换为 FinMappingVouchersSourcePayment 并落地到本地表
        String userName = userInfo.getUserName();
        List<FinMappingVouchersSourcePayment> paymentRecords = convertToPaymentRecords(completedOrders, groupid);
        int persistedCount = finMappingVouchersSourcePaymentService.batchUpsertPaymentRecords(groupid, paymentRecords, userName);
        log.info("日期 [{}] ERP付款记录落地：{} 条", date, persistedCount);

        // 查询待同步的付款记录（含新增、变更和撤销的）
        List<FinMappingVouchersSourcePayment> needSyncRecords = finMappingVouchersSourcePaymentService.selectNeedSync(groupid);
        if (needSyncRecords.isEmpty()) {
            log.info("日期 [{}] 无待同步的付款记录", date);
            result.put("voucherCount", 0);
            result.put("message", "付款记录已落地，无变更");
            return result;
        }

        log.info("日期 [{}] 待同步付款记录：{} 条", date, needSyncRecords.size());

        // 每笔付款记录生成一张凭证
        int successCount = 0;
        int updateCount = 0;
        Date now = new Date();

        for (FinMappingVouchersSourcePayment paymentRecord : needSyncRecords) {
            String paymentId = paymentRecord.getPaymentId();
            String orderId = paymentRecord.getOrderId();

            // 从单条付款记录构建订单数据结构
            List<FinMappingVouchersSourcePayment> singleList = Collections.singletonList(paymentRecord);
            Map<String, Object> orderMap = buildOrderMap(singleList);

            // 查找已有的 fin_mapping_vouchers_source 记录
            FinMappingVouchersSource existingRecord = null;
            List<FinMappingVouchersSource> existingOrders = finMappingVouchersSourceService.selectByOrderIds(groupid, Collections.singletonList(orderId));
            if (existingOrders != null && !existingOrders.isEmpty()) {
                existingRecord = existingOrders.stream()
                        .filter(o -> "payment".equals(o.getVoucherType()))
                        .findFirst().orElse(null);
                if (existingRecord != null) {
                    updateCount++;
                }
            }

            Long voucherId = generateOrderVoucher(orderMap, VOUCHER_TYPE, groupid, date,
                    feeTypeSubjectMap, accountSubjectMap, userName, now, existingRecord);

            if (voucherId != null) {
                // 更新单条付款记录的同步状态（凭证关联存储在 fin_mapping_vouchers_source_payment）
                finMappingVouchersSourcePaymentService.batchUpdateSyncStatus(
                        Collections.singletonList(paymentRecord.getId()), SYNC_DONE, voucherId, now);
                // 更新 fin_mapping_vouchers_source 订单信息（不存储 voucher_id）
                if (existingRecord != null) {
                    existingRecord.setSyncStatus(SYNC_DONE);
                    existingRecord.setUpdatedTime(now);
                    finMappingVouchersSourceService.updateFinMappingVouchersSource(existingRecord);
                } else {
                    // 创建订单记录（不带 voucher_id）
                    saveOrUpdateSyncRecord(orderId,
                            paymentRecord.getOrderNumber(),
                            paymentRecord.getWarehouseName(),
                            paymentRecord.getSupplierName(),
                            paymentRecord.getAmount(),
                            null, "payment", SYNC_DONE, orderMap, paymentRecord.getSku(),
                            userName, now, null);
                }
                successCount++;
            } else {
                // 付款记录被撤销，从 fin_mapping_vouchers_source_payment 获取凭证ID删除
                FinMappingVouchersSourcePayment existingPayment = finMappingVouchersSourcePaymentService.selectByPaymentId(groupid, paymentId);
                if (existingPayment != null && existingPayment.getVoucherId() != null) {
                    try {
                        iFinVouchersService.deleteFinVouchersByVoucherId(existingPayment.getVoucherId());
                        log.info("日期 [{}] 付款 [{}] 撤销，已删除凭证: voucherId={}", date, paymentId, existingPayment.getVoucherId());
                    } catch (Exception e) {
                        log.error("删除凭证失败: voucherId={}, error={}", existingPayment.getVoucherId(), e.getMessage());
                    }
                }
                // 标记已撤销的记录为已处理
                finMappingVouchersSourcePaymentService.batchUpdateSyncStatus(
                        Collections.singletonList(paymentRecord.getId()), SYNC_DONE, null, now);
            }
        }

        result.put("voucherCount", successCount);
        result.put("newCount", successCount - updateCount);
        result.put("updateCount", updateCount);
        result.put("message", String.format("日期 [%s] 共 %d 笔付款，生成 %d 个凭证（新增 %d，更新 %d）",
                date, needSyncRecords.size(), successCount, successCount - updateCount, updateCount));

        log.info("日期 [{}] 凭证生成完成：{} 笔付款，{} 个凭证（新增 {}，更新 {}）",
                date, needSyncRecords.size(), successCount, successCount - updateCount, updateCount);

        // 同步生成在途确认库存凭证（阶段1：付款完成时触发）
        try {
            erpInventoryVoucherStrategy.generateVoucher(userInfo, groupid, null);
        } catch (Exception e) {
            log.error("日期 [{}] 在途确认库存凭证生成失败：{}", date, e.getMessage(), e);
        }
        return result;
    }

    /**
     * 为单个订单生成/更新凭证
     */
    private Long generateOrderVoucher(Map<String, Object> order, String voucherType,
                                       String groupid, String targetDate,
                                       Map<String, String> feeTypeSubjectMap,
                                       Map<String, String> accountSubjectMap,
                                       String userName, Date now,
                                       FinMappingVouchersSource existingRecord) {
        String orderId = getStringValue(order, "formid");
        String orderNumber = getStringValue(order, "number");
        String warehouseName = getStringValue(order, "warehouseName");
        String supplierName = getSupplierName(order);

        // 构建凭证分录：每个付款批次 → 一对借贷分录
        List<FinVoucherEntries> entryList = new ArrayList<>();
        BigDecimal debitTotal = BigDecimal.ZERO;
        BigDecimal creditTotal = BigDecimal.ZERO;
        
        // 构建JSON格式的datalog
        Map<String, Object> datalogMap = new LinkedHashMap<>();
        datalogMap.put("orderId", orderId);
        datalogMap.put("orderNumber", orderNumber);
        datalogMap.put("warehouseName", warehouseName);
        datalogMap.put("supplierName", supplierName);
        datalogMap.put("targetDate", targetDate);
        datalogMap.put("voucherType", voucherType);
        
        // 记录原始订单信息
        Map<String, Object> originalOrder = new LinkedHashMap<>();
        originalOrder.put("formid", orderId);
        originalOrder.put("number", orderNumber);
        originalOrder.put("warehouseName", warehouseName);
        originalOrder.put("supplierName", supplierName);
        datalogMap.put("originalOrder", originalOrder);
        
        // 记录映射配置
        Map<String, Object> mappingConfig = new LinkedHashMap<>();
        mappingConfig.put("feeTypeSubjectMap", feeTypeSubjectMap);
        mappingConfig.put("accountSubjectMap", accountSubjectMap);
        datalogMap.put("mappingConfig", mappingConfig);
        
        // 记录映射结果
        List<Map<String, Object>> mappingResults = new ArrayList<>();
        List<String> skippedReasons = new ArrayList<>();
        
        long entryNo = 1L;

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> entries = (List<Map<String, Object>>) order.get("entries");
        // 记录原始entries信息
        datalogMap.put("originalEntries", entries);
        
        if (entries != null) {
            for (Map<String, Object> entry : entries) {
                String sku = getStringValue(entry, "sku");

                @SuppressWarnings("unchecked")
                List<Map<String, Object>> payments = (List<Map<String, Object>>) entry.get("payments");
                if (payments != null) {
                    for (Map<String, Object> payment : payments) {
                        String payProjectId = getStringValue(payment, "projectid");
                        String payAcct = getStringValue(payment, "acct");
                        BigDecimal payPrice = getBigDecimalValue(payment, "payprice");
                        String paymentId = getStringValue(payment, "id");

                        if (payPrice == null || payPrice.compareTo(BigDecimal.ZERO) == 0) {
                            continue;
                        }

                        // 记录单个付款的映射结果
                        Map<String, Object> resultMap = new LinkedHashMap<>();
                        resultMap.put("paymentId", paymentId);
                        resultMap.put("sku", sku);
                        resultMap.put("feeTypeId", payProjectId);
                        resultMap.put("accountId", payAcct);
                        resultMap.put("amount", payPrice);
                        resultMap.put("originalPayment", payment);

                        // 查找借方科目（费用类型映射）
                        String debitSubjectId = feeTypeSubjectMap.get(payProjectId);
                        if (StrUtil.isBlank(debitSubjectId)) {
                            resultMap.put("status", "skipped");
                            resultMap.put("reason", "费用类型" + payProjectId + "未配置映射");
                            mappingResults.add(resultMap);
                            skippedReasons.add("费用类型" + payProjectId + "未配置映射");
                            continue;
                        }

                        // 查找贷方科目（采购账户映射）
                        String creditSubjectId = accountSubjectMap.get(payAcct);
                        if (StrUtil.isBlank(creditSubjectId)) {
                            resultMap.put("status", "skipped");
                            resultMap.put("reason", "采购账户" + payAcct + "未配置映射");
                            mappingResults.add(resultMap);
                            skippedReasons.add("采购账户" + payAcct + "未配置映射");
                            continue;
                        }

                        String summary = buildSummary(sku, supplierName, orderNumber, payPrice);

                        // 借方分录
                        FinVoucherEntries debitEntry = new FinVoucherEntries();
                        debitEntry.setSubjectId(debitSubjectId);
                        debitEntry.setDebitAmount(payPrice);
                        debitEntry.setSummary(summary);
                        debitEntry.setEntryNo(entryNo++);
                        entryList.add(debitEntry);

                        // 贷方分录
                        FinVoucherEntries creditEntry = new FinVoucherEntries();
                        creditEntry.setSubjectId(creditSubjectId);
                        creditEntry.setCreditAmount(payPrice);
                        creditEntry.setSummary(summary);
                        creditEntry.setEntryNo(entryNo++);
                        entryList.add(creditEntry);

                        debitTotal = debitTotal.add(payPrice);
                        creditTotal = creditTotal.add(payPrice);

                        // 记录成功的映射
                        resultMap.put("status", "success");
                        resultMap.put("debitSubjectId", debitSubjectId);
                        resultMap.put("creditSubjectId", creditSubjectId);
                        resultMap.put("summary", summary);
                        mappingResults.add(resultMap);
                    }
                }
            }
        }

        // 完成datalog构建
        datalogMap.put("mappingResults", mappingResults);
        datalogMap.put("skippedReasons", skippedReasons);
        datalogMap.put("totalMappings", mappingResults.size());
        datalogMap.put("successCount", mappingResults.stream().filter(r -> "success".equals(r.get("status"))).count());
        datalogMap.put("skippedCount", mappingResults.stream().filter(r -> "skipped".equals(r.get("status"))).count());
        datalogMap.put("debitTotal", debitTotal);
        datalogMap.put("creditTotal", creditTotal);
        String datalog = JSONUtil.toJsonStr(datalogMap);

        if (entryList.isEmpty()) {
            log.warn("订单[{}]未生成任何凭证分录，跳过", orderNumber);
            return null;
        }

        if (debitTotal.compareTo(creditTotal) != 0) {
            throw new BizException("凭证生成失败：订单[" + orderNumber + "]借贷不平衡，借方：" + debitTotal + "，贷方：" + creditTotal);
        }

        // 创建/更新凭证
        FinVouchers finVouchers = new FinVouchers();
        finVouchers.setVoucherType(voucherType);
        finVouchers.setGroupid(groupid);
        finVouchers.setVoucherDate(GeneralUtil.getDatez(targetDate));
        finVouchers.setTotalAmount(debitTotal);
        finVouchers.setEntries(entryList);
        finVouchers.setVoucherStatus(3);
        finVouchers.setDataSource(3);

        finVouchers.setRemark(buildVoucherSummary(orderNumber, supplierName, warehouseName, entries));

        Long voucherId;
        if (existingRecord != null && existingRecord.getVoucherId() != null) {
            voucherId = existingRecord.getVoucherId();
            FinVouchers oldFinVouchers = iFinVouchersService.selectFinVouchersByVoucherId(voucherId);
            if (oldFinVouchers != null) {
                // 旧凭证存在，更新
                finVouchers.setVoucherNo(oldFinVouchers.getVoucherNo());
                finVouchers.setVoucherId(voucherId);
                finVouchers.setUpdateBy(userName);
                finVouchers.setUpdatedTime(now);

                iFinVoucherEntriesService.deleteByVoucherId(voucherId);
                for (FinVoucherEntries e : finVouchers.getEntries()) {
                    e.setVoucherId(voucherId);
                }
                iFinVouchersService.updateFinVouchers(finVouchers);
                log.info("更新ERP付款凭证：voucherId={}, 订单={}", voucherId, orderNumber);
            } else {
                // 旧凭证已被删除，创建新凭证
                log.info("旧凭证已删除，创建新凭证：原voucherId={}, 订单={}", voucherId, orderNumber);
                finVouchers.setVoucherNo(iFinVouchersService.selectNextVoucherNo(finVouchers));
                finVouchers.setCreateBy(userName);
                finVouchers.setUpdateBy(userName);
                finVouchers.setCreatedTime(now);
                finVouchers.setUpdatedTime(now);
                iFinVouchersService.insertFinVouchers(finVouchers);
                voucherId = finVouchers.getVoucherId();
            }
        } else {
            finVouchers.setVoucherNo(iFinVouchersService.selectNextVoucherNo(finVouchers));
            finVouchers.setCreateBy(userName);
            finVouchers.setUpdateBy(userName);
            finVouchers.setCreatedTime(now);
            finVouchers.setUpdatedTime(now);
            iFinVouchersService.insertFinVouchers(finVouchers);
            voucherId = finVouchers.getVoucherId();
            log.info("创建ERP付款凭证：voucherId={}, 订单={}", voucherId, orderNumber);
        }

        // 记录映射凭证关联日志（fin_mapping_vouchers）
        FinMappingVouchers queryMapping = new FinMappingVouchers();
        queryMapping.setGroupid(groupid);
        queryMapping.setVouchersId(voucherId);
        List<FinMappingVouchers> existMappingList = finMappingVouchersService.selectFinMappingVouchersList(queryMapping);

        FinMappingVouchers mappingLog;
        if (existMappingList != null && !existMappingList.isEmpty()) {
            mappingLog = existMappingList.get(0);
            mappingLog.setDatalog(datalog);
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
            mappingLog.setDatalog(datalog);
            finMappingVouchersService.insertFinMappingVouchers(mappingLog);
        }

        // 更新同步追踪记录
        BigDecimal totalAmount = calculateTotalAmount(entries);
        String sku = getFirstSku(entries);
        saveOrUpdateSyncRecord(orderId, orderNumber, warehouseName, supplierName,
                totalAmount, voucherId, "payment", SYNC_DONE, order, sku, userName, now, existingRecord);
        return voucherId;
    }

    /**
     * 保存或更新同步追踪记录
     * 注意：付款凭证(payment)不在 fin_mapping_vouchers_source 存储 voucher_id，
     * 因为一个订单会对应多笔付款记录，每笔付款生成一张凭证。
     * 凭证关联存储在 fin_mapping_vouchers_source_payment 中。
     */
    private void saveOrUpdateSyncRecord(String orderId, String orderNumber, String warehouseName,
                                         String supplierName, BigDecimal totalAmount, Long voucherId,
                                         String voucherType, int status, Map<String, Object> order,
                                         String sku, String userName, Date now, FinMappingVouchersSource existing) {
        // 付款凭证不存储 voucher_id 到 fin_mapping_vouchers_source
        boolean isPayment = "payment".equals(voucherType);
        if (existing != null) {
            existing.setOrderNumber(orderNumber);
            existing.setWarehouseName(warehouseName);
            existing.setSupplierName(supplierName);
            existing.setTotalAmount(totalAmount);
            existing.setVoucherType(voucherType);
            existing.setSku(sku);
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
            record.setSku(sku);
            record.setWarehouseName(warehouseName);
            record.setSupplierName(supplierName);
            record.setTotalAmount(totalAmount);
            record.setVoucherType(voucherType);
            if (!isPayment) {
                record.setVoucherId(voucherId);
            }
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

    private String getFirstSku(List<Map<String, Object>> entries) {
        if (entries != null && !entries.isEmpty()) {
            return getStringValue(entries.get(0), "sku");
        }
        return null;
    }

    private List<Map<String, Object>> queryCompletedOrders(String groupid, String changedDate) {
        Result<?> result = remoteERPService.getCompletedOrdersForVoucher(groupid, changedDate);
        if (result == null || result.getData() == null) {
            return new ArrayList<>();
        }
        return parseList(result.getData());
    }

    /**
     * 按订单ID查询ERP订单数据（用于检测已同步订单的付款状态变更）
     */
    private List<Map<String, Object>> queryOrdersByIds(String groupid, List<String> orderIds) {
        if (orderIds == null || orderIds.isEmpty()) {
            return new ArrayList<>();
        }
        String orderIdsStr = String.join(",", orderIds);
        Result<?> result = remoteERPService.getOrdersByIds(groupid, orderIdsStr);
        if (result == null || result.getData() == null) {
            return new ArrayList<>();
        }
        return parseList(result.getData());
    }

    /**
     * 将ERP返回的订单数据转换为 FinMappingVouchersSourcePayment 列表
     * <p>
     * 按 payment_id 聚合：同一 payment_id 可能出现在多个分录中，
     * 合并为一个记录，entryIds 存储逗号分隔的分录ID列表。
     * </p>
     */
    private List<FinMappingVouchersSourcePayment> convertToPaymentRecords(List<Map<String, Object>> orders, String groupid) {
        // 按 paymentId 聚合，key=paymentId, value=record
        Map<String, FinMappingVouchersSourcePayment> recordMap = new LinkedHashMap<>();
        Date now = new Date();

        for (Map<String, Object> order : orders) {
            String orderId = getStringValue(order, "formid");
            String orderNumber = getStringValue(order, "number");
            String warehouseName = getStringValue(order, "warehouseName");
            String supplierName = getSupplierName(order);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> entries = (List<Map<String, Object>>) order.get("entries");
            if (entries != null) {
                for (Map<String, Object> entry : entries) {
                    String entryId = getStringValue(entry, "entryid");

                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> payments = (List<Map<String, Object>>) entry.get("payments");
                    if (payments != null) {
                        for (Map<String, Object> payment : payments) {
                            String paymentId = getStringValue(payment, "id");
                            if (StrUtil.isBlank(paymentId)) {
                                continue;
                            }

                            // 根据ERP的auditstatus设置付款状态：1=已付款，0=已撤销/驳回
                            Object auditStatus = payment.get("auditstatus");
                            int paymentStatus = (auditStatus != null && "1".equals(auditStatus.toString())) ? 1 : 0;

                            FinMappingVouchersSourcePayment record = recordMap.get(paymentId);
                            if (record == null) {
                                record = new FinMappingVouchersSourcePayment();
                                record.setGroupid(groupid);
                                record.setOrderId(orderId);
                                record.setOrderNumber(orderNumber);
                                record.setWarehouseName(warehouseName);
                                record.setPaymentId(paymentId);
                                // 付款日期：从payment.createdate获取
                                Object optimeObj = payment.get("createdate");
                                Date paymentDate = null;
                                if (optimeObj instanceof Date) {
                                    paymentDate = (Date) optimeObj;
                                } else if (optimeObj != null) {
                                    try {
                                        paymentDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                                                .parse(optimeObj.toString());
                                    } catch (ParseException e) {
                                        log.warn("解析付款日期失败: {}", optimeObj);
                                    }
                                }
                                record.setPaymentDate(paymentDate);
                                record.setFeeTypeId(getStringValue(payment, "projectid"));
                                record.setAccountId(getStringValue(payment, "acct"));
                                record.setAmount(getBigDecimalValue(payment, "payprice"));
                                record.setPaymentStatus(paymentStatus);
                                record.setSupplierName(supplierName);
                                recordMap.put(paymentId, record);
                            }

                            // 追加 entryId 到 entryIds
                            if (StrUtil.isNotBlank(entryId)) {
                                if (StrUtil.isBlank(record.getEntryIds())) {
                                    record.setEntryIds(entryId);
                                } else {
                                    record.setEntryIds(record.getEntryIds() + "," + entryId);
                                }
                            }
                        }
                    }
                }
            }
        }

        // 计算 hash
        List<FinMappingVouchersSourcePayment> records = new ArrayList<>();

        // 批量解析费用类型名称和采购账户名称
        Map<String, String> feeTypeNameMap = new HashMap<>();
        Map<String, String> accountNameMap = new HashMap<>();
        if (!recordMap.isEmpty()) {
            // 查询费用类型→名称映射（fin_mapping_erp_account.feeTypeId → feeTypeName）
            FinMappingErpAccount queryAccount = new FinMappingErpAccount();
            queryAccount.setGroupid(groupid);
            List<FinMappingErpAccount> accountMappings = finMappingErpAccountService.selectFinMappingErpAccountList(queryAccount);
            for (FinMappingErpAccount mapping : accountMappings) {
                if (StrUtil.isNotBlank(mapping.getFeeTypeId()) && StrUtil.isNotBlank(mapping.getFeeTypeName())) {
                    feeTypeNameMap.put(mapping.getFeeTypeId(), mapping.getFeeTypeName());
                }
            }
            // 查询采购账户→名称映射（fin_mapping_erp_feetype.accountId → accountName）
            FinMappingErpFeetype queryFeetype = new FinMappingErpFeetype();
            queryFeetype.setGroupid(groupid);
            List<FinMappingErpFeetype> feetypeMappings = finMappingErpFeetypeService.selectFinMappingErpFeetypeList(queryFeetype);
            for (FinMappingErpFeetype mapping : feetypeMappings) {
                if (StrUtil.isNotBlank(mapping.getAccountId()) && StrUtil.isNotBlank(mapping.getAccountName())) {
                    accountNameMap.put(mapping.getAccountId(), mapping.getAccountName());
                }
            }
        }

        for (FinMappingVouchersSourcePayment record : recordMap.values()) {
            // 设置费用类型名称和采购账户名称
            record.setFeeTypeName(feeTypeNameMap.get(record.getFeeTypeId()));
            record.setAccountName(accountNameMap.get(record.getAccountId()));
            record.setDataHash(computePaymentHash(record));
            records.add(record);
        }
        return records;
    }

    /**
     * 计算单条付款记录的数据指纹
     */
    private String computePaymentHash(FinMappingVouchersSourcePayment record) {
        try {
            StringBuilder sb = new StringBuilder();
            sb.append(record.getOrderId()).append("|")
              .append(record.getPaymentId()).append("|")
              .append(record.getFeeTypeId()).append("|")
              .append(record.getFeeTypeName()).append("|")
              .append(record.getAccountId()).append("|")
              .append(record.getAccountName()).append("|")
              .append(record.getAmount()).append("|")
              .append(record.getPaymentStatus()).append("|")
              .append(record.getPaymentDate());
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(sb.toString().getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : digest) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            return String.valueOf(record.hashCode());
        }
    }

    /**
     * 从付款记录列表重建订单数据结构（用于兼容 generateOrderVoucher）
     */
    private Map<String, Object> buildOrderMap(List<FinMappingVouchersSourcePayment> records) {
        if (records.isEmpty()) {
            return new HashMap<>();
        }

        FinMappingVouchersSourcePayment first = records.get(0);
        Map<String, Object> orderMap = new HashMap<>();
        orderMap.put("formid", first.getOrderId());
        orderMap.put("number", first.getOrderNumber());
        orderMap.put("warehouseName", first.getWarehouseName());
        orderMap.put("groupid", first.getGroupid());

        // 每个付款记录作为一个 entry，每个 entry 只有一个 payment
        // 只包含已付款记录（paymentStatus=1），排除已撤销的（paymentStatus=0）
        List<Map<String, Object>> entries = new ArrayList<>();
        for (FinMappingVouchersSourcePayment pr : records) {
            if (pr.getPaymentStatus() == null || pr.getPaymentStatus() != 1) {
                continue;
            }
            Map<String, Object> entryMap = new HashMap<>();
            entryMap.put("entryid", pr.getPaymentId());
            entryMap.put("supplierName", pr.getSupplierName());
            entryMap.put("totalpay", pr.getAmount());

            List<Map<String, Object>> payments = new ArrayList<>();
            Map<String, Object> payMap = new HashMap<>();
            payMap.put("id", pr.getPaymentId());
            payMap.put("projectid", pr.getFeeTypeId());
            payMap.put("acct", pr.getAccountId());
            payMap.put("payprice", pr.getAmount());
            payments.add(payMap);

            entryMap.put("payments", payments);
            entries.add(entryMap);
        }

        orderMap.put("entries", entries);
        return orderMap;
    }

    private String computeOrderHash(Map<String, Object> order) {
        try {
            StringBuilder sb = new StringBuilder();
            sb.append(getStringValue(order, "formid")).append("|");

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> entries = (List<Map<String, Object>>) order.get("entries");
            if (entries != null) {
                for (Map<String, Object> entry : entries) {
                    sb.append(getStringValue(entry, "entryid")).append("|");
                    sb.append(getStringValue(entry, "closepaydate")).append("|");
                    sb.append(getBigDecimalValue(entry, "totalpay")).append("|");

                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> payments = (List<Map<String, Object>>) entry.get("payments");
                    if (payments != null) {
                        for (Map<String, Object> pay : payments) {
                            sb.append(getStringValue(pay, "id")).append("|");
                            sb.append(getStringValue(pay, "projectid")).append("|");
                            sb.append(getStringValue(pay, "acct")).append("|");
                            sb.append(getBigDecimalValue(pay, "payprice")).append("|");
                        }
                    }
                }
            }

            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(sb.toString().getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : digest) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            return String.valueOf(order.hashCode());
        }
    }

    // ==================== 映射加载 ====================

    private Map<String, String> loadAccountMapping(String groupid) {
        FinMappingErpAccount query = new FinMappingErpAccount();
        query.setGroupid(groupid);
        List<FinMappingErpAccount> mappings = finMappingErpAccountService.selectFinMappingErpAccountList(query);
        Map<String, String> map = new HashMap<>();
        if (mappings != null) {
            for (FinMappingErpAccount m : mappings) {
                if (StrUtil.isNotBlank(m.getFeeTypeId()) && StrUtil.isNotBlank(m.getSubjectId())) {
                    map.put(m.getFeeTypeId(), m.getSubjectId());
                }
            }
        }
        return map;
    }

    private Map<String, String> loadFeetypeMapping(String groupid) {
        FinMappingErpFeetype query = new FinMappingErpFeetype();
        query.setGroupid(groupid);
        List<FinMappingErpFeetype> mappings = finMappingErpFeetypeService.selectFinMappingErpFeetypeList(query);
        Map<String, String> map = new HashMap<>();
        if (mappings != null) {
            for (FinMappingErpFeetype m : mappings) {
                if (StrUtil.isNotBlank(m.getAccountId()) && StrUtil.isNotBlank(m.getSubjectId())) {
                    map.put(m.getAccountId(), m.getSubjectId());
                }
            }
        }
        return map;
    }

    // ==================== 辅助方法 ====================

    private String getSupplierName(Map<String, Object> order) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> entries = (List<Map<String, Object>>) order.get("entries");
        if (entries != null && !entries.isEmpty()) {
            return getStringValue(entries.get(0), "supplierName");
        }
        return null;
    }

    private BigDecimal calculateTotalAmount(List<Map<String, Object>> entries) {
        BigDecimal total = BigDecimal.ZERO;
        if (entries != null) {
            for (Map<String, Object> entry : entries) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> payments = (List<Map<String, Object>>) entry.get("payments");
                if (payments != null) {
                    for (Map<String, Object> pay : payments) {
                        BigDecimal price = getBigDecimalValue(pay, "payprice");
                        if (price != null) {
                            total = total.add(price);
                        }
                    }
                }
            }
        }
        return total;
    }

    private String buildSummary(String sku, String supplierName, String orderNumber, BigDecimal amount) {
        StringBuilder sb = new StringBuilder();
        if (StrUtil.isNotBlank(sku)) {
            sb.append("SKU:").append(sku);
        }
        if (StrUtil.isNotBlank(supplierName)) {
            if (sb.length() > 0) sb.append("，");
            sb.append("供应商:").append(supplierName);
        }
        if (StrUtil.isNotBlank(orderNumber)) {
            if (sb.length() > 0) sb.append("，");
            sb.append("订单:").append(orderNumber);
        }
        return sb.toString();
    }

    private String buildVoucherSummary(String orderNumber, String supplierName, String warehouseName,
                                        List<Map<String, Object>> entries) {
        int entryCount = entries != null ? entries.size() : 0;
        int paymentCount = 0;
        if (entries != null) {
            for (Map<String, Object> e : entries) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> payments = (List<Map<String, Object>>) e.get("payments");
                if (payments != null) {
                    paymentCount += payments.size();
                }
            }
        }
        return String.format("采购订单[%s] 供应商:%s 仓库:%s 共%d个SKU %d笔付款",
                orderNumber, supplierName, warehouseName, entryCount, paymentCount);
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> parseList(Object data) {
        if (data instanceof List) {
            return (List<Map<String, Object>>) data;
        }
        if (data instanceof Map) {
            Map<String, Object> dataMap = (Map<String, Object>) data;
            for (String key : new String[]{"rows", "data", "records", "items"}) {
                Object inner = dataMap.get(key);
                if (inner instanceof List) {
                    return (List<Map<String, Object>>) inner;
                }
            }
        }
        return new ArrayList<>();
    }

    private String getStringValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value != null ? value.toString() : null;
    }

    private BigDecimal getBigDecimalValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof BigDecimal) {
            return (BigDecimal) value;
        }
        try {
            return new BigDecimal(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 撤销付款凭证时还原原始单据状态
     * 删除本地付款记录（ERP每晚会重新同步，下次同步会重新拉取）
     */
    @Override
    public void revokeOriginalDocumentStatus(String groupid, String orderId, Long voucherId) {
        if (orderId != null) {
            int deleted = finMappingVouchersSourcePaymentService.deleteByOrderId(groupid, orderId);
            log.info("撤销付款凭证：已删除订单[{}]的本地付款记录 {} 条", orderId, deleted);
        }
    }

    @Override
    public Map<String, Object> getCalculationDetail(String groupid, String periodCode) {
        Map<String, Object> result = new HashMap<>();
        FinAccountingPeriods period = getPeriod(groupid, periodCode);

        result.put("templateName", "ERP付款凭证");
        result.put("periodName", period.getPeriodName());
        result.put("formula", "每晚定时从ERP获取closepaydate变更的已完成订单，按费用类型映射（借方）和采购账户映射（贷方）生成凭证");
        result.put("dataSource", "ERP采购订单（按订单维度，一个订单一个凭证）");

        FinMappingErpAccount queryAccount = new FinMappingErpAccount();
        queryAccount.setGroupid(groupid);
        List<FinMappingErpAccount> accountMappings = finMappingErpAccountService.selectFinMappingErpAccountList(queryAccount);
        List<Map<String, Object>> mappingDetails = new ArrayList<>();
        if (accountMappings != null) {
            for (FinMappingErpAccount m : accountMappings) {
                Map<String, Object> detail = new HashMap<>();
                detail.put("feeTypeId", m.getFeeTypeId());
                detail.put("feeTypeName", m.getFeeTypeName());
                detail.put("subjectId", m.getSubjectId());
                detail.put("subjectName", m.getSubjectName());
                mappingDetails.add(detail);
            }
        }
        result.put("accountMappings", mappingDetails);

        FinMappingErpFeetype queryFeetype = new FinMappingErpFeetype();
        queryFeetype.setGroupid(groupid);
        List<FinMappingErpFeetype> feetypeMappings = finMappingErpFeetypeService.selectFinMappingErpFeetypeList(queryFeetype);
        List<Map<String, Object>> feetypeDetails = new ArrayList<>();
        if (feetypeMappings != null) {
            for (FinMappingErpFeetype m : feetypeMappings) {
                Map<String, Object> detail = new HashMap<>();
                detail.put("accountId", m.getAccountId());
                detail.put("accountName", m.getAccountName());
                detail.put("subjectId", m.getSubjectId());
                detail.put("subjectName", m.getSubjectName());
                feetypeDetails.add(detail);
            }
        }
        result.put("feetypeMappings", feetypeDetails);

        return result;
    }
}