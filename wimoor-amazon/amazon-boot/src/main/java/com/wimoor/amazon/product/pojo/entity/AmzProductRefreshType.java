package com.wimoor.amazon.product.pojo.entity;

import java.io.Serializable;
import java.math.BigInteger;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import io.swagger.annotations.ApiModel;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 产品刷新类型表
 * 替代原t_amz_product_refresh表，使用type字段区分刷新类型
 *
 * @author wimoor team
 * @since 2022-06-17
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("t_amz_product_refresh_type")
@ApiModel(value="AmzProductRefreshType对象", description="产品刷新类型")
public class AmzProductRefreshType implements Serializable {

    private static final long serialVersionUID=1L;

    /**
     * 产品ID
     */
    @TableId(value="pid")
    private BigInteger pid;

    /**
     * 授权ID
     */
    @TableField(value="amazonauthid")
    private BigInteger amazonauthid;

    /**
     * 刷新类型: 1=detail, 2=price, 3=catalog
     */
    @TableField(value="type")
    private Integer type;

    /**
     * 刷新时间
     */
    @TableField(value="refresh_time")
    private LocalDateTime refreshTime;

    /**
     * 以下字段通过JOIN t_product_info获取，非数据库字段
     */
    @TableField(exist = false)
    private String sku;

    @TableField(exist = false)
    private String asin;

    @TableField(exist = false)
    private String marketplaceid;

    @TableField(exist = false)
    private Boolean isparent;

    @TableField(exist = false)
    private Boolean notfound;

    /**
     * 刷新类型常量
     */
    public static final int TYPE_DETAIL = 1;
    public static final int TYPE_PRICE = 2;
    public static final int TYPE_CATALOG = 3;
}
