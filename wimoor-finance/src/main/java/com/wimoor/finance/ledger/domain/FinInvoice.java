package com.wimoor.finance.ledger.domain;

import java.math.BigDecimal;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.wimoor.common.core.annotation.Excel;
import com.wimoor.common.core.web.domain.BaseEntity;

/**
 * 发票台账对象 fin_invoice
 *
 * @author wimoor
 * @date 2025-11-04
 */
public class FinInvoice extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 发票ID */
    private Long id;

    /** 发票代码 */
    @Excel(name = "发票代码")
    private String invoiceCode;

    /** 发票号码 */
    @Excel(name = "发票号码")
    private String invoiceNo;

    /** 数电发票号码 */
    @Excel(name = "数电发票号码")
    private String digitalInvoiceNo;

    /** 发票类型 */
    @Excel(name = "发票类型", alias = "发票票种,票种,发票种类,invoiceType")
    private String invoiceType;

    /** 租户ID */
    @Excel(name = "租户ID")
    private String groupid;

    /** 供应商ID */
    @Excel(name = "供应商ID")
    private String supplierId;

    /** 承运商ID/销方ID（关联 t_erp_ship_transcompany.id） */
    private String carrierId;

    /** 销方名称 */
    @Excel(name = "销方名称")
    private String sellerName;

    /** 销方税号 */
    @Excel(name = "销方税号")
    private String sellerTaxNo;

    /** 购方名称 */
    @Excel(name = "购方名称")
    private String buyerName;

    /** 购方税号 */
    @Excel(name = "购方税号")
    private String buyerTaxNo;

    /** 开票日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "开票日期", width = 30, dateFormat = "yyyy-MM-dd")
    private Date invoiceDate;

    /** 开票人 */
    @Excel(name = "开票人")
    private String drawer;

    /** 含税金额 */
    @Excel(name = "含税金额")
    private BigDecimal amountWithTax;

    /** 不含税金额 */
    @Excel(name = "不含税金额")
    private BigDecimal amountWithoutTax;

    /** 税额 */
    @Excel(name = "税额")
    private BigDecimal taxAmount;

    /** 币种 */
    @Excel(name = "币种")
    private String currency;

    /** 汇率 */
    @Excel(name = "汇率")
    private BigDecimal exchangeRate;

    /** 发票状态 */
    @Excel(name = "发票状态")
    private String status;

    /** 入账状态：0-未入账，1-已入账 */
    @Excel(name = "入账状态：0-未入账，1-已入账")
    private Integer postingStatus;

    /** 凭证ID */
    @Excel(name = "凭证ID")
    private Long voucherId;

    /** 关联采购订单ID */
    @Excel(name = "关联采购订单ID")
    private String purchaseOrderIds;

    /** 关联付款记录ID */
    @Excel(name = "关联付款记录ID")
    private String paymentIds;

    /** 附件地址 */
    @Excel(name = "附件地址")
    private String attachmentUrls;

    /** 备注 */
    @Excel(name = "备注")
    private String remark;

    /** 发票来源 */
    private String source;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "创建时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date createdTime;

    /** 更新时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updatedTime;

    /** 创建人 */
    @Excel(name = "创建人")
    private String createdBy;

    /** 更新人 */
    private String updatedBy;

    /** 查询用：开始日期 */
    private Date startDate;

    /** 查询用：结束日期 */
    private Date endDate;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getInvoiceCode() {
        return invoiceCode;
    }

    public void setInvoiceCode(String invoiceCode) {
        this.invoiceCode = invoiceCode;
    }

    public String getInvoiceNo() {
        return invoiceNo;
    }

    public void setInvoiceNo(String invoiceNo) {
        this.invoiceNo = invoiceNo;
    }

    public String getDigitalInvoiceNo() {
        return digitalInvoiceNo;
    }

    public void setDigitalInvoiceNo(String digitalInvoiceNo) {
        this.digitalInvoiceNo = digitalInvoiceNo;
    }

    public String getInvoiceType() {
        return invoiceType;
    }

    public void setInvoiceType(String invoiceType) {
        this.invoiceType = invoiceType;
    }

    public String getGroupid() {
        return groupid;
    }

    public void setGroupid(String groupid) {
        this.groupid = groupid;
    }

    public String getSupplierId() {
        return supplierId;
    }

    public void setSupplierId(String supplierId) {
        this.supplierId = supplierId;
    }

    public String getCarrierId() {
        return carrierId;
    }

    public void setCarrierId(String carrierId) {
        this.carrierId = carrierId;
    }

    public String getSellerName() {
        return sellerName;
    }

    public void setSellerName(String sellerName) {
        this.sellerName = sellerName;
    }

    public String getSellerTaxNo() {
        return sellerTaxNo;
    }

    public void setSellerTaxNo(String sellerTaxNo) {
        this.sellerTaxNo = sellerTaxNo;
    }

    public String getBuyerName() {
        return buyerName;
    }

    public void setBuyerName(String buyerName) {
        this.buyerName = buyerName;
    }

    public String getBuyerTaxNo() {
        return buyerTaxNo;
    }

    public void setBuyerTaxNo(String buyerTaxNo) {
        this.buyerTaxNo = buyerTaxNo;
    }

    public Date getInvoiceDate() {
        return invoiceDate;
    }

    public void setInvoiceDate(Date invoiceDate) {
        this.invoiceDate = invoiceDate;
    }

    public String getDrawer() {
        return drawer;
    }

    public void setDrawer(String drawer) {
        this.drawer = drawer;
    }

    public BigDecimal getAmountWithTax() {
        return amountWithTax;
    }

    public void setAmountWithTax(BigDecimal amountWithTax) {
        this.amountWithTax = amountWithTax;
    }

    public BigDecimal getAmountWithoutTax() {
        return amountWithoutTax;
    }

    public void setAmountWithoutTax(BigDecimal amountWithoutTax) {
        this.amountWithoutTax = amountWithoutTax;
    }

    public BigDecimal getTaxAmount() {
        return taxAmount;
    }

    public void setTaxAmount(BigDecimal taxAmount) {
        this.taxAmount = taxAmount;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getPostingStatus() {
        return postingStatus;
    }

    public void setPostingStatus(Integer postingStatus) {
        this.postingStatus = postingStatus;
    }

    public Long getVoucherId() {
        return voucherId;
    }

    public void setVoucherId(Long voucherId) {
        this.voucherId = voucherId;
    }

    public String getPurchaseOrderIds() {
        return purchaseOrderIds;
    }

    public void setPurchaseOrderIds(String purchaseOrderIds) {
        this.purchaseOrderIds = purchaseOrderIds;
    }

    public String getPaymentIds() {
        return paymentIds;
    }

    public void setPaymentIds(String paymentIds) {
        this.paymentIds = paymentIds;
    }

    public String getAttachmentUrls() {
        return attachmentUrls;
    }

    public void setAttachmentUrls(String attachmentUrls) {
        this.attachmentUrls = attachmentUrls;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public Date getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(Date createdTime) {
        this.createdTime = createdTime;
    }

    public Date getUpdatedTime() {
        return updatedTime;
    }

    public void setUpdatedTime(Date updatedTime) {
        this.updatedTime = updatedTime;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public Date getEndDate() {
        return endDate;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("invoiceNo", getInvoiceNo())
            .append("invoiceType", getInvoiceType())
            .append("groupid", getGroupid())
            .append("supplierId", getSupplierId())
            .append("sellerName", getSellerName())
            .append("sellerTaxNo", getSellerTaxNo())
            .append("invoiceDate", getInvoiceDate())
            .append("amountWithTax", getAmountWithTax())
            .append("amountWithoutTax", getAmountWithoutTax())
            .append("taxAmount", getTaxAmount())
            .append("currency", getCurrency())
            .append("exchangeRate", getExchangeRate())
            .append("status", getStatus())
            .append("postingStatus", getPostingStatus())
            .append("voucherId", getVoucherId())
            .append("purchaseOrderIds", getPurchaseOrderIds())
            .append("paymentIds", getPaymentIds())
            .append("remark", getRemark())
            .append("createdTime", getCreatedTime())
            .append("updatedTime", getUpdatedTime())
            .append("createdBy", getCreatedBy())
            .append("updatedBy", getUpdatedBy())
            .toString();
    }
}
