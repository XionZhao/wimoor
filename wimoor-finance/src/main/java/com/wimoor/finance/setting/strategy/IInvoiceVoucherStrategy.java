package com.wimoor.finance.setting.strategy;

import com.wimoor.common.user.UserInfo;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 发票凭证生成策略接口
 * <p>
 * 与 IErpVoucherStrategy 不同，此接口处理发票台账 → 凭证的转换，
 * 支持手动选择指定发票进行凭证生成。
 * </p>
 *
 * @author wimoor
 * @date 2026-08-21
 */
public interface IInvoiceVoucherStrategy {

    /**
     * 为选中的发票生成凭证
     *
     * @param userInfo     当前用户
     * @param groupid      租户ID
     * @param invoiceIds   发票ID列表
     * @param voucherType  凭证字类型
     * @param voucherDate  凭证日期
     * @param summary      摘要
     * @param voucherStatus 凭证状态
     * @param invoiceType  发票类型（可选）
     * @return 生成结果，包含 successCount、totalCount、errors 等
     */
    Map<String, Object> generateVoucher(UserInfo userInfo, String groupid,
                                         List<Long> invoiceIds, String voucherType,
                                         Date voucherDate, String summary,
                                         Integer voucherStatus, Integer invoiceType);
}
