package com.wimoor.finance.ledger.domain;

import java.math.BigDecimal;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * 发票明细行对象 fin_invoice_detail
 *
 * @author wimoor
 * @date 2025-11-04
 */
public class FinInvoiceDetail
{
    private static final long serialVersionUID = 1L;

    /** 明细行ID */
    private Long id;

    /** 关联发票ID */
    private Long invoiceId;

    /** 行号 */
    private Integer lineNo;

    /** 货物或应税劳务名称 */
    private String goodsName;

    /** 规格型号 */
    private String specModel;

    /** 单位 */
    private String unit;

    /** 数量 */
    private BigDecimal quantity;

    /** 不含税单价 */
    private BigDecimal unitPrice;

    /** 该行不含税金额 */
    private BigDecimal amountWithoutTax;

    /** 税率 */
    private String taxRate;

    /** 该行税额 */
    private BigDecimal taxAmount;

    /** 该行价税合计 */
    private BigDecimal amountWithTax;

    /** 税收分类编码 */
    private String taxCategoryCode;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createdTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getInvoiceId() {
        return invoiceId;
    }

    public void setInvoiceId(Long invoiceId) {
        this.invoiceId = invoiceId;
    }

    public Integer getLineNo() {
        return lineNo;
    }

    public void setLineNo(Integer lineNo) {
        this.lineNo = lineNo;
    }

    public String getGoodsName() {
        return goodsName;
    }

    public void setGoodsName(String goodsName) {
        this.goodsName = goodsName;
    }

    public String getSpecModel() {
        return specModel;
    }

    public void setSpecModel(String specModel) {
        this.specModel = specModel;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public BigDecimal getAmountWithoutTax() {
        return amountWithoutTax;
    }

    public void setAmountWithoutTax(BigDecimal amountWithoutTax) {
        this.amountWithoutTax = amountWithoutTax;
    }

    public String getTaxRate() {
        return taxRate;
    }

    public void setTaxRate(String taxRate) {
        this.taxRate = taxRate;
    }

    public BigDecimal getTaxAmount() {
        return taxAmount;
    }

    public void setTaxAmount(BigDecimal taxAmount) {
        this.taxAmount = taxAmount;
    }

    public BigDecimal getAmountWithTax() {
        return amountWithTax;
    }

    public void setAmountWithTax(BigDecimal amountWithTax) {
        this.amountWithTax = amountWithTax;
    }

    public String getTaxCategoryCode() {
        return taxCategoryCode;
    }

    public void setTaxCategoryCode(String taxCategoryCode) {
        this.taxCategoryCode = taxCategoryCode;
    }

    public Date getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(Date createdTime) {
        this.createdTime = createdTime;
    }
}
