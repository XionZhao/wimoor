package com.wimoor.finance.closing.controller;


import com.wimoor.common.core.utils.poi.ExcelUtil;
import com.wimoor.common.core.web.controller.BaseController;
import com.wimoor.common.core.web.domain.Result;
import com.wimoor.common.core.web.page.TableDataInfo;
import com.wimoor.common.user.UserInfo;
import com.wimoor.common.user.UserInfoContext;
import com.wimoor.finance.closing.domain.*;
import com.wimoor.finance.closing.service.*;
import com.wimoor.finance.closing.service.strategy.FinClosingTemplateStrategyFactory;
import com.wimoor.finance.closing.service.strategy.IFinClosingTemplateStrategy;
import com.wimoor.finance.setting.strategy.ErpVoucherStrategyFactory;
import com.wimoor.finance.setting.strategy.IErpVoucherStrategy;
import com.wimoor.finance.setting.domain.FinAccountingPeriods;
import com.wimoor.finance.setting.domain.FinAccountingSubjects;
import com.wimoor.finance.setting.mapper.FinAccountingSubjectsMapper;
import com.wimoor.finance.setting.service.IFinAccountingPeriodsService;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.wimoor.finance.api.RemoteAdminService;
import javax.servlet.http.HttpServletResponse;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/closing_template")
public class FinClosingTemplateController extends BaseController
{
    @Autowired
    private IFinClosingTemplateService finClosingTemplateService;
    @Autowired
    private FinClosingTemplateStrategyFactory strategyFactory;
    @Autowired
    private ErpVoucherStrategyFactory erpVoucherStrategyFactory;
    @Autowired
    private IFinClosingTemplateVouchersService finClosingTemplateVouchersService;
    @Autowired
    private IFinAccountingPeriodsService finAccountingPeriodsService;
    @Autowired
    private IFinClosingTemplateItemService finClosingTemplateItemService;
    @Autowired
    private IFinClosingTemplateProfitLossService finClosingTemplateProfitLossService;
    @Autowired
    private IFinClosingTemplateAmazonService finClosingTemplateAmazonService;
    @Autowired
    private IFinClosingTemplateFeishuService finClosingTemplateFeishuService;
    @Autowired
    private RemoteAdminService remoteAdminService;
    @Autowired
    @Qualifier("com.wimoor.finance.api.RemoteAdminService")
    private com.wimoor.finance.api.RemoteAdminService financeRemoteAdminService;
    @Autowired
    private FinAccountingSubjectsMapper finAccountingSubjectsMapper;

    @GetMapping("/list")
    public TableDataInfo list(FinClosingTemplate finClosingTemplate)
    {
        startPage();
        List<FinClosingTemplate> list = finClosingTemplateService.selectFinClosingTemplateList(finClosingTemplate);
        for(FinClosingTemplate item:list){
            FinAccountingPeriods period = finAccountingPeriodsService.getCurrentPeriod(item.getGroupid());
            Date voucherDate = period.getEndDate();
            String templateId = item.getId();
            String groupid=item.getGroupid();
            FinClosingTemplateVouchers query=new FinClosingTemplateVouchers();
            query.setGroupid(groupid);
            query.setTemplateId(templateId);
            query.setVoucherDate(voucherDate);
            List<FinClosingTemplateVouchers> voucher = finClosingTemplateVouchersService.selectFinClosingTemplateVouchersList(query);
            if(voucher!=null && !voucher.isEmpty()){
                item.setVourchesId(voucher.get(0).getVourchesId());
            }
        }
        return getDataTable(list);
    }

    @GetMapping("/templateVouchers")
    public Result templateVouchers(String templateid,String period){
            FinClosingTemplate template=finClosingTemplateService.selectFinClosingTemplateById(templateid);
            if(template==null){return error("模版不存在");}
            FinAccountingPeriods accPeriod = finAccountingPeriodsService.selectByPeriod(template.getGroupid(), period);
            if(accPeriod==null){return error("会计期间不存在");}
            if(accPeriod.getPeriodStatus()==3){return error("会计期间已关闭");}
            Date voucherDate = accPeriod.getEndDate();
            FinClosingTemplateVouchers query=new FinClosingTemplateVouchers();
            query.setGroupid(template.getGroupid());
            query.setTemplateId(template.getId());
            query.setVoucherDate(voucherDate);
            List<FinClosingTemplateVouchers> voucher = finClosingTemplateVouchersService.selectFinClosingTemplateVouchersList(query);
            if(voucher!=null && !voucher.isEmpty()){
                template.setVourchesId(voucher.get(0).getVourchesId());
            }
        return success(template);
    }

    @GetMapping("/voucherLog")
    public Result getVoucherLog(String templateid, String period){
        FinClosingTemplate template = finClosingTemplateService.selectFinClosingTemplateById(templateid);
        if(template == null){ return error("模版不存在"); }
        FinAccountingPeriods accPeriod = finAccountingPeriodsService.selectByPeriod(template.getGroupid(), period);
        if(accPeriod == null){ return error("会计期间不存在"); }
        Date voucherDate = accPeriod.getEndDate();
        FinClosingTemplateVouchers query = new FinClosingTemplateVouchers();
        query.setGroupid(template.getGroupid());
        query.setTemplateId(template.getId());
        query.setVoucherDate(voucherDate);
        List<FinClosingTemplateVouchers> vouchers = finClosingTemplateVouchersService.selectFinClosingTemplateVouchersList(query);
        Map<String, Object> result = new HashMap<>();
        result.put("ftype", template.getFtype());
        result.put("templateName", template.getName());
        if(vouchers != null && !vouchers.isEmpty()){
            FinClosingTemplateVouchers voucher = vouchers.get(0);
            result.put("datalog", voucher.getDatalog());
            result.put("vourchesId", voucher.getVourchesId());
            result.put("voucherDate", voucher.getVoucherDate());
            result.put("createBy", voucher.getCreateBy());
            result.put("createdTime", voucher.getCreatedTime());
        }
        return success(result);
    }

    @PostMapping("/export")
    public void export(HttpServletResponse response, FinClosingTemplate finClosingTemplate)
    {
        List<FinClosingTemplate> list = finClosingTemplateService.selectFinClosingTemplateList(finClosingTemplate);
        ExcelUtil<FinClosingTemplate> util = new ExcelUtil<FinClosingTemplate>(FinClosingTemplate.class);
        util.exportExcel(response, list, "财务结算的各个模版名称数据");
    }

    @GetMapping(value = "/{id}")
    public Result getInfo(@PathVariable("id") String id)
    {
        return success(finClosingTemplateService.selectFinClosingTemplateById(id));
    }

    @PostMapping
    public Result add(@RequestBody FinClosingTemplate finClosingTemplate)
    {
        UserInfo userinfo = UserInfoContext.get();
        finClosingTemplate.setCreateBy(userinfo.getUserName());
        finClosingTemplate.setCreatedTime(new Date());
        finClosingTemplate.setModifyBy(userinfo.getUserName());
        finClosingTemplate.setUpdatedTime(new Date());
        return toResult(finClosingTemplateService.insertFinClosingTemplate(finClosingTemplate));
    }

    @PutMapping
    public Result edit(@RequestBody FinClosingTemplate finClosingTemplate)
    {
        UserInfo userinfo = UserInfoContext.get();
        finClosingTemplate.setModifyBy(userinfo.getUserName());
        finClosingTemplate.setUpdatedTime(new Date());
        return toResult(finClosingTemplateService.updateFinClosingTemplate(finClosingTemplate));
    }

