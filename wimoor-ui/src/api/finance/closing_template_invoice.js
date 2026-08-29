import request from '@/utils/request'

// 查询发票凭证映射模版列表
export function listClosingTemplateInvoice(query) {
  return request({
    url: '/api/finance/closing_template_invoice/list',
    method: 'get',
    params: query
  })
}

// 查询发票凭证映射模版详情
export function getClosingTemplateInvoice(id) {
  return request({
    url: '/api/finance/closing_template_invoice/' + id,
    method: 'get'
  })
}

// 新增发票凭证映射模版
export function addClosingTemplateInvoice(data) {
  return request({
    url: '/api/finance/closing_template_invoice',
    method: 'post',
    data: data
  })
}

// 修改发票凭证映射模版
export function updateClosingTemplateInvoice(data) {
  return request({
    url: '/api/finance/closing_template_invoice',
    method: 'put',
    data: data
  })
}

// 删除发票凭证映射模版
export function delClosingTemplateInvoice(ids) {
  return request({
    url: '/api/finance/closing_template_invoice/' + ids,
    method: 'delete'
  })
}

// 发票生成凭证
export function generateVoucherFromInvoice(data) {
  return request({
    url: '/api/finance/closing_template_invoice/generateVoucher',
    method: 'post',
    data: data
  })
}
