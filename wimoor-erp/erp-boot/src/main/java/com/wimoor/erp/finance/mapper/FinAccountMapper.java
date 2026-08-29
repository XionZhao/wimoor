package com.wimoor.erp.finance.mapper;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wimoor.erp.finance.pojo.entity.FinAccount;

@Mapper
public interface FinAccountMapper extends BaseMapper<FinAccount> {

	FinAccount findAcBanlance(String shopid);

	List<FinAccount> findAccountAll(String shopid);
	List<FinAccount> findAccountArchiveAll(String shopid);

	void saveAccountIndex(Map<String, Object> item);

    // ==================== 台账Feign接口 ====================
    
    /**
     * 进销存台账汇总
     */
    List<Map<String, Object>> getInventoryLedgerSummary(@Param("param") Map<String, Object> param);

    /**
     * 进销存台账明细
     */
    List<Map<String, Object>> getInventoryLedgerDetail(@Param("param") Map<String, Object> param);

    /**
     * 进销存台账明细总数
     */
    long getInventoryLedgerDetailCount(@Param("param") Map<String, Object> param);
}