	@DeleteMapping("/{ids}")
    public Result remove(@PathVariable String[] ids)
    {
        return toResult(finClosingTemplateService.deleteFinClosingTemplateByIds(ids));
    }
    @GetMapping("calculationDetail")
    public Result getCalculationDetail(String templateId, String period) {
        FinClosingTemplate template = finClosingTemplateService.selectFinClosingTemplateById(templateId);
        if (template == null) {
            return Result.error("模板不存在");
        }
        String fType = template.getFtype();

        // ERP类型使用新策略
        if (erpVoucherStrategyFactory.hasStrategy(fType)) {
            IErpVoucherStrategy strategy = erpVoucherStrategyFactory.getStrategy(fType);
            Map<String, Object> detail = strategy.getCalculationDetail(template.getGroupid(), period);
            return success(detail);
        }

        if (!strategyFactory.hasStrategy(fType)) {
            return Result.error("未找到模板类型为 [" + fType + "] 的处理策略");
        }
        IFinClosingTemplateStrategy strategy = strategyFactory.getStrategy(fType);
        Map<String, Object> detail = strategy.getCalculationDetail(templateId, period);
        return success(detail);
    }
    
    @GetMapping("initTemplateItem")
    public Result initTemplateItem(String templateid){
        FinClosingTemplate finClosingTemp = finClosingTemplateService.selectFinClosingTemplateById(templateid);
        if(finClosingTemp == null){
            return Result.error("模板不存在");
        }
        strategyFactory.initTemplateItem(finClosingTemp);
        return success();
    }

    @GetMapping("voucher")
    @Transactional
    public Result generateVoucher(String templateId,String period){
        FinClosingTemplate finClosingTemp = finClosingTemplateService.selectFinClosingTemplateById(templateId);
        if(finClosingTemp == null){
            return Result.error("模板不存在");
        }
        String fType = finClosingTemp.getFtype();
        UserInfo userinfo = UserInfoContext.get();

        // ERP类型使用新策略，直接传入 groupid
        if (erpVoucherStrategyFactory.hasStrategy(fType)) {
            IErpVoucherStrategy strategy = erpVoucherStrategyFactory.getStrategy(fType);
            strategy.generateVoucher(userinfo, finClosingTemp.getGroupid(), period);
            return success();
        }

        if(!strategyFactory.hasStrategy(fType)){
            return Result.error("未找到模板类型为 [" + fType + "] 的处理策略");
        }
        IFinClosingTemplateStrategy strategy = strategyFactory.getStrategy(fType);
        strategy.generateVoucher(userinfo,templateId,period);
        return success();
    }

    /**
     * 导出配置 - 按模板类型分页签
     */
    @PostMapping("/exportConfig")
    public void exportConfig(HttpServletResponse response, @RequestBody Map<String, Object> params) throws Exception {
        String groupid = (String) params.get("groupid");
        List<String> templateIds = (List<String>) params.get("templateIds");
        
        System.out.println("导出配置 - groupid: " + groupid + ", templateIds: " + templateIds);
        
        if (templateIds == null || templateIds.isEmpty()) {
            response.setContentType("application/json;charset=utf-8");
            response.getWriter().write("{\"code\":500,\"msg\":\"请选择要导出的配置\"}");
            return;
        }

        // 查询选中的模板
        List<FinClosingTemplate> templates = new ArrayList<>();
        for (String id : templateIds) {
            FinClosingTemplate template = finClosingTemplateService.selectFinClosingTemplateById(id);
            if (template != null) {
                templates.add(template);
                System.out.println("查询到模板: " + template.getId() + " - " + template.getName() + " - " + template.getFtype() + " - 国家: " + template.getCountry());
            } else {
                System.out.println("未查询到模板, id: " + id);
            }
        }
        
        System.out.println("总共查询到 " + templates.size() + " 个模板");

        // 按类型分组
        Map<String, List<FinClosingTemplate>> typeGroupMap = templates.stream()
            .collect(Collectors.groupingBy(FinClosingTemplate::getFtype));
        
        System.out.println("模板类型分组: " + typeGroupMap.keySet());

        // 创建工作簿
        Workbook workbook = new XSSFWorkbook();
        
        // 类型名称映射
        Map<String, String> typeNameMap = new HashMap<>();
        typeNameMap.put("loss", "结转损益");
        typeNameMap.put("fct", "期末调汇");
        typeNameMap.put("amzpayment", "亚马逊报表");
        typeNameMap.put("feishu", "飞书表格");
        typeNameMap.put("other", "其他自定义");

        // 为每个类型创建页签
        for (Map.Entry<String, List<FinClosingTemplate>> entry : typeGroupMap.entrySet()) {
            String ftype = entry.getKey();
            List<FinClosingTemplate> typeTemplates = entry.getValue();
            String sheetName = typeNameMap.getOrDefault(ftype, ftype);
            
            // 如果页签名重复，添加数字后缀
            String finalSheetName = sheetName;
            int suffix = 1;
            while (workbook.getSheet(finalSheetName) != null) {
                finalSheetName = sheetName + "_" + (++suffix);
            }
            
            Sheet sheet = workbook.createSheet(finalSheetName);
            
            // 创建表头样式
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            // 根据类型创建不同的表头
            Row headerRow = sheet.createRow(0);
            List<String> headers = getExportHeaders(ftype);
            for (int i = 0; i < headers.size(); i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers.get(i));
                cell.setCellStyle(headerStyle);
            }

            // 填充数据
            int rowNum = 1;
            for (FinClosingTemplate template : typeTemplates) {
                List<Map<String, Object>> rows = getExportData(ftype, template, groupid);
                for (Map<String, Object> rowData : rows) {
                    Row row = sheet.createRow(rowNum++);
                    for (int i = 0; i < headers.size(); i++) {
                        String key = headers.get(i);
                        Object value = rowData.get(key);
                        Cell cell = row.createCell(i);
                        if (value != null) {
                            cell.setCellValue(value.toString());
                        }
                    }
                }
            }

            // 自动调整列宽
            for (int i = 0; i < headers.size(); i++) {
                sheet.autoSizeColumn(i);
            }
        }

