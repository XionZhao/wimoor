package com.wimoor.finance.setting.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.wimoor.common.core.annotation.Excel;
import com.wimoor.common.core.web.domain.BaseEntity;

import java.util.Date;

/**
 * 映射凭证关联对象 fin_mapping_vouchers
 * 
 * @author wimoor
 * @date 2025-07-09
 */
public class FinMappingVouchers extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long id;

    /** 租户ID（账簿） */
    private String groupid;

    /** 凭证ID */
    @Excel(name = "凭证ID")
    private Long vouchersId;

    /** 数据日志（JSON） */
    private String datalog;

    /** 凭证日期 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date voucherDate;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createdTime;

    /** 更新时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updatedTime;

    /** 修改人 */
    private String modifyBy;

    /** 创建人 */
    private String createBy;

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

    public void setVouchersId(Long vouchersId) 
    {
        this.vouchersId = vouchersId;
    }

    public Long getVouchersId() 
    {
        return vouchersId;
    }

    public void setDatalog(String datalog) 
    {
        this.datalog = datalog;
    }

    public String getDatalog() 
    {
        return datalog;
    }

    public void setVoucherDate(Date voucherDate) 
    {
        this.voucherDate = voucherDate;
    }

    public Date getVoucherDate() 
    {
        return voucherDate;
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

    public void setModifyBy(String modifyBy) 
    {
        this.modifyBy = modifyBy;
    }

    public String getModifyBy() 
    {
        return modifyBy;
    }

    public void setCreateBy(String createBy) 
    {
        this.createBy = createBy;
    }

    public String getCreateBy() 
    {
        return createBy;
    }

    @Override
    public String toString() {
        return new org.apache.commons.lang3.builder.ToStringBuilder(this, org.apache.commons.lang3.builder.ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("groupid", getGroupid())
            .append("vouchersId", getVouchersId())
            .append("datalog", getDatalog())
            .append("voucherDate", getVoucherDate())
            .append("createdTime", getCreatedTime())
            .append("updatedTime", getUpdatedTime())
            .append("modifyBy", getModifyBy())
            .append("createBy", getCreateBy())
            .toString();
    }
}