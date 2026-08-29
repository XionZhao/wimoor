package com.wimoor.finance.setting.domain;

import com.wimoor.common.core.annotation.Excel;
import com.wimoor.common.core.web.domain.BaseEntity;

/**
 * 费用类型级别映射对象 fin_mapping_erp_feetype
 *
 * @author wimoor
 * @date 2025-07-07
 */
public class FinMappingErpFeetype extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long id;

    /** 租户ID（账簿） */
    private String groupid;

    /** 采购账户ID（关联 fin_purchase_account） */
    private String accountId;

    /** 科目ID（关联 fin_accounting_subjects） */
    private String subjectId;

    /** 采购账户名称（关联查询） */
    @Excel(name = "采购账户名称")
    private String accountName;

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

    public void setAccountId(String accountId)
    {
        this.accountId = accountId;
    }

    public String getAccountId()
    {
        return accountId;
    }

    public void setSubjectId(String subjectId)
    {
        this.subjectId = subjectId;
    }

    public String getSubjectId()
    {
        return subjectId;
    }

    public String getAccountName()
    {
        return accountName;
    }

    public void setAccountName(String accountName)
    {
        this.accountName = accountName;
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
            .append("accountId", getAccountId())
            .append("subjectId", getSubjectId())
            .toString();
    }
}
