package com.wimoor.finance.closing.service.strategy.impl;

import cn.hutool.core.util.StrUtil;
import com.wimoor.common.mvc.BizException;
import com.wimoor.common.user.UserInfo;
import com.wimoor.finance.closing.domain.FinClosingTemplate;
import com.wimoor.finance.closing.domain.FinClosingTemplateProfitLoss;
import com.wimoor.finance.closing.domain.FinClosingTemplateVouchers;
import com.wimoor.finance.closing.service.IFinClosingTemplateProfitLossService;
import com.wimoor.finance.closing.service.IFinClosingTemplateService;
import com.wimoor.finance.closing.service.IFinClosingTemplateVouchersService;
import com.wimoor.finance.closing.service.strategy.IFinClosingTemplateStrategy;
import com.wimoor.finance.ledger.domain.FinGeneralLedger;
import com.wimoor.finance.ledger.service.IFinGeneralLedgerService;
import com.wimoor.finance.voucher.service.IFinVoucherEntriesService;
import com.wimoor.finance.setting.domain.FinAccountingPeriods;
import com.wimoor.finance.setting.domain.FinAccountingSubjects;
import com.wimoor.finance.setting.service.IFinAccountingPeriodsService;
import com.wimoor.finance.setting.service.IFinAccountingSubjectsService;
import com.wimoor.finance.voucher.domain.FinVoucherEntries;
import com.wimoor.finance.voucher.domain.FinVouchers;
import com.wimoor.finance.voucher.service.IFinVouchersService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
@Slf4j
public class ProfitLossTemplateStrategy implements IFinClosingTemplateStrategy {

    private static final String FTYPE = "loss";

    @Resource
    IFinClosingTemplateService finClosingTemplateService;
    @Resource
    IFinClosingTemplateProfitLossService finClosingTemplateProfitLossService;
    @Resource
    IFinVouchersService iFinVouchersService;
    @Resource
    IFinClosingTemplateVouchersService finClosingTemplateVouchersService;
    @Resource
    IFinAccountingPeriodsService iFinAccountingPeriodsService;
    @Resource
    IFinAccountingSubjectsService finAccountingSubjectsService;
    @Resource
    IFinGeneralLedgerService finGeneralLedgerService;
    @Resource
    IFinVoucherEntriesService finVoucherEntriesService;

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
     * 根据模板ID和租户ID获取结转损益配置
     */
    private FinClosingTemplateProfitLoss getProfitLossConfig(String templateId, String groupid) {
        FinClosingTemplateProfitLoss query = new FinClosingTemplateProfitLoss();
        query.setTemplateId(templateId);
        query.setGroupid(groupid);
        List<FinClosingTemplateProfitLoss> list = finClosingTemplateProfitLossService.selectFinClosingTemplateProfitLossList(query);
        if (list != null && !list.isEmpty()) {
            return list.get(0);
        }
        return null;
    }

    /**
     * 判断是否为年末期间（12月）
     * 期间编码格式为YYYYMM，如"202612"表示2026年12月
     */
    private boolean isYearEndPeriod(String periodCode) {
        if (StrUtil.isBlank(periodCode) || periodCode.length() < 6) {
            return false;
        }
        return periodCode.endsWith("12");
    }

    /**
     * 根据科目编码查找科目
     */
    private FinAccountingSubjects getSubjectByCode(String groupid, String subjectCode) {
        if (StrUtil.isBlank(subjectCode)) {
            return null;
        }
        return finAccountingSubjectsService.selectByGroupCode(groupid, subjectCode);
    }

