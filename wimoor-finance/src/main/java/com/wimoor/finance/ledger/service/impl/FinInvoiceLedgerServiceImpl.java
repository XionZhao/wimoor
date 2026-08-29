package com.wimoor.finance.ledger.service.impl;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

import com.wimoor.common.core.utils.poi.ExcelUtil;
import com.wimoor.common.result.Result;
import com.wimoor.common.user.UserInfo;
import com.wimoor.common.user.UserInfoContext;
import com.wimoor.finance.api.RemoteERPService;
import com.wimoor.finance.ledger.domain.FinInvoice;
import com.wimoor.finance.ledger.domain.FinInvoiceDetail;
import com.wimoor.finance.ledger.domain.FinInvoiceExtension;
import com.wimoor.finance.ledger.mapper.FinInvoiceDetailMapper;
import com.wimoor.finance.ledger.mapper.FinInvoiceExtensionMapper;
import com.wimoor.finance.ledger.mapper.FinInvoiceLedgerMapper;
import com.wimoor.finance.ledger.service.IFinInvoiceLedgerService;
import com.wimoor.finance.voucher.domain.FinVouchers;
import com.wimoor.finance.voucher.service.IFinVouchersService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * 发票台账Service业务层处理
 *
 * @author wimoor
 * @date 2025-11-04
 */
@Service
public class FinInvoiceLedgerServiceImpl implements IFinInvoiceLedgerService
{
    private static final Logger log = LoggerFactory.getLogger(FinInvoiceLedgerServiceImpl.class);

    @Autowired
    private FinInvoiceLedgerMapper finInvoiceLedgerMapper;

    @Autowired
    private FinInvoiceDetailMapper finInvoiceDetailMapper;

    @Autowired
    private FinInvoiceExtensionMapper finInvoiceExtensionMapper;

    @Autowired
    private RemoteERPService remoteERPService;

    @Autowired
    private IFinVouchersService finVouchersService;

    /**
     * 查询发票详情
     *
     * @param id 发票主键
     * @return 发票
     */
    @Override
    public FinInvoice selectFinInvoiceById(Long id)
    {
        return finInvoiceLedgerMapper.selectFinInvoiceById(id);
    }

    /**
     * 查询发票列表
     *
     * @param finInvoice 发票查询条件
     * @return 发票集合
     */
    @Override
    public List<FinInvoice> selectFinInvoiceList(FinInvoice finInvoice)
    {
        return finInvoiceLedgerMapper.selectFinInvoiceList(finInvoice);
    }

    /**
     * 统计发票信息
     *
     * @param finInvoice 查询条件
     * @return 统计结果
     */
    @Override
    public Map<String, Object> selectInvoiceStatistics(FinInvoice finInvoice)
    {
        return finInvoiceLedgerMapper.selectInvoiceStatistics(finInvoice);
    }

    /**
     * 查询发票关联的采购订单和付款记录
     *
     * @param id 发票主键
     * @return 关联信息
     */
    @Override
    public Map<String, Object> selectInvoiceRelations(Long id)
    {
        return finInvoiceLedgerMapper.selectInvoiceRelations(id);
    }

    /**
     * 查询发票商品明细行
     *
     * @param invoiceId 发票ID
     * @return 明细行列表
     */
    @Override
    public List<FinInvoiceDetail> selectInvoiceDetails(Long invoiceId)
    {
        return finInvoiceDetailMapper.selectByInvoiceId(invoiceId);
    }

    /**
     * 查询发票扩展信息
     *
     * @param invoiceId 发票ID
     * @return 扩展信息列表
     */
    @Override
    public List<FinInvoiceExtension> selectInvoiceExtensions(Long invoiceId)
    {
        return finInvoiceExtensionMapper.selectByInvoiceId(invoiceId);
    }

    /**
     * 从税局API同步发票（暂用mock实现）
     *
     * @param params 同步参数
     * @return 同步数量
     */
    @Override
    @Transactional
    public int syncInvoices(Map<String, Object> params)
    {
        UserInfo user = UserInfoContext.get();
        String groupid = user.getCompanyid();

        // TODO: 对接税局API获取发票数据，当前使用mock数据
        List<FinInvoice> mockList = buildMockInvoiceList(groupid, user.getUserName());

        if (mockList.isEmpty()) {
            return 0;
        }

        return finInvoiceLedgerMapper.batchInsertFinInvoice(mockList);
    }

    /**
     * 手动导入发票
     *
     * @param file 导入文件
     * @param groupid 租户ID
     * @return 导入数量
     */
    @Override
    @Transactional
    public int importInvoices(MultipartFile file, String groupid)
    {
        UserInfo user = UserInfoContext.get();
        ExcelUtil<FinInvoice> util = new ExcelUtil<>(FinInvoice.class);
        List<FinInvoice> invoiceList;
        try {
            invoiceList = util.importExcel(file.getInputStream());
        } catch (Exception e) {
            throw new RuntimeException("导入文件解析失败：" + e.getMessage());
        }

        if (invoiceList == null || invoiceList.isEmpty()) {
            log.warn("Excel导入发票：解析后数据为空");
            return 0;
        }

        log.info("Excel导入发票：解析成功，共{}条数据", invoiceList.size());
        // 打印第一条数据的发票类型，用于调试
        FinInvoice firstInvoice = invoiceList.get(0);
        log.info("Excel导入发票：第一条数据的invoiceType={}", firstInvoice.getInvoiceType());

        Date now = new Date();
        for (FinInvoice invoice : invoiceList) {
            invoice.setGroupid(groupid);
            // 转换发票类型为数据库编码格式
            if (invoice.getInvoiceType() != null && !invoice.getInvoiceType().isEmpty()) {
                invoice.setInvoiceType(mapInvoiceType(invoice.getInvoiceType()));
            }
            if (invoice.getPostingStatus() == null) {
                invoice.setPostingStatus(0);
            }
            if (invoice.getCurrency() == null || invoice.getCurrency().isEmpty()) {
                invoice.setCurrency("CNY");
            }
            if (invoice.getExchangeRate() == null) {
                invoice.setExchangeRate(BigDecimal.ONE);
            }
            invoice.setCreatedBy(user.getUserName());
            invoice.setUpdatedBy(user.getUserName());
            invoice.setCreatedTime(now);
            invoice.setUpdatedTime(now);
        }

        int count = finInvoiceLedgerMapper.batchInsertFinInvoice(invoiceList);
        // 导入后自动匹配supplier_id和carrier_id
        if (count > 0) {
            matchAndSetSupplierId(groupid);
            matchAndSetCarrierId(groupid);
        }
        return count;
    }

