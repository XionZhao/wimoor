package com.wimoor.finance.setting.strategy;

import com.wimoor.common.user.UserInfo;

import java.util.Map;

/**
 * ERP凭证生成策略接口
 * <p>
 * 与 IFinClosingTemplateStrategy 不同，此接口是 ERP 配置驱动的，
 * 不依赖模板子项（FinClosingTemplateItem），而是从 setting 包的
 * ERP 映射配置（FinMappingErp*）中获取借贷科目映射。
 * </p>
 * <p>
 * generateVoucher 直接传入 groupid 而非 templateId，
 * 策略内部通过 groupid + ftype 反查模板获取 voucherType 等信息。
 * </p>
 *
 * @author wimoor
 * @date 2026-08-11
 */
public interface IErpVoucherStrategy {

    /** 凭证类型标识，如 erppayment / erpinventory */
    String getFtype();

    /**
     * 生成凭证
     * @param userInfo   当前用户
     * @param groupid    租户ID
     * @param periodCode 会计期间编码
     */
    void generateVoucher(UserInfo userInfo, String groupid, String periodCode);

    /**
     * 按指定日期生成凭证（手动触发）
     * <p>
     * 与 generateVoucher 的区别：直接使用指定日期而非从会计期间推导，
     * 适用于手动指定日期范围批量生成凭证的场景。
     * </p>
     * @param userInfo 当前用户
     * @param groupid  租户ID
     * @param date     日期 yyyy-MM-dd
     * @return 生成结果摘要（订单数、凭证数等）
     */
    default Map<String, Object> generateVoucherByDate(UserInfo userInfo, String groupid, String date) {
        return null;
    }

    /**
     * 获取金额计算逻辑明细（只读，用于展示）
     * @param groupid    租户ID
     * @param periodCode 期间编码
     * @return 计算逻辑明细数据
     */
    default Map<String, Object> getCalculationDetail(String groupid, String periodCode) {
        return null;
    }

    /**
     * 撤销凭证时还原原始单据状态
     * <p>
     * 每个策略负责还原自己业务类型的原始单据：
     * <ul>
     *   <li>付款策略：删除本地付款记录（ERP每晚会重新同步）</li>
     *   <li>库存策略：无额外状态需要还原</li>
     *   <li>发票策略：还原发票的 posting_status 和 voucher_id</li>
     * </ul>
     * </p>
     *
     * @param groupid   租户ID
     * @param orderId   原始单据ID
     * @param voucherId 凭证ID
     */
    default void revokeOriginalDocumentStatus(String groupid, String orderId, Long voucherId) {
        // 默认无操作，子类按需重写
    }
}