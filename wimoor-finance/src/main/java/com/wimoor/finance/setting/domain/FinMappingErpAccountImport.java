package com.wimoor.finance.setting.domain;

import com.wimoor.common.core.annotation.Excel;

/**
 * 费用类型-科目映射规则导入对象
 * 
 * @author wimoor
 * @date 2025-07-07
 */
public class FinMappingErpAccountImport
{
    /** 费用类型名称 */
    @Excel(name = "费用类型")
    private String feeTypeName;

    /** 科目编码 */
    @Excel(name = "科目编码")
    private String subjectCode;

    public String getFeeTypeName() {
        return feeTypeName;
    }

    public void setFeeTypeName(String feeTypeName) {
        this.feeTypeName = feeTypeName;
    }

    public String getSubjectCode() {
        return subjectCode;
    }

    public void setSubjectCode(String subjectCode) {
        this.subjectCode = subjectCode;
    }
}
