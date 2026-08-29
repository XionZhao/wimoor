package com.wimoor.finance.ledger.controller;

import com.wimoor.common.core.web.controller.BaseController;
import com.wimoor.common.result.Result;
import com.wimoor.common.user.UserInfo;
import com.wimoor.common.user.UserInfoContext;
import com.wimoor.finance.ledger.domain.FinCarrierReconcileRecord;
import com.wimoor.finance.ledger.service.IFinCarrierLedgerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 承运商台账Controller
 *
 * @author wimoor
 */
@RestController
@RequestMapping("/api/v1/carrier/ledger")
public class FinCarrierLedgerController extends BaseController {

    @Autowired
    private IFinCarrierLedgerService finCarrierLedgerService;

    /**
     * 查询承运商台账汇总（按承运商维度）
     */
    @PostMapping("/summary")
    public Result summary(@RequestBody Map<String, Object> params) {
        UserInfo userInfo = UserInfoContext.get();
        params.put("shopid", userInfo.getCompanyid());
        params.put("groupid", userInfo.getGroupid());
        List<Map<String, Object>> list = finCarrierLedgerService.getCarrierLedgerSummary(params);
        Result result = Result.success(list);
        result.setTotal(list != null ? list.size() : 0);
        return result;
    }

    /**
     * 查询承运商台账统计数据（全局汇总）
     */
    @PostMapping("/statistics")
    public Result statistics(@RequestBody Map<String, Object> params) {
        UserInfo userInfo = UserInfoContext.get();
        params.put("shopid", userInfo.getCompanyid());
        Map<String, Object> statistics = finCarrierLedgerService.getCarrierLedgerStatistics(params);
        return Result.success(statistics);
    }

    /**
     * 承运商对账操作
     */
    @PostMapping("/reconcile")
    public Result reconcile(@RequestBody Map<String, Object> params) {
        UserInfo userInfo = UserInfoContext.get();
        String groupid = userInfo.getGroupid();
        String operator = userInfo.getUserName();
        String carrierId = params.get("carrierId") != null ? params.get("carrierId").toString() : null;
        if (carrierId == null || carrierId.isEmpty()) {
            return Result.failed("承运商ID不能为空");
        }
        boolean success = finCarrierLedgerService.reconcileCarrier(groupid, carrierId, operator, params);
        return success ? Result.success("对账成功") : Result.failed("对账失败");
    }

    /**
     * 获取承运商对账详情
     */
    @GetMapping("/reconcile/detail")
    public Result getReconcileDetail(@RequestParam String carrierId, @RequestParam String reconcileMonth) {
        UserInfo userInfo = UserInfoContext.get();
        String groupid = userInfo.getGroupid();
        FinCarrierReconcileRecord record = finCarrierLedgerService.getReconcileDetail(groupid, carrierId, reconcileMonth);
        return Result.success(record);
    }
}
