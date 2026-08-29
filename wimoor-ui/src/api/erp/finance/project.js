import request from '@/utils/request'

// 获取费用类型列表
export function getProject() {
  return request({
    url: '/erp/api/v1/fin/project/getProject',
    method: 'get'
  })
}

// 获取采购账户列表
export function getAccountAll() {
  return request({
    url: '/erp/api/v1/faccount/getAccountAll',
    method: 'get'
  })
}
