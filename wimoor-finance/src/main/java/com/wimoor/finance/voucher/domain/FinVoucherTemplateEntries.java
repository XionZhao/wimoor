package com.wimoor.finance.voucher.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.wimoor.common.core.web.domain.BaseEntity;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * 凭证模版分录对象 fin_voucher_template_entries
 */
public class FinVoucherTemplateEntries extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 分录主键ID */
    private Long entryId;

    /** 租户ID */
    private String groupid;

    /** 关联的凭证模版ID */
    private Long templateId;

    /** 分录序号 */
    private Integer entryNo;

    /** 会计科目ID */
    private Long subjectId;

    /** 摘要说明 */
    private String summary;

    /** 借方金额 */
    private BigDecimal debitAmount;

    /** 贷方金额 */
    private BigDecimal creditAmount;

    /** 原币金额 */
    private BigDecimal originalAmount;

    /** 币种 */
    private String currency;

    /** 汇率 */
    private BigDecimal exchangeRate;

    /** 数量 */
    private Integer quantity;

    /** 单价 */
    private BigDecimal unitPrice;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createdTime;

    /** 科目编码（非数据库字段） */
    private String subjectCode;

    /** 科目名称（非数据库字段） */
    private String subjectName;

    /** 辅助核算列表（非数据库字段） */
    private List<FinVoucherTemplateEntriesAuxiliary> auxiliaryList;

    public Long getEntryId() {
        return entryId;
    }

    public void setEntryId(Long entryId) {
        this.entryId = entryId;
    }

    public String getGroupid() {
        return groupid;
    }

    public void setGroupid(String groupid) {
        this.groupid = groupid;
    }

    public Long getTemplateId() {
        return templateId;
    }

    public void setTemplateId(Long templateId) {
        this.templateId = templateId;
    }

    public Integer getEntryNo() {
        return entryNo;
    }

    public void setEntryNo(Integer entryNo) {
        this.entryNo = entryNo;
    }

    public Long getSubjectId() {
        return subjectId;
    }

    public void setSubjectId(Long subjectId) {
        this.subjectId = subjectId;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public BigDecimal getDebitAmount() {
        return debitAmount;
    }

    public void setDebitAmount(BigDecimal debitAmount) {
        this.debitAmount = debitAmount;
    }

    public BigDecimal getCreditAmount() {
        return creditAmount;
    }

    public void setCreditAmount(BigDecimal creditAmount) {
        this.creditAmount = creditAmount;
    }

    public BigDecimal getOriginalAmount() {
        return originalAmount;
    }

    public void setOriginalAmount(BigDecimal originalAmount) {
        this.originalAmount = originalAmount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public BigDecimal getExchangeRate() {
        return exchangeRate;
    }

    public void setExchangeRate(BigDecimal exchangeRate) {
        this.exchangeRate = exchangeRate;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public Date getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(Date createdTime) {
        this.createdTime = createdTime;
    }

    public String getSubjectCode() {
        return subjectCode;
    }

    public void setSubjectCode(String subjectCode) {
        this.subjectCode = subjectCode;
    }

    public String getSubjectName() {
        return subjectName;
    }

    public void setSubjectName(String subjectName) {
        this.subjectName = subjectName;
    }

    public List<FinVoucherTemplateEntriesAuxiliary> getAuxiliaryList() {
        return auxiliaryList;
    }

    public void setAuxiliaryList(List<FinVoucherTemplateEntriesAuxiliary> auxiliaryList) {
        this.auxiliaryList = auxiliaryList;
    }
}