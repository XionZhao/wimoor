package com.wimoor.erp.purchase.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.wimoor.common.user.UserInfo;
import com.wimoor.erp.common.pojo.entity.ERPBizException;
import com.wimoor.erp.purchase.pojo.dto.PurchaseSaveDTO;
import com.wimoor.erp.purchase.pojo.entity.PurchaseForm;
import com.wimoor.erp.purchase.pojo.entity.PurchaseFormEntry;
import com.wimoor.erp.purchase.pojo.entity.PurchaseFormPrintIP;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;

public interface IPurchaseFormService extends IService<PurchaseForm> {

	IPage<Map<String, Object>> getPurchaseFormEntry(Page<?> page,  Map<String, Object> param );
	public Map<String, Object> savePurchaseDataAction(PurchaseSaveDTO dto) throws ERPBizException ;

	boolean updatePurchaseFormEntry(PurchaseFormEntry item, String warehouseid) throws ERPBizException;

	PurchaseFormEntry updatePrice(UserInfo user,String id, Float itemprice, Integer amount,Float orderprice) throws ERPBizException;
    
    int approvals(UserInfo user,String ids) throws ERPBizException;

	Map<String, Object> getDetailMap(String id,String shopid) throws ERPBizException;
 
	Map<String, Object> getTraceDetailMap(String id, String shopid,String ftype,String actiontype);


	
    int findSumaryNeedApply(String shopid);
    
	int findSumaryNeedin(String shopid);
	
	int findSumaryNeedpay(String shopid);
    
	void purchaseReturn(UserInfo user, String id,  String remark) throws ERPBizException;

	Map<String, Object> updateNotice(String id, String notice, String shopid, String userid) throws ERPBizException;

	boolean updateWarehouse(UserInfo user, String id, String warehouseid) throws ERPBizException;

	List<Map<String, Object>> getLastFormByMaterial(Object id,int i);
	
	Map<String, Object> getLastOneFormByMaterial(Object id);

	Map<String, Object> getLastOneFormByMaterial(Object id,String warehouseid);

	List<Map<String,Object>> getPurchaseRecSumReport(  Map<String, Object> param);

	List<Map<String, Object>> purchaseFormReport(Map<String, Object> param);
	public IPage<Map<String, Object>> purchaseFormReport(Page<?>page,Map<String, Object> param) ;
	List<Map<String,Object>> getPurchaseSumReport(Map<String,Object> param);
	
	

	
	public Map<String,Object> getViewData(String id, String shopid);

	Map<String, Object> getPurchaseFormSummary(UserInfo user, Map<String, Object> param);

	IPage<Map<String, Object>> getPurchaseForm(Page<?> page,Map<String, Object> param);

	List<Map<String, Object>> getFormDetail(String id, String shopid);


	Map<String, Object> purchaseFormReportTotal(Map<String, Object> param);

	Map<String, Object> getLastPurchaseRecord(String shopid, String warehouseid);

	List<Map<String, Object>> getPurchaseSumReportNew(Map<String, Object> param);

	IPage<Map<String, Object>> getPayRecSumReport(Page<?> page,Map<String, Object> param);

	void setExcelBook(Workbook workbook, Map<String, Object> param);
	
	void setPurchaseSkuItemExcelBook(SXSSFWorkbook workbook, Map<String, Object> param);
	
	public List<Map<String, Object>> loadPurchaseFormDate(UserInfo user, String planid, String warehouseid, String ftype, List<String> item_material_list);

	void getPurchaseRecInfoExcelBook(SXSSFWorkbook workbook, Map<String, Object> param);

	List<Map<String, Object>> findSupplierByForm(String formid);
	
	void downloadPurchasePaymentWord(HttpServletRequest request, HttpServletResponse response, String recid);
	
	Map<String, Object> getPurchaseNumAllStatus(Map<String, Object> param);
	
	Map<String, Object> uploadPurchaseListByExcel(Sheet sheet, Map<String, Object> map);
	
	Map<String, Object> getPurchaseRecordInfo(String reciveId);
	void setPurchaseFormReportExcelBook(SXSSFWorkbook workbook, Map<String, Object> param);
	public boolean updateDeliveryDate(String entryid,String deliverydate) ;
	List<Map<String, Object>> selectNeedSendMsgShop();
	List<Map<String, Object>> selectPurchaseNotify(String shopid);
	public int savePurchaseForm(UserInfo user,List<PurchaseForm> formList, String planwarehouseid) throws ERPBizException;

	PurchaseFormEntry recallEntry(UserInfo user,String id);
	List<Map<String, Object>> getEntryData(String id);
	Map<String, Object> deleteEntry(UserInfo user, String id);
	List<Map<String, Object>> getLastFormsByMaterials(List<String> ids);


	void setPrintIp(UserInfo userinfo, String ip,String paper,String addressid);

	PurchaseFormPrintIP getPrintIp(UserInfo userinfo);

	PurchaseFormPrintIP getPrintIpByAddress(UserInfo userinfo, String addressid);

    Map<String, Object> reSubmit(UserInfo userinfo, String id);

    // ==================== 台账Feign接口 ====================
    
    /**
     * 采购订单列表（台账用）
     */
    Map<String, Object> getLedgerList(Map<String, Object> params);

    /**
     * 采购订单统计（台账用）
     */
    Map<String, Object> getLedgerStatistics(Map<String, Object> params);

    /**
     * 供应商台账汇总
     */
    List<Map<String, Object>> getSupplierLedgerSummary(Map<String, Object> params);

    /**
     * 供应商台账统计
     */
    Map<String, Object> getSupplierLedgerStatistics(Map<String, Object> params);

    /**
     * 供应商台账汇总合计行
     */
    Map<String, Object> getSupplierLedgerSummaryTotal(Map<String, Object> params);

    /**
     * 供应商订单明细
     */
    List<Map<String, Object>> getSupplierOrders(Map<String, Object> params);

    /**
     * 供应商付款明细
     */
    List<Map<String, Object>> getSupplierPayments(Map<String, Object> params);

    /**
     * 供应商订单总数
     */
    int getSupplierOrdersCount(Map<String, Object> params);

    /**
     * 供应商付款总数
     */
    int getSupplierPaymentsCount(Map<String, Object> params);

    /**
     * 获取供应商列表
     */
    List<Map<String, Object>> getSupplierList(String shopid);

    /**
     * 采购台账付款操作
     */
    void payPurchaseOrder(Map<String, Object> params);

    /**
     * 查询已完成的采购订单（用于财务模块凭证生成）
     * 返回订单及其所有分录和付款明细的完整数据
     * @param groupid 租户ID（账簿），对应 t_erp_purchase_form.groupid
     * @param changedDate 变更日期（yyyy-MM-dd），查询closepaydate在当天变更的订单
     */
    List<Map<String, Object>> getCompletedOrdersForVoucher(String groupid, String changedDate);

    /**
     * 查询已入库的采购订单（用于库存凭证生成-入库验收）
     * @param groupid 租户ID
     * @param changedDate 变更日期（yyyy-MM-dd），查询closerecdate在当天变更的订单
     */
    List<Map<String, Object>> getCompletedOrdersForInventory(String groupid, String changedDate);
}
