package com.wimoor.amazon.finances.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 亚马逊交易报告表
 * @TableName t_amz_transaction_report
 */
@Data
@TableName(value ="t_amz_transaction_report")
public class TransactionReport implements Serializable {
    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private String id;

    /**
     * 亚马逊授权ID
     */
    private String amazonauthid;

    /**
     * 站点ID
     */
    private String marketplaceid;

    /**
     * 数据哈希值（用于去重）
     */
    @TableField(value = "data_hash")
    private String dataHash;

    /**
     * 交易日期时间
     */
    private Date dateTime;

    /**
     * 结算ID
     */
    private String settlementId;

    /**
     * 交易类型
     */
    private String transactionType;

    /**
     * 交易描述（仅存储交易描述，不存储商品名称）
     */
    private String description;

    /**
     * 订单ID
     */
    private String orderId;

    /**
     * 商品SKU
     */
    private String sku;

    /**
     * 数量
     */
    private Integer quantity;

    /**
     * 商城
     */
    private String marketplace;

    /**
     * 履约方式
     */
    private String fulfillment;

    /**
     * 订单城市
     */
    private String orderCity;

    /**
     * 订单州/省
     */
    private String orderState;

    /**
     * 订单邮政编码
     */
    private String orderPostCode;

    /**
     * 征税模型
     */
    private String taxCollectionModel;

    /**
     * 产品销售金额
     */
    private BigDecimal productSales;

    /**
     * 产品销售税
     */
    private BigDecimal productSalesTax;

    /**
     * 运费抵扣
     */
    private BigDecimal shippingCredits;

    /**
     * 运费抵扣税
     */
    private BigDecimal shippingCreditsTax;

    /**
     * 礼品包装抵扣
     */
    private BigDecimal giftwrapCredits;

    /**
     * 礼品包装抵扣税
     */
    private BigDecimal giftwrapCreditsTax;

    /**
     * 促销返利
     */
    private BigDecimal promotionalRebates;

    /**
     * 促销返利税
     */
    private BigDecimal promotionalRebatesTax;

    /**
     * 商城代扣税
     */
    private BigDecimal marketplaceWithheldTax;

    /**
     * 销售费用
     */
    private BigDecimal sellingFees;

    /**
     * FBA费用
     */
    private BigDecimal fbaFees;

    /**
     * 其他交易费用
     */
    private BigDecimal otherTransactionFees;

    /**
     * 其他
     */
    private BigDecimal other;

    /**
     * 总计金额
     */
    private BigDecimal total;

    /**
     * 交易状态
     */
    private String transactionStatus;

    /**
     * 交易发布日期
     */
    private Date transactonReleaseDate;

    /**
     * 创建时间
     */
    private Date createtime;

    /**
     * 更新时间
     */
    private Date opttime;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
