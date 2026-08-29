package com.wimoor.amazon.finances.pojo.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

@Data
@TableName(value = "t_amz_transaction_report_column_mapping")
public class AmzTransactionReportColumnMapping implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id")
    private Long id;

    @TableField(value = "marketplaceid")
    private String marketplaceid;

    @TableField(value = "field_name")
    private String fieldName;

    @TableField(value = "column_name")
    private String columnName;
}