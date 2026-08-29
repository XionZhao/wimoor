package com.wimoor.finance.ledger.domain.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 供应商台账查询参数DTO
 */
public class SupplierLedgerQueryDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 账套ID */
    private String groupid;

    /** 多个账套ID（用户拥有多个店铺时） */
    private List<String> groupids;

    /** 供应商ID */
    private String supplierId;

    /** 供应商名称（模糊搜索） */
    private String supplierName;

    /** 开始日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private Date startDate;

    /** 结束日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private Date endDate;

    /** 对账状态：0-未对账，1-已对账 */
    private Integer reconcileStatus;

    /** 产品SKU搜索 */
    private String search;

    /** 仓库ID */
    private String warehouseid;

    /** 联系人 */
    private String contactPerson;

    /** 页码 */
    private Integer pageNum;

    /** 每页条数 */
    private Integer pageSize;

    /** 销方名称列表（用于过滤匹配到供应商的发票） */
    private List<String> sellerNames;

    public String getGroupid() {
        return groupid;
    }

    public void setGroupid(String groupid) {
        this.groupid = groupid;
    }

    public List<String> getGroupids() {
        return groupids;
    }

    public void setGroupids(List<String> groupids) {
        this.groupids = groupids;
    }

    public String getSupplierId() {
        return supplierId;
    }

    public void setSupplierId(String supplierId) {
        this.supplierId = supplierId;
    }

    public String getSupplierName() {
        return supplierName;
    }

    public void setSupplierName(String supplierName) {
        this.supplierName = supplierName;
    }

    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public Date getEndDate() {
        return endDate;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    public Integer getReconcileStatus() {
        return reconcileStatus;
    }

    public void setReconcileStatus(Integer reconcileStatus) {
        this.reconcileStatus = reconcileStatus;
    }

    public String getSearch() {
        return search;
    }

    public void setSearch(String search) {
        this.search = search;
    }

    public String getWarehouseid() {
        return warehouseid;
    }

    public void setWarehouseid(String warehouseid) {
        this.warehouseid = warehouseid;
    }

    public String getContactPerson() {
        return contactPerson;
    }

    public void setContactPerson(String contactPerson) {
        this.contactPerson = contactPerson;
    }

    public Integer getPageNum() {
        return pageNum;
    }

    public void setPageNum(Integer pageNum) {
        this.pageNum = pageNum;
    }

    public Integer getPageSize() {
        return pageSize;
    }

    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
    }

    public List<String> getSellerNames() {
        return sellerNames;
    }

    public void setSellerNames(List<String> sellerNames) {
        this.sellerNames = sellerNames;
    }
}
