package com.wimoor.finance.ledger.domain.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.wimoor.common.core.web.domain.BaseEntity;

import java.util.Date;
import java.util.List;

/**
 * 进销存台账查询参数DTO
 *
 * @author wimoor
 * @date 2026-07-10
 */
public class FinInventoryLedgerDTO extends BaseEntity {
    private static final long serialVersionUID = 1L;

    /** 公司ID */
    private String shopid;

    /** 仓库ID */
    private String warehouseid;

    /** 产品物料ID */
    private String materialid;

    /** 多个产品物料ID */
    private List<String> materialids;

    /** SKU模糊搜索 */
    private String sku;

    /** 产品名称模糊搜索 */
    private String name;

    /** 开始日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date startDate;

    /** 结束日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date endDate;

    /** 期间格式：YYYYMM */
    private String startPeriod;
    private String endPeriod;

    /** 单据类型 */
    private String formtype;

    /** 变动记录ID列表（批量生成凭证用） */
    private List<Long> recordIds;

    public String getShopid() {
        return shopid;
    }

    public void setShopid(String shopid) {
        this.shopid = shopid;
    }

    public String getWarehouseid() {
        return warehouseid;
    }

    public void setWarehouseid(String warehouseid) {
        this.warehouseid = warehouseid;
    }

    public String getMaterialid() {
        return materialid;
    }

    public void setMaterialid(String materialid) {
        this.materialid = materialid;
    }

    public List<String> getMaterialids() {
        return materialids;
    }

    public void setMaterialids(List<String> materialids) {
        this.materialids = materialids;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public String getStartPeriod() {
        return startPeriod;
    }

    public void setStartPeriod(String startPeriod) {
        this.startPeriod = startPeriod;
    }

    public String getEndPeriod() {
        return endPeriod;
    }

    public void setEndPeriod(String endPeriod) {
        this.endPeriod = endPeriod;
    }

    public String getFormtype() {
        return formtype;
    }

    public void setFormtype(String formtype) {
        this.formtype = formtype;
    }

    public List<Long> getRecordIds() {
        return recordIds;
    }

    public void setRecordIds(List<Long> recordIds) {
        this.recordIds = recordIds;
    }
}