    @Override
    public void generateVoucher(UserInfo userInfo, String templateId, String periodCode) {
        // 第一步：获取模板对象
        FinClosingTemplate template = finClosingTemplateService.selectFinClosingTemplateById(templateId);
        if (template == null) {
            throw new BizException("模板不存在");
        }
        
        log.info("模板信息: id={}, groupid={}, ftype={}", template.getId(), template.getGroupid(), template.getFtype());

        // 第二步：获取结转损益配置
        FinClosingTemplateProfitLoss plConfig = getProfitLossConfig(templateId, template.getGroupid());
        if (plConfig == null) {
            log.error("未找到结转损益配置: templateId={}, groupid={}", templateId, template.getGroupid());
            throw new BizException("未找到结转损益配置，请先进行设置");
        }

        // 第三步：获取会计期间
        FinAccountingPeriods period = getPeriod(template.getGroupid(), periodCode);
        String periodCodeForLedger = period.getPeriodCode();

        // 第四步：确定凭证日期
        Date voucherDate= period.getEndDate();


        // 第五步：获取结转目标科目（损益类科目的结转科目，通常为"本年利润"）
        String targetSubjectCode = plConfig.getProfitLossSubjectCode();
        if (StrUtil.isBlank(targetSubjectCode)) {
            targetSubjectCode = "3103";
        }
        FinAccountingSubjects targetSubject = getSubjectByCode(template.getGroupid(), targetSubjectCode);
        if (targetSubject == null) {
            throw new BizException("未找到结转目标科目：" + targetSubjectCode + "，请检查配置");
        }

        // 第六步：获取所有损益类科目
        List<FinAccountingSubjects> profitLossSubjects = finAccountingSubjectsService.getProfitLossSubjects(template.getGroupid());
        if (profitLossSubjects == null || profitLossSubjects.isEmpty()) {
            throw new BizException("未配置损益类科目");
        }
        log.info("损益类科目总数: {}, groupid={}", profitLossSubjects.size(), template.getGroupid());

        // 第七步：确定凭证分类方式
        Integer voucherClass = template.getVoucherClass();
        boolean separateVoucher = voucherClass != null && voucherClass == 1;

        // 第八步：确定摘要
        String summary = StrUtil.isNotBlank(plConfig.getSummary()) ? plConfig.getSummary() : "结转本期损益";

        // 第九步：确定方向处理方式
        boolean positiveAmount = plConfig.getDirectionHandling() != null && plConfig.getDirectionHandling() == 1;

        // 第十步：按科目分类收集需要结转的数据（基于凭证汇总，而非总账）
        List<SubjectTransferInfo> incomeTransfers = new ArrayList<>();
        List<SubjectTransferInfo> expenseTransfers = new ArrayList<>();

        int noVoucherEntryCount = 0;
        int zeroEntryCount = 0;
        int noDirectionCount = 0;
        Date startDate = period.getStartDate();
        Date endDate = period.getEndDate();
        for (FinAccountingSubjects subject : profitLossSubjects) {
            if (subject.getSubjectId() == null) {
                noVoucherEntryCount++;
                continue;
            }

            // 通过凭证分录汇总获取本期发生额，排除结账模板生成的凭证
            Map<String, Object> summaryMap = finVoucherEntriesService.sumBySubjectAndPeriod(
                    template.getGroupid(), startDate, endDate, subject.getSubjectId().toString());

            if (summaryMap == null || summaryMap.isEmpty()) {
                noVoucherEntryCount++;
                continue;
            }

            BigDecimal debitTotal = (BigDecimal) summaryMap.get("debitTotal");
            BigDecimal creditTotal = (BigDecimal) summaryMap.get("creditTotal");
            if (debitTotal == null) debitTotal = BigDecimal.ZERO;
            if (creditTotal == null) creditTotal = BigDecimal.ZERO;

            if (debitTotal.compareTo(BigDecimal.ZERO) == 0 && creditTotal.compareTo(BigDecimal.ZERO) == 0) {
                zeroEntryCount++;
                continue;
            }

            Integer direction = subject.getDirection();
            if (direction == null) {
                noDirectionCount++;
                continue;
            }

            // 根据科目方向计算净发生额
            BigDecimal balance;
            if (direction == 2) {
                // 收入类（贷方余额）：净额 = 贷方合计 - 借方合计
                balance = creditTotal.subtract(debitTotal);
            } else {
                // 费用类（借方余额）：净额 = 借方合计 - 贷方合计
                balance = debitTotal.subtract(creditTotal);
            }

            if (balance.compareTo(BigDecimal.ZERO) == 0) {
                zeroEntryCount++;
                continue;
            }

            SubjectTransferInfo info = new SubjectTransferInfo();
            info.subject = subject;
            info.balance = positiveAmount ? balance.abs() : balance;
            info.direction = direction;
            info.summary = summary;

            if (direction == 2) {
                incomeTransfers.add(info);
            } else {
                expenseTransfers.add(info);
            }
        }

        log.info("损益结转统计(凭证汇总): 科目总数={}, 无凭证记录={}, 发生额为零={}, 无方向={}, 收入类={}, 费用类={}, period={}",
                profitLossSubjects.size(), noVoucherEntryCount, zeroEntryCount, noDirectionCount,
                incomeTransfers.size(), expenseTransfers.size(), periodCode);

        if (incomeTransfers.isEmpty() && expenseTransfers.isEmpty()) {
            log.info("所有损益类科目本期无发生额，跳过损益结转，期间: {}", periodCode);
            return;
        }

        // 第十一步：生成凭证
        if (separateVoucher) {
            // 收益与损益分开结转
            if (!incomeTransfers.isEmpty()) {
                createAndSaveVoucher(userInfo, template, period, voucherDate,
                        incomeTransfers, targetSubject, summary, "收益结转");
            }
            if (!expenseTransfers.isEmpty()) {
                createAndSaveVoucher(userInfo, template, period, voucherDate,
                        expenseTransfers, targetSubject, summary, "损益结转");
            }
        } else {
            // 收益与损益同时结转
            List<SubjectTransferInfo> allTransfers = new ArrayList<>();
            allTransfers.addAll(incomeTransfers);
            allTransfers.addAll(expenseTransfers);
            createAndSaveVoucher(userInfo, template, period, voucherDate,
                    allTransfers, targetSubject, summary, null);
        }

        // 第十二步：处理本年利润结转（结转到未分配利润）
        // 仅在年末（12月）执行，每月结账时不执行
        if (isYearEndPeriod(periodCodeForLedger)) {
            handleCurrentYearProfitTransfer(userInfo, template, period, voucherDate, plConfig);
        } else {
            log.info("非年末期间[{}]，跳过本年利润结转（本年利润→未分配利润）", periodCodeForLedger);
        }

        // 第十三步：处理以前年度损益调整
        // 仅在年末（12月）执行
        if (isYearEndPeriod(periodCodeForLedger)) {
            handlePriorYearAdjustment(userInfo, template, period, voucherDate, plConfig);
        } else {
            log.info("非年末期间[{}]，跳过以前年度损益调整结转", periodCodeForLedger);
        }
    }

