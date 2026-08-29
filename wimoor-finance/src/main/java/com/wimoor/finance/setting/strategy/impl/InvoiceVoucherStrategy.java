package com.wimoor.finance.setting.strategy.impl;

import com.wimoor.common.mvc.BizException;
import com.wimoor.common.user.UserInfo;
import com.wimoor.finance.ledger.domain.FinInvoice;
import com.wimoor.finance.ledger.service.IFinInvoiceLedgerService;
import com.wimoor.finance.setting.domain.FinAccountingPeriods;
import com.wimoor.finance.setting.domain.FinMappingInvoice;
import com.wimoor.finance.setting.domain.FinMappingVouchersSource;
import com.wimoor.finance.setting.service.IFinAccountingPeriodsService;
import com.wimoor.finance.setting.service.IFinMappingInvoiceService;
import com.wimoor.finance.setting.service.IFinMappingVouchersSourceService;
import com.wimoor.finance.setting.strategy.IInvoiceVoucherStrategy;
import com.wimoor.finance.voucher.domain.FinVoucherEntries;
import com.wimoor.finance.voucher.domain.FinVouchers;
import com.wimoor.finance.voucher.service.IFinVoucherEntriesService;
import com.wimoor.finance.voucher.service.IFinVouchersService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.*;

/**
 * 发票凭证生成策略
 * <p>
 * 根据 FinMappingInvoice 映射规则，将选中的发票转换为凭证。
 * 每张发票生成一个凭证，每个映射规则产生一对借贷分录。
 * </p>
 *
 * @author wimoor
 * @date 2026-08-21
 */
@Service
@Slf4j
public class InvoiceVoucherStrategy implements IInvoiceVoucherStrategy {

    private static final String VOUCHER_TYPE = "记"; // 发票凭证字

    @Resource
    private IFinMappingInvoiceService finMappingInvoiceService;
    @Resource
    private IFinInvoiceLedgerService finInvoiceLedgerService;
    @Resource
    private IFinVouchersService finVouchersService;
    @Resource
    private IFinVoucherEntriesService finVoucherEntriesService;
    @Resource
    private IFinAccountingPeriodsService finAccountingPeriodsService;
    @Resource
    private IFinMappingVouchersSourceService finMappingVouchersSourceService;

