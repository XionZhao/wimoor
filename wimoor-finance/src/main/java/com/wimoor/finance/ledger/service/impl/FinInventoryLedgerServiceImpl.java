package com.wimoor.finance.ledger.service.impl;

import cn.hutool.core.util.StrUtil;
import com.wimoor.common.mvc.BizException;
import com.wimoor.common.result.Result;
import com.wimoor.finance.api.RemoteERPService;
import com.wimoor.finance.ledger.domain.dto.FinInventoryLedgerDTO;
import com.wimoor.finance.ledger.service.IFinInventoryLedgerService;
import com.wimoor.finance.util.QueryParamUtil;
import com.wimoor.finance.voucher.domain.FinVoucherEntries;
import com.wimoor.finance.voucher.domain.FinVouchers;
import com.wimoor.finance.voucher.service.IFinVouchersService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

/**
 * 进销存台账Service业务层处理
 *
 * @author wimoor
 * @date 2026-07-10
 */
@Service
public class FinInventoryLedgerServiceImpl implements IFinInventoryLedgerService {

    @Autowired
    private RemoteERPService remoteERPService;

    @Autowired
    private IFinVouchersService finVouchersService;

    /**
     * 查询汇总账（按SKU+仓库维度）
     */
    @Override
    public List<Map<String, Object>> selectSummary(FinInventoryLedgerDTO dto) {
        // 通过Feign调用ERP模块
        Map<String, Object> params = buildSummaryParams(dto);
        Result<?> result = remoteERPService.getInventoryLedgerSummary(params);
        if (Result.isSuccess(result) && result.getData() instanceof List) {
            return (List<Map<String, Object>>) result.getData();
        }
        return new ArrayList<>();
    }

    /**
     * 查询明细账（按SKU查询变动记录）
     */
    @Override
    public List<Map<String, Object>> selectDetail(FinInventoryLedgerDTO dto) {
        // 通过Feign调用ERP模块
        Map<String, Object> params = buildDetailParams(dto);
        Result<?> result = remoteERPService.getInventoryLedgerDetail(params);
        if (Result.isSuccess(result) && result.getData() instanceof List) {
            return (List<Map<String, Object>>) result.getData();
        }
        return new ArrayList<>();
    }

    /**
     * 查询明细账总数
     */
    @Override
    public long selectDetailCount(FinInventoryLedgerDTO dto) {
        Map<String, Object> params = buildDetailParams(dto);
        Result<?> result = remoteERPService.getInventoryLedgerDetailCount(params);
        if (Result.isSuccess(result) && result.getData() instanceof Long) {
            return (Long) result.getData();
        }
        return 0;
    }

    /**
     * 分页查询明细账
     */
    @Override
    public Map<String, Object> selectDetailPage(FinInventoryLedgerDTO dto, int pageNum, int pageSize) {
        Map<String, Object> params = buildDetailParamsWithPage(dto, pageNum, pageSize);
        Result<?> result = remoteERPService.getInventoryLedgerDetail(params);
        List<Map<String, Object>> rows = new ArrayList<>();
        if (Result.isSuccess(result) && result.getData() instanceof List) {
            rows = (List<Map<String, Object>>) result.getData();
        }
        long total = 0;
        if (pageNum == 1 && rows.size() < pageSize) {
            total = rows.size();
        } else {
            total = selectDetailCount(dto);
        }
        Map<String, Object> pageResult = new HashMap<>();
        pageResult.put("total", total);
        pageResult.put("rows", rows);
        return pageResult;
    }

    /**
     * 勾稽校验（Finance模块特有功能）
     */
    @Override
    public List<Map<String, Object>> selectCheckResult(FinInventoryLedgerDTO dto) {
        // 勾稽校验是财务特有功能，需要本地处理
        // 这里返回空列表，实际实现需要根据业务逻辑调整
        return new ArrayList<>();
    }

    /**
     * 库存趋势图数据（Finance模块特有功能）
     */
    @Override
    public List<Map<String, Object>> selectChartTrend(FinInventoryLedgerDTO dto) {
        // 趋势图是财务特有功能，需要本地处理
        // 这里返回空列表，实际实现需要根据业务逻辑调整
        return new ArrayList<>();
    }

