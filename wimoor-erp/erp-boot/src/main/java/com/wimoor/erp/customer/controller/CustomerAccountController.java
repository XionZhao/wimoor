package com.wimoor.erp.customer.controller;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;

import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.wimoor.common.mvc.BizException;
import com.wimoor.common.result.Result;
import com.wimoor.common.user.UserInfo;
import com.wimoor.common.user.UserInfoContext;
import com.wimoor.erp.customer.pojo.entity.CustomerAccount;
import com.wimoor.erp.customer.service.ICustomerAccountService;
import com.wimoor.erp.util.UUIDUtil;

import io.swagger.annotations.Api;
import lombok.RequiredArgsConstructor;

@Api(tags = "供应商收款账户接口")
@RestController
@RequestMapping("/api/v1/customer/account")
@RequiredArgsConstructor
public class CustomerAccountController {

    final ICustomerAccountService customerAccountService;

    @PostMapping(value = "/uploadAccountFile", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<String> uploadAccountFile(@RequestParam("file") MultipartFile file, @RequestParam("importType") String importType) {
        UserInfo user = UserInfoContext.get();
        if (file != null) {
            try {
                InputStream inputStream = file.getInputStream();
                Workbook workbook = WorkbookFactory.create(inputStream);
                Sheet sheet = workbook.getSheetAt(0);
                customerAccountService.importAccounts(user, sheet, importType);
                workbook.close();
                return Result.success("导入成功");
            } catch (IOException e) {
                e.printStackTrace();
                throw new BizException("文件读取失败");
            } catch (EncryptedDocumentException e) {
                e.printStackTrace();
                throw new BizException("文件格式错误");
            } catch (BizException e) {
                throw e;
            } catch (Exception e) {
                e.printStackTrace();
                throw new BizException("导入失败，请检查文件内容");
            }
        }
        throw new BizException("文件不能为空");
    }

    @GetMapping("/list")
    public Result<List<Map<String, Object>>> getList(String customerId, Integer status) {
        return Result.success(customerAccountService.findByCustomerId(customerId, status));
    }
    
    @GetMapping("/downloadAccountList")
    public void downloadAccountList(HttpServletResponse response){
        SXSSFWorkbook workbook = new SXSSFWorkbook();
        UserInfo userinfo = UserInfoContext.get();
        String shopid = userinfo.getCompanyid();
        
        List<Map<String, Object>> list = customerAccountService.findByShopId(shopid);
        
        Sheet sheet = workbook.createSheet("付款账户");
        // 标题行
        String[] titles = {"供应商名称", "公司名称", "银行账号", "开户行", "备注", "是否默认", "状态"};
        Row trow = sheet.createRow(0);
        for (int i = 0; i < titles.length; i++) {
            trow.createCell(i).setCellValue(titles[i]);
        }
        
        if (list != null && list.size() > 0) {
            for (int i = 0; i < list.size(); i++) {
                Row row = sheet.createRow(i + 1);
                Map<String, Object> map = list.get(i);
                row.createCell(0).setCellValue(map.get("customer_name") != null ? map.get("customer_name").toString() : "");
                row.createCell(1).setCellValue(map.get("company_name") != null ? map.get("company_name").toString() : "");
                row.createCell(2).setCellValue(map.get("account_number") != null ? map.get("account_number").toString() : "");
                row.createCell(3).setCellValue(map.get("bank_name") != null ? map.get("bank_name").toString() : "");
                row.createCell(4).setCellValue(map.get("remark") != null ? map.get("remark").toString() : "");
                row.createCell(5).setCellValue(isTrueValue(map.get("is_default")) ? "是" : "否");
                row.createCell(6).setCellValue(isTrueValue(map.get("status")) ? "启用" : "停用");
            }
        }
        
        try {
            response.setContentType("application/force-download");
            response.addHeader("Content-Disposition", "attachment;fileName=AccountList" + System.currentTimeMillis() + ".xlsx");
            ServletOutputStream fOut = response.getOutputStream();
            workbook.write(fOut);
            workbook.close();
            fOut.flush();
            fOut.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @PostMapping("/save")
    public Result<String> saveData(@RequestBody CustomerAccount account) {
        UserInfo user = UserInfoContext.get();
        String operator = user.getId();
        account.setOperator(operator);
        if (account.getStatus() == null) {
            account.setStatus(1);
        }
        if (account.getIsDefault() == null) {
            account.setIsDefault(false);
        }
        if (account.getId() == null || account.getId().isEmpty()) {
            account.setId(UUIDUtil.getUUIDshort());
        }
        customerAccountService.saveAccount(account);
        return Result.success("OK");
    }

    @GetMapping("/setDefault")
    public Result<String> setDefault(String id, String customerId) {
        customerAccountService.setDefault(id, customerId);
        return Result.success("OK");
    }

    @GetMapping("/toggleStatus")
    public Result<String> toggleStatus(String id) {
        customerAccountService.toggleStatus(id);
        return Result.success("OK");
    }

    @GetMapping("/delete")
    public Result<String> deleteData(String id) {
        customerAccountService.removeById(id);
        return Result.success("OK");
    }

    private boolean isTrueValue(Object value) {
        if (value == null) {
            return false;
        }
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue() == 1;
        }
        String str = value.toString().trim();
        return "1".equals(str) || "true".equalsIgnoreCase(str) || "是".equals(str) || "启用".equals(str);
    }
}
