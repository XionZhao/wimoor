import request from '@/utils/request'

// 查询存货映射规则列表
export function listMappingErpInventory(query) {
  return request({
    url: '/api/finance/mappingErpInventory/list',
    method: 'get',
    params: query
  })
}

// 查询存货映射规则详细
export function getMappingErpInventory(id) {
  return request({
    url: '/api/finance/mappingErpInventory/' + id,
    method: 'get'
  })
}

// 新增存货映射规则
export function addMappingErpInventory(data) {
  return request({
    url: '/api/finance/mappingErpInventory',
    method: 'post',
    data: data
  })
}

// 修改存货映射规则
export function updateMappingErpInventory(data) {
  return request({
    url: '/api/finance/mappingErpInventory',
    method: 'put',
    data: data
  })
}

// 删除存货映射规则
export function delMappingErpInventory(id) {
  return request({
    url: '/api/finance/mappingErpInventory/' + id,
    method: 'delete'
  })
}
