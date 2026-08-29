import request from '@/utils/request'

// 查询费用类型映射规则列表
export function listMappingErpFeetype(query) {
  return request({
    url: '/api/finance/mappingErpFeetype/list',
    method: 'get',
    params: query
  })
}

// 查询费用类型映射规则详细
export function getMappingErpFeetype(id) {
  return request({
    url: '/api/finance/mappingErpFeetype/' + id,
    method: 'get'
  })
}

// 新增费用类型映射规则
export function addMappingErpFeetype(data) {
  return request({
    url: '/api/finance/mappingErpFeetype',
    method: 'post',
    data: data
  })
}

// 修改费用类型映射规则
export function updateMappingErpFeetype(data) {
  return request({
    url: '/api/finance/mappingErpFeetype',
    method: 'put',
    data: data
  })
}

// 删除费用类型映射规则
export function delMappingErpFeetype(id) {
  return request({
    url: '/api/finance/mappingErpFeetype/' + id,
    method: 'delete'
  })
}
