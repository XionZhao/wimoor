package com.wimoor.finance.ledger.controller;

import com.wimoor.common.core.utils.poi.ExcelUtil;
import com.wimoor.common.core.web.controller.BaseController;
import com.wimoor.common.core.web.domain.Result;
import com.wimoor.common.core.web.page.TableDataInfo;
import com.wimoor.common.user.UserInfo;
import com.wimoor.common.user.UserInfoContext;
import com.wimoor.finance.ledger.domain.dto.FinInventoryLedgerDTO;
import com.wimoor.finance.ledger.service.IFinInventoryLedgerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;

/**
 * 进销存台账Controller
 *
 * @author wimoor
 * @date 2026-07-10
 */
@RestController
@RequestMapping("/api/v1/inventory/ledger")
public class FinInventoryLedgerController extends BaseController {

    @Autowired
    private IFinInventoryLedgerService finInventoryLedgerService;

    /**
     * 查询汇总账（按SKU+仓库维度，查询数量、金额、移动平均单价）
     */
    @GetMapping("/summary")
    public TableDataInfo summary(FinInventoryLedgerDTO dto) {
        UserInfo userInfo = UserInfoContext.get();
        dto.setShopid(userInfo.getCompanyid());
        startPage();
        List<Map<String, Object>> list = finInventoryLedgerService.selectSummary(dto);
        return getDataTable(list);
    }

    /**
     * 查询明细账（按SKU查询变动记录，包含入库/出库/调拨等）
     */
    @GetMapping("/detail")
    public TableDataInfo detail(FinInventoryLedgerDTO dto,
                                @RequestParam(defaultValue = "1") int pageNum,
                                @RequestParam(defaultValue = "20") int pageSize) {
        UserInfo userInfo = UserInfoContext.get();
        dto.setShopid(userInfo.getCompanyid());
        Map<String, Object> pageResult = finInventoryLedgerService.selectDetailPage(dto, pageNum, pageSize);
        TableDataInfo rspData = new TableDataInfo();
        rspData.setCode(0);
        rspData.setMsg("查询成功");
        rspData.setRows((List<?>) pageResult.get("rows"));
        rspData.setTotal((long) pageResult.get("total"));
        return rspData;
    }

    /**
     * 勾稽校验（验证库存金额与凭证金额是否一致）
     */
    @GetMapping("/check")
    public TableDataInfo check(FinInventoryLedgerDTO dto) {
        UserInfo userInfo = UserInfoContext.get();
        dto.setShopid(userInfo.getCompanyid());
        startPage();
        List<Map<String, Object>> list = finInventoryLedgerService.selectCheckResult(dto);
        return getDataTable(list);
    }

    /**
     * 库存趋势图数据（按日期聚合库存金额变化）
     */
    @GetMapping("/chart")
    public Result chart(FinInventoryLedgerDTO dto) {
        UserInfo userInfo = UserInfoContext.get();
        dto.setShopid(userInfo.getCompanyid());
        List<Map<String, Object>> list = finInventoryLedgerService.selectChartTrend(dto);
        return success(list);
    }

    /**
     * 批量生成凭证
     */
    @PostMapping("/voucher")
    public Result batchGenerateVoucher(@RequestBody FinInventoryLedgerDTO dto) {
        UserInfo userInfo = UserInfoContext.get();
        dto.setShopid(userInfo.getCompanyid());
        Map<String, Object> result = finInventoryLedgerService.batchGenerateVoucher(dto, userInfo.getUserName());
        return success(result);
    }

    /**
     * 导出进销存台账
     */
    @GetMapping("/export")
    public void export(HttpServletResponse response, FinInventoryLedgerDTO dto) {
        UserInfo userInfo = UserInfoContext.get();
        dto.setShopid(userInfo.getCompanyid());
        List<Map<String, Object>> list = finInventoryLedgerService.exportData(dto);
        ExcelUtil util = new ExcelUtil(Map.class);
        util.exportExcel(response, list, "进销存台账数据");
    }
}