    /**
     * 批量生成凭证（Finance模块特有功能）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> batchGenerateVoucher(FinInventoryLedgerDTO dto, String userName) {
        if (dto.getRecordIds() == null || dto.getRecordIds().isEmpty()) {
            throw new BizException("请选择需要生成凭证的变动记录");
        }
        if (StrUtil.isBlank(dto.getShopid())) {
            throw new BizException("缺少公司ID参数");
        }

        // 构建凭证对象
        Date now = new Date();
        FinVouchers voucher = new FinVouchers();
        voucher.setGroupid(dto.getShopid());
        voucher.setVoucherType("1"); // 普通凭证
        voucher.setVoucherDate(now);
        voucher.setDataSource(4); // 单据同步
        voucher.setPreparerBy(userName);
        voucher.setCreatedTime(now);
        voucher.setUpdatedTime(now);
        voucher.setTotalAmount(BigDecimal.ZERO);
        voucher.setVoucherStatus(1); // 草稿状态

        // 构建凭证分录
        List<FinVoucherEntries> entries = new ArrayList<>();
        long entryNo = 1;

        // 简化实现：生成一张汇总凭证
        FinVoucherEntries debitEntry = new FinVoucherEntries();
        debitEntry.setEntryNo(entryNo++);
        debitEntry.setDebitAmount(BigDecimal.ZERO);
        debitEntry.setCreditAmount(BigDecimal.ZERO);
        debitEntry.setSummary("进销存台账-批量生成");
        entries.add(debitEntry);

        voucher.setEntries(entries);

        // 保存凭证
        int result = finVouchersService.insertFinVouchers(voucher);
        Map<String, Object> resultMap = new HashMap<>();
        if (result > 0) {
            Long voucherId = voucher.getVoucherId();
            resultMap.put("successCount", dto.getRecordIds().size());
            resultMap.put("voucherIds", Arrays.asList(voucherId));
        } else {
            resultMap.put("successCount", 0);
            resultMap.put("voucherIds", new ArrayList<>());
        }
        return resultMap;
    }

    /**
     * 导出进销存台账数据
     */
    @Override
    public List<Map<String, Object>> exportData(FinInventoryLedgerDTO dto) {
        // 通过Feign调用ERP模块
        Map<String, Object> params = buildDetailParams(dto);
        Result<?> result = remoteERPService.exportInventoryLedger(params);
        if (Result.isSuccess(result) && result.getData() instanceof List) {
            return (List<Map<String, Object>>) result.getData();
        }
        return new ArrayList<>();
    }

    /**
     * 构建汇总查询参数
     */
    private Map<String, Object> buildSummaryParams(FinInventoryLedgerDTO dto) {
        Map<String, Object> params = new HashMap<>();
        params.put("shopid", dto.getShopid());
        if (StrUtil.isNotBlank(dto.getWarehouseid())) {
            params.put("warehouseId", dto.getWarehouseid());
        }
        if (StrUtil.isNotBlank(dto.getMaterialid())) {
            params.put("materialId", dto.getMaterialid());
        }
        if (StrUtil.isNotBlank(dto.getSku())) {
            params.put("sku", dto.getSku());
        }
        return params;
    }

    /**
     * 构建明细查询参数
     */
    private Map<String, Object> buildDetailParams(FinInventoryLedgerDTO dto) {
        Map<String, Object> params = new HashMap<>();
        params.put("shopid", dto.getShopid());
        if (StrUtil.isNotBlank(dto.getWarehouseid())) {
            params.put("warehouseId", dto.getWarehouseid());
        }
        if (StrUtil.isNotBlank(dto.getMaterialid())) {
            params.put("materialId", dto.getMaterialid());
        }
        if (StrUtil.isNotBlank(dto.getFormtype())) {
            params.put("formType", dto.getFormtype());
        }
        if (StrUtil.isNotBlank(dto.getStartPeriod())) {
            Date[] dateRange = QueryParamUtil.parseDatePeriodRange(dto.getStartPeriod(), dto.getEndPeriod());
            params.put("fromDate", dateRange[0]);
            params.put("toDate", dateRange[1]);
        }
        return params;
    }

    /**
     * 构建明细查询参数（带分页）
     */
    private Map<String, Object> buildDetailParamsWithPage(FinInventoryLedgerDTO dto, int pageNum, int pageSize) {
        Map<String, Object> params = buildDetailParams(dto);
        params.put("pageSize", pageSize);
        params.put("offset", (pageNum - 1) * pageSize);
        return params;
    }
}
