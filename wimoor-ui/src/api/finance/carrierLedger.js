import request from "@/utils/request.js";

// 承运商对账操作
export function reconcileCarrier(data) {
  return request.post('/finance/api/v1/carrier/ledger/reconcile', data);
}

// 获取承运商对账详情
export function getCarrierReconcileDetail(params) {
  return request.get('/finance/api/v1/carrier/ledger/reconcile/detail', { params });
}

// 查询承运商台账汇总（按承运商维度）
export function getCarrierLedgerSummary(data) {
  return request.post('/finance/api/v1/carrier/ledger/summary', data);
}

// 查询承运商台账统计数据（全局汇总）
export function getCarrierLedgerStatistics(data) {
  return request.post('/finance/api/v1/carrier/ledger/statistics', data);
}
