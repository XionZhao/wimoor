package com.wimoor.erp.customer.mapper;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wimoor.erp.customer.pojo.entity.CustomerAccount;

@Mapper
public interface CustomerAccountMapper extends BaseMapper<CustomerAccount> {

    List<Map<String, Object>> findByCustomerId(@Param("customerId") String customerId, @Param("status") Integer status);

    void resetDefault(@Param("customerId") String customerId);
    
    List<Map<String, Object>> findByShopId(@Param("shopid") String shopid);
}
