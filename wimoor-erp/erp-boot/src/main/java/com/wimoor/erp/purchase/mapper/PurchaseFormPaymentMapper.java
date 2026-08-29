package com.wimoor.erp.purchase.mapper;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wimoor.erp.purchase.pojo.entity.PurchaseFormPayment;
@Mapper
public interface PurchaseFormPaymentMapper  extends BaseMapper<PurchaseFormPayment> {
 
	  List<Map<String,Object>> paymentReport(@Param("param")Map<String, Object> param);
	  IPage<Map<String,Object>> paymentReport(Page<?> page,@Param("param") Map<String, Object> param);
	  Map<String,Object> paymentReportSummary(Map<String, Object> param);

	  // 已结转订单列表
	  List<Map<String,Object>> matchedOrders(@Param("param")Map<String, Object> param);
	  IPage<Map<String,Object>> matchedOrdersPage(Page<?> page,@Param("param") Map<String, Object> param);

	  // 未结转订单列表
	  List<Map<String,Object>> unsettledList(@Param("param")Map<String, Object> param);
	  IPage<Map<String,Object>> unsettledListPage(Page<?> page,@Param("param") Map<String, Object> param);

	  // 获取当前筛选条件下的未结转记录ID
	  List<String> getAllUnsettledIds(@Param("param") Map<String, Object> param);

	  // 按条件统计未结转记录数量（与paymentReport完全一致的WHERE条件）
	  int countUnsettledByCondition(@Param("param") Map<String, Object> param);

	  // 按条件获取未结转记录ID列表（与paymentReport完全一致的WHERE条件）
	  List<String> getUnsettledIdsByCondition(@Param("param") Map<String, Object> param);

	  // 根据订单编码和SKU获取未结转记录ID列表
	  List<String> getUnsettledIdsByOrderAndSku(@Param("param") Map<String, Object> param);
}