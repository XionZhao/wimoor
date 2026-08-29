package com.wimoor.finance.ledger.controller;

import com.wimoor.common.core.web.controller.BaseController;
import com.wimoor.common.core.web.domain.Result;
import com.wimoor.common.core.web.page.TableDataInfo;
import com.wimoor.common.user.UserInfo;
import com.wimoor.common.user.UserInfoContext;
import com.wimoor.finance.ledger.domain.dto.FinPurchaseLedgerQuery;
import com.wimoor.finance.ledger.service.IFinPurchaseLedgerService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 采购账户台账Controller
 *
 * @author wimoor
 */
@RestController
@RequestMapping("/api/v1/purchase/ledger")
public class FinPurchaseLedgerController extends BaseController {

    @Autowired
    private IFinPurchaseLedgerService finPurchaseLedgerService;

    /**
     * 查询采购订单列表
     */
    @GetMapping("/list")
    public TableDataInfo list(FinPurchaseLedgerQuery query) {
        fillGroupid(query);
        startPage();
        List<Map<String, Object>> list = finPurchaseLedgerService.selectPurchaseLedgerList(query);
        return getDataTable(list);
    }

    /**
     * 统计订单总额、已付总额、未付总额
     */
    @GetMapping("/statistics")
    public Result statistics(FinPurchaseLedgerQuery query) {
        fillGroupid(query);
        Map<String, Object> statistics = finPurchaseLedgerService.selectPurchaseLedgerStatistics(query);
        return success(statistics);
    }

    /**
     * 查询所有采购账户及余额
     */
    @GetMapping("/accounts")
    public Result accounts(FinPurchaseLedgerQuery query) {
        fillGroupid(query);
        List<Map<String, Object>> list = finPurchaseLedgerService.selectAccounts(query.getGroupid());
        return success(list);
    }

    /**
     * 查询某个订单的付款明细
     */
    @GetMapping("/payments")
    public Result payments(@RequestParam String entryId) {
        List<Map<String, Object>> list = finPurchaseLedgerService.selectPayments(entryId);
        return success(list);
    }

    /**
     * 付款操作
     */
    @PostMapping("/pay")
    public Result pay(@RequestBody Map<String, Object> params) {
        String entryId = (String) params.get("entryId");
        String acct = (String) params.get("acct");
        String projectid = (String) params.get("projectid");
        BigDecimal payprice = new BigDecimal(params.get("payprice").toString());
        String remark = (String) params.get("remark");
        String operator = (String) params.get("operator");
        Integer paymentMethod = params.get("paymentMethod") != null ?
                Integer.parseInt(params.get("paymentMethod").toString()) : null;

        finPurchaseLedgerService.pay(entryId, acct, projectid, payprice, remark, operator, paymentMethod);
        return success();
    }

    /**
     * 填充groupid，如果前端未传则从当前用户上下文获取
     */
    private void fillGroupid(FinPurchaseLedgerQuery query) {
        if (StringUtils.isBlank(query.getGroupid())) {
            UserInfo user = UserInfoContext.get();
            query.setGroupid(user.getCompanyid());
        }
    }
}