    /**
     * 处理本年利润结转
     * 将"本年利润"科目的余额结转到指定的结转科目（通常为"未分配利润"）
     */
    private void handleCurrentYearProfitTransfer(UserInfo userInfo, FinClosingTemplate template,
                                                  FinAccountingPeriods period, Date voucherDate,
                                                  FinClosingTemplateProfitLoss plConfig) {
        String groupid = template.getGroupid();
        String periodCode = period.getPeriodCode();

        // 获取本年利润科目（默认3103）
        String currentYearProfitSubjectCode = plConfig.getProfitLossSubjectCode();
        if (StrUtil.isBlank(currentYearProfitSubjectCode)) {
            currentYearProfitSubjectCode = "3103";
        }
        FinAccountingSubjects currentYearProfitSubject = getSubjectByCode(groupid, currentYearProfitSubjectCode);
        if (currentYearProfitSubject == null) {
            log.info("未找到本年利润科目：{}，跳过处理", currentYearProfitSubjectCode);
            return;
        }

        // 获取本年利润的结转科目（默认310415-未分配利润）
        String transferSubjectCode = plConfig.getCurrentYearProfitSubjectCode();
        if (StrUtil.isBlank(transferSubjectCode)) {
            transferSubjectCode = "310415";
        }
        FinAccountingSubjects transferSubject = getSubjectByCode(groupid, transferSubjectCode);
        if (transferSubject == null) {
            log.info("未找到本年利润的结转科目：{}，跳过处理", transferSubjectCode);
            return;
        }

        // 查询本年利润科目的总账余额
        FinGeneralLedger ledger = finGeneralLedgerService.selectBySubjectAndPeriod(
                groupid, currentYearProfitSubject.getSubjectId().toString(), periodCode);

        if (ledger == null || ledger.getEndBalance() == null
                || ledger.getEndBalance().compareTo(BigDecimal.ZERO) == 0) {
            log.info("本年利润科目[{}]余额为零，跳过结转", currentYearProfitSubjectCode);
            return;
        }

        BigDecimal balance = ledger.getEndBalance();
        Integer direction = ledger.getEndDirection();
        if (direction == null) {
            direction = currentYearProfitSubject.getDirection();
        }
        if (direction == null) {
            log.warn("本年利润科目[{}]方向为空，跳过结转", currentYearProfitSubjectCode);
            return;
        }

        // 生成本年利润结转凭证
        String profitSummary = "结转本年利润";
        List<SubjectTransferInfo> profitTransfers = new ArrayList<>();
        SubjectTransferInfo info = new SubjectTransferInfo();
        info.subject = currentYearProfitSubject;
        info.balance = balance;
        info.direction = direction;
        info.summary = profitSummary;
        profitTransfers.add(info);

        createAndSaveVoucher(userInfo, template, period, voucherDate,
                profitTransfers, transferSubject, profitSummary, "本年利润结转");
    }

