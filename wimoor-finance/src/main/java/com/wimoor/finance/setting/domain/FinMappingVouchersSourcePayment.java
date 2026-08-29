package com.wimoor.finance.setting.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.wimoor.common.core.web.domain.BaseEntity;

import java.math.BigDecimal;
import java.util.Date;

/**
 * ERP付款记录落地对象 fin_mapping_vouchers_source_payment
 * <p>
 * 将ERP实时返回的付款明细在财务模块本地持久化，
 * 支持独立查询、变更检测和审计追溯。
 * </p>
 * <p>
 * 粒度：每条ERP付款记录（order_id + entry_id + payment_id）
 * </p>
 *
 * @author wimoor
 * @date 2026-08-11
 */
public class FinMappingVouchersSourcePayment extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long id;

    /** 租户ID（账簿） */
    private String groupid;

    /** ERP采购订单ID(formid) */
    private String orderId;

    /** ERP付款记录ID（采购台账entryId） */
    private String paymentId;

    /** 付款单号 */
    private String paymentNo;

    /** 付款日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date paymentDate;

    /** 付款金额 */
    private BigDecimal amount;

    /** 费用类型ID */
    private String feeTypeId;

    /** 费用类型名称 */
    private String feeTypeName;

    private String sku;
    /** 采购账户ID */
    private String accountId;

    /** 采购账户名称 */
    private String accountName;

    /** 采购订单编号 */
    private String orderNumber;

    /** 仓库名称 */
    private String warehouseName;

    /** 供应商名称 */
    private String supplierName;

    /** 关联凭证ID */
    private Long voucherId;

    /** 关联的凭证分录号 */
    private String entryIds;

    /** 同步状态：0-待同步，1-已同步，2-已变更 */
    private Integer syncStatus;

    /** 付款状态：1-已付款，0-已撤销/驳回 */
    private Integer paymentStatus;

    /** 数据指纹（MD5），用于检测数据变更 */
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

    /** 创建人 */
    private String createBy;

    /** 修改人 */
    private String modifyBy;

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

    public String getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(String paymentId) {
        this.paymentId = paymentId;
    }

    public String getPaymentNo() {
        return paymentNo;
    }

    public void setPaymentNo(String paymentNo) {
        this.paymentNo = paymentNo;
    }

    public Date getPaymentDate() {
        return paymentDate;
    }

    public void setPaymentDate(Date paymentDate) {
        this.paymentDate = paymentDate;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getFeeTypeId() {
        return feeTypeId;
    }

    public void setFeeTypeId(String feeTypeId) {
        this.feeTypeId = feeTypeId;
    }

    public String getFeeTypeName() {
        return feeTypeName;
    }

    public void setFeeTypeName(String feeTypeName) {
        this.feeTypeName = feeTypeName;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public String getAccountName() {
        return accountName;
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public void setOrderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
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

    public Long getVoucherId() {
        return voucherId;
    }

    public void setVoucherId(Long voucherId) {
        this.voucherId = voucherId;
    }

    public String getEntryIds() {
        return entryIds;
    }

    public void setEntryIds(String entryIds) {
        this.entryIds = entryIds;
    }

    public Integer getSyncStatus() {
        return syncStatus;
    }

    public void setSyncStatus(Integer syncStatus) {
        this.syncStatus = syncStatus;
    }

    public Integer getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(Integer paymentStatus) {
        this.paymentStatus = paymentStatus;
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

    public String getCreateBy() {
        return createBy;
    }

    public void setCreateBy(String createBy) {
        this.createBy = createBy;
    }

    public String getModifyBy() {
        return modifyBy;
    }

    public void setModifyBy(String modifyBy) {
        this.modifyBy = modifyBy;
    }

    public String getSku(){return this.sku;}

    public void setSku(String sku){this.sku=sku;}

    @Override
    public String toString() {
        return new org.apache.commons.lang3.builder.ToStringBuilder(this, org.apache.commons.lang3.builder.ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("groupid", getGroupid())
            .append("orderId", getOrderId())
            .append("paymentId", getPaymentId())
            .append("voucherId", getVoucherId())
            .append("syncStatus", getSyncStatus())
            .toString();
    }
}
