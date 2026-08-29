package com.wimoor.erp.customer.pojo.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.wimoor.erp.common.pojo.entity.ErpBaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.Size;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_erp_customer_account")
public class CustomerAccount extends ErpBaseEntity {

    private static final long serialVersionUID = 1L;

    @TableField(value = "customer_id")
    private String customerId;

    @Size(max = 50, message = "公司名称的长度不能超过50个字符")
    @TableField(value = "company_name")
    private String companyName;

    @Size(max = 50, message = "银行账号的长度不能超过50个字符")
    @TableField(value = "account_number")
    private String accountNumber;

    @TableField(value = "is_default")
    private Boolean isDefault;

    @TableField(value = "status")
    private Integer status;

    @TableField(value = "bank_name")
    private String bankName;

    @TableField(value = "remark")
    private String remark;
}
