import request from "@/utils/request.js";

// 获取进销存台账汇总账
export function getInventoryLedgerSummary(params) {
  return request.get('/finance/api/v1/inventory/ledger/summary', { params });
}

// 获取进销存台账明细账
export function getInventoryLedgerDetail(params) {
  return request.get('/finance/api/v1/inventory/ledger/detail', { params });
}

// 获取勾稽校验结果
export function getInventoryLedgerCheck(params) {
  return request.get('/finance/api/v1/inventory/ledger/check', { params });
}

// 获取趋势图数据
export function getInventoryLedgerChart(params) {
  return request.get('/finance/api/v1/inventory/ledger/chart', { params });
}

// 批量生成凭证
export function batchGenerateVoucher(data) {
  return request.post('/finance/api/v1/inventory/ledger/voucher', data);
}

// 导出
export function exportInventoryLedger(params) {
  return request.get('/finance/api/v1/inventory/ledger/export', { params, responseType: 'blob' });
}