    /**
     * JSON批量导入发票（按页签分组：主表→fin_invoice，明细→fin_invoice_detail，扩展→fin_invoice_extension）
     */
    @Override
    @Transactional
    public String importInvoicesFromJson(Map<String, List<Map<String, Object>>> sheets, String groupid, String userName)
    {
        if (sheets == null || sheets.isEmpty()) {
            log.warn("导入发票：数据为空");
            return "发票数据为空";
        }

        Date now = new Date();
        List<FinInvoice> allInvoices = new ArrayList<>();
        // 按页签名保留原始行数据，用于后续插入明细/扩展
        Map<String, List<Map<String, Object>>> sheetRowsMap = new HashMap<>();

        // 收集所有页签的发票主表数据
        for (Map.Entry<String, List<Map<String, Object>>> entry : sheets.entrySet()) {
            String sheetName = entry.getKey();
            List<Map<String, Object>> rows = entry.getValue();
            if (rows == null || rows.isEmpty()) continue;

            log.info("处理页签[{}]，共{}行", sheetName, rows.size());
            List<Map<String, Object>> validRows = new ArrayList<>();

            for (Map<String, Object> item : rows) {
                FinInvoice invoice = buildInvoiceFromRow(item, groupid, userName, now, sheetName);
                if (invoice.getDigitalInvoiceNo() == null && invoice.getInvoiceNo() == null) {
                    log.warn("页签[{}]中旡法确定发票唯一标识的行，跳过：{}", sheetName, item);
                    continue;
                }
                allInvoices.add(invoice);
                validRows.add(item);
            }
            if (!validRows.isEmpty()) {
                sheetRowsMap.put(sheetName, validRows);
            }
        }

        if (allInvoices.isEmpty()) {
            log.warn("导入发票：解析后数据为空");
            return "解析后发票数据为空";
        }

        // 1. 批量插入/更新主表
        log.info("准备插入主表，共{}条发票", allInvoices.size());
        int mainCount = finInvoiceLedgerMapper.batchInsertFinInvoice(allInvoices);
        log.info("主表影响{}行", mainCount);

        // 2. 查询所有涉及的发票号码→ID映射（同时支持数电发票号码和普通发票号码）
        Set<String> allInvoiceNos = new LinkedHashSet<>();
        for (FinInvoice inv : allInvoices) {
            if (inv.getDigitalInvoiceNo() != null) {
                allInvoiceNos.add(inv.getDigitalInvoiceNo());
            }
            if (inv.getInvoiceNo() != null) {
                allInvoiceNos.add(inv.getInvoiceNo());
            }
        }
        // 发票号码→ID映射（包含数电发票号码和普通发票号码）
        Map<String, Long> invoiceNoToIdMap = new HashMap<>();
        if (!allInvoiceNos.isEmpty()) {
            List<String> noList = new ArrayList<>(allInvoiceNos);
            // 分批查询，每批500
            for (int i = 0; i < noList.size(); i += 500) {
                List<String> batch = noList.subList(i, Math.min(i + 500, noList.size()));
                // 同时查询数电发票号码和普通发票号码
                List<FinInvoice> found = finInvoiceLedgerMapper.selectByDigitalInvoiceNos(batch);
                for (FinInvoice fi : found) {
                    if (fi.getDigitalInvoiceNo() != null) {
                        invoiceNoToIdMap.put(fi.getDigitalInvoiceNo(), fi.getId());
                    }
                    if (fi.getInvoiceNo() != null) {
                        invoiceNoToIdMap.put(fi.getInvoiceNo(), fi.getId());
                    }
                }
            }
        }
        log.info("发票号码→ID映射：{}条", invoiceNoToIdMap.size());

        // 3. 处理明细行（"信息汇总表"页签包含货物明细信息）
        int detailCount = 0;
        List<Map<String, Object>> summaryRows = sheetRowsMap.get("信息汇总表");
        if (summaryRows != null && !summaryRows.isEmpty()) {
            List<FinInvoiceDetail> details = new ArrayList<>();
            Set<Long> invoiceIdsToDelete = new LinkedHashSet<>();
            // 按发票号码分组，为同一发票的多行明细分配行号
            Map<String, Integer> lineNoMap = new HashMap<>();
            for (Map<String, Object> row : summaryRows) {
                // 尝试多种发票号码列名（信息汇总表可能使用不同的列名）
                String digitalNo = getStringValue(row, "数电发票号码", "全电发票号码", "数电票号码",
                        "发票号码", "发票号", "号码", "invoiceNo", "InvoiceNo", "invoice_no",
                        "发票代码号码", "电子客票号", "票号");
                if (digitalNo == null || digitalNo.isEmpty()) {
                    log.warn("信息汇总表中无法识别发票号码的行，跳过：{}", row);
                    continue;
                }
                Long invoiceId = invoiceNoToIdMap.get(digitalNo);
                if (invoiceId == null) {
                    log.warn("信息汇总表中发票号码[{}]未找到对应的主表记录，跳过", digitalNo);
                    continue;
                }

                // 只有当行包含明细字段时才插入明细表
                String goodsName = getStringValue(row, "货物或应税劳务名称", "货物名称", "商品名称", "项目名称");
                BigDecimal amount = getBigDecimalValue(row, "金额", "amountWithoutTax");
                if (goodsName == null && amount == null) {
                    log.warn("信息汇总表中发票号码[{}]的行缺少货物名称或金额，跳过", digitalNo);
                    continue;
                }

                invoiceIdsToDelete.add(invoiceId);
                int lineNo = lineNoMap.merge(digitalNo, 1, Integer::sum);
                FinInvoiceDetail detail = new FinInvoiceDetail();
                detail.setInvoiceId(invoiceId);
                detail.setLineNo(lineNo);
                detail.setGoodsName(goodsName);
                detail.setSpecModel(getStringValue(row, "规格型号", "规格"));
                detail.setUnit(getStringValue(row, "单位"));
                detail.setQuantity(getBigDecimalValue(row, "数量"));
                detail.setUnitPrice(getBigDecimalValue(row, "单价", "不含税单价"));
                detail.setAmountWithoutTax(amount);
                detail.setTaxRate(parseTaxRate(getStringValue(row, "税率")));
                detail.setTaxAmount(getBigDecimalValue(row, "税额", "taxAmount"));
                detail.setAmountWithTax(getBigDecimalValue(row, "价税合计", "含税金额"));
                detail.setTaxCategoryCode(getStringValue(row, "税收分类编码", "税收分类"));
                details.add(detail);
            }
            if (!details.isEmpty()) {
                // 先删除这些发票的旧明细记录，避免重复导入
                if (!invoiceIdsToDelete.isEmpty()) {
                    List<Long> idList = new ArrayList<>(invoiceIdsToDelete);
                    for (int i = 0; i < idList.size(); i += 500) {
                        List<Long> batch = idList.subList(i, Math.min(i + 500, idList.size()));
                        for (Long id : batch) {
                            finInvoiceDetailMapper.deleteByInvoiceId(id);
                        }
                    }
                    log.info("清理旧明细记录，涉及{}张发票", idList.size());
                }
                // 分批插入，每批200条
                for (int i = 0; i < details.size(); i += 200) {
                    detailCount += finInvoiceDetailMapper.batchInsert(details.subList(i, Math.min(i + 200, details.size())));
                }
                log.info("插入明细行{}条", detailCount);
            }
        }

        // 4. 处理扩展信息（货物运输服务、铁路电子客票等特定业务字段）
        int extCount = 0;
        // 货物运输服务扩展字段（支持多种页签名）
        extCount += processTransportExtension(sheetRowsMap.get("货物运输服务"), invoiceNoToIdMap, "TRANSPORT");
        extCount += processTransportExtension(sheetRowsMap.get("货物运输"), invoiceNoToIdMap, "TRANSPORT");
        extCount += processTransportExtension(sheetRowsMap.get("运输服务"), invoiceNoToIdMap, "TRANSPORT");
        // 铁路电子客票扩展字段（支持多种页签名）
        extCount += processTransportExtension(sheetRowsMap.get("铁路电子客票"), invoiceNoToIdMap, "RAILWAY");
        extCount += processTransportExtension(sheetRowsMap.get("铁路客票"), invoiceNoToIdMap, "RAILWAY");
        extCount += processTransportExtension(sheetRowsMap.get("电子客票"), invoiceNoToIdMap, "RAILWAY");
        // 其他可能的特定业务类型页签可以继续扩展...
        log.info("扩展信息处理完成，共{}条", extCount);

        // 构建导入结果描述
        StringBuilder msg = new StringBuilder();
        msg.append("导入完成：主表").append(allInvoices.size()).append("条");
        if (detailCount > 0) msg.append("，明细").append(detailCount).append("条");
        if (extCount > 0) msg.append("，扩展").append(extCount).append("条");
        log.info(msg.toString());
        // 导入后自动匹配supplier_id和carrier_id
        if (!allInvoices.isEmpty()) {
            log.info("========== 准备匹配供应商supplier_id ==========");
            matchAndSetSupplierId(groupid);
            log.info("========== 准备匹配承运商carrier_id ==========");
            matchAndSetCarrierId(groupid);
            log.info("========== 匹配完成 ==========");
        }
        return msg.toString();
    }

