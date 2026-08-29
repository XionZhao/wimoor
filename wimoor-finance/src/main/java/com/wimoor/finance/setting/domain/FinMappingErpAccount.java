package com.wimoor.finance.setting.domain;

import com.wimoor.common.core.annotation.Excel;
import com.wimoor.common.core.web.domain.BaseEntity;

/**
 * 费用类型-科目映射规则对象 fin_mapping_erp_account
 * 
 * @author wimoor
 * @date 2025-07-07
 */
public class FinMappingErpAccount extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long id;

    /** 租户ID（账簿） */
    private String groupid;

    /** 费用类型ID（关联 fin_fee_type） */
    private String feeTypeId;

    /** 科目ID（关联 fin_accounting_subjects） */
    private String subjectId;

    /** 费用类型名称（关联查询） */
    @Excel(name = "费用类型名称")
    private String feeTypeName;

    /** 科目名称（关联查询） */
    @Excel(name = "科目名称")
    private String subjectName;

    public void setId(Long id) 
    {
        this.id = id;
    }

    public Long getId() 
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

    public void setFeeTypeId(String feeTypeId)
    {
        this.feeTypeId = feeTypeId;
    }

    public String getFeeTypeId()
    {
        return feeTypeId;
    }

    public void setSubjectId(String subjectId)
    {
        this.subjectId = subjectId;
    }

    public String getSubjectId()
    {
        return subjectId;
    }

    public String getFeeTypeName() 
    {
        return feeTypeName;
    }

    public void setFeeTypeName(String feeTypeName) 
    {
        this.feeTypeName = feeTypeName;
    }

    public String getSubjectName() 
    {
        return subjectName;
    }

    public void setSubjectName(String subjectName) 
    {
        this.subjectName = subjectName;
    }

    @Override
    public String toString() {
        return new org.apache.commons.lang3.builder.ToStringBuilder(this, org.apache.commons.lang3.builder.ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("groupid", getGroupid())
            .append("feeTypeId", getFeeTypeId())
            .append("subjectId", getSubjectId())
            .toString();
    }
}