    /**
     * 处理以前年度损益调整
     * 将"以前年度损益调整"科目的余额结转到指定的结转科目（通常为"未分配利润"）
     */
    private void handlePriorYearAdjustment(UserInfo userInfo, FinClosingTemplate template,
                                            FinAccountingPeriods period, Date voucherDate,
                                            FinClosingTemplateProfitLoss plConfig) {
        String groupid = template.getGroupid();
        String periodCode = period.getPeriodCode();

        // 获取以前年度损益调整科目（默认6000）
        String priorYearAdjustSubjectCode = plConfig.getPriorYearAdjustmentSubjectCode();
        if (StrUtil.isBlank(priorYearAdjustSubjectCode)) {
            priorYearAdjustSubjectCode = "6000";
        }
        FinAccountingSubjects priorYearAdjustSubject = getSubjectByCode(groupid, priorYearAdjustSubjectCode);
        if (priorYearAdjustSubject == null) {
            log.info("未找到以前年度损益调整科目：{}，跳过处理", priorYearAdjustSubjectCode);
            return;
        }

        // 获取以前年度损益调整科目的结转科目（默认310415-未分配利润）
        String priorYearTransferSubjectCode = plConfig.getPriorYearAdjustTransferSubjectCode();
        if (StrUtil.isBlank(priorYearTransferSubjectCode)) {
            priorYearTransferSubjectCode = "310415";
        }
        FinAccountingSubjects priorYearTransferSubject = getSubjectByCode(groupid, priorYearTransferSubjectCode);
        if (priorYearTransferSubject == null) {
            log.info("未找到以前年度损益调整的结转科目：{}，跳过处理", priorYearTransferSubjectCode);
            return;
        }

        // 查询以前年度损益调整科目的总账余额
        FinGeneralLedger ledger = finGeneralLedgerService.selectBySubjectAndPeriod(
                groupid, priorYearAdjustSubject.getSubjectId().toString(), periodCode);

        if (ledger == null || ledger.getEndBalance() == null
                || ledger.getEndBalance().compareTo(BigDecimal.ZERO) == 0) {
            log.info("以前年度损益调整科目[{}]余额为零，跳过结转", priorYearAdjustSubjectCode);
            return;
        }

        BigDecimal balance = ledger.getEndBalance();
        Integer direction = ledger.getEndDirection();
        if (direction == null) {
            direction = priorYearAdjustSubject.getDirection();
        }
        if (direction == null) {
            log.warn("以前年度损益调整科目[{}]方向为空，跳过结转", priorYearAdjustSubjectCode);
            return;
        }

        // 生成以前年度损益调整结转凭证
        String priorYearSummary = "结转以前年度损益调整";
        List<SubjectTransferInfo> priorYearTransfers = new ArrayList<>();
        SubjectTransferInfo info = new SubjectTransferInfo();
        info.subject = priorYearAdjustSubject;
        info.balance = balance;
        info.direction = direction;
        info.summary = priorYearSummary;
        priorYearTransfers.add(info);

        createAndSaveVoucher(userInfo, template, period, voucherDate,
                priorYearTransfers, priorYearTransferSubject, priorYearSummary, "以前年度损益调整");
    }