    /**
     * 处理特定业务页签的扩展字段
     */
    private int processTransportExtension(List<Map<String, Object>> rows,
                                           Map<String, Long> invoiceNoToIdMap,
                                           String businessType) {
        if (rows == null || rows.isEmpty()) return 0;

        log.info("处理[{}]扩展信息，共{}行", businessType, rows.size());
        // 输出第一行的列名，用于调试
        if (!rows.isEmpty()) {
            log.info("[{}]第一行列名：{}", businessType, rows.get(0).keySet());
        }

        List<FinInvoiceExtension> extensions = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            String invoiceNo = getStringValue(row, "数电发票号码", "全电发票号码", "数电票号码",
                    "发票号码", "发票号", "号码");
            if (invoiceNo == null || invoiceNo.isEmpty()) continue;
            Long invoiceId = invoiceNoToIdMap.get(invoiceNo);
            if (invoiceId == null) continue;

            // 根据业务类型提取对应的扩展字段
            if ("TRANSPORT".equals(businessType)) {
                addExtension(extensions, invoiceId, businessType, "transportToolType", row, "运输工具种类", "交通工具类型", "交通工具种类");
                addExtension(extensions, invoiceId, businessType, "transportToolNo", row, "运输工具牌号", "交通工具号码", "车牌号", "航班号", "车次号");
                addExtension(extensions, invoiceId, businessType, "origin", row, "起运地", "出发地", "出发站", "起运地/出发地");
                addExtension(extensions, invoiceId, businessType, "destination", row, "到达地", "目的地", "到达站", "到达地/目的地");
                addExtension(extensions, invoiceId, businessType, "cargoName", row, "运输货物名称", "货物名称");
            } else if ("RAILWAY".equals(businessType)) {
                addExtension(extensions, invoiceId, businessType, "passengerName", row, "旅客姓名", "乘客姓名", "姓名");
                addExtension(extensions, invoiceId, businessType, "idNo", row, "有效身份证号", "证件号码", "身份证号", "身份证");
                addExtension(extensions, invoiceId, businessType, "departure", row, "出发地", "出发站", "起运地");
                addExtension(extensions, invoiceId, businessType, "arrival", row, "到达地", "到达站", "目的地");
                addExtension(extensions, invoiceId, businessType, "trainNo", row, "出行车次", "车次", "列车车次", "车次号");
                addExtension(extensions, invoiceId, businessType, "travelDate", row, "日期", "乘车日期", "出行日期", "出发日期");
                addExtension(extensions, invoiceId, businessType, "travelTime", row, "时间", "乘车时间", "出行时间", "出发时间");
            }
        }

        if (extensions.isEmpty()) return 0;

        // 更新发票类型（根据业务类型设置正确的发票类型）
        String invoiceType = "TRANSPORT".equals(businessType) ? "FREIGHT" : 
                            "RAILWAY".equals(businessType) ? "RAILWAY" : null;
        if (invoiceType != null) {
            Set<Long> invoiceIds = extensions.stream()
                    .map(FinInvoiceExtension::getInvoiceId)
                    .collect(Collectors.toSet());
            for (Long invoiceId : invoiceIds) {
                finInvoiceLedgerMapper.updateInvoiceType(invoiceId, invoiceType);
            }
            log.info("更新[{}]发票类型为{}，涉及{}张发票", businessType, invoiceType, invoiceIds.size());
        }

