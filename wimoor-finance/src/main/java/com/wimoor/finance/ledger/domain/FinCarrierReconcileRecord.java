package com.wimoor.finance.ledger.domain;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * 承运商对账记录对象 fin_carrier_reconcile_record
 *
 * @author wimoor
 */
public class FinCarrierReconcileRecord implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long id;

    /** 租户ID */
    private String groupid;

    /** 承运商ID */
    private String carrierId;

    /** 承运商名称 */
    private String carrierName;

    /** 公司名称 */
    private String companyName;

    /** 对账月份，格式：yyyy-MM */
    private String reconcileMonth;

    /** 计划发货数量 */
    private Integer totalPlanQty;

    /** 实际发货数量 */
    private Integer totalActualQty;

    /** 实际接收数量 */
    private Integer totalReceivedQty;

    /** 运输费用 */
    private BigDecimal totalShipFee;

    /** 关税/其他费用 */
    private BigDecimal totalOtherFee;

    /** 发货货值 */
    private BigDecimal totalWorth;

    /** 货件票数 */
    private Integer totalShipmentNum;

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

    public String getCarrierId() { return carrierId; }
    public void setCarrierId(String carrierId) { this.carrierId = carrierId; }

    public String getCarrierName() { return carrierName; }
    public void setCarrierName(String carrierName) { this.carrierName = carrierName; }

    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }

    public String getReconcileMonth() { return reconcileMonth; }
    public void setReconcileMonth(String reconcileMonth) { this.reconcileMonth = reconcileMonth; }

    public Integer getTotalPlanQty() { return totalPlanQty; }
    public void setTotalPlanQty(Integer totalPlanQty) { this.totalPlanQty = totalPlanQty; }

    public Integer getTotalActualQty() { return totalActualQty; }
    public void setTotalActualQty(Integer totalActualQty) { this.totalActualQty = totalActualQty; }

    public Integer getTotalReceivedQty() { return totalReceivedQty; }
    public void setTotalReceivedQty(Integer totalReceivedQty) { this.totalReceivedQty = totalReceivedQty; }

    public BigDecimal getTotalShipFee() { return totalShipFee; }
    public void setTotalShipFee(BigDecimal totalShipFee) { this.totalShipFee = totalShipFee; }

    public BigDecimal getTotalOtherFee() { return totalOtherFee; }
    public void setTotalOtherFee(BigDecimal totalOtherFee) { this.totalOtherFee = totalOtherFee; }

    public BigDecimal getTotalWorth() { return totalWorth; }
    public void setTotalWorth(BigDecimal totalWorth) { this.totalWorth = totalWorth; }

    public Integer getTotalShipmentNum() { return totalShipmentNum; }
    public void setTotalShipmentNum(Integer totalShipmentNum) { this.totalShipmentNum = totalShipmentNum; }

    public String getReconcileBy() { return reconcileBy; }
    public void setReconcileBy(String reconcileBy) { this.reconcileBy = reconcileBy; }

    public Date getReconcileTime() { return reconcileTime; }
    public void setReconcileTime(Date reconcileTime) { this.reconcileTime = reconcileTime; }

    public Date getCreatedTime() { return createdTime; }
    public void setCreatedTime(Date createdTime) { this.createdTime = createdTime; }

    public Date getUpdatedTime() { return updatedTime; }
    public void setUpdatedTime(Date updatedTime) { this.updatedTime = updatedTime; }
}