    /**
     * 创建并保存凭证
     */
    private void createAndSaveVoucher(UserInfo userInfo, FinClosingTemplate template,
                                       FinAccountingPeriods period, Date voucherDate,
                                       List<SubjectTransferInfo> transfers,
                                       FinAccountingSubjects targetSubject,
                                       String summary, String voucherSuffix) {
        FinVouchers finVouchers = new FinVouchers();
        finVouchers.setVoucherType(template.getVoucherType());
        finVouchers.setGroupid(template.getGroupid());
        finVouchers.setVoucherDate(voucherDate);
        finVouchers.setVoucherNo(iFinVouchersService.selectNextVoucherNo(finVouchers));
        finVouchers.setVoucherStatus(3);
        finVouchers.setDataSource(3); // 结账模版生成

        List<FinVoucherEntries> entryList = new ArrayList<>();
        BigDecimal debitTotal = BigDecimal.ZERO;
        BigDecimal creditTotal = BigDecimal.ZERO;
        long entryNo = 1;
        StringBuilder datalog = new StringBuilder();

        String entrySummary = summary;
        if (StrUtil.isNotBlank(voucherSuffix)) {
            entrySummary = summary + "（" + voucherSuffix + "）";
        }

        // 在datalog开头存储凭证类型前缀，用于后续查询时区分收入/费用凭证
        if (StrUtil.isNotBlank(voucherSuffix)) {
            datalog.append("[").append(voucherSuffix).append("]");
        }

        // 按科目余额方向分组统计
        // 目标科目（本年利润）的借贷方合计
        BigDecimal targetCreditTotal = BigDecimal.ZERO; // 本年利润贷方
        BigDecimal targetDebitTotal = BigDecimal.ZERO;  // 本年利润借方

        for (SubjectTransferInfo transfer : transfers) {
            FinAccountingSubjects subject = transfer.subject;
            BigDecimal balance = transfer.balance;
            Integer direction = transfer.direction;
            BigDecimal absBalance = balance.abs();
            boolean isNegative = balance.compareTo(BigDecimal.ZERO) < 0;

            if (direction == 2) {
                // 收入类科目
                if (isNegative) {
                    // 余额为负（净借方）：贷方记该科目归零，借方记本年利润
                    FinVoucherEntries creditEntry = new FinVoucherEntries();
                    creditEntry.setSubjectId(subject.getSubjectId().toString());
                    creditEntry.setCreditAmount(absBalance);
                    creditEntry.setSummary(entrySummary);
                    creditEntry.setEntryNo(entryNo++);
                    entryList.add(creditEntry);
                    creditTotal = creditTotal.add(absBalance);
                    targetDebitTotal = targetDebitTotal.add(absBalance);
                    datalog.append(creditEntry.getEntryNo()).append("-贷-").append(subject.getSubjectName())
                            .append("-").append(absBalance).append(";");
                } else {
                    // 余额为正（净贷方）：借方记该科目归零，贷方记本年利润
                    FinVoucherEntries debitEntry = new FinVoucherEntries();
                    debitEntry.setSubjectId(subject.getSubjectId().toString());
                    debitEntry.setDebitAmount(absBalance);
                    debitEntry.setSummary(entrySummary);
                    debitEntry.setEntryNo(entryNo++);
                    entryList.add(debitEntry);
                    debitTotal = debitTotal.add(absBalance);
                    targetCreditTotal = targetCreditTotal.add(absBalance);
                    datalog.append(debitEntry.getEntryNo()).append("-借-").append(subject.getSubjectName())
                            .append("-").append(absBalance).append(";");
                }
            } else {
                // 费用类科目
                if (isNegative) {
                    // 余额为负（净贷方）：借方记该科目归零，贷方记本年利润
                    FinVoucherEntries debitEntry = new FinVoucherEntries();
                    debitEntry.setSubjectId(subject.getSubjectId().toString());
                    debitEntry.setDebitAmount(absBalance);
                    debitEntry.setSummary(entrySummary);
                    debitEntry.setEntryNo(entryNo++);
                    entryList.add(debitEntry);
                    debitTotal = debitTotal.add(absBalance);
                    targetCreditTotal = targetCreditTotal.add(absBalance);
                    datalog.append(debitEntry.getEntryNo()).append("-借-").append(subject.getSubjectName())
                            .append("-").append(absBalance).append(";");
                } else {
                    // 余额为正（净借方）：贷方记该科目归零，借方记本年利润
                    FinVoucherEntries creditEntry = new FinVoucherEntries();
                    creditEntry.setSubjectId(subject.getSubjectId().toString());
                    creditEntry.setCreditAmount(absBalance);
                    creditEntry.setSummary(entrySummary);
                    creditEntry.setEntryNo(entryNo++);
                    entryList.add(creditEntry);
                    creditTotal = creditTotal.add(absBalance);
                    targetDebitTotal = targetDebitTotal.add(absBalance);
                    datalog.append(creditEntry.getEntryNo()).append("-贷-").append(subject.getSubjectName())
                            .append("-").append(absBalance).append(";");
                }
            }
        }

        // 汇总目标科目（本年利润）分录
        if (targetCreditTotal.compareTo(BigDecimal.ZERO) > 0) {
            // 贷：本年利润
            FinVoucherEntries creditEntry = new FinVoucherEntries();
            creditEntry.setSubjectId(targetSubject.getSubjectId().toString());
            creditEntry.setCreditAmount(targetCreditTotal);
            creditEntry.setSummary(entrySummary);
            creditEntry.setEntryNo(entryNo++);
            entryList.add(creditEntry);
            creditTotal = creditTotal.add(targetCreditTotal);
            datalog.append(creditEntry.getEntryNo()).append("-贷-").append(targetSubject.getSubjectName())
                    .append("-").append(targetCreditTotal).append(";");
        }
        if (targetDebitTotal.compareTo(BigDecimal.ZERO) > 0) {
            // 借：本年利润
            FinVoucherEntries debitEntry = new FinVoucherEntries();
            debitEntry.setSubjectId(targetSubject.getSubjectId().toString());
            debitEntry.setDebitAmount(targetDebitTotal);
            debitEntry.setSummary(entrySummary);
            debitEntry.setEntryNo(entryNo++);
            entryList.add(debitEntry);
            debitTotal = debitTotal.add(targetDebitTotal);
            datalog.append(debitEntry.getEntryNo()).append("-借-").append(targetSubject.getSubjectName())
                    .append("-").append(targetDebitTotal).append(";");
        }

        // 检查借贷平衡
        if (debitTotal.compareTo(creditTotal) != 0) {
            throw new BizException("凭证生成失败：借贷不平衡，借方金额：" + debitTotal + "，贷方金额：" + creditTotal);
        }

        finVouchers.setTotalAmount(debitTotal);
        finVouchers.setEntries(entryList);

        // 检查是否已存在该模板该期间的凭证（按凭证类型区分，避免收入/费用凭证互相覆盖）
        FinClosingTemplateVouchers queryVouchers = new FinClosingTemplateVouchers();
        queryVouchers.setTemplateId(template.getId());
        queryVouchers.setGroupid(template.getGroupid());
        queryVouchers.setVoucherDate(voucherDate);
        List<FinClosingTemplateVouchers> existingList = finClosingTemplateVouchersService.selectFinClosingTemplateVouchersList(queryVouchers);
        FinClosingTemplateVouchers existingTemplateVouchers = null;

        if (existingList != null && !existingList.isEmpty()) {
            if (StrUtil.isNotBlank(voucherSuffix)) {
                // 分开结转模式：通过datalog前缀匹配对应类型的凭证
                String suffixPrefix = "[" + voucherSuffix + "]";
                for (FinClosingTemplateVouchers existing : existingList) {
                    if (existing.getDatalog() != null && existing.getDatalog().startsWith(suffixPrefix)) {
                        existingTemplateVouchers = existing;
                        break;
                    }
                }
                // 兼容旧数据：如果前缀匹配失败，且仅有一条无前缀的旧记录，则复用
                if (existingTemplateVouchers == null && existingList.size() == 1) {
                    FinClosingTemplateVouchers onlyExisting = existingList.get(0);
                    if (onlyExisting.getDatalog() == null || !onlyExisting.getDatalog().startsWith("[")) {
                        existingTemplateVouchers = onlyExisting;
                        log.info("复用旧格式记录（无前缀），将更新为[{}]前缀", voucherSuffix);
                    }
                }
            } else {
                // 合并结转模式：取第一条
                existingTemplateVouchers = existingList.get(0);
            }
        }

        if (existingTemplateVouchers != null) {
            // 更新现有凭证
            FinVouchers oldFinVouchers = iFinVouchersService.selectFinVouchersByVoucherId(
                    Long.valueOf(existingTemplateVouchers.getVourchesId()));
            finVouchers.setVoucherNo(oldFinVouchers.getVoucherNo());
            finVouchers.setUpdateBy(userInfo.getUserName());
            finVouchers.setUpdatedTime(new Date());
            finVouchers.setVoucherId(Long.valueOf(existingTemplateVouchers.getVourchesId()));
            for (FinVoucherEntries entry : finVouchers.getEntries()) {
                entry.setVoucherId(Long.valueOf(existingTemplateVouchers.getVourchesId()));
            }
            iFinVouchersService.updateFinVouchers(finVouchers);

            existingTemplateVouchers.setDatalog(datalog.toString());
            existingTemplateVouchers.setUpdateBy(userInfo.getUserName());
            existingTemplateVouchers.setVoucherDate(voucherDate);
            existingTemplateVouchers.setUpdatedTime(new Date());
            finClosingTemplateVouchersService.updateFinClosingTemplateVouchers(existingTemplateVouchers);
        } else {
            finVouchers.setCreateBy(userInfo.getUserName());
            finVouchers.setUpdateBy(userInfo.getUserName());
            finVouchers.setCreatedTime(new Date());
            finVouchers.setUpdatedTime(new Date());
            iFinVouchersService.insertFinVouchers(finVouchers);

            FinClosingTemplateVouchers templateVouchers = new FinClosingTemplateVouchers();
            templateVouchers.setTemplateId(template.getId());
            templateVouchers.setGroupid(template.getGroupid());
            templateVouchers.setDatalog(datalog.toString());
            templateVouchers.setVourchesId(finVouchers.getVoucherId().toString());
            templateVouchers.setVoucherDate(voucherDate);
            templateVouchers.setCreatedTime(new Date());
            templateVouchers.setUpdatedTime(new Date());
            templateVouchers.setCreateBy(userInfo.getUserName());
            templateVouchers.setUpdateBy(userInfo.getUserName());
            finClosingTemplateVouchersService.insertFinClosingTemplateVouchers(templateVouchers);
        }
    }

