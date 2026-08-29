package com.wimoor.erp.finance.pojo.entity;

import java.math.BigDecimal;
import java.util.Date;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.wimoor.erp.common.pojo.entity.BaseEntity;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 账期末结转记录实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_erp_fin_account_period_rollover")
public class FinAccountPeriodRollover extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private String id;

    /**
     * 账户ID，t_erp_fin_account表的ID
     */
    @TableField(value = "acct")
    private String acct;

    /**
     * 本次结转的费用
     */
    @TableField(value = "total_amount")
    private BigDecimal totalAmount;

    /**
     * 操作人
     */
    @TableField(value = "operator")
    private String operator;

    /**
     * 创建人
     */
    @TableField(value = "creator")
    private String creator;

    /**
     * 操作时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(value = "opttime")
    private Date opttime;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(value = "createtime")
    private Date createtime;
}
