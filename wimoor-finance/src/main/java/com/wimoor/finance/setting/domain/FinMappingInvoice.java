package com.wimoor.finance.setting.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.wimoor.common.core.annotation.Excel;
import com.wimoor.common.core.web.domain.BaseEntity;

import java.util.Date;

/**
 * 发票凭证映射模版对象 fin_mapping_invoice
 * 
 * @author wimoor
 * @date 2026-07-20
 */
public class FinMappingInvoice extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private String id;

    /** 租户ID（账簿） */
    private String groupid;

    /** 摘要 */
    @Excel(name = "摘要")
    private String summary;

    /** 发票类型：0 采购发票，1 承运商发票 */
    @Excel(name = "发票类型", readConverterExp = "0=采购发票,1=承运商发票")
    private Integer invoiceType;

    /** 借方科目ID */
    @Excel(name = "借方科目")
    private String debitSubjectId;

    /** 贷方科目ID */
    @Excel(name = "贷方科目")
    private String creditSubjectId;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createdTime;

    /** 更新时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updatedTime;

    /** 创建人 */
    private String createBy;

    /** 修改人 */
    private String updateBy;

    /** 借方科目名称（非数据库字段） */
    @JsonIgnore
    private transient String debitSubjectName;

    /** 贷方科目名称（非数据库字段） */
    @JsonIgnore
    private transient String creditSubjectName;

    public void setId(String id) 
    {
        this.id = id;
    }

    public String getId() 
    {
        return id;
    }

    public void setGroupid(String groupid) 
    {
        this.groupid = groupid;
    }

    public String getGroupid() 
    {
        return groupid;
    }

    public void setSummary(String summary) 
    {
        this.summary = summary;
    }

    public String getSummary() 
    {
        return summary;
    }

    public void setInvoiceType(Integer invoiceType) 
    {
        this.invoiceType = invoiceType;
    }

    public Integer getInvoiceType() 
    {
        return invoiceType;
    }

    public void setDebitSubjectId(String debitSubjectId) 
    {
        this.debitSubjectId = debitSubjectId;
    }

    public String getDebitSubjectId() 
    {
        return debitSubjectId;
    }

    public void setCreditSubjectId(String creditSubjectId) 
    {
        this.creditSubjectId = creditSubjectId;
    }

    public String getCreditSubjectId() 
    {
        return creditSubjectId;
    }

    public void setCreatedTime(Date createdTime) 
    {
        this.createdTime = createdTime;
    }

    public Date getCreatedTime() 
    {
        return createdTime;
    }

    public void setUpdatedTime(Date updatedTime) 
    {
        this.updatedTime = updatedTime;
    }

    public Date getUpdatedTime() 
    {
        return updatedTime;
    }

    public void setCreateBy(String createBy) 
    {
        this.createBy = createBy;
    }

    public String getCreateBy() 
    {
        return createBy;
    }

    public void setUpdateBy(String updateBy) 
    {
        this.updateBy = updateBy;
    }

    public String getUpdateBy() 
    {
        return updateBy;
    }

    public String getDebitSubjectName() 
    {
        return debitSubjectName;
    }

    public void setDebitSubjectName(String debitSubjectName) 
    {
        this.debitSubjectName = debitSubjectName;
    }

    public String getCreditSubjectName() 
    {
        return creditSubjectName;
    }

    public void setCreditSubjectName(String creditSubjectName) 
    {
        this.creditSubjectName = creditSubjectName;
    }

    @Override
    public String toString() {
        return new org.apache.commons.lang3.builder.ToStringBuilder(this, org.apache.commons.lang3.builder.ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("groupid", getGroupid())
            .append("summary", getSummary())
            .append("invoiceType", getInvoiceType())
            .append("debitSubjectId", getDebitSubjectId())
            .append("creditSubjectId", getCreditSubjectId())
            .append("createdTime", getCreatedTime())
            .append("updatedTime", getUpdatedTime())
            .append("createBy", getCreateBy())
            .append("updateBy", getUpdateBy())
            .toString();
    }
}