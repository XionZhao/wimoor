package com.wimoor.finance.ledger.service.impl;

import com.wimoor.common.result.Result;
import com.wimoor.finance.api.RemoteERPService;
import com.wimoor.finance.ledger.domain.dto.FinPurchaseLedgerQuery;
import com.wimoor.finance.ledger.service.IFinPurchaseLedgerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

/**
 * 采购账户台账Service业务层处理
 *
 * @author wimoor
 */
@Service
public class FinPurchaseLedgerServiceImpl implements IFinPurchaseLedgerService {
    private static final Logger log = LoggerFactory.getLogger(FinPurchaseLedgerServiceImpl.class);

    @Autowired
    private RemoteERPService remoteERPService;

    /**
     * 查询采购订单列表
     */
    @Override
    public List<Map<String, Object>> selectPurchaseLedgerList(FinPurchaseLedgerQuery query) {
        // 通过Feign调用ERP模块
        Map<String, Object> params = buildParams(query);
        Result<?> result = remoteERPService.getPurchaseLedgerList(params);
        if (Result.isSuccess(result) && result.getData() instanceof Map) {
            Map<String, Object> data = (Map<String, Object>) result.getData();
            Object list = data.get("list");
            if (list instanceof List) {
                return (List<Map<String, Object>>) list;
            }
        }
        return new ArrayList<>();
    }

    /**
     * 统计订单总额、已付总额、未付总额
     */
    @Override
    public Map<String, Object> selectPurchaseLedgerStatistics(FinPurchaseLedgerQuery query) {
        // 通过Feign调用ERP模块
        Map<String, Object> params = buildParams(query);
        Result<?> result = remoteERPService.getPurchaseLedgerStatistics(params);
        if (Result.isSuccess(result) && result.getData() instanceof Map) {
            return (Map<String, Object>) result.getData();
        }
        return new HashMap<>();
    }

    /**
     * 查询所有采购账户及余额
     */
    @Override
    public List<Map<String, Object>> selectAccounts(String groupid) {
        // 通过Feign调用ERP模块
        Result<?> result = remoteERPService.getPurchaseLedgerAccounts(groupid);
        if (Result.isSuccess(result) && result.getData() instanceof List) {
            return (List<Map<String, Object>>) result.getData();
        }
        return new ArrayList<>();
    }

    /**
     * 查询某个订单的付款明细
     */
    @Override
    public List<Map<String, Object>> selectPayments(String entryId) {
        // 通过Feign调用ERP模块
        Result<?> result = remoteERPService.getPurchaseLedgerPayments(entryId);
        if (Result.isSuccess(result) && result.getData() instanceof List) {
            return (List<Map<String, Object>>) result.getData();
        }
        return new ArrayList<>();
    }

    /**
     * 付款操作（通过Feign调用ERP模块的付款接口）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void pay(String entryId, String acct, String projectid, BigDecimal payprice,
                    String remark, String operator, Integer paymentMethod) {
        // 通过Feign调用ERP模块的付款接口
        Map<String, Object> params = new HashMap<>();
        params.put("entryId", entryId);
        params.put("acct", acct);
        params.put("projectid", projectid);
        params.put("payprice", payprice);
        params.put("remark", remark);
        params.put("operator", operator);
        params.put("paymentMethod", paymentMethod);
        
        Result<?> result = remoteERPService.payPurchaseLedger(params);
        if (!Result.isSuccess(result)) {
            throw new RuntimeException("付款操作失败: " + result.getMsg());
        }
    }

    /**
     * 构建Feign调用参数
     */
    private Map<String, Object> buildParams(FinPurchaseLedgerQuery query) {
        Map<String, Object> params = new HashMap<>();
        params.put("groupid", query.getGroupid());
        params.put("shopid", query.getGroupid());
        if (query.getSupplier() != null) {
            params.put("supplierId", query.getSupplier());
        }
        if (query.getFromDate() != null) {
            params.put("fromDate", query.getFromDate());
        }
        if (query.getToDate() != null) {
            params.put("toDate", query.getToDate());
        }
        if (query.getAuditstatus() != null) {
            params.put("auditstatus", query.getAuditstatus());
        }
        if (query.getKeyword() != null) {
            params.put("search", query.getKeyword());
        }
        if (query.getPaystatus() != null) {
            params.put("paystatus", query.getPaystatus());
        }
        return params;
    }
}
