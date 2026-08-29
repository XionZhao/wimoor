package com.wimoor.erp.customer.service.impl;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wimoor.common.mvc.BizException;
import com.wimoor.common.user.UserInfo;
import com.wimoor.erp.customer.mapper.CustomerAccountMapper;
import com.wimoor.erp.customer.mapper.CustomerMapper;
import com.wimoor.erp.customer.pojo.entity.Customer;
import com.wimoor.erp.customer.pojo.entity.CustomerAccount;
import com.wimoor.erp.customer.service.ICustomerAccountService;
import com.wimoor.erp.util.UUIDUtil;

import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;

@Service("customerAccountService")
@RequiredArgsConstructor
public class CustomerAccountServiceImpl extends ServiceImpl<CustomerAccountMapper, CustomerAccount>
        implements ICustomerAccountService {

    private final CustomerMapper customerMapper;

    @Override
    public List<Map<String, Object>> findByCustomerId(String customerId, Integer status) {
        return this.baseMapper.findByCustomerId(customerId, status);
    }
    
    @Override
    public List<Map<String, Object>> findByShopId(String shopid) {
        return this.baseMapper.findByShopId(shopid);
    }

    @Override
    public void saveAccount(CustomerAccount account) {
        Calendar c = Calendar.getInstance();
        Date date = c.getTime();
        account.setOpttime(date);

        if (StrUtil.isNotEmpty(account.getId())) {
            // 更新
            this.updateById(account);
        } else {
            // 新增
            this.save(account);
        }

        // 如果设置为默认，需要重置其他账户的默认状态
        if (Boolean.TRUE.equals(account.getIsDefault())) {
            this.baseMapper.resetDefault(account.getCustomerId());
            // 再把当前账户设为默认
            this.update(new LambdaUpdateWrapper<CustomerAccount>()
                    .eq(CustomerAccount::getId, account.getId())
                    .set(CustomerAccount::getIsDefault, true));
        }
    }

    @Override
    public void setDefault(String id, String customerId) {
        // 先重置所有默认
        this.baseMapper.resetDefault(customerId);
        // 再设置当前为默认
        this.update(new LambdaUpdateWrapper<CustomerAccount>()
                .eq(CustomerAccount::getId, id)
                .set(CustomerAccount::getIsDefault, true));
    }

    @Override
    public void toggleStatus(String id) {
        CustomerAccount account = this.getById(id);
        if (account == null) {
            throw new BizException("收款账户不存在");
        }
        // 切换状态：1-启用 <-> 0-停用
        int newStatus = account.getStatus() != null && account.getStatus() == 1 ? 0 : 1;
        this.update(new LambdaUpdateWrapper<CustomerAccount>()
                .eq(CustomerAccount::getId, id)
                .set(CustomerAccount::getStatus, newStatus));
    }
    
    @Override
    public void importAccounts(UserInfo user, Sheet sheet, String importType) {
        String shopid = user.getCompanyid();
        String operator = user.getId();
        
        // 读取表头映射
        Row headerRow = sheet.getRow(0);
        Map<String, Integer> headerMap = new HashMap<>();
        for (int i = 0; i < headerRow.getLastCellNum(); i++) {
            String cellValue = getCellValue(headerRow.getCell(i)).trim();
            headerMap.put(cellValue, i);
        }
        
        // 校验必要列
        if (!headerMap.containsKey("供应商名称") || !headerMap.containsKey("银行账号") || !headerMap.containsKey("开户行")) {
            throw new BizException("文件必须包含：供应商名称、银行账号、开户行");
        }
        
        Map<String, List<CustomerAccount>> importMap = new HashMap<>();
        
        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;
            
            String customerName = getCellValue(row.getCell(headerMap.get("供应商名称"))).trim();
            String accountNumber = getCellValue(row.getCell(headerMap.get("银行账号"))).trim();
            String bankName = getCellValue(row.getCell(headerMap.get("开户行"))).trim();
            
            if (StrUtil.hasBlank(customerName, accountNumber, bankName)) continue;
            
            // 根据供应商名称查找供应商
            Customer customer = customerMapper.selectOne(new LambdaQueryWrapper<Customer>()
                    .eq(Customer::getName, customerName)
                    .eq(Customer::getShopid, shopid));
            
            if (customer == null) {
                // 可以记录日志，但这里按需求跳过
                continue;
            }
            
            CustomerAccount account = new CustomerAccount();
            account.setCustomerId(customer.getId());
            account.setAccountNumber(accountNumber);
            account.setBankName(bankName);
            account.setOperator(operator);
            account.setOpttime(new Date());
            
            // 读取可选字段
            if (headerMap.containsKey("公司名称")) {
                String companyName = getCellValue(row.getCell(headerMap.get("公司名称")));
                account.setCompanyName(StrUtil.isNotBlank(companyName) ? companyName.trim() : null);
            }
            if (headerMap.containsKey("备注")) {
                String remark = getCellValue(row.getCell(headerMap.get("备注")));
                account.setRemark(StrUtil.isNotBlank(remark) ? remark.trim() : null);
            }
            if (headerMap.containsKey("是否默认")) {
                String isDefault = getCellValue(row.getCell(headerMap.get("是否默认"))).trim();
                account.setIsDefault("是".equals(isDefault) || "TRUE".equalsIgnoreCase(isDefault) || "1".equals(isDefault));
            } else {
                account.setIsDefault(false);
            }
            if (headerMap.containsKey("状态")) {
                String status = getCellValue(row.getCell(headerMap.get("状态"))).trim();
                account.setStatus("启用".equals(status) || "TRUE".equalsIgnoreCase(status) || "1".equals(status) ? 1 : 0);
            } else {
                account.setStatus(1);
            }
            
            importMap.computeIfAbsent(customer.getId(), k -> new ArrayList<>()).add(account);
        }
        
        for (Map.Entry<String, List<CustomerAccount>> entry : importMap.entrySet()) {
            String customerId = entry.getKey();
            List<CustomerAccount> importList = entry.getValue();
            
            if ("overwrite".equals(importType)) {
                // 覆盖导入：删除旧数据，插入新数据
                this.remove(new LambdaQueryWrapper<CustomerAccount>()
                        .eq(CustomerAccount::getCustomerId, customerId));
                for (CustomerAccount acc : importList) {
                    acc.setId(UUIDUtil.getUUIDshort());
                    this.save(acc);
                }
            } else {
                // 增量导入
                List<CustomerAccount> existingList = this.list(new LambdaQueryWrapper<CustomerAccount>()
                        .eq(CustomerAccount::getCustomerId, customerId));
                
                for (CustomerAccount acc : importList) {
                    boolean found = false;
                    for (CustomerAccount existing : existingList) {
                        if (acc.getAccountNumber().equals(existing.getAccountNumber()) &&
                            acc.getBankName().equals(existing.getBankName())) {
                            // 更新已有
                            acc.setId(existing.getId());
                            this.updateById(acc);
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        acc.setId(UUIDUtil.getUUIDshort());
                        this.save(acc);
                    }
                }
            }
            
            // 处理默认逻辑
            if (!importList.isEmpty() && importList.stream().anyMatch(CustomerAccount::getIsDefault)) {
                this.baseMapper.resetDefault(customerId);
                importList.stream().filter(CustomerAccount::getIsDefault).findFirst().ifPresent(acc -> {
                    this.update(new LambdaUpdateWrapper<CustomerAccount>()
                            .eq(CustomerAccount::getId, acc.getId())
                            .set(CustomerAccount::getIsDefault, true));
                });
            }
        }
    }
    
    private String getCellValue(org.apache.poi.ss.usermodel.Cell cell) {
        if (cell == null) {
            return "";
        }
        switch (cell.getCellType()) {
            case NUMERIC:
                // 避免科学计数法
                java.text.DecimalFormat df = new java.text.DecimalFormat("#");
                return df.format(cell.getNumericCellValue());
            case BOOLEAN:
                return cell.getBooleanCellValue() ? "是" : "否";
            case FORMULA:
                try {
                    return cell.getStringCellValue();
                } catch (Exception e) {
                    return String.valueOf(cell.getNumericCellValue());
                }
            default:
                return cell.getStringCellValue();
        }
    }
}
