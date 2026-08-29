import request from "@/utils/request.js";

// 获取绑定信息
function getBindInfo() {
  return request({
    url: "/admin/api/v1/feishu/getBindInfo",
    method: "get"
  });
}

// 保存绑定信息
function save(data) {
  return request({
    url: "/admin/api/v1/feishu/save",
    method: "post",
    data
  });
}

// 更新绑定信息
function update(data) {
  return request({
    url: "/admin/api/v1/feishu/update",
    method: "post",
    data
  });
}
function createDocx(data) {
  return request({
    url: "/admin/api/v1/feishu/docx/create",
    method: "post",
    data
  });
}
function createAppAndTables(data) {
  return request({
    url: "/admin/api/v1/feishu/appAndTables/create",
    method: "post",
    data
  });
}

// 获取表格字段
function getTableFields(tableUrl, tableType) {
  return request({
    url: "/admin/api/feishu/table/getFields",
    method: "post",
    data: { url: tableUrl,name: tableType }
  });
}

// 获取表格列表
function getTableList(data) {
  return request({
    url: "/admin/api/feishu/table/list",
    method: "get",
    params: data
  });
}
// 更新数据表类型
function updateTable(data) {
  return request({
    url: "/admin/api/feishu/table/update",
    method: "put",
    data
  });
}
function getTableInfo(data) {
  return request({
    url: "/admin/api/feishu/table/getTableInfo",
    method: "post",
    data
  });
}

// 获取数据表类型列表
function getTypeList() {
  return request({
    url: "/admin/api/feishu/table/typeList",
    method: "get"
  });
}
function getRecord(params) {
  return request({
    url: "/admin/api/feishu/table/getRecord",
    method: "get",
    params: {
      url: params.url,
      // 直接传递内层条件对象，后端处理外层包装
      filter: params.filter ? JSON.stringify(params.filter) : undefined
    }
  });
}
function updateCallback(data){
  return request({
    url: "/admin/api/feishu/table/updateCallback",
    method: "post",
    data:data
  });
}
// 获取数据表类型列表
function addCallback(data) {
  return request({
    url: "/admin/api/feishu/table/addCallback",
    method: "post",
    data:data
  });
}

// ========== 聊天记录相关接口 ==========

// 获取群组列表
function getChatGroups(appId) {
  return request({
    url: "/admin/api/v1/feishu/chat/groups",
    method: "get",
    params: { appId }
  });
}

// 获取群组详情
function getChatGroup(chatId) {
  return request({
    url: `/admin/api/v1/feishu/chat/group/${chatId}`,
    method: "get"
  });
}

// 获取群成员列表
function getChatMembers(chatId) {
  return request({
    url: "/admin/api/v1/feishu/chat/members",
    method: "get",
    params: { chatId }
  });
}

// 分页查询消息列表
function getChatMessages(params) {
  return request({
    url: "/admin/api/v1/feishu/chat/messages",
    method: "get",
    params
  });
}

// 分页查询文件列表
function getChatFiles(params) {
  return request({
    url: "/admin/api/v1/feishu/chat/files",
    method: "get",
    params
  });
}

// 获取消息关联的文件
function getMessageFiles(messageId) {
  return request({
    url: `/admin/api/v1/feishu/chat/files/${messageId}`,
    method: "get"
  });
}

// 下载文件
function downloadChatFile(fileId) {
  return request({
    url: `/admin/api/v1/feishu/chat/file/download/${fileId}`,
    method: "get",
    responseType: "blob"
  });
}

export default{
    getBindInfo,
    update,
    save,
    createDocx,
    createAppAndTables,
    getTableFields,
    getTableList,
    getTypeList,
    addCallback,
    getRecord,
    updateCallback,
    updateTable,
    getTableInfo,
    getChatGroups,
    getChatGroup,
    getChatMembers,
    getChatMessages,
    getChatFiles,
    getMessageFiles,
    downloadChatFile,
}