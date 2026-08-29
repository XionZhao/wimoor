import request from "@/utils/request.js";

// 获取供应商台账汇总
export function getSupplierLedgerSummary(params) {
  return request.get('/finance/api/v1/supplier/ledger/summary', { params });
}

// 获取供应商台账统计
export function getSupplierLedgerStatistics(params) {
  return request.get('/finance/api/v1/supplier/ledger/statistics', { params });
}

// 获取供应商订单明细
export function getSupplierLedgerOrders(params) {
  return request.get('/finance/api/v1/supplier/ledger/orders', { params });
}

// 获取供应商付款明细
export function getSupplierLedgerPayments(params) {
  return request.get('/finance/api/v1/supplier/ledger/payments', { params });
}

// 获取供应商发票明细
export function getSupplierLedgerInvoices(params) {
  return request.get('/finance/api/v1/supplier/ledger/invoices', { params });
}

// 对账操作
export function reconcileSupplier(data) {
  return request.post('/finance/api/v1/supplier/ledger/reconcile', data);
}

// 获取对账详情（点击最后对账日期弹窗展示）
export function getSupplierReconcileDetail(params) {
  return request.get('/finance/api/v1/supplier/ledger/reconcile/detail', { params });
}

// 导出未开票订单
export function exportUninvoicedOrders(params) {
  return request.get('/finance/api/v1/supplier/ledger/export', { params, responseType: 'blob' });
}

// 修复发票数据（匹配supplier_id）
export function matchInvoiceSupplier() {
  return request.post('/finance/api/v1/invoice/matchSupplier');
}
