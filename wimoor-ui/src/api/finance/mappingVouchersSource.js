import request from '@/utils/request'

// 查询ERP订单凭证生成记录列表（分页）
export function listMappingVouchersSource(params) {
  return request({
    url: '/finance/api/mappingVouchersSource/list',
    method: 'get',
    params: params
  })
}

// 查询ERP订单凭证生成记录详情
export function getMappingVouchersSource(id) {
  return request({
    url: '/finance/api/mappingVouchersSource/' + id,
    method: 'get'
  })
}

// 按日期区间批量生成采购付款凭证
export function generateVoucherByDateRange(data) {
  return request({
    url: '/finance/api/mappingVouchersSource/generateByDateRange',
    method: 'post',
    params: data
  })
}

// 查询订单的付款记录明细
export function getPaymentRecords(params) {
  return request({
    url: '/finance/api/mappingVouchersSource/paymentRecords',
    method: 'get',
    params: params
  })
}

// 按日期区间批量生成手动记账凭证
export function generateJournalVoucherByDateRange(data) {
  return request({
    url: '/finance/api/mappingVouchersSource/generateJournalByDateRange',
    method: 'post',
    params: data
  })
}

// 按日期区间批量生成采购入库凭证
export function generateInventoryVoucherByDateRange(data) {
  return request({
    url: '/finance/api/mappingVouchersSource/generateInventoryByDateRange',
    method: 'post',
    params: data
  })
}

// 按日期区间批量生成发票凭证
export function generateInvoiceVoucherByDateRange(data) {
  return request({
    url: '/finance/api/mappingVouchersSource/generateInvoiceByDateRange',
    method: 'post',
    params: data
  })
}

// 删除ERP订单凭证生成记录
export function deleteMappingVouchersSource(id) {
  return request({
    url: '/finance/api/mappingVouchersSource/' + id,
    method: 'delete'
  })
}

// 批量删除ERP订单凭证生成记录
export function deleteMappingVouchersSourceByIds(ids) {
  return request({
    url: '/finance/api/mappingVouchersSource/deleteBatch/' + ids.join(','),
    method: 'delete'
  })
}
