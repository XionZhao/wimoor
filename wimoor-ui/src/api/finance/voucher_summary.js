import request from '@/utils/request'

// 查询凭证摘要列表
export function listVoucherSummary(query) {
  return request({
    url: '/api/finance/summary/list',
    method: 'get',
    params: query
  })
}

// 查询凭证摘要详细
export function getVoucherSummary(id) {
  return request({
    url: '/api/finance/summary/' + id,
    method: 'get'
  })
}

// 新增凭证摘要
export function addVoucherSummary(data) {
  return request({
    url: '/api/finance/summary',
    method: 'post',
    data: data
  })
}

// 修改凭证摘要
export function updateVoucherSummary(data) {
  return request({
    url: '/api/finance/summary',
    method: 'put',
    data: data
  })
}

// 删除凭证摘要
export function delVoucherSummary(ids) {
  return request({
    url: '/api/finance/summary/' + ids,
    method: 'delete'
  })
}
