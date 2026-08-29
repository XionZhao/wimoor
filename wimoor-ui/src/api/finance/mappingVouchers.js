import request from '@/utils/request'

// 查询映射凭证关联列表
export function listMappingVouchers(query) {
  return request({
    url: '/api/finance/mappingVouchers/list',
    method: 'get',
    params: query
  })
}

// 查询映射凭证关联详情
export function getMappingVouchers(id) {
  return request({
    url: '/api/finance/mappingVouchers/' + id,
    method: 'get'
  })
}

// 新增映射凭证关联
export function addMappingVouchers(data) {
  return request({
    url: '/api/finance/mappingVouchers',
    method: 'post',
    data: data
  })
}

// 修改映射凭证关联
export function updateMappingVouchers(data) {
  return request({
    url: '/api/finance/mappingVouchers',
    method: 'put',
    data: data
  })
}

// 删除映射凭证关联
export function delMappingVouchers(id) {
  return request({
    url: '/api/finance/mappingVouchers/' + id,
    method: 'delete'
  })
}