        // 分批插入
        int count = 0;
        for (int i = 0; i < extensions.size(); i += 200) {
            count += finInvoiceExtensionMapper.batchInsert(extensions.subList(i, Math.min(i + 200, extensions.size())));
        }
        log.info("插入[{}]扩展信息{}条", businessType, count);
        return count;
    }

    /**
     * 从行数据中取值并添加到扩展信息列表
     */
    private void addExtension(List<FinInvoiceExtension> list, Long invoiceId, String businessType,
                               String attrKey, Map<String, Object> row, String... colNames) {
        String value = getStringValue(row, colNames);
        if (value != null && !value.isEmpty()) {
            FinInvoiceExtension ext = new FinInvoiceExtension();
            ext.setInvoiceId(invoiceId);
            ext.setBusinessType(businessType);
            ext.setAttrKey(attrKey);
            ext.setAttrValue(value);
            list.add(ext);
        }
    }

    /**
     * 从单行数据构建FinInvoice对象
     */
    private FinInvoice buildInvoiceFromRow(Map<String, Object> item, String groupid,
                                            String userName, Date now, String sheetName) {
        FinInvoice invoice = new FinInvoice();
        invoice.setGroupid(groupid);

        // 发票号码
        invoice.setInvoiceNo(getStringValue(item, "发票号码", "发票号", "号码", "invoiceNo", "InvoiceNo",
                "invoice_no", "发票代码号码", "电子客票号", "票号", "运输票号"));

        // 发票代码
        invoice.setInvoiceCode(getStringValue(item, "发票代码", "代码", "invoiceCode", "InvoiceCode", "invoice_code"));

        // 数电发票号码
        String digitalNo = getStringValue(item, "数电发票号码", "全电发票号码", "digitalInvoiceNo",
                "digital_invoice_no", "数电票号码");
        if (digitalNo != null && !digitalNo.isEmpty()) {
            invoice.setDigitalInvoiceNo(digitalNo);
            if (invoice.getInvoiceNo() == null || invoice.getInvoiceNo().isEmpty()) {
                invoice.setInvoiceNo(digitalNo);
            }
        }

        // 发票类型/票种
        String rawInvoiceType = getStringValue(item, "发票票种", "票种", "发票类型", "发票种类", "invoiceType", "InvoiceType",
                "invoice_type");
        if (rawInvoiceType == null || rawInvoiceType.isEmpty()) {
            if ("铁路电子客票".equals(sheetName)) {
                rawInvoiceType = "铁路电子客票";
            } else if ("货物运输服务".equals(sheetName)) {
                rawInvoiceType = "货物运输服务增值税专用发票";
            }
        }
        invoice.setInvoiceType(mapInvoiceType(rawInvoiceType));

        // 销方名称/税号
        invoice.setSellerName(getStringValue(item, "销方名称", "销售方名称", "销售方", "sellerName",
                "SellerName", "seller_name", "销方", "承运人", "承运人名称"));
        invoice.setSellerTaxNo(getStringValue(item, "销方税号", "销方识别号", "销售方纳税人识别号", "销售方税号",
                "sellerTaxNo", "SellerTaxNo", "seller_tax_no", "销方纳税识别号",
                "承运人纳税人识别号", "承运人识别号", "承运人税号"));

        // 购方名称/税号
        invoice.setBuyerName(getStringValue(item, "购方名称", "购买方名称", "购买方", "buyerName",
                "BuyerName", "buyer_name", "购方", "接受方", "接受方名称", "旅客姓名"));
        invoice.setBuyerTaxNo(getStringValue(item, "购方税号", "购方识别号", "购买方纳税人识别号", "购买方税号",
                "buyerTaxNo", "BuyerTaxNo", "buyer_tax_no", "购方纳税识别号",
                "接受方纳税人识别号", "接受方识别号", "证件号码", "身份证号"));

        // 开票日期
        invoice.setInvoiceDate(parseDateValue(item, "开票日期", "invoiceDate", "InvoiceDate",
                "invoice_date", "开票时间"));

        // 金额
        invoice.setAmountWithTax(getBigDecimalValue(item, "价税合计", "含税金额", "价税合计金额",
                "amountWithTax", "AmountWithTax", "amount_with_tax", "合计金额", "票价", "合计"));
        invoice.setAmountWithoutTax(getBigDecimalValue(item, "金额", "不含税金额",
                "amountWithoutTax", "AmountWithoutTax", "amount_without_tax", "运输费用", "运费"));
        invoice.setTaxAmount(getBigDecimalValue(item, "税额", "taxAmount", "TaxAmount", "tax_amount"));

        // 币种/汇率
        invoice.setCurrency(getStringValue(item, "币种", "currency", "Currency"));
        if (invoice.getCurrency() == null || invoice.getCurrency().isEmpty()) {
            invoice.setCurrency("CNY");
        }
        invoice.setExchangeRate(getBigDecimalValue(item, "汇率", "exchangeRate", "ExchangeRate", "exchange_rate"));
        if (invoice.getExchangeRate() == null) {
            invoice.setExchangeRate(new BigDecimal("1"));
        }

        // 状态/开票人/备注
        invoice.setStatus(getStringValue(item, "发票状态", "状态", "status", "Status"));
        invoice.setDrawer(getStringValue(item, "开票人", "drawer", "Drawer"));
        String remarkValue = getStringValue(item, "备注", "remark", "Remark");
        invoice.setRemark(remarkValue != null ? remarkValue : "税控文件导入");

        invoice.setSource("ERP_IMPORT");
        invoice.setPostingStatus(0);
        invoice.setCreatedBy(userName);
        invoice.setUpdatedBy(userName);
        invoice.setCreatedTime(now);
        invoice.setUpdatedTime(now);
        return invoice;
    }

    /**
     * 从Map中获取字符串值（支持多个key）
     */
    /**
     * 解析税率：去掉百分号，将 "1%" 转为 "1"，"免税"/"不征税" 等文本转为 "0"
     */
    private String parseTaxRate(String raw) {
        if (raw == null || raw.trim().isEmpty()) return null;
        String val = raw.trim().replace("%", "").replace("％", "");
        // 尝试直接解析为数字
        try {
            new java.math.BigDecimal(val);
            return val;
        } catch (NumberFormatException e) {
            // 非数字文本，如"免税"、"不征税"、"***"等，按0处理
            return "0";
        }
    }

    private String getStringValue(Map<String, Object> map, String... keys) {
        for (String key : keys) {
            Object value = map.get(key);
            if (value != null && !value.toString().trim().isEmpty()) {
                return value.toString().trim();
            }
        }
        return null;
    }

    /**
     * 从Map中获取BigDecimal值
     */
    private BigDecimal getBigDecimalValue(Map<String, Object> map, String... keys) {
        String strValue = getStringValue(map, keys);
        if (strValue == null) {
            return null;
        }
        try {
            return new BigDecimal(strValue.replaceAll("[,，]", ""));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 从Map中获取日期值
     */
    private Date parseDateValue(Map<String, Object> map, String... keys) {
        String strValue = getStringValue(map, keys);
        if (strValue == null) {
            return null;
        }
        try {
            // 支持多种日期格式
            String[] patterns = {"yyyy-MM-dd", "yyyy/MM/dd", "yyyyMMdd", "yyyy年MM月dd日"};
            for (String pattern : patterns) {
                try {
                    return new java.text.SimpleDateFormat(pattern).parse(strValue);
                } catch (Exception ignored) {
                }
            }
        } catch (Exception e) {
            // 解析失败返回null
        }
        return null;
    }

    /**
     * 映射发票类型：税控文件中的中文名称 -> 数据库编码
     */
    private String mapInvoiceType(String rawType) {
        if (rawType == null || rawType.trim().isEmpty()) {
            return null;
        }
        String type = rawType.trim();
        // 先检查是否已经是编码格式
        if ("VAT_SPECIAL".equalsIgnoreCase(type) || "VAT_NORMAL".equalsIgnoreCase(type)
                || "MOTOR".equalsIgnoreCase(type) || "FREIGHT".equalsIgnoreCase(type)
                || "TOLL".equalsIgnoreCase(type)) {
            return type.toUpperCase();
        }
        // 优先判断专用发票（必须在普通之前，因为"专用"更具体）
        if (type.contains("专用") || type.contains("专票")) {
            return "VAT_SPECIAL";
        }
        // 普通发票（包括：增值税普通发票、数电发票（普通发票）、电子普通发票等）
        if (type.contains("普通") || type.contains("普票")) {
            return "VAT_NORMAL";
        }
        // 机动车发票
        if (type.contains("机动车")) {
            return "MOTOR";
        }
        // 货物运输
        if (type.contains("运输")) {
            return "FREIGHT";
        }
        // 铁路电子客票
        if (type.contains("铁路") || type.contains("客票")) {
            return "RAILWAY";
        }
        // 通行费
        if (type.contains("通行")) {
            return "TOLL";
        }
        // 仅含"数电发票"但无法判断类型，默认普通发票
        if (type.contains("数电发票") || type.contains("数电票") || type.contains("全电")) {
            return "VAT_NORMAL";
        }
        // 未知类型，原样返回
        return type;
    }

    /**
     * 发票入账（生成凭证）
     *
     * @param ids 发票ID集合
     * @return 入账数量
     */
    @Override
    @Transactional
    public int postingInvoices(List<Long> ids)
    {
        if (ids == null || ids.isEmpty()) {
            return 0;
        }

        UserInfo user = UserInfoContext.get();

        // TODO: 根据发票生成凭证，获取凭证ID，当前暂设为null
        Long voucherId = null;

        return finInvoiceLedgerMapper.batchUpdatePostingStatus(ids, 1, voucherId, user.getUserName());
    }

    /**
     * 新增发票
     *
     * @param finInvoice 发票
     * @return 结果
     */
    @Override
    public int insertFinInvoice(FinInvoice finInvoice)
    {
        return finInvoiceLedgerMapper.insertFinInvoice(finInvoice);
    }

    /**
     * 修改发票
     *
     * @param finInvoice 发票
     * @return 结果
     */
    @Override
    public int updateFinInvoice(FinInvoice finInvoice)
    {
        return finInvoiceLedgerMapper.updateFinInvoice(finInvoice);
    }

    /**
     * 批量更新发票入账状态
     *
     * @param ids 发票ID集合
     * @param postingStatus 入账状态
     * @param voucherId 凭证ID
     * @param updatedBy 更新人
     * @return 结果
     */
    @Override
    public int batchUpdatePostingStatus(List<Long> ids, Integer postingStatus, Long voucherId, String updatedBy)
    {
        return finInvoiceLedgerMapper.batchUpdatePostingStatus(ids, postingStatus, voucherId, updatedBy);
    }

    /**
     * 批量删除发票
     *
     * @param ids 需要删除的发票主键集合
     * @return 结果
     */
    @Override
    public int deleteFinInvoiceByIds(Long[] ids)
    {
        return finInvoiceLedgerMapper.deleteFinInvoiceByIds(ids);
    }

    /**
     * 新增发票明细行
     *
     * @param detail 明细行
     * @return 结果
     */
    @Override
    public int insertInvoiceDetail(FinInvoiceDetail detail)
    {
        // 处理空字符串字段，避免decimal类型报错
        if (detail.getTaxRate() != null && detail.getTaxRate().trim().isEmpty()) {
            detail.setTaxRate(null);
        }
        List<FinInvoiceDetail> list = new ArrayList<>();
        list.add(detail);
        return finInvoiceDetailMapper.batchInsert(list);
    }

    /**
     * 构建mock发票数据
     */
    private List<FinInvoice> buildMockInvoiceList(String groupid, String userName)
    {
        List<FinInvoice> list = new ArrayList<>();
        Date now = new Date();

        FinInvoice invoice1 = new FinInvoice();
        invoice1.setInvoiceNo("MOCK-" + System.currentTimeMillis() + "-001");
        invoice1.setInvoiceType("增值税专用发票");
        invoice1.setGroupid(groupid);
        invoice1.setSellerName("模拟供应商A");
        invoice1.setSellerTaxNo("91110000MA01XXXXX1");
        invoice1.setInvoiceDate(now);
        invoice1.setAmountWithTax(new BigDecimal("11300.00"));
        invoice1.setAmountWithoutTax(new BigDecimal("10000.00"));
        invoice1.setTaxAmount(new BigDecimal("1300.00"));
        invoice1.setCurrency("CNY");
        invoice1.setExchangeRate(BigDecimal.ONE);
        invoice1.setStatus("正常");
        invoice1.setPostingStatus(0);
        invoice1.setCreatedBy(userName);
        invoice1.setUpdatedBy(userName);
        invoice1.setCreatedTime(now);
        invoice1.setUpdatedTime(now);
        list.add(invoice1);

        FinInvoice invoice2 = new FinInvoice();
        invoice2.setInvoiceNo("MOCK-" + System.currentTimeMillis() + "-002");
        invoice2.setInvoiceType("增值税普通发票");
        invoice2.setGroupid(groupid);
        invoice2.setSellerName("模拟供应商B");
        invoice2.setSellerTaxNo("91110000MA01XXXXX2");
        invoice2.setInvoiceDate(now);
        invoice2.setAmountWithTax(new BigDecimal("5650.00"));
        invoice2.setAmountWithoutTax(new BigDecimal("5000.00"));
        invoice2.setTaxAmount(new BigDecimal("650.00"));
        invoice2.setCurrency("CNY");
        invoice2.setExchangeRate(BigDecimal.ONE);
        invoice2.setStatus("正常");
        invoice2.setPostingStatus(0);
        invoice2.setCreatedBy(userName);
        invoice2.setUpdatedBy(userName);
        invoice2.setCreatedTime(now);
        invoice2.setUpdatedTime(now);
        list.add(invoice2);

        return list;
    }

    /**
     * 自动匹配发票的supplier_id
     * 匹配逻辑：优先使用 fullname 匹配，再使用 t_erp_customer_account 的 company_name 匹配
     */
    private void matchAndSetSupplierId(String groupid) {
        try {
            // 1. 查询supplier_id为空的发票，获取去重的seller_name列表
            List<Map<String, Object>> unmatchedSellers = finInvoiceLedgerMapper.selectDistinctSellerNamesWithoutSupplier();
            if (unmatchedSellers == null || unmatchedSellers.isEmpty()) {
                return;
            }
            log.info("待匹配供应商的发票销方数量：{}", unmatchedSellers.size());

            // 2. 从ERP模块获取供应商列表（包含 fullname 和 accountNames）
            Result<?> supplierResult = remoteERPService.getSupplierList();
            if (!Result.isSuccess(supplierResult) || !(supplierResult.getData() instanceof List)) {
                log.warn("获取供应商列表失败，跳过supplier_id匹配");
                return;
            }
            List<Map<String, Object>> supplierList = (List<Map<String, Object>>) supplierResult.getData();
            
            // 构建匹配映射：name/fullname/accountName -> supplierId
            // 优先匹配 fullname，其次匹配 accountNames
            Map<String, String> nameToSupplierIdMap = new HashMap<>();
            for (Map<String, Object> supplier : supplierList) {
                Object idObj = supplier.get("id");
                if (idObj == null) continue;
                String supplierId = idObj.toString();
                
                // 1. 使用 fullname 匹配（优先级最高）
                Object fullnameObj = supplier.get("fullname");
                if (fullnameObj != null && !fullnameObj.toString().trim().isEmpty()) {
                    nameToSupplierIdMap.putIfAbsent(fullnameObj.toString().trim(), supplierId);
                }
                
                // 2. 使用 t_erp_customer_account 的 company_name 匹配
                Object accountNamesObj = supplier.get("accountNames");
                if (accountNamesObj != null && !accountNamesObj.toString().trim().isEmpty()) {
                    String[] accountNames = accountNamesObj.toString().split(",");
                    for (String accountName : accountNames) {
                        if (accountName != null && !accountName.trim().isEmpty()) {
                            nameToSupplierIdMap.putIfAbsent(accountName.trim(), supplierId);
                        }
                    }
                }
                
                // 3. 使用 name 匹配（最后备选）
                Object nameObj = supplier.get("name");
                if (nameObj != null && !nameObj.toString().trim().isEmpty()) {
                    nameToSupplierIdMap.putIfAbsent(nameObj.toString().trim(), supplierId);
                }
            }

            // 3. 逐个匹配并更新
            int totalUpdated = 0;
            for (Map<String, Object> seller : unmatchedSellers) {
                String sellerName = seller.get("sellerName") != null ? seller.get("sellerName").toString().trim() : null;
                if (sellerName == null || sellerName.isEmpty()) continue;

                String supplierId = nameToSupplierIdMap.get(sellerName);
                if (supplierId != null) {
                    int updated = finInvoiceLedgerMapper.batchUpdateSupplierIdBySellerName(sellerName, supplierId);
                    totalUpdated += updated;
                    log.info("匹配供应商成功：sellerName={}, supplierId={}, 更新{}条发票", sellerName, supplierId, updated);
                }
            }
            log.info("supplier_id匹配完成，共更新{}条发票", totalUpdated);
        } catch (Exception e) {
            log.error("匹配supplier_id时发生异常", e);
        }
    }

    /**
     * 批量匹配所有租户的发票supplier_id（供手动触发，修复历史数据）
     */
    @Override
    public Map<String, Object> batchMatchSupplierId() {
        Map<String, Object> result = new HashMap<>();
        result.put("updated", 0);
        result.put("unmatchedSellerCount", 0);
        result.put("supplierCount", 0);
        result.put("unmatchedSellers", new ArrayList<>());
        try {
            // 诊断：查询发票数据概况
            Map<String, Object> diagInfo = finInvoiceLedgerMapper.selectInvoiceDiagInfo();
            result.put("diagInfo", diagInfo);
            log.info("发票数据概况：{}", diagInfo);

            // 查询所有supplier_id为空的发票，获取去重的seller_name列表
            List<Map<String, Object>> unmatchedSellers = finInvoiceLedgerMapper.selectDistinctSellerNamesWithoutSupplier();
            if (unmatchedSellers == null || unmatchedSellers.isEmpty()) {
                log.info("没有需要匹配supplier_id的发票");
                String msg = "没有需要匹配的发票";
                if (diagInfo != null) {
                    Object total = diagInfo.get("totalCount");
                    Object nullCount = diagInfo.get("nullSupplierCount");
                    Object hasCount = diagInfo.get("hasSupplierCount");
                    Object nullNameCount = diagInfo.get("nullSellerNameCount");
                    msg = "发票总数" + total + "条，supplier_id为空" + nullCount + "条，已有supplier_id" + hasCount + "条，seller_name为空" + nullNameCount + "条";
                    if (hasCount != null && Integer.parseInt(hasCount.toString()) > 0) {
                        msg += "。已有supplier_id的发票无需匹配，请检查groupids是否正确";
                    }
                }
                result.put("msg", msg);
                return result;
            }
            result.put("unmatchedSellerCount", unmatchedSellers.size());
            log.info("待匹配供应商的发票销方数量：{}", unmatchedSellers.size());

            // 从ERP模块获取供应商列表
            Result<?> supplierResult = remoteERPService.getSupplierList();
            if (!Result.isSuccess(supplierResult) || !(supplierResult.getData() instanceof List)) {
                log.warn("获取供应商列表失败，跳过supplier_id匹配");
                result.put("msg", "获取ERP供应商列表失败");
                return result;
            }
            List<Map<String, Object>> supplierList = (List<Map<String, Object>>) supplierResult.getData();
            result.put("supplierCount", supplierList.size());
            
            // 构建匹配映射：name/fullname/accountName -> supplierId
            // 优先匹配 fullname，其次匹配 accountNames
            Map<String, String> nameToSupplierIdMap = new HashMap<>();
            List<String> supplierNames = new ArrayList<>();
            for (Map<String, Object> supplier : supplierList) {
                Object idObj = supplier.get("id");
                if (idObj == null) continue;
                String supplierId = idObj.toString();
                
                // 1. 使用 fullname 匹配（优先级最高）
                Object fullnameObj = supplier.get("fullname");
                if (fullnameObj != null && !fullnameObj.toString().trim().isEmpty()) {
                    nameToSupplierIdMap.putIfAbsent(fullnameObj.toString().trim(), supplierId);
                    supplierNames.add(fullnameObj.toString().trim());
                }
                
                // 2. 使用 t_erp_customer_account 的 company_name 匹配
                Object accountNamesObj = supplier.get("accountNames");
                if (accountNamesObj != null && !accountNamesObj.toString().trim().isEmpty()) {
                    String[] accountNames = accountNamesObj.toString().split(",");
                    for (String accountName : accountNames) {
                        if (accountName != null && !accountName.trim().isEmpty()) {
                            nameToSupplierIdMap.putIfAbsent(accountName.trim(), supplierId);
                        }
                    }
                }
                
                // 3. 使用 name 匹配（最后备选）
                Object nameObj = supplier.get("name");
                if (nameObj != null && !nameObj.toString().trim().isEmpty()) {
                    nameToSupplierIdMap.putIfAbsent(nameObj.toString().trim(), supplierId);
                }
            }
            log.info("ERP供应商列表（共{}个）：前10个={}", supplierNames.size(), supplierNames.subList(0, Math.min(10, supplierNames.size())));
            log.info("发票销方名称（共{}个）：前10个={}", unmatchedSellers.size(), 
                    unmatchedSellers.subList(0, Math.min(10, unmatchedSellers.size())).stream()
                            .map(m -> m.get("sellerName")).collect(java.util.stream.Collectors.toList()));

            // 逐个匹配并更新
            int totalUpdated = 0;
            List<String> unmatchedNames = new ArrayList<>();
            for (Map<String, Object> seller : unmatchedSellers) {
                String sellerName = seller.get("sellerName") != null ? seller.get("sellerName").toString().trim() : null;
                if (sellerName == null || sellerName.isEmpty()) continue;

                String supplierId = nameToSupplierIdMap.get(sellerName);
                if (supplierId != null) {
                    int updated = finInvoiceLedgerMapper.batchUpdateSupplierIdBySellerName(sellerName, supplierId);
                    totalUpdated += updated;
                    log.info("匹配供应商成功：sellerName={}, supplierId={}, 更新{}条发票", sellerName, supplierId, updated);
                } else {
                    unmatchedNames.add(sellerName);
                }
            }
            result.put("updated", totalUpdated);
            result.put("unmatchedSellers", unmatchedNames);
            // 返回ERP供应商名称列表，方便用户对比
            result.put("erpSupplierNames", supplierNames.subList(0, Math.min(20, supplierNames.size())));
            result.put("msg", "匹配完成：共更新" + totalUpdated + "条发票，" + unmatchedNames.size() + "个销方未匹配到供应商");
            log.info("批量匹配supplier_id完成，共更新{}条发票，{}个未匹配", totalUpdated, unmatchedNames.size());
            return result;
        } catch (Exception e) {
            log.error("批量匹配supplier_id时发生异常", e);
            result.put("msg", "匹配异常：" + e.getMessage());
            return result;
        }
    }

    /**
     * 自动匹配发票的carrier_id（通过seller_name匹配承运商名称）
     * 匹配逻辑：使用 t_erp_ship_transcompany 的 name 和 simplename 匹配
     */
    private void matchAndSetCarrierId(String groupid) {
        try {
            log.info("========== 开始匹配承运商carrier_id ==========");
            // 1. 查询carrier_id为空的发票，获取去重的seller_name列表
            List<Map<String, Object>> unmatchedSellers = finInvoiceLedgerMapper.selectDistinctSellerNamesWithoutCarrier();
            log.info("carrier_id为空的发票销方数量：{}", unmatchedSellers != null ? unmatchedSellers.size() : 0);
            if (unmatchedSellers == null || unmatchedSellers.isEmpty()) {
                log.info("没有需要匹配承运商的发票，退出");
                return;
            }
            log.info("待匹配承运商的发票销方数量：{}", unmatchedSellers.size());

            // 2. 从ERP模块获取承运商列表
            Result<?> transCompanyResult = remoteERPService.getTransCompanyList();
            log.info("获取承运商列表结果：success={}, data type={}, data={}", 
                    Result.isSuccess(transCompanyResult), 
                    transCompanyResult.getData() != null ? transCompanyResult.getData().getClass().getName() : "null",
                    transCompanyResult.getData());
            if (!Result.isSuccess(transCompanyResult) || transCompanyResult.getData() == null) {
                log.warn("获取承运商列表失败，跳过carrier_id匹配");
                return;
            }
            
            // 构建匹配映射：name/simplename -> transCompanyId
            Map<String, String> nameToCarrierIdMap = new HashMap<>();
            List<?> transCompanyList = (List<?>) transCompanyResult.getData();
            log.info("承运商列表数量：{}", transCompanyList.size());
            
            for (Object company : transCompanyList) {
                String carrierId = null;
                String name = null;
                String simpleName = null;
                
                // 处理 Map 类型
                if (company instanceof Map) {
                    Map<String, Object> companyMap = (Map<String, Object>) company;
                    Object idObj = companyMap.get("id");
                    if (idObj != null) carrierId = idObj.toString();
                    Object nameObj = companyMap.get("name");
                    if (nameObj != null) name = nameObj.toString().trim();
                    Object simpleNameObj = companyMap.get("simplename");
                    if (simpleNameObj != null) simpleName = simpleNameObj.toString().trim();
                } 
                // 处理对象类型（通过反射获取属性）
                else {
                    try {
                        java.lang.reflect.Method getIdMethod = company.getClass().getMethod("getId");
                        java.lang.reflect.Method getNameMethod = company.getClass().getMethod("getName");
                        java.lang.reflect.Method getSimplenameMethod = company.getClass().getMethod("getSimplename");
                        Object idVal = getIdMethod.invoke(company);
                        Object nameVal = getNameMethod.invoke(company);
                        Object simpleNameVal = getSimplenameMethod.invoke(company);
                        if (idVal != null) carrierId = idVal.toString();
                        if (nameVal != null) name = nameVal.toString().trim();
                        if (simpleNameVal != null) simpleName = simpleNameVal.toString().trim();
                    } catch (Exception e) {
                        log.warn("反射获取承运商属性失败：{}", e.getMessage());
                    }
                }
                
                if (carrierId == null) continue;
                
                // 1. 使用 name 匹配
                if (name != null && !name.isEmpty()) {
                    nameToCarrierIdMap.putIfAbsent(name, carrierId);
                }
                
                // 2. 使用 simplename 匹配
                if (simpleName != null && !simpleName.isEmpty()) {
                    nameToCarrierIdMap.putIfAbsent(simpleName, carrierId);
                }
            }
            log.info("承运商匹配映射数量：{}", nameToCarrierIdMap.size());

            // 3. 逐个匹配并更新
            int totalUpdated = 0;
            for (Map<String, Object> seller : unmatchedSellers) {
                String sellerName = seller.get("sellerName") != null ? seller.get("sellerName").toString().trim() : null;
                if (sellerName == null || sellerName.isEmpty()) continue;

                String carrierId = nameToCarrierIdMap.get(sellerName);
                if (carrierId != null) {
                    int updated = finInvoiceLedgerMapper.batchUpdateCarrierIdBySellerName(sellerName, carrierId);
                    totalUpdated += updated;
                    log.info("匹配承运商成功：sellerName={}, carrierId={}, 更新{}条发票", sellerName, carrierId, updated);
                }
            }
            log.info("carrier_id匹配完成，共更新{}条发票", totalUpdated);
        } catch (Exception e) {
            log.error("匹配carrier_id时发生异常", e);
        }
    }

    /**
     * 批量匹配所有租户的发票carrier_id（供手动触发，修复历史数据）
     */
    @Override
    public Map<String, Object> batchMatchCarrierId() {
        Map<String, Object> result = new HashMap<>();
        result.put("updated", 0);
        result.put("unmatchedSellerCount", 0);
        result.put("transCompanyCount", 0);
        result.put("unmatchedSellers", new ArrayList<>());
        try {
            // 查询所有carrier_id为空的发票，获取去重的seller_name列表
            List<Map<String, Object>> unmatchedSellers = finInvoiceLedgerMapper.selectDistinctSellerNamesWithoutCarrier();
            if (unmatchedSellers == null || unmatchedSellers.isEmpty()) {
                log.info("没有需要匹配carrier_id的发票");
                result.put("msg", "没有需要匹配的发票");
                return result;
            }
            result.put("unmatchedSellerCount", unmatchedSellers.size());
            log.info("待匹配承运商的发票销方数量：{}", unmatchedSellers.size());

            // 从ERP模块获取承运商列表
            Result<?> transCompanyResult = remoteERPService.getTransCompanyList();
            log.info("获取承运商列表结果：success={}, data type={}", 
                    Result.isSuccess(transCompanyResult), 
                    transCompanyResult.getData() != null ? transCompanyResult.getData().getClass().getName() : "null");
            if (!Result.isSuccess(transCompanyResult) || transCompanyResult.getData() == null) {
                log.warn("获取承运商列表失败，跳过carrier_id匹配");
                result.put("msg", "获取ERP承运商列表失败");
                return result;
            }
            
            // 构建匹配映射：name/simplename -> transCompanyId
            Map<String, String> nameToCarrierIdMap = new HashMap<>();
            List<String> carrierNames = new ArrayList<>();
            List<?> transCompanyList = (List<?>) transCompanyResult.getData();
            result.put("transCompanyCount", transCompanyList.size());
            log.info("承运商列表数量：{}", transCompanyList.size());
            
            for (Object company : transCompanyList) {
                String carrierId = null;
                String name = null;
                String simpleName = null;
                
                // 处理 Map 类型
                if (company instanceof Map) {
                    Map<String, Object> companyMap = (Map<String, Object>) company;
                    Object idObj = companyMap.get("id");
                    if (idObj != null) carrierId = idObj.toString();
                    Object nameObj = companyMap.get("name");
                    if (nameObj != null) name = nameObj.toString().trim();
                    Object simpleNameObj = companyMap.get("simplename");
                    if (simpleNameObj != null) simpleName = simpleNameObj.toString().trim();
                } 
                // 处理对象类型（通过反射获取属性）
                else {
                    try {
                        java.lang.reflect.Method getIdMethod = company.getClass().getMethod("getId");
                        java.lang.reflect.Method getNameMethod = company.getClass().getMethod("getName");
                        java.lang.reflect.Method getSimplenameMethod = company.getClass().getMethod("getSimplename");
                        Object idVal = getIdMethod.invoke(company);
                        Object nameVal = getNameMethod.invoke(company);
                        Object simpleNameVal = getSimplenameMethod.invoke(company);
                        if (idVal != null) carrierId = idVal.toString();
                        if (nameVal != null) name = nameVal.toString().trim();
                        if (simpleNameVal != null) simpleName = simpleNameVal.toString().trim();
                    } catch (Exception e) {
                        log.warn("反射获取承运商属性失败：{}", e.getMessage());
                    }
                }
                
                if (carrierId == null) continue;
                
                // 1. 使用 name 匹配
                if (name != null && !name.isEmpty()) {
                    nameToCarrierIdMap.putIfAbsent(name, carrierId);
                    carrierNames.add(name);
                }
                
                // 2. 使用 simplename 匹配
                if (simpleName != null && !simpleName.isEmpty()) {
                    nameToCarrierIdMap.putIfAbsent(simpleName, carrierId);
                }
            }
            log.info("承运商匹配映射数量：{}", nameToCarrierIdMap.size());
            log.info("ERP承运商列表（共{}个）：前10个={}", carrierNames.size(), carrierNames.subList(0, Math.min(10, carrierNames.size())));
            log.info("发票销方名称（共{}个）：前10个={}", unmatchedSellers.size(), 
                    unmatchedSellers.subList(0, Math.min(10, unmatchedSellers.size())).stream()
                            .map(m -> m.get("sellerName")).collect(java.util.stream.Collectors.toList()));

            // 逐个匹配并更新
            int totalUpdated = 0;
            List<String> unmatchedNames = new ArrayList<>();
            for (Map<String, Object> seller : unmatchedSellers) {
                String sellerName = seller.get("sellerName") != null ? seller.get("sellerName").toString().trim() : null;
                if (sellerName == null || sellerName.isEmpty()) continue;

                String carrierId = nameToCarrierIdMap.get(sellerName);
                if (carrierId != null) {
                    int updated = finInvoiceLedgerMapper.batchUpdateCarrierIdBySellerName(sellerName, carrierId);
                    totalUpdated += updated;
                    log.info("匹配承运商成功：sellerName={}, carrierId={}, 更新{}条发票", sellerName, carrierId, updated);
                } else {
                    unmatchedNames.add(sellerName);
                }
            }
            result.put("updated", totalUpdated);
            result.put("unmatchedSellers", unmatchedNames);
            // 返回ERP承运商名称列表，方便用户对比
            result.put("erpCarrierNames", carrierNames.subList(0, Math.min(20, carrierNames.size())));
            result.put("msg", "匹配完成：共更新" + totalUpdated + "条发票，" + unmatchedNames.size() + "个销方未匹配到承运商");
            log.info("批量匹配carrier_id完成，共更新{}条发票，{}个未匹配", totalUpdated, unmatchedNames.size());
            return result;
        } catch (Exception e) {
            log.error("批量匹配carrier_id时发生异常", e);
            result.put("msg", "匹配异常：" + e.getMessage());
            return result;
        }
    }

    /**
     * 查询凭证信息（凭证号、凭证字等）
     */
    @Override
    public Map<String, Object> selectVoucherInfo(Long voucherId)
    {
        Map<String, Object> result = new HashMap<>();
        try {
            FinVouchers voucher = finVouchersService.selectFinVouchersByVoucherId(voucherId);
            if (voucher != null) {
                result.put("voucherId", voucher.getVoucherId());
                result.put("voucherNo", voucher.getVoucherNo());
                result.put("voucherType", voucher.getVoucherType());
                result.put("voucherDate", voucher.getVoucherDate());
                result.put("totalAmount", voucher.getTotalAmount());
                result.put("voucherStatus", voucher.getVoucherStatus());
            }
        } catch (Exception e) {
            log.error("查询凭证信息失败，voucherId={}", voucherId, e);
        }
        return result;
    }
}