    @Override
    public Map<String, Object> generateVoucher(UserInfo userInfo, String groupid,
                                                List<Long> invoiceIds, String voucherType,
                                                Date voucherDate, String summary,
                                                Integer voucherStatus, Integer invoiceType) {
        Map<String, Object> result = new HashMap<>();
        result.put("totalCount", invoiceIds.size());

        // 校验会计期间
        FinAccountingPeriods period = finAccountingPeriodsService.selectFinAccountingPeriodsByDate(groupid, voucherDate);
        if (period == null) {
            throw new BizException("未找到凭证日期对应的会计期间");
        }
        if (period.getPeriodStatus() == 3) {
            throw new BizException("会计期间已关闭，无法生成凭证");
        }

        // 获取映射规则
        FinMappingInvoice queryMap = new FinMappingInvoice();
        queryMap.setGroupid(groupid);
        queryMap.setInvoiceType(invoiceType);
        List<FinMappingInvoice> mappingList = finMappingInvoiceService.selectFinMappingInvoiceList(queryMap);
        if (mappingList == null || mappingList.isEmpty()) {
            throw new BizException("未配置发票凭证映射规则，请先在财务配置中设置");
        }

        int successCount = 0;
        List<String> errors = new ArrayList<>();

        for (Long invoiceId : invoiceIds) {
            try {
                FinInvoice invoice = finInvoiceLedgerService.selectFinInvoiceById(invoiceId);
                if (invoice == null) {
                    errors.add("发票ID[" + invoiceId + "]不存在");
                    continue;
                }
                if (invoice.getPostingStatus() != null && invoice.getPostingStatus() == 1) {
                    errors.add("发票[" + invoice.getInvoiceNo() + "]已入账，跳过");
                    continue;
                }

                // 构建凭证
                FinVouchers voucher = buildVoucher(userInfo, groupid, period, voucherDate, voucherStatus);

                // 根据映射规则构建分录
                List<FinVoucherEntries> entries = new ArrayList<>();
                long entryNo = 1;
                BigDecimal totalDebit = BigDecimal.ZERO;

                for (FinMappingInvoice mapping : mappingList) {
                    BigDecimal amount = invoice.getAmountWithTax() != null ?
                            invoice.getAmountWithTax() : BigDecimal.ZERO;

                    String entrySummary = mapping.getSummary() != null && !mapping.getSummary().isEmpty() ?
                            mapping.getSummary() : summary;

                    // 借方分录
                    FinVoucherEntries debitEntry = new FinVoucherEntries();
                    debitEntry.setGroupid(groupid);
                    debitEntry.setEntryNo(entryNo++);
                    debitEntry.setSubjectId(mapping.getDebitSubjectId());
                    debitEntry.setSummary(entrySummary);
                    debitEntry.setDebitAmount(amount);
                    debitEntry.setCreditAmount(BigDecimal.ZERO);
                    debitEntry.setCurrency(invoice.getCurrency() != null ? invoice.getCurrency() : "CNY");
                    if (invoice.getExchangeRate() != null) {
                        debitEntry.setExchangeRate(invoice.getExchangeRate());
                    }
                    entries.add(debitEntry);

                    // 贷方分录
                    FinVoucherEntries creditEntry = new FinVoucherEntries();
                    creditEntry.setGroupid(groupid);
                    creditEntry.setEntryNo(entryNo++);
                    creditEntry.setSubjectId(mapping.getCreditSubjectId());
                    creditEntry.setSummary(entrySummary);
                    creditEntry.setDebitAmount(BigDecimal.ZERO);
                    creditEntry.setCreditAmount(amount);
                    creditEntry.setCurrency(invoice.getCurrency() != null ? invoice.getCurrency() : "CNY");
                    if (invoice.getExchangeRate() != null) {
                        creditEntry.setExchangeRate(invoice.getExchangeRate());
                    }
                    entries.add(creditEntry);

                    totalDebit = totalDebit.add(amount);
                }

                voucher.setTotalAmount(totalDebit);
                voucher.setEntries(entries);

                // 生成凭证编号并保存
                voucher.setVoucherNo(finVouchersService.selectNextVoucherNo(voucher));
                finVouchersService.insertFinVouchers(voucher);

                // 更新发票入账状态
                finInvoiceLedgerService.batchUpdatePostingStatus(
                        Collections.singletonList(invoiceId), 1, voucher.getVoucherId(), userInfo.getUserName());

                // 保存凭证生成记录到 fin_mapping_vouchers_source 表
                saveMappingVoucherSource(groupid, invoiceId, invoice, voucher, invoiceType, userInfo.getUserName());

                successCount++;
                log.info("发票[{}]生成凭证成功，凭证号：{}，凭证ID：{}",
                        invoice.getInvoiceNo(), voucher.getVoucherNo(), voucher.getVoucherId());

            } catch (Exception e) {
                log.error("发票ID[{}]生成凭证失败", invoiceId, e);
                errors.add("发票ID[" + invoiceId + "]生成凭证失败：" + e.getMessage());
            }
        }

        result.put("successCount", successCount);
        result.put("errors", errors);

        if (successCount == 0) {
            result.put("message", "生成凭证失败：" + (errors.isEmpty() ? "未知错误" : String.join("；", errors)));
        } else if (!errors.isEmpty()) {
            result.put("message", "部分成功：成功" + successCount + "条，失败" + errors.size() + "条");
        } else {
            result.put("message", "生成凭证成功，共处理" + successCount + "条");
        }

        return result;
    }

    private FinVouchers buildVoucher(UserInfo userInfo, String groupid, FinAccountingPeriods period,
                                      Date voucherDate, Integer voucherStatus) {
        FinVouchers voucher = new FinVouchers();
        voucher.setGroupid(groupid);
        voucher.setVoucherType(VOUCHER_TYPE);
        voucher.setVoucherDate(voucherDate);
        voucher.setPreparerBy(userInfo.getUserName());
        voucher.setPeriodId(period.getPeriodId());
        voucher.setVoucherStatus(voucherStatus != null ? voucherStatus : 1);
        voucher.setDataSource(4); // 单据同步
        voucher.setAttachmentCount(1);
        voucher.setCreateBy(userInfo.getUserName());
        voucher.setUpdateBy(userInfo.getUserName());
        voucher.setCreatedTime(new Date());
        voucher.setUpdatedTime(new Date());
        return voucher;
    }

    private void saveMappingVoucherSource(String groupid, Long invoiceId, FinInvoice invoice,
                                           FinVouchers voucher, Integer invoiceType, String userName) {
        FinMappingVouchersSource voucherLog = new FinMappingVouchersSource();
        voucherLog.setGroupid(groupid);
        voucherLog.setOrderId(String.valueOf(invoiceId));
        voucherLog.setOrderNumber(invoice.getInvoiceNo() != null ? invoice.getInvoiceNo() : invoice.getDigitalInvoiceNo());
        voucherLog.setSupplierName(invoice.getSellerName());
        voucherLog.setTotalAmount(invoice.getAmountWithTax());
        voucherLog.setVoucherType(invoiceType != null && invoiceType == 1 ? "invoice_carrier" : "invoice");
        voucherLog.setVoucherId(voucher.getVoucherId());
        voucherLog.setSyncStatus(1);
        voucherLog.setSyncTime(new Date());
        voucherLog.setCreateBy(userName);
        finMappingVouchersSourceService.insertFinMappingVouchersSource(voucherLog);
    }
}
