import request from "@/utils/request.js";

// 获取采购账户台账列表
export function getPurchaseLedgerList(params) {
  return request.get('/finance/api/v1/purchase/ledger/list', { params });
}

// 获取采购账户台账统计
export function getPurchaseLedgerStatistics(params) {
  return request.get('/finance/api/v1/purchase/ledger/statistics', { params });
}

// 获取采购账户卡片数据（带余额统计）
export function getPurchaseLedgerAccounts(params) {
  return request.get('/finance/api/v1/purchase/ledger/accounts', { params });
}

// 获取付款明细
export function getPurchaseLedgerPayments(params) {
  return request.get('/finance/api/v1/purchase/ledger/payments', { params });
}

// 台账付款
export function payPurchaseLedger(data) {
  return request.post('/finance/api/v1/purchase/ledger/pay', data);
}

// 批量上传对账单
export function uploadPurchaseLedgerReconcile(data) {
  return request.post('/finance/api/v1/purchase/ledger/upload', data, {
    headers: { 'Content-Type': 'multipart/form-data' }
  });
}

// 对账操作
export function reconcilePurchaseLedger(data) {
  return request.post('/finance/api/v1/purchase/ledger/reconcile', data);
}

// 导出
export function exportPurchaseLedger(params) {
  return request.get('/finance/api/v1/purchase/ledger/export', { params, responseType: 'blob' });
}
