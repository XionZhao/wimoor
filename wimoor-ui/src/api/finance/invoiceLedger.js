import request from "@/utils/request.js";

// 获取发票列表
export function getInvoiceList(params) {
  return request.get('/finance/api/v1/invoice/list', { params });
}

// 获取发票统计
export function getInvoiceStatistics(params) {
  return request.get('/finance/api/v1/invoice/statistics', { params });
}

// 获取发票详情
export function getInvoiceDetail(params) {
  return request.get('/finance/api/v1/invoice/detail', { params });
}

// 获取关联单据
export function getInvoiceRelations(params) {
  return request.get('/finance/api/v1/invoice/relations', { params });
}

// 同步发票（从税局API）
export function syncInvoices(data) {
  return request.post('/finance/api/v1/invoice/sync', data);
}

// 导入发票（手动）
export function importInvoices(data) {
  return request.post('/finance/api/v1/invoice/import', data, {
    headers: { 'Content-Type': 'multipart/form-data' }
  });
}

// JSON批量导入发票（前端解析税控文件后调用，按页签分组发送）
export function importInvoicesFromJson(sheets, groupid) {
  return request.post('/finance/api/v1/invoice/importJson', { sheets, groupid }, {
    headers: { 'Content-Type': 'application/json;charset=utf-8' }
  });
}

// 发票入账（生成凭证）
export function postingInvoices(data) {
  return request.post('/finance/api/v1/invoice/posting', data);
}

// 新增发票
export function createInvoice(data) {
  return request.post('/finance/api/v1/invoice/create', data);
}

// 新增发票明细行
export function createInvoiceDetail(data) {
  return request.post('/finance/api/v1/invoice/detail/create', data);
}

// 导出
export function exportInvoices(params) {
  return request.get('/finance/api/v1/invoice/export', { params, responseType: 'blob' });
}

// 批量匹配供应商（修复历史数据）
export function matchSupplier() {
  return request.post('/finance/api/v1/invoice/matchSupplier');
}

// 批量匹配承运商（修复历史数据）
export function matchCarrier() {
  return request.post('/finance/api/v1/invoice/matchCarrier');
}
