package com.wimoor.finance.setting.domain;

import com.wimoor.common.core.annotation.Excel;

/**
 * 费用类型级别映射导入对象
 *
 * @author wimoor
 * @date 2025-07-07
 */
public class FinMappingErpFeetypeImport
{
    /** 采购账户名称 */
    @Excel(name = "采购账户")
    private String accountName;

    /** 科目编码 */
    @Excel(name = "科目编码")
    private String subjectCode;

    public String getAccountName() {
        return accountName;
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    public String getSubjectCode() {
        return subjectCode;
    }

    public void setSubjectCode(String subjectCode) {
        this.subjectCode = subjectCode;
    }
}
