package com.wimoor.finance.ledger.domain.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.wimoor.common.core.web.domain.BaseEntity;

import java.util.Date;

/**
 * 采购账户台账查询参数
 *
 * @author wimoor
 */
public class FinPurchaseLedgerQuery extends BaseEntity {
    private static final long serialVersionUID = 1L;

    /** 租户ID */
    private String groupid;

    /** 供应商ID */
    private String supplier;

    /** 审核状态 */
    private Integer auditstatus;

    /** 付款状态 */
    private Integer paystatus;

    /** 创建时间-开始 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date fromDate;

    /** 创建时间-结束 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date toDate;

    /** 关键词搜索（订单号/供应商名称） */
    private String keyword;

    /** 订单号 */
    private String formNumber;

    /** 订单明细ID */
    private String entryId;

    public String getGroupid() {
        return groupid;
    }

    public void setGroupid(String groupid) {
        this.groupid = groupid;
    }

    public String getSupplier() {
        return supplier;
    }

    public void setSupplier(String supplier) {
        this.supplier = supplier;
    }

    public Integer getAuditstatus() {
        return auditstatus;
    }

    public void setAuditstatus(Integer auditstatus) {
        this.auditstatus = auditstatus;
    }

    public Integer getPaystatus() {
        return paystatus;
    }

    public void setPaystatus(Integer paystatus) {
        this.paystatus = paystatus;
    }

    public Date getFromDate() {
        return fromDate;
    }

    public void setFromDate(Date fromDate) {
        this.fromDate = fromDate;
    }

    public Date getToDate() {
        return toDate;
    }

    public void setToDate(Date toDate) {
        this.toDate = toDate;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public String getFormNumber() {
        return formNumber;
    }

    public void setFormNumber(String formNumber) {
        this.formNumber = formNumber;
    }

    public String getEntryId() {
        return entryId;
    }

    public void setEntryId(String entryId) {
        this.entryId = entryId;
    }
}
