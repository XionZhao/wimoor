package com.wimoor.finance.ledger.mapper;

import com.wimoor.finance.ledger.domain.FinInvoice;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 发票台账Mapper接口
 *
 * @author wimoor
 * @date 2025-11-04
 */
public interface FinInvoiceLedgerMapper
{
    /**
     * 查询发票详情
     *
     * @param id 发票主键
     * @return 发票
     */
    FinInvoice selectFinInvoiceById(Long id);

    /**
     * 查询发票列表
     *
     * @param finInvoice 发票查询条件
     * @return 发票集合
     */
    List<FinInvoice> selectFinInvoiceList(FinInvoice finInvoice);

    /**
     * 统计发票信息
     *
     * @param finInvoice 查询条件
     * @return 统计结果
     */
    Map<String, Object> selectInvoiceStatistics(FinInvoice finInvoice);

    /**
     * 查询发票关联的采购订单和付款记录
     *
     * @param id 发票主键
     * @return 关联信息
     */
    Map<String, Object> selectInvoiceRelations(Long id);

    /**
     * 新增发票
     *
     * @param finInvoice 发票
     * @return 结果
     */
    int insertFinInvoice(FinInvoice finInvoice);

    /**
     * 批量新增发票
     *
     * @param list 发票列表
     * @return 结果
     */
    int batchInsertFinInvoice(@Param("list") List<FinInvoice> list);

    /**
     * 修改发票
     *
     * @param finInvoice 发票
     * @return 结果
     */
    int updateFinInvoice(FinInvoice finInvoice);

    /**
     * 批量更新发票入账状态
     *
     * @param ids 发票ID集合
     * @param postingStatus 入账状态
     * @param voucherId 凭证ID
     * @param updatedBy 更新人
     * @return 结果
     */
    int batchUpdatePostingStatus(@Param("ids") List<Long> ids,
                                  @Param("postingStatus") Integer postingStatus,
                                  @Param("voucherId") Long voucherId,
                                  @Param("updatedBy") String updatedBy);

    /**
     * 根据数电发票号码查询发票ID
     *
     * @param digitalInvoiceNo 数电发票号码
     * @return 发票对象（仅含id和digitalInvoiceNo）
     */
    FinInvoice selectByDigitalInvoiceNo(@Param("digitalInvoiceNo") String digitalInvoiceNo);

    /**
     * 批量根据数电发票号码查询发票（返回id和digitalInvoiceNo的映射）
     *
     * @param digitalInvoiceNos 数电发票号码列表
     * @return 发票列表
     */
    List<FinInvoice> selectByDigitalInvoiceNos(@Param("list") List<String> digitalInvoiceNos);

    /**
     * 删除发票
     *
     * @param id 发票主键
     * @return 结果
     */
    int deleteFinInvoiceById(Long id);

    /**
     * 批量删除发票
     *
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    int deleteFinInvoiceByIds(Long[] ids);

    /**
     * 更新发票类型
     *
     * @param id 发票ID
     * @param invoiceType 发票类型
     * @return 结果
     */
    int updateInvoiceType(@Param("id") Long id, @Param("invoiceType") String invoiceType);

    /**
     * 查询supplier_id为空的发票（去重，按seller_name分组）
     *
     * @return 去重后的seller_name列表
     */
    List<Map<String, Object>> selectDistinctSellerNamesWithoutSupplier();

    /**
     * 批量更新发票的supplier_id（按seller_name匹配）
     *
     * @param sellerName 卖方名称
     * @param supplierId 供应商ID
     * @return 更新行数
     */
    int batchUpdateSupplierIdBySellerName(@Param("sellerName") String sellerName, @Param("supplierId") String supplierId);

    /**
     * 诊断：查询发票数据概况（总数、supplier_id为空的数量、groupid分布）
     */
    Map<String, Object> selectInvoiceDiagInfo();

    /**
     * 查询carrier_id为空的发票（去重，按seller_name分组）
     *
     * @return 去重后的seller_name列表
     */
    List<Map<String, Object>> selectDistinctSellerNamesWithoutCarrier();

    /**
     * 批量更新发票的carrier_id（按seller_name匹配）
     *
     * @param sellerName 卖方名称
     * @param carrierId 承运商ID
     * @return 更新行数
     */
    int batchUpdateCarrierIdBySellerName(@Param("sellerName") String sellerName, @Param("carrierId") String carrierId);
}
