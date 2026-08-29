package com.wimoor.finance.ledger.domain;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * 供应商对账记录对象 fin_supplier_reconcile_record
 *
 * @author wimoor
 */
public class FinSupplierReconcileRecord implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long id;

    /** 租户ID */
    private String groupid;

    /** 供应商ID */
    private String supplierId;

    /** 供应商名称 */
    private String supplierName;

    /** 公司名称 */
    private String companyName;

    /** 对账月份，格式：yyyy-MM */
    private String reconcileMonth;

    /** 订单数 */
    private Integer orderCount;

    /** 订单总额（采购汇总） */
    private BigDecimal totalOrderAmount;

    /** 已收货数量 */
    private Integer totalReceived;

    /** 已付总额（付款汇总） */
    private BigDecimal totalPaidAmount;

    /** 未付总额 */
    private BigDecimal totalUnpaidAmount;

    /** 已开票总额（发票汇总） */
    private BigDecimal totalInvoicedAmount;

    /** 未开票总额 */
    private BigDecimal totalUninvoicedAmount;

    /** 对账人 */
    private String reconcileBy;

    /** 对账时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date reconcileTime;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createdTime;

    /** 更新时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updatedTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getGroupid() { return groupid; }
    public void setGroupid(String groupid) { this.groupid = groupid; }

    public String getSupplierId() { return supplierId; }
    public void setSupplierId(String supplierId) { this.supplierId = supplierId; }

    public String getSupplierName() { return supplierName; }
    public void setSupplierName(String supplierName) { this.supplierName = supplierName; }

    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }

    public String getReconcileMonth() { return reconcileMonth; }
    public void setReconcileMonth(String reconcileMonth) { this.reconcileMonth = reconcileMonth; }

    public Integer getOrderCount() { return orderCount; }
    public void setOrderCount(Integer orderCount) { this.orderCount = orderCount; }

    public BigDecimal getTotalOrderAmount() { return totalOrderAmount; }
    public void setTotalOrderAmount(BigDecimal totalOrderAmount) { this.totalOrderAmount = totalOrderAmount; }

    public Integer getTotalReceived() { return totalReceived; }
    public void setTotalReceived(Integer totalReceived) { this.totalReceived = totalReceived; }

    public BigDecimal getTotalPaidAmount() { return totalPaidAmount; }
    public void setTotalPaidAmount(BigDecimal totalPaidAmount) { this.totalPaidAmount = totalPaidAmount; }

    public BigDecimal getTotalUnpaidAmount() { return totalUnpaidAmount; }
    public void setTotalUnpaidAmount(BigDecimal totalUnpaidAmount) { this.totalUnpaidAmount = totalUnpaidAmount; }

    public BigDecimal getTotalInvoicedAmount() { return totalInvoicedAmount; }
    public void setTotalInvoicedAmount(BigDecimal totalInvoicedAmount) { this.totalInvoicedAmount = totalInvoicedAmount; }

    public BigDecimal getTotalUninvoicedAmount() { return totalUninvoicedAmount; }
    public void setTotalUninvoicedAmount(BigDecimal totalUninvoicedAmount) { this.totalUninvoicedAmount = totalUninvoicedAmount; }

    public String getReconcileBy() { return reconcileBy; }
    public void setReconcileBy(String reconcileBy) { this.reconcileBy = reconcileBy; }

    public Date getReconcileTime() { return reconcileTime; }
    public void setReconcileTime(Date reconcileTime) { this.reconcileTime = reconcileTime; }

    public Date getCreatedTime() { return createdTime; }
    public void setCreatedTime(Date createdTime) { this.createdTime = createdTime; }

    public Date getUpdatedTime() { return updatedTime; }
    public void setUpdatedTime(Date updatedTime) { this.updatedTime = updatedTime; }
}
