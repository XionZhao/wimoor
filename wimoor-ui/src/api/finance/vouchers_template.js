import request from '@/utils/request'

// ==================== 凭证模版分类 ====================

// 查询凭证模版分类列表
export function listTemplateType(query) {
  return request({
    url: '/api/finance/voucher-template-type/list',
    method: 'get',
    params: query
  })
}

// 查询所有凭证模版分类（不分页）
export function listAllTemplateType(query) {
  return request({
    url: '/api/finance/voucher-template-type/all',
    method: 'get',
    params: query
  })
}

// 查询凭证模版分类详细
export function getTemplateType(id) {
  return request({
    url: '/api/finance/voucher-template-type/' + id,
    method: 'get'
  })
}

// 新增凭证模版分类
export function addTemplateType(data) {
  return request({
    url: '/api/finance/voucher-template-type',
    method: 'post',
    data: data
  })
}

// 修改凭证模版分类
export function updateTemplateType(data) {
  return request({
    url: '/api/finance/voucher-template-type',
    method: 'put',
    data: data
  })
}

// 删除凭证模版分类
export function delTemplateType(ids) {
  return request({
    url: '/api/finance/voucher-template-type/' + ids,
    method: 'delete'
  })
}

// ==================== 凭证模版 ====================

// 查询凭证模版列表
export function listTemplate(query) {
  return request({
    url: '/api/finance/voucher-template/list',
    method: 'get',
    params: query
  })
}

// 查询凭证模版详细（包含分录信息）
export function getTemplate(id) {
  return request({
    url: '/api/finance/voucher-template/' + id,
    method: 'get'
  })
}

// 新增凭证模版
export function addTemplate(data) {
  return request({
    url: '/api/finance/voucher-template',
    method: 'post',
    data: data
  })
}

// 修改凭证模版
export function updateTemplate(data) {
  return request({
    url: '/api/finance/voucher-template',
    method: 'put',
    data: data
  })
}

// 删除凭证模版
export function delTemplate(ids) {
  return request({
    url: '/api/finance/voucher-template/' + ids,
    method: 'delete'
  })
}

// 获取凭证模版详情（包含所有分录和辅助核算信息）
export function getTemplateDetail(id) {
  return request({
    url: '/api/finance/voucher-template/detail/' + id,
    method: 'get'
  })
}
