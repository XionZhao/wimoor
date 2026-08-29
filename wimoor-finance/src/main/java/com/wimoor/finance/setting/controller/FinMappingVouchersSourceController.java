package com.wimoor.finance.setting.controller;

import com.wimoor.common.core.web.domain.Result;
import com.wimoor.common.core.web.controller.BaseController;
import com.wimoor.common.core.web.page.TableDataInfo;
import com.wimoor.common.user.UserInfo;
import com.wimoor.common.user.UserInfoContext;
import com.wimoor.finance.setting.domain.FinMappingVouchersSource;
import com.wimoor.finance.setting.service.IFinMappingVouchersSourceService;
import com.wimoor.finance.setting.domain.FinMappingVouchersSourcePayment;
import com.wimoor.finance.setting.service.IFinMappingVouchersSourcePaymentService;
import com.wimoor.finance.setting.strategy.ErpVoucherStrategyFactory;
import com.wimoor.finance.setting.strategy.IErpVoucherStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 凭证生成记录Controller
 * 供前端查看每个订单的凭证生成状态和日志
 *
 * @author wimoor
 * @date 2026-08-07
 */
@RestController
@RequestMapping("/api/mappingVouchersSource")
@Slf4j
public class FinMappingVouchersSourceController extends BaseController
{
    @Resource
    private IFinMappingVouchersSourceService finMappingVouchersSourceService;

    @Resource
    private ErpVoucherStrategyFactory erpVoucherStrategyFactory;

    @Resource
    private IFinMappingVouchersSourcePaymentService finMappingVouchersSourcePaymentService;

    /**
     * 查询凭证生成记录列表（分页）
     * @param groupid 租户ID
     * @param voucherType 凭证类型：payment/inventory_transit/inventory_inbound
     * @param syncStatus 同步状态：0-待同步，1-已同步，2-已变更
     */
    @GetMapping("/list")
    public TableDataInfo list(@RequestParam("groupid") String groupid,
                              @RequestParam(value = "voucherType", required = false) String voucherType,
                              @RequestParam(value = "syncStatus", required = false) Integer syncStatus)
    {
        FinMappingVouchersSource query = new FinMappingVouchersSource();
        query.setGroupid(groupid);
        query.setVoucherType(voucherType);
        query.setSyncStatus(syncStatus);
        startPage();
        List<FinMappingVouchersSource> list = finMappingVouchersSourceService.selectFinMappingVouchersSourceList(query);
        return getDataTable(list);
    }

    /**
     * 获取凭证生成记录详情
     */
    @GetMapping(value = "/{id}")
    public Result getInfo(@PathVariable("id") Long id)
    {
        return success(finMappingVouchersSourceService.selectFinMappingVouchersSourceById(id));
    }

    /**
     * 查询订单的付款记录明细
     * @param groupid 租户ID
     * @param orderId 采购订单ID
     */
    @GetMapping("/paymentRecords")
    public Result paymentRecords(@RequestParam("groupid") String groupid,
                                  @RequestParam("orderId") String orderId)
    {
        List<FinMappingVouchersSourcePayment> records = finMappingVouchersSourcePaymentService.selectByOrderId(groupid, orderId);
        return success(records);
    }

    /**
     * 按日期区间批量生成采购付款凭证
     * <p>
     * 为区间内的每一天单独处理：从ERP获取当天完成付款的订单，
     * 落地到 fin_mapping_vouchers_source_payment，生成凭证。已生成过的凭证自动更新。
     * </p>
     *
     * @param groupid   租户ID
     * @param startDate 开始日期 yyyy-MM-dd
     * @param endDate   结束日期 yyyy-MM-dd
     * @return 每天的处理结果汇总
     */
    @PostMapping("/generateByDateRange")
    public Result generateByDateRange(@RequestParam("groupid") String groupid,
                                       @RequestParam("startDate") String startDate,
                                       @RequestParam("endDate") String endDate) {
        try {
            UserInfo userInfo = UserInfoContext.get();
            if (userInfo == null) {
                return Result.error("用户未登录或登录已过期");
            }
            IErpVoucherStrategy strategy = erpVoucherStrategyFactory.getStrategy("erppayment");

            LocalDate start = LocalDate.parse(startDate);
            LocalDate end = LocalDate.parse(endDate);

            if (start.isAfter(end)) {
                return Result.error("开始日期不能晚于结束日期");
            }

            List<Map<String, Object>> dailyResults = new ArrayList<>();
            int totalOrders = 0;
            int totalVouchers = 0;
            int totalErrors = 0;

            LocalDate current = start;
            while (!current.isAfter(end)) {
                String date = current.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                try {
                    Map<String, Object> result = strategy.generateVoucherByDate(userInfo, groupid, date);
                    dailyResults.add(result);
                    totalOrders += (int) result.getOrDefault("orderCount", 0);
                    totalVouchers += (int) result.getOrDefault("voucherCount", 0);
                } catch (Exception e) {
                    log.error("日期 [{}] 生成凭证异常", date, e);
                    Map<String, Object> errorResult = new HashMap<>();
                    errorResult.put("date", date);
                    errorResult.put("orderCount", 0);
                    errorResult.put("voucherCount", 0);
                    errorResult.put("message", e.getMessage());
                    dailyResults.add(errorResult);
                    totalErrors++;
                }
                current = current.plusDays(1);
            }

            Map<String, Object> summary = new HashMap<>();
            summary.put("totalDays", dailyResults.size());
            summary.put("totalOrders", totalOrders);
            summary.put("totalVouchers", totalVouchers);
            summary.put("errorDays", totalErrors);
            summary.put("dailyResults", dailyResults);

            return success(summary);
        } catch (Exception e) {
            log.error("批量生成凭证接口异常", e);
            return Result.error("批量生成凭证失败：" + e.getMessage());
        }
    }

    /**
     * 删除凭证生成记录
     */
    @DeleteMapping("/{id}")
    public Result delete(@PathVariable("id") Long id) {
        try {
            int rows = finMappingVouchersSourceService.deleteFinMappingVouchersSourceById(id);
            return rows > 0 ? success("删除成功") : Result.error("删除失败，记录不存在");
        } catch (Exception e) {
            log.error("删除凭证记录异常", e);
            return Result.error("删除失败：" + e.getMessage());
        }
    }

    /**
     * 批量删除凭证生成记录
     */
    @DeleteMapping("/deleteBatch/{ids}")
    public Result deleteBatch(@PathVariable("ids") Long[] ids) {
        try {
            int rows = finMappingVouchersSourceService.deleteFinMappingVouchersSourceByIds(ids);
            return rows > 0 ? success("成功删除 " + rows + " 条记录") : Result.error("删除失败，记录不存在");
        } catch (Exception e) {
            log.error("批量删除凭证记录异常", e);
            return Result.error("批量删除失败：" + e.getMessage());
        }
    }
}
