package com.wimoor.finance.ledger.service;

import com.wimoor.finance.ledger.domain.FinInvoice;
import com.wimoor.finance.ledger.domain.FinInvoiceDetail;
import com.wimoor.finance.ledger.domain.FinInvoiceExtension;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * 发票台账Service接口
 *
 * @author wimoor
 * @date 2025-11-04
 */
public interface IFinInvoiceLedgerService
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
     * 查询发票商品明细行
     *
     * @param invoiceId 发票ID
     * @return 明细行列表
     */
    List<FinInvoiceDetail> selectInvoiceDetails(Long invoiceId);

    /**
     * 查询发票扩展信息
     *
     * @param invoiceId 发票ID
     * @return 扩展信息列表
     */
    List<FinInvoiceExtension> selectInvoiceExtensions(Long invoiceId);

    /**
     * 从税局API同步发票
     *
     * @param params 同步参数
     * @return 同步结果
     */
    int syncInvoices(Map<String, Object> params);

    /**
     * 手动导入发票
     *
     * @param file 导入文件
     * @param groupid 租户ID
     * @return 导入数量
     */
    int importInvoices(MultipartFile file, String groupid);

    /**
     * JSON批量导入发票（前端解析税控文件后调用，按页签分组）
     *
     * @param sheets   页签数据，key为页签名，value为该页签下的行数据
     * @param groupid  租户ID
     * @param userName 操作人
     * @return 导入结果描述
     */
    String importInvoicesFromJson(Map<String, List<Map<String, Object>>> sheets, String groupid, String userName);

    /**
     * 发票入账（生成凭证）
     *
     * @param ids 发票ID集合
     * @return 入账数量
     */
    int postingInvoices(List<Long> ids);

    /**
     * 新增发票
     *
     * @param finInvoice 发票
     * @return 结果
     */
    int insertFinInvoice(FinInvoice finInvoice);

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
    int batchUpdatePostingStatus(List<Long> ids, Integer postingStatus, Long voucherId, String updatedBy);

    /**
     * 批量删除发票
     *
     * @param ids 需要删除的发票主键集合
     * @return 结果
     */
    int deleteFinInvoiceByIds(Long[] ids);

    /**
     * 新增发票明细行
     *
     * @param detail 明细行
     * @return 结果
     */
    int insertInvoiceDetail(FinInvoiceDetail detail);

    /**
     * 批量匹配发票的supplier_id（通过seller_name匹配供应商名称，修复历史数据）
     *
     * @return 诊断信息，包含匹配数量和未匹配的销方名称
     */
    Map<String, Object> batchMatchSupplierId();

    /**
     * 批量匹配发票的carrier_id（通过seller_name匹配承运商名称，修复历史数据）
     *
     * @return 诊断信息，包含匹配数量和未匹配的销方名称
     */
    Map<String, Object> batchMatchCarrierId();

    /**
     * 查询凭证信息（凭证号、凭证字等）
     *
     * @param voucherId 凭证ID
     * @return 凭证信息
     */
    Map<String, Object> selectVoucherInfo(Long voucherId);
}
