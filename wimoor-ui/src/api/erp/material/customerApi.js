import request from "@/utils/request.js";
import downloadhandler from "@/utils/download-handler.js";
// function getMaterialList(data){
// 	 return request.get('/erp/api/v1/material',{params:data});
// }
// function uploadImage(FormData){
// 	return request({'method':'POST',
// 	                'url':"/erp/api/v1/material/uploadimg",
// 				    'data':FormData,
// 					'headers':{'Content-Type':"multipart/form-data"},
				
// 	});
// }

function getData(data){
	return request.get('/erp/api/v1/customer/getData',{params:data});
}
function saveData(data){
	return request.post('/erp/api/v1/customer/saveData',data);
}
function downloadCustomerList(data){
	return request({url:"/erp/api/v1/customer/downloadCustomerList",
				                    responseType:"blob",
									params:data,
									method:'get'}).then(res => {
											downloadhandler.downloadSuccess(res,"customerList.xlsx")
									}).catch(e=>{
											downloadhandler.downloadFail(e);
									}); 
}
function uploadCustomerFile(FormData){
	return request({'method':'POST',
	                 'url':"/erp/api/v1/customer/uploadCustomerFile",
				    'data':FormData,
					'headers':{'Content-Type':"multipart/form-data"},
				
	});
	
}
function list(data){
	return request.post('/erp/api/v1/customer/list',data);
}
function getCustomer(data){
	return request.get('/erp/api/v1/customer/getCustomer',{params:data});
}
function deletecust(data){
	return request.get('/erp/api/v1/customer/delete',{params:data});
}
function getSupplierByMid(data){
	return request.get('/erp/api/v1/customer/getSupplierByMid',{params:data});
}
function listAll(){
	return request.get('/erp/api/v1/customer/listAll');
}

// 收款账户相关接口
function getAccountList(data){
	return request.get('/erp/api/v1/customer/account/list',{params:data});
}
function saveAccount(data){
	return request.post('/erp/api/v1/customer/account/save',data);
}
function setDefaultAccount(data){
	return request.get('/erp/api/v1/customer/account/setDefault',{params:data});
}
function toggleAccountStatus(data){
	return request.get('/erp/api/v1/customer/account/toggleStatus',{params:data});
}
function deleteAccount(data){
	return request.get('/erp/api/v1/customer/account/delete',{params:data});
}
function downloadAccountList(data){
	return request({url:"/erp/api/v1/customer/account/downloadAccountList",
				                    responseType:"blob",
									params:data,
									method:'get'}).then(res => {
											downloadhandler.downloadSuccess(res,"accountList.xlsx")
									}).catch(e=>{
											downloadhandler.downloadFail(e);
									}); 
}
function uploadAccountFile(FormData){
	return request({'method':'POST',
	                 'url':"/erp/api/v1/customer/account/uploadAccountFile",
				    'data':FormData,
					'headers':{'Content-Type':"multipart/form-data"},
				
	});
	
}

// 采购产品汇总
function summaryProduct(data){
	return request.get('/erp/api/v1/customer/summaryProduct',{params:data});
}

export default{
	 getData,saveData,downloadCustomerList,list,getCustomer,deletecust,uploadCustomerFile,getSupplierByMid,listAll,
	 getAccountList,saveAccount,setDefaultAccount,toggleAccountStatus,deleteAccount,downloadAccountList,uploadAccountFile,summaryProduct
}