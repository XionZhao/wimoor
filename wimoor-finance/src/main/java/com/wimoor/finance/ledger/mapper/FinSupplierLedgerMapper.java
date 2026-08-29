package com.wimoor.finance.ledger.mapper;

import com.wimoor.finance.ledger.domain.FinSupplierReconcileRecord;
import com.wimoor.finance.ledger.domain.dto.SupplierLedgerQueryDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 供应商台账Mapper接口（Finance模块，只处理发票/对账数据）
 *
 * @author wimoor
 */
@Mapper
public interface FinSupplierLedgerMapper {

    /**
     * 查询供应商发票明细
     */
    List<Map<String, Object>> selectSupplierLedgerInvoices(SupplierLedgerQueryDTO query);

    /**
     * 更新对账状态
     */
    int updateReconcileStatus(@Param("groupid") String groupid,
                              @Param("supplierId") String supplierId,
                              @Param("operator") String operator);

    /**
     * 按销方名称+groupid查询发票金额汇总（日期必填）
     */
    List<Map<String, Object>> selectInvoiceAmountByNames(@Param("groupid") String groupid,
                                                          @Param("sellerNames") List<String> sellerNames,
                                                          @Param("startDate") String startDate,
                                                          @Param("endDate") String endDate);

    /**
     * 按销方名称列表查询发票明细（只显示匹配到供应商的发票）
     */
    List<Map<String, Object>> selectSupplierLedgerInvoicesByNames(SupplierLedgerQueryDTO query);

    /**
     * 按销方名称列表更新对账状态
     */
    int updateReconcileStatusByNames(@Param("groupid") String groupid,
                                      @Param("sellerNames") List<String> sellerNames,
                                      @Param("operator") String operator);

    /**
     * 插入对账记录
     */
    int insertReconcileRecord(FinSupplierReconcileRecord record);

    /**
     * 更新对账记录
     */
    int updateReconcileRecord(FinSupplierReconcileRecord record);

    /**
     * 查询对账记录（按供应商+月份）
     */
    FinSupplierReconcileRecord selectReconcileRecord(@Param("groupid") String groupid,
                                                      @Param("supplierId") String supplierId,
                                                      @Param("reconcileMonth") String reconcileMonth);

    /**
     * 查询最新的对账记录（按供应商）
     */
    FinSupplierReconcileRecord selectLatestReconcileRecord(@Param("groupid") String groupid,
                                                            @Param("supplierId") String supplierId);
}
