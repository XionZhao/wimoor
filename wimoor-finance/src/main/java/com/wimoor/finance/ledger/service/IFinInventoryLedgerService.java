package com.wimoor.finance.ledger.service;

import com.wimoor.finance.ledger.domain.dto.FinInventoryLedgerDTO;

import java.util.List;
import java.util.Map;

/**
 * 进销存台账Service接口
 *
 * @author wimoor
 * @date 2026-07-10
 */
public interface IFinInventoryLedgerService {

    /**
     * 查询汇总账（按SKU+仓库维度，查询数量、金额、移动平均单价）
     *
     * @param dto 查询参数
     * @return 汇总数据列表
     */
    List<Map<String, Object>> selectSummary(FinInventoryLedgerDTO dto);

    /**
     * 查询明细账（按SKU查询变动记录，包含入库/出库/调拨等）
     *
     * @param dto 查询参数
     * @return 明细数据列表
     */
    List<Map<String, Object>> selectDetail(FinInventoryLedgerDTO dto);

    /**
     * 查询明细账总数
     *
     * @param dto 查询参数
     * @return 总数
     */
    long selectDetailCount(FinInventoryLedgerDTO dto);

    /**
     * 分页查询明细账
     *
     * @param dto      查询参数
     * @param pageNum  页码
     * @param pageSize 每页数量
     * @return 包含total和rows的Map
     */
    Map<String, Object> selectDetailPage(FinInventoryLedgerDTO dto, int pageNum, int pageSize);

    /**
     * 勾稽校验（验证库存金额与凭证金额是否一致）
     *
     * @param dto 查询参数
     * @return 校验结果列表
     */
    List<Map<String, Object>> selectCheckResult(FinInventoryLedgerDTO dto);

    /**
     * 库存趋势图数据（按日期聚合库存金额变化）
     *
     * @param dto 查询参数
     * @return 趋势数据列表
     */
    List<Map<String, Object>> selectChartTrend(FinInventoryLedgerDTO dto);

    /**
     * 批量生成凭证
     *
     * @param dto  包含recordIds和shopid
     * @param userName 操作人姓名
     * @return 生成结果
     */
    Map<String, Object> batchGenerateVoucher(FinInventoryLedgerDTO dto, String userName);

    /**
     * 导出进销存台账数据
     *
     * @param dto 查询参数
     * @return 导出数据列表
     */
    List<Map<String, Object>> exportData(FinInventoryLedgerDTO dto);
}
