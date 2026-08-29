package com.wimoor.finance.voucher.domain;

import com.wimoor.common.core.web.domain.BaseEntity;

/**
 * 凭证模版分录辅助核算对象 fin_voucher_template_entries_auxiliary
 */
public class FinVoucherTemplateEntriesAuxiliary extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** ID */
    private Long id;

    /** 关联分录 */
    private Long entryId;

    /** 核算类型ID */
    private Long auxiliaryTypeId;

    /** 具体核算项的ID */
    private Long auxiliaryItemId;

    /** 租户ID */
    private String groupid;

    /** 核算类型名称（非数据库字段） */
    private String auxiliaryTypeName;

    /** 核算项名称（非数据库字段） */
    private String auxiliaryItemName;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getEntryId() {
        return entryId;
    }

    public void setEntryId(Long entryId) {
        this.entryId = entryId;
    }

    public Long getAuxiliaryTypeId() {
        return auxiliaryTypeId;
    }

    public void setAuxiliaryTypeId(Long auxiliaryTypeId) {
        this.auxiliaryTypeId = auxiliaryTypeId;
    }

    public Long getAuxiliaryItemId() {
        return auxiliaryItemId;
    }

    public void setAuxiliaryItemId(Long auxiliaryItemId) {
        this.auxiliaryItemId = auxiliaryItemId;
    }

    public String getGroupid() {
        return groupid;
    }

    public void setGroupid(String groupid) {
        this.groupid = groupid;
    }

    public String getAuxiliaryTypeName() {
        return auxiliaryTypeName;
    }

    public void setAuxiliaryTypeName(String auxiliaryTypeName) {
        this.auxiliaryTypeName = auxiliaryTypeName;
    }

    public String getAuxiliaryItemName() {
        return auxiliaryItemName;
    }

    public void setAuxiliaryItemName(String auxiliaryItemName) {
        this.auxiliaryItemName = auxiliaryItemName;
    }
}