        // 输出文件
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment;filename=closing_config.xlsx");
        workbook.write(response.getOutputStream());
        workbook.close();
    }

    /**
     * 获取导出表头
     */
    private List<String> getExportHeaders(String ftype) {
        List<String> headers = new ArrayList<>();
        headers.add("模板名称");
        
        switch (ftype) {
            case "loss":
                headers.add("结转周期");
                headers.add("结转方式");
                headers.add("凭证摘要");
                headers.add("方向处理");
                headers.add("损益科目编码");
                headers.add("本年利润科目编码");
                headers.add("以前年度损益调整科目编码");
                headers.add("调整结转科目编码");
                break;
            case "fct":
                headers.add("摘要");
                headers.add("科目编码");
                headers.add("金额字段");
                headers.add("方向");
                break;
            case "amzpayment":
                headers.add("国家");
                headers.add("摘要");
                headers.add("科目编码");
                headers.add("金额字段");
                headers.add("方向");
                break;
            case "other":
                headers.add("摘要");
                headers.add("科目编码");
                headers.add("金额字段");
                headers.add("方向");
                break;
            case "feishu":
                headers.add("表格名称");
                headers.add("摘要字段");
                headers.add("日期字段");
                headers.add("科目字段");
                headers.add("金额字段");
                headers.add("汇总方式");
                headers.add("过滤条件");
                headers.add("借方科目编码");
                headers.add("贷方科目编码");
                headers.add("内容配对");
                break;
        }
        
        return headers;
    }

    /**
     * 获取导出数据
     */
    private List<Map<String, Object>> getExportData(String ftype, FinClosingTemplate template, String groupid) {
        List<Map<String, Object>> rows = new ArrayList<>();
        
        switch (ftype) {
            case "loss":
                // 查询结转损益配置
                FinClosingTemplateProfitLoss queryLoss = new FinClosingTemplateProfitLoss();
                queryLoss.setTemplateId(template.getId());
                System.out.println("查询结转损益配置 - templateId: " + template.getId());
                List<FinClosingTemplateProfitLoss> lossList = finClosingTemplateProfitLossService.selectFinClosingTemplateProfitLossList(queryLoss);
                System.out.println("查询到 " + (lossList != null ? lossList.size() : 0) + " 条结转损益配置");
                for (FinClosingTemplateProfitLoss loss : lossList) {
                    Map<String, Object> row = new HashMap<>();
                    row.put("模板名称", template.getName());
                    // 结转周期：0-按月结转，1-按年结转
                    row.put("结转周期", "0".equals(loss.getTransferCycle()) ? "按月结转" : "1".equals(loss.getTransferCycle()) ? "按年结转" : loss.getTransferCycle());
                    // 结转方式：0-追加结转，1-重新结转
                    row.put("结转方式", "0".equals(loss.getTransferMethod()) ? "追加结转" : "1".equals(loss.getTransferMethod()) ? "重新结转" : loss.getTransferMethod());
                    row.put("凭证摘要", loss.getSummary());
                    // 方向处理：0-按科目方向反向结转，1-按金额正数结转
                    Integer directionHandling = loss.getDirectionHandling();
                    row.put("方向处理", directionHandling != null && directionHandling == 0 ? "按科目方向反向结转" : directionHandling != null && directionHandling == 1 ? "按金额正数结转" : directionHandling);
                    row.put("损益科目编码", loss.getProfitLossSubjectCode());
                    row.put("本年利润科目编码", loss.getCurrentYearProfitSubjectCode());
                    row.put("以前年度损益调整科目编码", loss.getPriorYearAdjustmentSubjectCode());
                    row.put("调整结转科目编码", loss.getPriorYearAdjustTransferSubjectCode());
                    rows.add(row);
                }
                break;
            case "fct":
                // 期末调汇模板，查询 fin_closing_template_item 表
                FinClosingTemplateItem queryFctItem = new FinClosingTemplateItem();
                queryFctItem.setClosingTemplateId(String.valueOf(template.getId()));
                System.out.println("查询期末调汇模板子项 - templateId: " + template.getId());
                List<FinClosingTemplateItem> fctItemList = finClosingTemplateItemService.selectFinClosingTemplateItemList(queryFctItem);
                System.out.println("查询到 " + (fctItemList != null ? fctItemList.size() : 0) + " 条期末调汇模板子项");
                for (FinClosingTemplateItem item : fctItemList) {
                    Map<String, Object> row = new HashMap<>();
                    row.put("模板名称", template.getName());
                    row.put("摘要", item.getSummary());
                    // 根据subjectId查询科目编码
                    String subjectCode = getSubjectCodeById(item.getSubjectId());
                    row.put("科目编码", subjectCode);
                    row.put("金额字段", item.getAmountField());
                    // 方向：1-借方，2-贷方
                    row.put("方向", item.getDirection() != null && item.getDirection() == 1 ? "借方" : item.getDirection() != null && item.getDirection() == 2 ? "贷方" : item.getDirection());
                    rows.add(row);
                }
                break;
            case "amzpayment":
                // 查询亚马逊报表模板子项
                FinClosingTemplateAmazon queryAmazon = new FinClosingTemplateAmazon();
                queryAmazon.setClosingTemplateId(String.valueOf(template.getId()));
                System.out.println("查询亚马逊模板子项 - templateId: " + template.getId());
                List<FinClosingTemplateAmazon> amazonList = finClosingTemplateAmazonService.selectFinClosingTemplateAmazonList(queryAmazon);
                System.out.println("查询到 " + (amazonList != null ? amazonList.size() : 0) + " 条亚马逊模板子项");
                for (FinClosingTemplateAmazon amazon : amazonList) {
                    Map<String, Object> row = new HashMap<>();
                    row.put("模板名称", template.getName());
                    row.put("国家", template.getCountry());
                    row.put("摘要", amazon.getSummary());
                    // 根据subjectId查询科目编码
                    String subjectCode = getSubjectCodeById(amazon.getSubjectId());
                    row.put("科目编码", subjectCode);
                    row.put("金额字段", amazon.getAmountField());

                    // 方向：1-借方，2-贷方
                    row.put("方向", amazon.getDirection() != null && amazon.getDirection() == 1 ? "借方" : amazon.getDirection() != null && amazon.getDirection() == 2 ? "贷方" : amazon.getDirection());
                    rows.add(row);
                }
                break;
            case "other":
                // 查询通用模板子项
                FinClosingTemplateItem queryItem = new FinClosingTemplateItem();
                queryItem.setClosingTemplateId(String.valueOf(template.getId()));
                System.out.println("查询通用模板子项 - templateId: " + template.getId());
                List<FinClosingTemplateItem> itemList = finClosingTemplateItemService.selectFinClosingTemplateItemList(queryItem);
                System.out.println("查询到 " + (itemList != null ? itemList.size() : 0) + " 条通用模板子项");
                for (FinClosingTemplateItem item : itemList) {
                    Map<String, Object> row = new HashMap<>();
                    row.put("模板名称", template.getName());
                    row.put("摘要", item.getSummary());
                    // 根据subjectId查询科目编码
                    String subjectCode = getSubjectCodeById(item.getSubjectId());
                    row.put("科目编码", subjectCode);
                    row.put("金额字段", item.getAmountField());
                    // 方向：1-借方，2-贷方
                    row.put("方向", item.getDirection() != null && item.getDirection() == 1 ? "借方" : item.getDirection() != null && item.getDirection() == 2 ? "贷方" : item.getDirection());
                    rows.add(row);
                }
                break;
            case "feishu":
                // 飞书表格模板
                FinClosingTemplateFeishu feishuConfig = finClosingTemplateFeishuService.selectFinClosingTemplateFeishuByTemplateid(template.getId());
                // 查询会计科目映射配置
                FinClosingTemplateItem queryFeishuItem = new FinClosingTemplateItem();
                queryFeishuItem.setClosingTemplateId(String.valueOf(template.getId()));
                List<FinClosingTemplateItem> feishuItemList = finClosingTemplateItemService.selectFinClosingTemplateItemList(queryFeishuItem);
                
                // 如果有会计科目映射配置，为每条配置创建一行
                if (feishuItemList != null && !feishuItemList.isEmpty()) {
                    for (FinClosingTemplateItem item : feishuItemList) {
                        Map<String, Object> feishuRow = new HashMap<>();
                        feishuRow.put("模板名称", template.getName());
                        // 通过feign调用获取表格名称
                        String tableName = getFeishuTableName(feishuConfig != null ? feishuConfig.getFeishuTableId() : "");
                        feishuRow.put("表格名称", tableName);
                        feishuRow.put("摘要字段", feishuConfig != null ? feishuConfig.getSummaryField() : "");
                        feishuRow.put("日期字段", feishuConfig != null ? feishuConfig.getVoucherDateField() : "");
                        feishuRow.put("科目字段", feishuConfig != null ? feishuConfig.getSubjectField() : "");
                        feishuRow.put("金额字段", feishuConfig != null ? feishuConfig.getAmountField() : "");
                        // 汇总方式：0-按日，1-按月，2-单笔生成凭证
                        Integer datetype = feishuConfig != null ? feishuConfig.getDatetype() : null;
                        feishuRow.put("汇总方式", datetype != null && datetype == 0 ? "按日" : datetype != null && datetype == 1 ? "按月" : datetype != null && datetype == 2 ? "单笔生成凭证" : datetype);
                        feishuRow.put("过滤条件", feishuConfig != null ? feishuConfig.getFilter() : "");
                        // 会计科目映射配置
                        String debitCode = getSubjectCodeById(item.getSummary());
                        String creditCode = getSubjectCodeById(item.getSubjectId());
                        feishuRow.put("借方科目编码", debitCode);
                        feishuRow.put("贷方科目编码", creditCode);
                        feishuRow.put("内容配对", item.getAmountField());
                        rows.add(feishuRow);
                    }
                } else {
                    // 没有会计科目映射配置，只导出基本信息
                    Map<String, Object> feishuRow = new HashMap<>();
                    feishuRow.put("模板名称", template.getName());
                    String tableName = getFeishuTableName(feishuConfig != null ? feishuConfig.getFeishuTableId() : "");
                    feishuRow.put("表格名称", tableName);
                    feishuRow.put("摘要字段", feishuConfig != null ? feishuConfig.getSummaryField() : "");
                    feishuRow.put("日期字段", feishuConfig != null ? feishuConfig.getVoucherDateField() : "");
                    feishuRow.put("科目字段", feishuConfig != null ? feishuConfig.getSubjectField() : "");
                    feishuRow.put("金额字段", feishuConfig != null ? feishuConfig.getAmountField() : "");
                    // 汇总方式：0-按日，1-按月，2-单笔生成凭证
                    Integer datetype = feishuConfig != null ? feishuConfig.getDatetype() : null;
                    feishuRow.put("汇总方式", datetype != null && datetype == 0 ? "按日" : datetype != null && datetype == 1 ? "按月" : datetype != null && datetype == 2 ? "单笔生成凭证" : datetype);
                    feishuRow.put("过滤条件", feishuConfig != null ? feishuConfig.getFilter() : "");
                    rows.add(feishuRow);
                }
                break;
            default:
                // 其他类型只导出模板基本信息
                Map<String, Object> row = new HashMap<>();
                row.put("模板名称", template.getName());
                rows.add(row);
                break;
        }
        
        // 如果没有子项数据，至少导出模板基本信息
        if (rows.isEmpty()) {
            Map<String, Object> row = new HashMap<>();
            row.put("模板名称", template.getName());
            rows.add(row);
        }
        
        return rows;
    }

    /**
     * 根据科目ID获取科目编码
     */
    private String getSubjectCodeById(String subjectId) {
        if (subjectId == null || subjectId.isEmpty()) {
            return "";
        }
        try {
            FinAccountingSubjects subject = finAccountingSubjectsMapper.selectFinAccountingSubjectsBySubjectId(subjectId);
            return subject != null ? subject.getSubjectCode() : "";
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * 根据表格ID获取表格名称
     */
    private String getFeishuTableName(String tableId) {
        if (tableId == null || tableId.isEmpty()) {
            return "";
        }
        try {
            com.wimoor.common.result.Result<?> result = remoteAdminService.getFeishuTableById(tableId);
            if (result != null && result.getCode() == 200 && result.getData() != null) {
                Map<String, Object> data = (Map<String, Object>) result.getData();
                return data.get("name") != null ? data.get("name").toString() : "";
            }
        } catch (Exception e) {
            System.out.println("获取飞书表格名称失败: " + e.getMessage());
        }
        return "";
    }

    /**
     * 根据表格名称查找表格ID
     */
    private Long findFeishuTableIdByName(String tableName) {
        if (tableName == null || tableName.isEmpty()) {
            return null;
        }
        try {
            // 通过feign调用获取所有表格列表
            com.wimoor.common.result.Result<?> result = financeRemoteAdminService.getFeishuTableList();
            if (result != null && result.getCode() == 200 && result.getData() != null) {
                java.util.List<?> list = (java.util.List<?>) result.getData();
                for (Object obj : list) {
                    Map<String, Object> data = (Map<String, Object>) obj;
                    if (tableName.equals(data.get("name"))) {
                        return Long.valueOf(data.get("id").toString());
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("根据名称查找飞书表格失败: " + e.getMessage());
        }
        return null;
    }

    /**
     * 下载导入模板
     */
    @GetMapping("/importTemplate")
    public void importTemplate(HttpServletResponse response, String groupid) throws Exception {
        Workbook workbook = new XSSFWorkbook();
        
        // 创建表头样式
        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);
        headerStyle.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        // 结转损益页签
        Sheet lossSheet = workbook.createSheet("结转损益");
        String[] lossHeaders = {"模板名称", "结转周期(0-按月,1-按年)", "结转方式(0-追加,1-重新)", 
            "凭证摘要", "方向处理(0-按方向,1-按正数)", "损益科目编码", "本年利润科目编码", 
            "以前年度损益调整科目编码", "调整结转科目编码"};
        Row lossHeaderRow = lossSheet.createRow(0);
        for (int i = 0; i < lossHeaders.length; i++) {
            Cell cell = lossHeaderRow.createCell(i);
            cell.setCellValue(lossHeaders[i]);
            cell.setCellStyle(headerStyle);
        }
        // 添加示例数据
        Row lossExample = lossSheet.createRow(1);
        lossExample.createCell(0).setCellValue("结转损益");
        lossExample.createCell(1).setCellValue("按月结转");
        lossExample.createCell(2).setCellValue("追加结转");
        lossExample.createCell(3).setCellValue("结转本期损益");
        lossExample.createCell(4).setCellValue("按科目方向反向结转");
        lossExample.createCell(5).setCellValue("6001");
        lossExample.createCell(6).setCellValue("4103");
        lossExample.createCell(7).setCellValue("6901");
        lossExample.createCell(8).setCellValue("4104");
        for (int i = 0; i < lossHeaders.length; i++) {
            lossSheet.autoSizeColumn(i);
        }

        // 期末调汇页签
        Sheet fctSheet = workbook.createSheet("期末调汇");
        String[] fctHeaders = {"模板名称", "币种", "汇率", "借方科目编码", "贷方科目编码"};
        Row fctHeaderRow = fctSheet.createRow(0);
        for (int i = 0; i < fctHeaders.length; i++) {
            Cell cell = fctHeaderRow.createCell(i);
            cell.setCellValue(fctHeaders[i]);
            cell.setCellStyle(headerStyle);
        }
        for (int i = 0; i < fctHeaders.length; i++) {
            fctSheet.autoSizeColumn(i);
        }

        // 亚马逊报表页签
        Sheet amzSheet = workbook.createSheet("亚马逊报表");
        String[] amzHeaders = {"模板名称","国家", "摘要", "科目编码", "金额字段", "方向(1-借方,2-贷方)"};
        Row amzHeaderRow = amzSheet.createRow(0);
        for (int i = 0; i < amzHeaders.length; i++) {
            Cell cell = amzHeaderRow.createCell(i);
            cell.setCellValue(amzHeaders[i]);
            cell.setCellStyle(headerStyle);
        }
        for (int i = 0; i < amzHeaders.length; i++) {
            amzSheet.autoSizeColumn(i);
        }

        // 其他自定义页签
        Sheet otherSheet = workbook.createSheet("其他自定义");
        String[] otherHeaders = {"模板名称", "摘要", "科目编码", "金额字段", "方向(1-借方,2-贷方)"};
        Row otherHeaderRow = otherSheet.createRow(0);
        for (int i = 0; i < otherHeaders.length; i++) {
            Cell cell = otherHeaderRow.createCell(i);
            cell.setCellValue(otherHeaders[i]);
            cell.setCellStyle(headerStyle);
        }
        for (int i = 0; i < otherHeaders.length; i++) {
            otherSheet.autoSizeColumn(i);
        }

        // 飞书表格页签
        Sheet feishuSheet = workbook.createSheet("飞书表格");
        String[] feishuHeaders = {"模板名称", "表格ID", "摘要字段", "日期字段", "科目字段", 
            "金额字段", "汇总方式", "借方科目编码", "贷方科目编码", "过滤条件"};
        Row feishuHeaderRow = feishuSheet.createRow(0);
        for (int i = 0; i < feishuHeaders.length; i++) {
            Cell cell = feishuHeaderRow.createCell(i);
            cell.setCellValue(feishuHeaders[i]);
            cell.setCellStyle(headerStyle);
        }
        for (int i = 0; i < feishuHeaders.length; i++) {
            feishuSheet.autoSizeColumn(i);
        }

        // 输出文件
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment;filename=closing_config_template.xlsx");
        workbook.write(response.getOutputStream());
        workbook.close();
    }

    /**
     * 导入配置
     */
    @PostMapping("/importConfig")
    @Transactional
    public Result importConfig(MultipartFile file, String groupid) throws Exception {
        if (file == null || file.isEmpty()) {
            return Result.error("请选择要导入的文件");
        }

        try (InputStream is = file.getInputStream(); Workbook workbook = new XSSFWorkbook(is)) {
            int totalCount = 0;
            List<String> errors = new ArrayList<>();

            // 处理结转损益页签
            Sheet lossSheet = workbook.getSheet("结转损益");
            if (lossSheet != null && lossSheet.getLastRowNum() > 0) {
                // 解析表头，获取列索引
                Row headerRow = lossSheet.getRow(0);
                if (headerRow != null) {
                    Map<String, Integer> columnMap = new HashMap<>();
                    for (int j = 0; j < headerRow.getLastCellNum(); j++) {
                        String header = getCellStringValue(headerRow.getCell(j));
                        if (header != null) {
                            // 去掉括号内的说明文字进行匹配
                            String key = header.contains("(") ? header.substring(0, header.indexOf("(")) : header;
                            columnMap.put(key, j);
                        }
                    }
                    
                    for (int i = 1; i <= lossSheet.getLastRowNum(); i++) {
                        Row row = lossSheet.getRow(i);
                        if (row == null) continue;
                        
                        try {
                            Integer templateNameIdx = columnMap.get("模板名称");
                            String templateName = templateNameIdx != null ? getCellStringValue(row.getCell(templateNameIdx)) : null;
                            if (templateName == null || templateName.isEmpty()) continue;

                            // 查找或创建模板
                            FinClosingTemplate template = findOrCreateTemplate(templateName, "loss", groupid,null);
                            
                            // 创建结转损益配置
                            FinClosingTemplateProfitLoss profitLoss = new FinClosingTemplateProfitLoss();
                            profitLoss.setTemplateId(template.getId());
                            profitLoss.setGroupid(groupid);
                            // 结转周期：按月结转->0，按年结转->1
                            String transferCycle = columnMap.containsKey("结转周期") ? getCellStringValue(row.getCell(columnMap.get("结转周期"))) : null;
                            profitLoss.setTransferCycle("按月结转".equals(transferCycle) ? "0" : "按年结转".equals(transferCycle) ? "1" : transferCycle);
                            // 结转方式：追加结转->0，重新结转->1
                            String transferMethod = columnMap.containsKey("结转方式") ? getCellStringValue(row.getCell(columnMap.get("结转方式"))) : null;
                            profitLoss.setTransferMethod("追加结转".equals(transferMethod) ? "0" : "重新结转".equals(transferMethod) ? "1" : transferMethod);
                            profitLoss.setSummary(columnMap.containsKey("凭证摘要") ? getCellStringValue(row.getCell(columnMap.get("凭证摘要"))) : null);
                            // 方向处理：按科目方向反向结转->0，按金额正数结转->1
                            String directionHandling = columnMap.containsKey("方向处理") ? getCellStringValue(row.getCell(columnMap.get("方向处理"))) : null;
                            profitLoss.setDirectionHandling("按科目方向反向结转".equals(directionHandling) ? 0 : "按金额正数结转".equals(directionHandling) ? 1 : null);
                            profitLoss.setProfitLossSubjectCode(columnMap.containsKey("损益科目编码") ? getCellStringValue(row.getCell(columnMap.get("损益科目编码"))) : null);
                            profitLoss.setCurrentYearProfitSubjectCode(columnMap.containsKey("本年利润科目编码") ? getCellStringValue(row.getCell(columnMap.get("本年利润科目编码"))) : null);
                            profitLoss.setPriorYearAdjustmentSubjectCode(columnMap.containsKey("以前年度损益调整科目编码") ? getCellStringValue(row.getCell(columnMap.get("以前年度损益调整科目编码"))) : null);
                            profitLoss.setPriorYearAdjustTransferSubjectCode(columnMap.containsKey("调整结转科目编码") ? getCellStringValue(row.getCell(columnMap.get("调整结转科目编码"))) : null);
                            profitLoss.setCreateBy(UserInfoContext.get().getUserName());
                            profitLoss.setCreatedTime(new Date());
                            profitLoss.setModifyBy(UserInfoContext.get().getUserName());
                            profitLoss.setUpdatedTime(new Date());
                            
                            // 检查是否已存在该模板的配置
                            FinClosingTemplateProfitLoss existQuery = new FinClosingTemplateProfitLoss();
                            existQuery.setTemplateId(template.getId());
                            List<FinClosingTemplateProfitLoss> existList = finClosingTemplateProfitLossService.selectFinClosingTemplateProfitLossList(existQuery);
                            if (existList != null && !existList.isEmpty()) {
                                // 更新现有记录
                                profitLoss.setId(existList.get(0).getId());
                                finClosingTemplateProfitLossService.updateFinClosingTemplateProfitLoss(profitLoss);
                            } else {
                                // 插入新记录
                                finClosingTemplateProfitLossService.insertFinClosingTemplateProfitLoss(profitLoss);
                            }
                            totalCount++;
                        } catch (Exception e) {
                            errors.add("结转损益第" + (i + 1) + "行: " + e.getMessage());
                        }
                    }
                }
            }

            // 处理亚马逊报表页签
            Sheet amazonSheet = workbook.getSheet("亚马逊报表");
            System.out.println("亚马逊报表页签: " + (amazonSheet != null ? "存在" : "不存在"));
            if (amazonSheet != null) {
                System.out.println("亚马逊报表行数: " + amazonSheet.getLastRowNum());
            }
            if (amazonSheet != null && amazonSheet.getLastRowNum() > 0) {
                // 解析表头，获取列索引
                Row amazonHeaderRow = amazonSheet.getRow(0);
                if (amazonHeaderRow != null) {
                    Map<String, Integer> amazonColumnMap = new HashMap<>();
                    for (int j = 0; j < amazonHeaderRow.getLastCellNum(); j++) {
                        String header = getCellStringValue(amazonHeaderRow.getCell(j));
                        if (header != null) {
                            String key = header.contains("(") ? header.substring(0, header.indexOf("(")) : header;
                            amazonColumnMap.put(key, j);
                            System.out.println("亚马逊报表列: " + key + " -> " + j);
                        }
                    }
                    
                    Set<String> processedAmazonTemplates = new HashSet<>();

                    for (int i = 1; i <= amazonSheet.getLastRowNum(); i++) {
                        Row row = amazonSheet.getRow(i);
                        if (row == null) continue;
                        
                        try {
                            Integer templateNameIdx = amazonColumnMap.get("模板名称");
                            String templateName = templateNameIdx != null ? getCellStringValue(row.getCell(templateNameIdx)) : null;
                            System.out.println("亚马逊报表第" + i + "行模板名称: " + templateName);
                            if (templateName == null || templateName.isEmpty()) continue;
                            String country = amazonColumnMap.containsKey("国家") ? getCellStringValue(row.getCell(amazonColumnMap.get("国家"))) : null;
                            FinClosingTemplate template = findOrCreateTemplate(templateName, "amzpayment", groupid, country);
                            System.out.println("找到模板: " + template.getName() + ", ID: " + template.getId());
                            // 添加国家字段解析

                            this.finClosingTemplateService.updateFinClosingTemplate(template);
                            // 首次处理该模板时，删除旧记录
                            String templateIdStr = String.valueOf(template.getId());
                            if (!processedAmazonTemplates.contains(templateIdStr)) {
                                FinClosingTemplateAmazon deleteQuery = new FinClosingTemplateAmazon();
                                deleteQuery.setClosingTemplateId(template.getId());
                                List<FinClosingTemplateAmazon> oldList = finClosingTemplateAmazonService.selectFinClosingTemplateAmazonList(deleteQuery);
                                if (oldList != null && !oldList.isEmpty()) {
                                    List<String> ids = oldList.stream().map(FinClosingTemplateAmazon::getId).collect(Collectors.toList());
                                    finClosingTemplateAmazonService.deleteFinClosingTemplateAmazonByIds(ids);
                                    System.out.println("删除亚马逊模板 " + template.getName() + " 的旧记录 " + ids.size() + " 条");
                                }
                                processedAmazonTemplates.add(templateIdStr);
                            }
                            
                            // 创建亚马逊报表配置
                            FinClosingTemplateAmazon amazon = new FinClosingTemplateAmazon();
                            amazon.setClosingTemplateId(template.getId());
                            amazon.setSummary(amazonColumnMap.containsKey("摘要") ? getCellStringValue(row.getCell(amazonColumnMap.get("摘要"))) : null);
                            // 根据科目编码查询科目ID
                            String subjectCode = amazonColumnMap.containsKey("科目编码") ? getCellStringValue(row.getCell(amazonColumnMap.get("科目编码"))) : null;
                            System.out.println("科目编码: " + subjectCode);
                            if (subjectCode != null && !subjectCode.isEmpty()) {
                                FinAccountingSubjects subjectQuery = new FinAccountingSubjects();
                                subjectQuery.setGroupid(groupid);
                                subjectQuery.setSubjectCode(subjectCode);
                                List<FinAccountingSubjects> subjectList = finAccountingSubjectsMapper.selectFinAccountingSubjectsList(subjectQuery);
                                if (subjectList != null && !subjectList.isEmpty()) {
                                    amazon.setSubjectId(String.valueOf(subjectList.get(0).getSubjectId()));
                                    System.out.println("科目ID: " + subjectList.get(0).getSubjectId());
                                }
                            }
                            amazon.setAmountField(amazonColumnMap.containsKey("金额字段") ? getCellStringValue(row.getCell(amazonColumnMap.get("金额字段"))) : null);

                            // 方向：借方->1，贷方->2
                            String direction = amazonColumnMap.containsKey("方向") ? getCellStringValue(row.getCell(amazonColumnMap.get("方向"))) : null;
                            amazon.setDirection("借方".equals(direction) ? 1 : "贷方".equals(direction) ? 2 : null);
                            amazon.setCreateBy(UserInfoContext.get().getUserName());
                            amazon.setCreatedTime(new Date());
                            amazon.setModifyBy(UserInfoContext.get().getUserName());
                            amazon.setUpdatedTime(new Date());
                            
                            finClosingTemplateAmazonService.insertFinClosingTemplateAmazon(amazon);
                            totalCount++;
                            System.out.println("成功导入亚马逊报表第" + i + "行");
                        } catch (Exception e) {
                            System.out.println("导入亚马逊报表第" + i + "行失败: " + e.getMessage());
                            e.printStackTrace();
                            errors.add("亚马逊报表第" + (i + 1) + "行: " + e.getMessage());
                        }
                    }
                }
            }

            // 处理其他自定义页签
            Sheet otherSheet = workbook.getSheet("其他自定义");
            if (otherSheet != null && otherSheet.getLastRowNum() > 0) {
                // 解析表头，获取列索引
                Row otherHeaderRow = otherSheet.getRow(0);
                if (otherHeaderRow != null) {
                    Map<String, Integer> otherColumnMap = new HashMap<>();
                    for (int j = 0; j < otherHeaderRow.getLastCellNum(); j++) {
                        String header = getCellStringValue(otherHeaderRow.getCell(j));
                        if (header != null) {
                            String key = header.contains("(") ? header.substring(0, header.indexOf("(")) : header;
                            otherColumnMap.put(key, j);
                        }
                    }
                    
                    Set<String> processedOtherTemplates = new HashSet<>();
                    for (int i = 1; i <= otherSheet.getLastRowNum(); i++) {
                        Row row = otherSheet.getRow(i);
                        if (row == null) continue;
                        
                        try {
                            Integer templateNameIdx = otherColumnMap.get("模板名称");
                            String templateName = templateNameIdx != null ? getCellStringValue(row.getCell(templateNameIdx)) : null;
                            if (templateName == null || templateName.isEmpty()) continue;

                            FinClosingTemplate template = findOrCreateTemplate(templateName, "other", groupid, null);
                            
                            // 首次处理该模板时，删除旧记录
                            String templateIdStr = String.valueOf(template.getId());
                            if (!processedOtherTemplates.contains(templateIdStr)) {
                                FinClosingTemplateItem deleteQuery = new FinClosingTemplateItem();
                                deleteQuery.setClosingTemplateId(template.getId());
                                List<FinClosingTemplateItem> oldList = finClosingTemplateItemService.selectFinClosingTemplateItemList(deleteQuery);
                                if (oldList != null && !oldList.isEmpty()) {
                                    List<String> ids = oldList.stream().map(FinClosingTemplateItem::getId).collect(Collectors.toList());
                                    finClosingTemplateItemService.deleteFinClosingTemplateItemByIds(ids);
                                    System.out.println("删除其他自定义模板 " + template.getName() + " 的旧记录 " + ids.size() + " 条");
                                }
                                processedOtherTemplates.add(templateIdStr);
                            }
                            
                            // 创建其他自定义配置
                            FinClosingTemplateItem item = new FinClosingTemplateItem();
                            item.setClosingTemplateId(template.getId());
                            item.setSummary(otherColumnMap.containsKey("摘要") ? getCellStringValue(row.getCell(otherColumnMap.get("摘要"))) : null);
                            // 根据科目编码查询科目ID
                            String subjectCode = otherColumnMap.containsKey("科目编码") ? getCellStringValue(row.getCell(otherColumnMap.get("科目编码"))) : null;
                            if (subjectCode != null && !subjectCode.isEmpty()) {
                                FinAccountingSubjects subjectQuery = new FinAccountingSubjects();
                                subjectQuery.setGroupid(groupid);
                                subjectQuery.setSubjectCode(subjectCode);
                                List<FinAccountingSubjects> subjectList = finAccountingSubjectsMapper.selectFinAccountingSubjectsList(subjectQuery);
                                if (subjectList != null && !subjectList.isEmpty()) {
                                    item.setSubjectId(String.valueOf(subjectList.get(0).getSubjectId()));
                                }
                            }
                            item.setAmountField(otherColumnMap.containsKey("金额字段") ? getCellStringValue(row.getCell(otherColumnMap.get("金额字段"))) : null);
                            // 方向：借方->1，贷方->2
                            String direction = otherColumnMap.containsKey("方向") ? getCellStringValue(row.getCell(otherColumnMap.get("方向"))) : null;
                            item.setDirection("借方".equals(direction) ? 1 : "贷方".equals(direction) ? 2 : null);
                            item.setCreateBy(UserInfoContext.get().getUserName());
                            item.setCreatedTime(new Date());
                            item.setModifyBy(UserInfoContext.get().getUserName());
                            item.setUpdatedTime(new Date());
                            
                            finClosingTemplateItemService.insertFinClosingTemplateItem(item);
                            totalCount++;
                        } catch (Exception e) {
                            errors.add("其他自定义第" + (i + 1) + "行: " + e.getMessage());
                        }
                    }
                }
            }

            // 处理飞书表格页签
            Sheet feishuSheet = workbook.getSheet("飞书表格");
            System.out.println("飞书表格页签: " + (feishuSheet != null ? "存在" : "不存在"));
            if (feishuSheet != null) {
                System.out.println("飞书表格行数: " + feishuSheet.getLastRowNum());
            }
            if (feishuSheet != null && feishuSheet.getLastRowNum() > 0) {
                // 解析表头，获取列索引
                Row feishuHeaderRow = feishuSheet.getRow(0);
                if (feishuHeaderRow != null) {
                    Map<String, Integer> feishuColumnMap = new HashMap<>();
                    for (int j = 0; j < feishuHeaderRow.getLastCellNum(); j++) {
                        String header = getCellStringValue(feishuHeaderRow.getCell(j));
                        if (header != null) {
                            String key = header.contains("(") ? header.substring(0, header.indexOf("(")) : header;
                            feishuColumnMap.put(key, j);
                            System.out.println("飞书表格列: " + key + " -> " + j);
                        }
                    }
                    
                    Set<String> processedFeishuTemplates = new HashSet<>();
                    for (int i = 1; i <= feishuSheet.getLastRowNum(); i++) {
                        Row row = feishuSheet.getRow(i);
                        if (row == null) continue;
                        
                        try {
                            Integer templateNameIdx = feishuColumnMap.get("模板名称");
                            String templateName = templateNameIdx != null ? getCellStringValue(row.getCell(templateNameIdx)) : null;
                            System.out.println("飞书表格第" + i + "行模板名称: " + templateName);
                            if (templateName == null || templateName.isEmpty()) continue;

                            FinClosingTemplate template = findOrCreateTemplate(templateName, "feishu", groupid, null);
                            System.out.println("找到模板: " + template.getName() + ", ID: " + template.getId());
                            
                            // 首次处理该模板时，删除旧的映射配置
                            String templateIdStr = String.valueOf(template.getId());
                            if (!processedFeishuTemplates.contains(templateIdStr)) {
                                FinClosingTemplateItem deleteQuery = new FinClosingTemplateItem();
                                deleteQuery.setClosingTemplateId(template.getId());
                                List<FinClosingTemplateItem> oldItems = finClosingTemplateItemService.selectFinClosingTemplateItemList(deleteQuery);
                                if (oldItems != null && !oldItems.isEmpty()) {
                                    List<String> ids = oldItems.stream().map(FinClosingTemplateItem::getId).collect(Collectors.toList());
                                    finClosingTemplateItemService.deleteFinClosingTemplateItemByIds(ids);
                                    System.out.println("删除飞书模板 " + template.getName() + " 的旧映射配置 " + ids.size() + " 条");
                                }
                                processedFeishuTemplates.add(templateIdStr);
                            }
                            
                            // 创建或更新飞书表格配置
                            FinClosingTemplateFeishu feishuConfig = finClosingTemplateFeishuService.selectFinClosingTemplateFeishuByTemplateid(template.getId());
                            if (feishuConfig == null) {
                                feishuConfig = new FinClosingTemplateFeishu();
                                feishuConfig.setTemplateid(template.getId());
                                // 表格名称需要通过feign调用查找ID
                                String tableName = feishuColumnMap.containsKey("表格名称") ? getCellStringValue(row.getCell(feishuColumnMap.get("表格名称"))) : null;
                                if (tableName != null && !tableName.isEmpty()) {
                                    Long tableId = findFeishuTableIdByName(tableName);
                                    if (tableId != null) {
                                        feishuConfig.setFeishuTableId(String.valueOf(tableId));
                                    } else {
                                        errors.add("飞书表格第" + (i + 1) + "行: 未找到表格 '" + tableName + "'");
                                        continue;
                                    }
                                }
                                feishuConfig.setSummaryField(feishuColumnMap.containsKey("摘要字段") ? getCellStringValue(row.getCell(feishuColumnMap.get("摘要字段"))) : null);
                                feishuConfig.setVoucherDateField(feishuColumnMap.containsKey("日期字段") ? getCellStringValue(row.getCell(feishuColumnMap.get("日期字段"))) : null);
                                feishuConfig.setSubjectField(feishuColumnMap.containsKey("科目字段") ? getCellStringValue(row.getCell(feishuColumnMap.get("科目字段"))) : null);
                                feishuConfig.setAmountField(feishuColumnMap.containsKey("金额字段") ? getCellStringValue(row.getCell(feishuColumnMap.get("金额字段"))) : null);
                                // 汇总方式：按日->0，按月->1，单笔生成凭证->2
                                String datetype = feishuColumnMap.containsKey("汇总方式") ? getCellStringValue(row.getCell(feishuColumnMap.get("汇总方式"))) : null;
                                feishuConfig.setDatetype("按日".equals(datetype) ? 0 : "按月".equals(datetype) ? 1 : "单笔生成凭证".equals(datetype) ? 2 : null);
                                feishuConfig.setFilter(feishuColumnMap.containsKey("过滤条件") ? getCellStringValue(row.getCell(feishuColumnMap.get("过滤条件"))) : null);
                                finClosingTemplateFeishuService.insertFinClosingTemplateFeishu(feishuConfig);
                            } else {
                                // 更新现有配置
                                String tableName = feishuColumnMap.containsKey("表格名称") ? getCellStringValue(row.getCell(feishuColumnMap.get("表格名称"))) : null;
                                if (tableName != null && !tableName.isEmpty()) {
                                    Long tableId = findFeishuTableIdByName(tableName);
                                    if (tableId != null) {
                                        feishuConfig.setFeishuTableId(String.valueOf(tableId));
                                    }
                                }
                                feishuConfig.setSummaryField(feishuColumnMap.containsKey("摘要字段") ? getCellStringValue(row.getCell(feishuColumnMap.get("摘要字段"))) : null);
                                feishuConfig.setVoucherDateField(feishuColumnMap.containsKey("日期字段") ? getCellStringValue(row.getCell(feishuColumnMap.get("日期字段"))) : null);
                                feishuConfig.setSubjectField(feishuColumnMap.containsKey("科目字段") ? getCellStringValue(row.getCell(feishuColumnMap.get("科目字段"))) : null);
                                feishuConfig.setAmountField(feishuColumnMap.containsKey("金额字段") ? getCellStringValue(row.getCell(feishuColumnMap.get("金额字段"))) : null);
                                // 汇总方式：按日->0，按月->1，单笔生成凭证->2
                                String datetype = feishuColumnMap.containsKey("汇总方式") ? getCellStringValue(row.getCell(feishuColumnMap.get("汇总方式"))) : null;
                                feishuConfig.setDatetype("按日".equals(datetype) ? 0 : "按月".equals(datetype) ? 1 : "单笔生成凭证".equals(datetype) ? 2 : null);
                                feishuConfig.setFilter(feishuColumnMap.containsKey("过滤条件") ? getCellStringValue(row.getCell(feishuColumnMap.get("过滤条件"))) : null);
                                finClosingTemplateFeishuService.updateFinClosingTemplateFeishu(feishuConfig);
                            }
                            
                            // 处理会计科目映射配置
                            String debitCode = feishuColumnMap.containsKey("借方科目编码") ? getCellStringValue(row.getCell(feishuColumnMap.get("借方科目编码"))) : null;
                            String creditCode = feishuColumnMap.containsKey("贷方科目编码") ? getCellStringValue(row.getCell(feishuColumnMap.get("贷方科目编码"))) : null;
                            String contentMatch = feishuColumnMap.containsKey("内容配对") ? getCellStringValue(row.getCell(feishuColumnMap.get("内容配对"))) : null;
                            System.out.println("飞书映射配置 - 借方: " + debitCode + ", 贷方: " + creditCode + ", 内容配对: " + contentMatch);
                            
                            if ((debitCode != null && !debitCode.isEmpty()) || (creditCode != null && !creditCode.isEmpty()) || (contentMatch != null && !contentMatch.isEmpty())) {
                                // 根据科目编码查询科目ID
                                String debitSubjectId = null;
                                String creditSubjectId = null;
                                if (debitCode != null && !debitCode.isEmpty()) {
                                    FinAccountingSubjects subjectQuery = new FinAccountingSubjects();
                                    subjectQuery.setGroupid(groupid);
                                    subjectQuery.setSubjectCode(debitCode);
                                    List<FinAccountingSubjects> subjectList = finAccountingSubjectsMapper.selectFinAccountingSubjectsList(subjectQuery);
                                    if (subjectList != null && !subjectList.isEmpty()) {
                                        debitSubjectId = String.valueOf(subjectList.get(0).getSubjectId());
                                    }
                                }
                                if (creditCode != null && !creditCode.isEmpty()) {
                                    FinAccountingSubjects subjectQuery = new FinAccountingSubjects();
                                    subjectQuery.setGroupid(groupid);
                                    subjectQuery.setSubjectCode(creditCode);
                                    List<FinAccountingSubjects> subjectList = finAccountingSubjectsMapper.selectFinAccountingSubjectsList(subjectQuery);
                                    if (subjectList != null && !subjectList.isEmpty()) {
                                        creditSubjectId = String.valueOf(subjectList.get(0).getSubjectId());
                                    }
                                }
                                
                                // 创建会计科目映射（旧记录已删除）
                                FinClosingTemplateItem item = new FinClosingTemplateItem();
                                item.setClosingTemplateId(template.getId());
                                
                                if (debitSubjectId != null) {
                                    item.setSummary(debitSubjectId);
                                }
                                if (creditSubjectId != null) {
                                    item.setSubjectId(creditSubjectId);
                                }
                                item.setAmountField(contentMatch);
                                item.setCreateBy(UserInfoContext.get().getUserName());
                                item.setCreatedTime(new Date());
                                item.setModifyBy(UserInfoContext.get().getUserName());
                                item.setUpdatedTime(new Date());
                                
                                finClosingTemplateItemService.insertFinClosingTemplateItem(item);
                            }
                            
                            totalCount++;
                            System.out.println("成功导入飞书表格第" + i + "行");
                        } catch (Exception e) {
                            System.out.println("导入飞书表格第" + i + "行失败: " + e.getMessage());
                            e.printStackTrace();
                            errors.add("飞书表格第" + (i + 1) + "行: " + e.getMessage());
                        }
                    }
                }
            }

            if (!errors.isEmpty()) {
                return Result.error("导入完成，但有部分错误:\n" + String.join("\n", errors));
            }
            
            return Result.success("导入成功，共导入 " + totalCount + " 条数据");
        }
    }

    /**
     * 查找或创建模板
     */
    private FinClosingTemplate findOrCreateTemplate(String name, String ftype, String groupid, String country) {
        // 先查找是否存在同名模板（根据groupid+名称确定唯一）
        FinClosingTemplate query = new FinClosingTemplate();
        query.setName(name);
        query.setGroupid(groupid);
        List<FinClosingTemplate> list = finClosingTemplateService.selectFinClosingTemplateList(query);
        if (list != null && !list.isEmpty()) {
            // 存在则更新
            FinClosingTemplate template = list.get(0);
            template.setFtype(ftype);
            template.setCountry(country);
            template.setModifyBy(UserInfoContext.get().getUserName());
            template.setUpdatedTime(new Date());
            finClosingTemplateService.updateFinClosingTemplate(template);
            return template;
        }
        
        // 不存在则创建
        FinClosingTemplate template = new FinClosingTemplate();
        template.setId(UUID.randomUUID().toString().replace("-", ""));
        template.setName(name);
        template.setFtype(ftype);
        template.setGroupid(groupid);
        template.setCountry(country);
        template.setDisabled(0);
        template.setIssystem(0);
        template.setCreateBy(UserInfoContext.get().getUserName());
        template.setCreatedTime(new Date());
        template.setModifyBy(UserInfoContext.get().getUserName());
        template.setUpdatedTime(new Date());
        finClosingTemplateService.insertFinClosingTemplate(template);
        return template;
    }

    /**
     * 获取单元格字符串值
     */
    private String getCellStringValue(Cell cell) {
        if (cell == null) return null;
        try {
            DataFormatter formatter = new DataFormatter();
            String val = formatter.formatCellValue(cell);
            return val != null && !val.trim().isEmpty() ? val.trim() : null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 获取单元格整数值
     */
    private Integer getCellIntValue(Cell cell) {
        if (cell == null) return null;
        try {
            DataFormatter formatter = new DataFormatter();
            String val = formatter.formatCellValue(cell);
            if (val != null && !val.trim().isEmpty()) {
                return Integer.parseInt(val.trim());
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }
}
