package com.wimoor.finance.ledger.service;

import com.wimoor.finance.ledger.domain.FinDetailLedger;
import com.wimoor.finance.ledger.domain.dto.SubjectBalanceDTO;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface ISubjectBalanceService {
    /**
     * 获取所有科目余额
     *
     * @param groupid 集团ID
     * @param period 期间
     * @return 所有科目余额
     */
    Map<String,BigDecimal> getAllSubjectBalance(String groupid,String period);

    /**
     * 获取所有科目本期借方发生额
     *
     * @param groupid 集团ID
     * @param period 期间
     * @return 所有科目本期借方发生额
     */
    Map<String,BigDecimal> getAllSubjectDebitTotal(String groupid,String period);

    /**
     * 获取所有科目本期贷方发生额
     *
     * @param groupid 集团ID
     * @param period 期间
     * @return 所有科目本期贷方发生额
     */
    Map<String,BigDecimal> getAllSubjectCreditTotal(String groupid,String period);

    BigDecimal getSubjectBalance(String groupid, String period, String subjectCodes, String amountType);
    Map<String, BigDecimal> getSubjectBalances(String groupid, String period, List<String> subjectCodes, String amountType);
    List<FinDetailLedger> getSubjectDetails(String groupid, String period, String subjectCode);
    SubjectBalanceDTO getSubjectBalanceDetail(String groupid, String period, String subjectCode);

    /**
     * 获取所有科目的本年累计余额
     * 计算逻辑：从当年1月累加到指定期间的借方和贷方发生额
     * 用于利润表的本年累计金额计算
     *
     * @param groupid 租户ID
     * @param period 期间（格式：YYYYMM）
     * @return 所有科目的本年累计余额（key格式：ACC_科目代码）
     */
    Map<String, BigDecimal> getAllSubjectYearToDateBalance(String groupid, String period);
}
