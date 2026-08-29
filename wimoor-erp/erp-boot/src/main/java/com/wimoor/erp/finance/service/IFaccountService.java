package com.wimoor.erp.finance.service;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.baomidou.mybatisplus.extension.service.IService;
import com.wimoor.common.user.UserInfo;
import com.wimoor.erp.finance.pojo.entity.FinAccount;
import com.wimoor.erp.finance.pojo.entity.FinanceProject;
import com.wimoor.erp.purchase.pojo.entity.PurchaseFormPaymentMethod;

public interface IFaccountService extends IService<FinAccount> {

	BigDecimal getSummary(UserInfo currUser);

	public List<FinanceProject> findProList(String shopid, String search);

	public FinAccount readFinAccount(String shopid,Integer paymethod,String name);

	public void updateFinAfterChange(FinAccount account, String projectid, Date createtime, BigDecimal amount,String ftype);

	public void updateFinCancelChange(FinAccount account, String projectid, Date createtime, BigDecimal amount, String ftype);
	
	public List<PurchaseFormPaymentMethod> findPurchasePayMethod(String shopid);

	public List<FinAccount> findPayAccountByMethod(String paymethod, String shopid);

	public FinAccount getAccByMeth(String shopid,String paymethod);

	public List<FinAccount> findAccountAll(String shopid);

	public Boolean saveAccount(FinAccount fin);
	
	public List<FinAccount> findAccountArchiveAll(String shopid);

	void savePaymethodIndex(UserInfo currUser,List<Map<String, Object>> indexlist);

	void saveAccountIndex(UserInfo currUser,List<Map<String, Object>> indexlist);

    // ==================== 台账Feign接口 ====================
    
    /**
     * 进销存台账汇总（台账用）
     */
    List<Map<String, Object>> getInventoryLedgerSummary(Map<String, Object> params);

    /**
     * 进销存台账明细（台账用）
     */
    List<Map<String, Object>> getInventoryLedgerDetail(Map<String, Object> params);

    /**
     * 进销存台账明细总数
     */
    long getInventoryLedgerDetailCount(Map<String, Object> params);
}
