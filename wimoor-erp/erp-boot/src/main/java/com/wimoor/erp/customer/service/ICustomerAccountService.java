package com.wimoor.erp.customer.service;

import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.Sheet;

import com.baomidou.mybatisplus.extension.service.IService;
import com.wimoor.common.user.UserInfo;
import com.wimoor.erp.customer.pojo.entity.CustomerAccount;

public interface ICustomerAccountService extends IService<CustomerAccount> {

    List<Map<String, Object>> findByCustomerId(String customerId, Integer status);

    void saveAccount(CustomerAccount account);

    void setDefault(String id, String customerId);

    void toggleStatus(String id);
    
    List<Map<String, Object>> findByShopId(String shopid);
    
    void importAccounts(UserInfo user, Sheet sheet, String importType);
}
