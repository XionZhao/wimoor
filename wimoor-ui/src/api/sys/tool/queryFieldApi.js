import request from "@/utils/request.js";

 
function getMyVersionFieldByUser(data){
	 return request.get('/admin/api/v1/sysQueryField/getMyVersionFieldByUser',{params:data});
}
function deleteMyVersionField(data){
	 return request.get('/admin/api/v1/sysQueryField/deleteMyVersionField',{params:data});
}
function saveMyVersionFieldWithName(data){
	 return request.post('/admin/api/v1/sysQueryField/saveMyVersionFieldWithName',data);
}
function loadfield(data){
	 return request.get('/admin/api/v1/sysQueryField/loadfield',{params:data});
}
function saveMyVersionField(queryname,data){
	 return request.post('/admin/api/v1/sysQueryField/saveMyVersionField/'+queryname,data);
}
function getVersionFieldByUserQueryName(data){
	 return request.get('/admin/api/v1/sysQueryField/getVersionFieldByUserQueryName',{params:data});
}
function getVersionFieldById(data){
	 return request.get('/admin/api/v1/sysQueryField/getVersionFieldById',{params:data});
}

// 管理员接口
function getQueryNames(){
	 return request.get('/admin/api/v1/sysQueryField/admin/getQueryNames');
}
function getFields(data){
	 return request.get('/admin/api/v1/sysQueryField/admin/getFields',{params:data});
}
function saveField(data){
	 return request.post('/admin/api/v1/sysQueryField/admin/saveField',data);
}
function deleteField(data){
	 return request.delete('/admin/api/v1/sysQueryField/admin/deleteField',{params:data});
}

export default{
	 getMyVersionFieldByUser,deleteMyVersionField,getVersionFieldById,
	 saveMyVersionFieldWithName,loadfield,saveMyVersionField,getVersionFieldByUserQueryName,
	 // 管理员接口
	 getQueryNames,getFields,saveField,deleteField,
}