    /**
     * 科目结转信息内部类
     */
    private static class SubjectTransferInfo {
        FinAccountingSubjects subject;
        BigDecimal balance;
        Integer direction;
        String summary;
    }

    @Override
    public void initTemplateItem(FinClosingTemplate template) {
        // 结转损益模板不需要初始化Item
    }
    
    @Override
    public Map<String, Object> getCalculationDetail(String templateId, String periodCode) {
        Map<String, Object> result = new java.util.HashMap<>();
        
        FinClosingTemplate template = finClosingTemplateService.selectFinClosingTemplateById(templateId);
        if (template == null) {
            return result;
        }
        FinAccountingSubjects querySubject=new FinAccountingSubjects();
        querySubject.setGroupid(template.getGroupid());
        querySubject.setStatus(1);
        List<FinAccountingSubjects> subjects = finAccountingSubjectsService.selectFinAccountingSubjectsList(querySubject);
        Map<String, FinAccountingSubjects> codeMap = new HashMap<>();
        for (FinAccountingSubjects subject : subjects) {
            codeMap.put(subject.getSubjectCode(), subject);
        }
        String groupid = template.getGroupid();
        FinClosingTemplateProfitLoss plConfig = getProfitLossConfig(templateId, groupid);
        FinAccountingPeriods period = getPeriod(groupid, periodCode);
        
        // 目标科目
        String targetSubjectCode = plConfig != null ? plConfig.getProfitLossSubjectCode() : "3103";
        if (StrUtil.isBlank(targetSubjectCode)) targetSubjectCode = "3103";
        FinAccountingSubjects targetSubject = getSubjectByCode(groupid, targetSubjectCode);
        result.put("templateName", template.getName());
        result.put("targetSubjectCode", targetSubjectCode);
        result.put("targetSubjectName", targetSubject != null ?finAccountingSubjectsService.buildFullSubjectName(targetSubject,codeMap) : "");
        result.put("formula", "损益类科目期末余额 → 结转至 " + targetSubjectCode + " " + (targetSubject != null ? targetSubject.getSubjectName() : ""));
        
        // 获取损益类科目明细
        List<FinAccountingSubjects> profitLossSubjects = finAccountingSubjectsService.getProfitLossSubjects(groupid);
        List<Map<String, Object>> incomeItems = new ArrayList<>();
        List<Map<String, Object>> expenseItems = new ArrayList<>();
        BigDecimal totalIncome = BigDecimal.ZERO;
        BigDecimal totalExpense = BigDecimal.ZERO;
        
        if (profitLossSubjects != null) {
            Date startDate = period.getStartDate();
            Date endDate = period.getEndDate();
            for (FinAccountingSubjects subject : profitLossSubjects) {
                if (subject.getSubjectId() == null) continue;
                
                // 通过凭证分录汇总获取本期发生额
                Map<String, Object> summaryMap = finVoucherEntriesService.sumBySubjectAndPeriod(
                        groupid, startDate, endDate, subject.getSubjectId().toString());
                
                if (summaryMap == null || summaryMap.isEmpty()) continue;
                
                BigDecimal debitTotal = (BigDecimal) summaryMap.get("debitTotal");
                BigDecimal creditTotal = (BigDecimal) summaryMap.get("creditTotal");
                if (debitTotal == null) debitTotal = BigDecimal.ZERO;
                if (creditTotal == null) creditTotal = BigDecimal.ZERO;
                
                if (debitTotal.compareTo(BigDecimal.ZERO) == 0 && creditTotal.compareTo(BigDecimal.ZERO) == 0) continue;
                
                Integer direction = subject.getDirection();
                if (direction == null) continue;
                
                BigDecimal balance;
                if (direction == 2) {
                    balance = creditTotal.subtract(debitTotal);
                } else {
                    balance = debitTotal.subtract(creditTotal);
                }
                
                if (balance.compareTo(BigDecimal.ZERO) == 0) continue;
                
                Map<String, Object> item = new java.util.HashMap<>();
                item.put("subjectCode", subject.getSubjectCode());
                item.put("subjectName", subject.getSubjectCode() + " " + finAccountingSubjectsService.buildFullSubjectName(subject,codeMap));
                item.put("balance", balance);
                item.put("direction", direction);
                item.put("debitTotal", debitTotal);
                item.put("creditTotal", creditTotal);
                
                if (direction == 2) {
                    incomeItems.add(item);
                    totalIncome = totalIncome.add(balance.abs());
                } else {
                    expenseItems.add(item);
                    totalExpense = totalExpense.add(balance.abs());
                }
            }
        }
        
        result.put("incomeItems", incomeItems);
        result.put("expenseItems", expenseItems);
        result.put("totalIncome", totalIncome);
        result.put("totalExpense", totalExpense);
        result.put("periodName", period.getPeriodName());
        result.put("isYearEnd", isYearEndPeriod(period.getPeriodCode()));

        // 本年利润结转信息
        String currentYearProfitCode = plConfig != null ? plConfig.getProfitLossSubjectCode() : "3103";
        if (StrUtil.isBlank(currentYearProfitCode)) currentYearProfitCode = "3103";
        String currentYearTransferCode = plConfig != null ? plConfig.getCurrentYearProfitSubjectCode() : "310415";
        if (StrUtil.isBlank(currentYearTransferCode)) currentYearTransferCode = "310415";

        FinAccountingSubjects currentYearProfitSubject = getSubjectByCode(groupid, currentYearProfitCode);
        FinAccountingSubjects currentYearTransferSubject = getSubjectByCode(groupid, currentYearTransferCode);

        Map<String, Object> currentYearProfitInfo = new java.util.HashMap<>();
        currentYearProfitInfo.put("profitSubjectCode", currentYearProfitCode);
        currentYearProfitInfo.put("profitSubjectName", currentYearProfitSubject != null ?
                finAccountingSubjectsService.buildFullSubjectName(currentYearProfitSubject, codeMap) : "");
        currentYearProfitInfo.put("transferSubjectCode", currentYearTransferCode);
        currentYearProfitInfo.put("transferSubjectName", currentYearTransferSubject != null ?
                finAccountingSubjectsService.buildFullSubjectName(currentYearTransferSubject, codeMap) : "");

        // 查询本年利润科目余额
        if (currentYearProfitSubject != null) {
            FinGeneralLedger currentYearLedger = finGeneralLedgerService.selectBySubjectAndPeriod(
                    groupid, currentYearProfitSubject.getSubjectId().toString(), period.getPeriodCode());
            if (currentYearLedger != null && currentYearLedger.getEndBalance() != null) {
                currentYearProfitInfo.put("balance", currentYearLedger.getEndBalance());
                currentYearProfitInfo.put("direction", currentYearLedger.getEndDirection() != null ?
                        currentYearLedger.getEndDirection() : currentYearProfitSubject.getDirection());
            } else {
                currentYearProfitInfo.put("balance", BigDecimal.ZERO);
                currentYearProfitInfo.put("direction", currentYearProfitSubject.getDirection());
            }
        } else {
            currentYearProfitInfo.put("balance", BigDecimal.ZERO);
        }
        result.put("currentYearProfitInfo", currentYearProfitInfo);

        // 以前年度损益调整信息
        String priorYearAdjustSubjectCode = plConfig != null ? plConfig.getPriorYearAdjustmentSubjectCode() : "6000";
        if (StrUtil.isBlank(priorYearAdjustSubjectCode)) priorYearAdjustSubjectCode = "6000";
        String priorYearTransferSubjectCode = plConfig != null ? plConfig.getPriorYearAdjustTransferSubjectCode() : "310415";
        if (StrUtil.isBlank(priorYearTransferSubjectCode)) priorYearTransferSubjectCode = "310415";

        FinAccountingSubjects priorYearAdjustSubject = getSubjectByCode(groupid, priorYearAdjustSubjectCode);
        FinAccountingSubjects priorYearTransferSubject = getSubjectByCode(groupid, priorYearTransferSubjectCode);

        Map<String, Object> priorYearInfo = new java.util.HashMap<>();
        priorYearInfo.put("adjustSubjectCode", priorYearAdjustSubjectCode);
        priorYearInfo.put("adjustSubjectName", priorYearAdjustSubject != null ?
                finAccountingSubjectsService.buildFullSubjectName(priorYearAdjustSubject, codeMap) : "");
        priorYearInfo.put("transferSubjectCode", priorYearTransferSubjectCode);
        priorYearInfo.put("transferSubjectName", priorYearTransferSubject != null ?
                finAccountingSubjectsService.buildFullSubjectName(priorYearTransferSubject, codeMap) : "");

        // 查询以前年度损益调整科目余额
        if (priorYearAdjustSubject != null) {
            FinGeneralLedger priorYearLedger = finGeneralLedgerService.selectBySubjectAndPeriod(
                    groupid, priorYearAdjustSubject.getSubjectId().toString(), period.getPeriodCode());
            if (priorYearLedger != null && priorYearLedger.getEndBalance() != null) {
                priorYearInfo.put("balance", priorYearLedger.getEndBalance());
                priorYearInfo.put("direction", priorYearLedger.getEndDirection() != null ?
                        priorYearLedger.getEndDirection() : priorYearAdjustSubject.getDirection());
            } else {
                priorYearInfo.put("balance", BigDecimal.ZERO);
                priorYearInfo.put("direction", priorYearAdjustSubject.getDirection());
            }
        } else {
            priorYearInfo.put("balance", BigDecimal.ZERO);
        }
        result.put("priorYearAdjustInfo", priorYearInfo);

        return result;
    }
}
