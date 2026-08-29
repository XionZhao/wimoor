package com.wimoor.finance.setting.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.wimoor.common.core.annotation.Excel;
import com.wimoor.common.core.web.domain.BaseEntity;

import java.util.Date;

/**
 * 存货科目映射规则对象 fin_mapping_erp_inventory
 * 
 * @author wimoor
 * @date 2025-07-09
 */
public class FinMappingErpInventory extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long id;

    /** 租户ID（账簿） */
    private String groupid;

    /** 仓库类型：1-本地仓，2-FBA仓，3-海外仓 */
    @Excel(name = "仓库类型", readConverterExp = "1=本地仓,2=FBA仓,3=海外仓")
    private Integer warehouseType;

    /** 阶段：1-在途确认（付款时），2-入库验收（收货时） */
    @Excel(name = "阶段", readConverterExp = "1=在途确认,2=入库验收")
    private Integer stage;

    /** 借方科目ID（在途物资/库存商品） */
    private String debitSubjectId;

    /** 贷方科目ID（应付暂估/预付在途） */
    private String creditSubjectId;

    /** 借方辅助核算（通常为SKU） */
    @Excel(name = "借方辅助核算")
    private String debitAuxiliaryType;

    /** 贷方辅助核算（通常为供应商） */
    @Excel(name = "贷方辅助核算")
    private String creditAuxiliaryType;

    /** 是否启用 */
    @Excel(name = "启用状态", readConverterExp = "1=启用,0=停用")
    private Integer isEnabled;

    /** 优先级 */
    @Excel(name = "优先级")
    private Integer priority;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createdTime;

    /** 更新时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updatedTime;

    /** 借方科目名称（关联查询） */
    @Excel(name = "借方科目名称")
    private String debitSubjectName;

    /** 贷方科目名称（关联查询） */
    @Excel(name = "贷方科目名称")
    private String creditSubjectName;

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

    public void setWarehouseType(Integer warehouseType) 
    {
        this.warehouseType = warehouseType;
    }

    public Integer getWarehouseType() 
    {
        return warehouseType;
    }

    public void setStage(Integer stage) 
    {
        this.stage = stage;
    }

    public Integer getStage() 
    {
        return stage;
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

    public void setDebitAuxiliaryType(String debitAuxiliaryType) 
    {
        this.debitAuxiliaryType = debitAuxiliaryType;
    }

    public String getDebitAuxiliaryType() 
    {
        return debitAuxiliaryType;
    }

    public void setCreditAuxiliaryType(String creditAuxiliaryType) 
    {
        this.creditAuxiliaryType = creditAuxiliaryType;
    }

    public String getCreditAuxiliaryType() 
    {
        return creditAuxiliaryType;
    }

    public void setIsEnabled(Integer isEnabled) 
    {
        this.isEnabled = isEnabled;
    }

    public Integer getIsEnabled() 
    {
        return isEnabled;
    }

    public void setPriority(Integer priority) 
    {
        this.priority = priority;
    }

    public Integer getPriority() 
    {
        return priority;
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
            .append("warehouseType", getWarehouseType())
            .append("stage", getStage())
            .append("debitSubjectId", getDebitSubjectId())
            .append("creditSubjectId", getCreditSubjectId())
            .append("debitAuxiliaryType", getDebitAuxiliaryType())
            .append("creditAuxiliaryType", getCreditAuxiliaryType())
            .append("isEnabled", getIsEnabled())
            .append("priority", getPriority())
            .append("createdTime", getCreatedTime())
            .append("updatedTime", getUpdatedTime())
            .toString();
    }
}
