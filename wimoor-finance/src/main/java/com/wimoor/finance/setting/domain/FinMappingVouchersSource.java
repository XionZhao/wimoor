package com.wimoor.finance.setting.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.wimoor.common.core.web.domain.BaseEntity;

import java.math.BigDecimal;
import java.util.Date;

/**
 * ERP采购订单凭证同步追踪对象 fin_mapping_vouchers_source
 * 以采购订单为维度追踪凭证转换状态，一个订单 = 一个凭证
 * 每晚检测订单closepaydate变更，判断是否全部SKU已完成付款
 *
 * @author wimoor
 * @date 2026-08-07
 */
public class FinMappingVouchersSource extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long id;

    /** 租户ID（账簿） */
    private String groupid;

    /** 采购订单ID（formid） */
    private String orderId;

    /** 采购订单编号 */
    private String orderNumber;

    /** SKU */
    private String sku;

    /** 仓库名称 */
    private String warehouseName;

    /** 供应商名称 */
    private String supplierName;

    /** 订单总金额 */
    private BigDecimal totalAmount;

    /** 凭证类型：payment-付款凭证，inventory_transit-在途库存凭证，inventory_inbound-入库库存凭证 */
    private String voucherType;

    /** 关联的凭证ID */
    private Long voucherId;

    /** 凭证字（如：记、付、转），关联查询 fin_vouchers.voucher_type */
    private String voucherWord;

    /** 凭证编号，关联查询 fin_vouchers.voucher_no */
    private String voucherNumber;

    /** 同步状态：0-待同步，1-已同步，2-已变更（需重新同步） */
    private Integer syncStatus;

    /** 数据指纹（MD5），检测订单数据是否变更 */
    private String dataHash;

    /** 同步时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date syncTime;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createdTime;

    /** 更新时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updatedTime;

    // ==================== getter/setter ====================

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getGroupid() {
        return groupid;
    }

    public void setGroupid(String groupid) {
        this.groupid = groupid;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public void setOrderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public String getWarehouseName() {
        return warehouseName;
    }

    public void setWarehouseName(String warehouseName) {
        this.warehouseName = warehouseName;
    }

    public String getSupplierName() {
        return supplierName;
    }

    public void setSupplierName(String supplierName) {
        this.supplierName = supplierName;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getVoucherType() {
        return voucherType;
    }

    public void setVoucherType(String voucherType) {
        this.voucherType = voucherType;
    }

    public Long getVoucherId() {
        return voucherId;
    }

    public void setVoucherId(Long voucherId) {
        this.voucherId = voucherId;
    }

    public String getVoucherWord() {
        return voucherWord;
    }

    public void setVoucherWord(String voucherWord) {
        this.voucherWord = voucherWord;
    }

    public String getVoucherNumber() {
        return voucherNumber;
    }

    public void setVoucherNumber(String voucherNumber) {
        this.voucherNumber = voucherNumber;
    }

    public Integer getSyncStatus() {
        return syncStatus;
    }

    public void setSyncStatus(Integer syncStatus) {
        this.syncStatus = syncStatus;
    }

    public String getDataHash() {
        return dataHash;
    }

    public void setDataHash(String dataHash) {
        this.dataHash = dataHash;
    }

    public Date getSyncTime() {
        return syncTime;
    }

    public void setSyncTime(Date syncTime) {
        this.syncTime = syncTime;
    }

    public Date getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(Date createdTime) {
        this.createdTime = createdTime;
    }

    public Date getUpdatedTime() {
        return updatedTime;
    }

    public void setUpdatedTime(Date updatedTime) {
        this.updatedTime = updatedTime;
    }

    @Override
    public String toString() {
        return new org.apache.commons.lang3.builder.ToStringBuilder(this, org.apache.commons.lang3.builder.ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("orderId", getOrderId())
            .append("orderNumber", getOrderNumber())
            .append("voucherId", getVoucherId())
            .append("syncStatus", getSyncStatus())
            .toString();
    }
}
