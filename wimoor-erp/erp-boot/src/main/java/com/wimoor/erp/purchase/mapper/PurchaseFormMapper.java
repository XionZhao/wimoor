package com.wimoor.erp.purchase.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wimoor.erp.purchase.pojo.entity.PurchaseForm;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;
@Mapper
public interface PurchaseFormMapper extends BaseMapper<PurchaseForm> {

	IPage<Map<String, Object>> selectByCondition(Page<?> page,@Param("param") Map<String, Object> param);

	List<Map<String, Object>> selectByCondition(@Param("param")Map<String, Object> param);
	
	List<Map<String, Object>> purchaseSumReport(Map<String, Object> param);

	List<Map<String, Object>> findeLastByMaterialid(Page<?> page,@Param("materialid") String materialid,@Param("warehouseid") String warehouseid);

	Map<String, Object> selectSummarytByCondition(Map<String, Object> param);

	List<Map<String, Object>> purchaseRecSumReport(Map<String, Object> param);

	IPage<Map<String, Object>> selectByConditionForm(Page<?> page,Map<String, Object> param);
	
	List<Map<String, Object>> selectByConditionForm(@Param("param")Map<String, Object> param);

	Map<String, Object> getLastPurchaseRecord(@Param("shopid")String shopid, @Param("warehouseid")String warehouseid);

	List<Map<String, Object>> getPurchaseSumReportNew(Map<String, Object> param);

	IPage<Map<String, Object>> getPayRecSumReport(Page<?> page,@Param("param")Map<String, Object> param);
	
	Map<String, Object>  getPayRecSumTotal(Map<String, Object> param);

	List<Map<String, Object>> getPayRecSumReport(@Param("param")Map<String, Object> param);
	
	List<Map<String,Object>> findEntryByIdAndSupplier(@Param("formid")String formid,@Param("supplier") String supplierid);
	
	List<Map<String, Object>> getPurchaseEntryStatus(@Param("shopid")String shopid, @Param("warehouseid")String warehouseid);

	List<Map<String, Object>> getEnteyInfo(Map<String, Object> param);
	
	List<Map<String,Object>> findSupplierByForm(@Param("formid")String formid);

	Map<String, Object> selectPurchaseNumAllStatus(@Param("param")Map<String, Object> param);

	List<Map<String, Object>> findeLastsByMaterialids(@Param("list")List<String> ids);

    // ==================== 台账Feign接口 ====================
    
    /**
     * 供应商台账汇总
     */
    List<Map<String, Object>> getSupplierLedgerSummary(@Param("param") Map<String, Object> param);

    /**
     * 供应商台账统计
     */
    Map<String, Object> getSupplierLedgerStatistics(@Param("param") Map<String, Object> param);

    /**
     * 供应商台账汇总合计行
     */
    Map<String, Object> getSupplierLedgerSummaryTotal(@Param("param") Map<String, Object> param);

    /**
     * 供应商订单明细
     */
    List<Map<String, Object>> getSupplierOrders(@Param("param") Map<String, Object> param);

    /**
     * 供应商付款明细
     */
    List<Map<String, Object>> getSupplierPayments(@Param("param") Map<String, Object> param);

    /**
     * 供应商订单总数
     */
    int getSupplierOrdersCount(@Param("param") Map<String, Object> param);

    /**
     * 供应商付款总数
     */
    int getSupplierPaymentsCount(@Param("param") Map<String, Object> param);

    /**
     * 获取供应商列表
     */
    List<Map<String, Object>> selectSupplierList(@Param("shopid") String shopid);

    /**
     * 查询已完成的采购订单（用于财务模块凭证生成）
     * 条件：至少一个分录closepaydate=变更日期，且所有分录closepaydate不为空且<今天
     */
    List<Map<String, Object>> getCompletedOrdersForVoucher(@Param("param") Map<String, Object> param);

    /**
     * 查询已入库的采购订单（用于库存凭证生成 - 入库验收阶段）
     * 条件：至少一个分录closerecdate=变更日期，且所有分录closerecdate不为空且<今天
     */
    List<Map<String, Object>> getCompletedOrdersForInventory(@Param("param") Map<String, Object> param);
}