package com.wimoor.finance.ledger.domain;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * 发票扩展信息对象 fin_invoice_extension
 *
 * @author wimoor
 * @date 2025-11-04
 */
public class FinInvoiceExtension
{
    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long id;

    /** 关联发票ID */
    private Long invoiceId;

    /** 特定业务类型：TRANSPORT/RAILWAY等 */
    private String businessType;

    /** 属性名称 */
    private String attrKey;

    /** 属性值 */
    private String attrValue;

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

    public String getBusinessType() {
        return businessType;
    }

    public void setBusinessType(String businessType) {
        this.businessType = businessType;
    }

    public String getAttrKey() {
        return attrKey;
    }

    public void setAttrKey(String attrKey) {
        this.attrKey = attrKey;
    }

    public String getAttrValue() {
        return attrValue;
    }

    public void setAttrValue(String attrValue) {
        this.attrValue = attrValue;
    }

    public Date getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(Date createdTime) {
        this.createdTime = createdTime;
    }
}
