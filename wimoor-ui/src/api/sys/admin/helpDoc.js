import request from "@/utils/request.js";

/**
 * 根据页面路径获取帮助文档
 * @param {string} path 页面路径
 * @returns {Promise}
 */
export function getHelpDocByPath(path) {
  return request({
    url: '/admin/api/v1/help-doc/get-by-path',
    method: 'get',
    params: { path }
  });
}

/**
 * 根据关键词搜索帮助文档
 * @param {string} keyword 搜索关键词
 * @param {string} category 文档分类
 * @param {number} limit 返回数量限制
 * @returns {Promise}
 */
export function searchHelpDoc(keyword, category, limit) {
  return request({
    url: '/admin/api/v1/help-doc/search',
    method: 'get',
    params: { keyword, category, limit }
  });
}

/**
 * 根据文档标识获取帮助文档
 * @param {string} docKey 文档标识
 * @returns {Promise}
 */
export function getHelpDocByKey(docKey) {
  return request({
    url: '/admin/api/v1/help-doc/get',
    method: 'get',
    params: { docKey }
  });
}

/**
 * 获取帮助文档索引
 * @returns {Promise}
 */
export function getHelpDocIndex() {
  return request({
    url: '/admin/api/v1/help-doc/index',
    method: 'get'
  });
}
