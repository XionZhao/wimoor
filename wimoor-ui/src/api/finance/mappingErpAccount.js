import request from '@/utils/request'

// 查询账户映射规则列表
export function listMappingErpAccount(query) {
  return request({
    url: '/api/finance/mappingErpAccount/list',
    method: 'get',
    params: query
  })
}

// 查询账户映射规则详细
export function getMappingErpAccount(id) {
  return request({
    url: '/api/finance/mappingErpAccount/' + id,
    method: 'get'
  })
}

// 新增账户映射规则
export function addMappingErpAccount(data) {
  return request({
    url: '/api/finance/mappingErpAccount',
    method: 'post',
    data: data
  })
}

// 修改账户映射规则
export function updateMappingErpAccount(data) {
  return request({
    url: '/api/finance/mappingErpAccount',
    method: 'put',
    data: data
  })
}

// 删除账户映射规则
export function delMappingErpAccount(id) {
  return request({
    url: '/api/finance/mappingErpAccount/' + id,
    method: 'delete'
  })
}
