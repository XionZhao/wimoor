import request from "@/utils/request.js";
import uploadhandler from "@/utils/upload-handler";
import downloadhandler from "@/utils/download-handler.js";
function uploadPayDateFile(FormData,callback){
	return request({'method':'POST',
	                 'url':"/erp/api/v1/purchase/alibaba/entry/purchaseAlibabaSettlement/uploadPayDate",
				    'data':FormData,
					'responseType':"blob",
					'headers':{'Content-Type':"multipart/form-data"},
	}).then(function(res){
		  	  uploadhandler.uploadResult(res,(response)=>{
				 if(callback){
				 	 callback(response);
				 }
		  	  });
		  	 
		  }).catch(e=>{
		  	  uploadhandler.uploadResult(e,(response)=>{
				  if(callback){
				  	  callback(response);
				  }
			  });
			   
	  });
}
 function list(data){
 	return request.post('/erp/api/v1/purchase/alibaba/entry/purchaseAlibabaSettlement/list',data);
 }
 function orderList(data){
 	return request.post('/erp/api/v1/purchase/alibaba/entry/purchaseAlibabaSettlement/orderList',data);
 }
 function payList(data){
 	return request.post('/erp/api/v1/purchase/alibaba/entry/purchaseAlibabaSettlement/payList',data);
 }
 function returnPayList(data){
 	return request.post('/erp/api/v1/purchase/alibaba/entry/purchaseAlibabaSettlement/returnPayList',data);
 }
 function orderReturnList(data){
 	return request.post('/erp/api/v1/purchase/alibaba/entry/purchaseAlibabaSettlement/orderReturnList',data);
 }
 function deleteSettlement(data){
 	return request.post('/erp/api/v1/purchase/alibaba/entry/purchaseAlibabaSettlement/delete',data);
 }
 function unsettledList(data){
	return request.post('/erp/api/v1/purchase/alibaba/entry/purchaseAlibabaSettlement/unsettledList',data);
}
function settledList(data){
	return request.post('/erp/api/v1/purchase/alibaba/entry/purchaseAlibabaSettlement/settledList',data);
}
function summary(data){
	return request.post('/erp/api/v1/purchase/alibaba/entry/purchaseAlibabaSettlement/summary',data);
}
function orderSummary(data){
	return request.post('/erp/api/v1/purchase/alibaba/entry/purchaseAlibabaSettlement/orderSummary',data);
}
function matchedOrders(data){
	return request.post('/erp/api/v1/purchase/alibaba/entry/purchaseAlibabaSettlement/matchedOrders',data);
}
function exportMatchedOrders(data, callback){
	return request({
		url: '/erp/api/v1/purchase/alibaba/entry/purchaseAlibabaSettlement/exportMatchedOrders',
		responseType: 'blob',
		data: data,
		method: 'post'
	}).then(res => {
		downloadhandler.downloadSuccess(res, "已结转订单.xlsx");
		if(callback) callback();
	}).catch(e => {
		downloadhandler.downloadFail(e);
		if(callback) callback(e);
	});
}
function exportUnsettledList(data, callback){
	return request({
		url: '/erp/api/v1/purchase/alibaba/entry/purchaseAlibabaSettlement/exportUnsettledList',
		responseType: 'blob',
		data: data,
		method: 'post'
	}).then(res => {
		downloadhandler.downloadSuccess(res, "未结转订单.xlsx");
		if(callback) callback();
	}).catch(e => {
		downloadhandler.downloadFail(e);
		if(callback) callback(e);
	});
}
function settle(data){
	return request.post('/erp/api/v1/purchase/alibaba/entry/purchaseAlibabaSettlement/settle',data);
}
function rolloverList(data){
	return request.post('/erp/api/v1/purchase/alibaba/entry/purchaseAlibabaSettlement/rolloverList',data);
}
function rolloverDetail(data){
	return request.post('/erp/api/v1/purchase/alibaba/entry/purchaseAlibabaSettlement/rolloverDetail',data);
}
function cancelRollover(data){
	return request.post('/erp/api/v1/purchase/alibaba/entry/purchaseAlibabaSettlement/cancelRollover',data);
}
function getAllUnsettledIds(data){
	return request.post('/erp/api/v1/purchase/alibaba/entry/purchaseAlibabaSettlement/getAllUnsettledIds',data);
}
function settleAll(data){
	return request.post('/erp/api/v1/purchase/alibaba/entry/purchaseAlibabaSettlement/settleAll',data);
}
function importSettle(FormData,callback){
	return request({'method':'POST',
	                 'url':"/erp/api/v1/purchase/alibaba/entry/purchaseAlibabaSettlement/importSettle",
				    'data':FormData,
					'headers':{'Content-Type':"multipart/form-data"},
	}).then(function(res){
		 if(callback){
			 callback(res);
		 }
	  }).catch(e=>{
		 console.error('导入失败',e);
	  });
}
export default{
	uploadPayDateFile,list,orderList,payList,returnPayList,orderReturnList,deleteSettlement,unsettledList,settledList,summary,orderSummary,matchedOrders,exportMatchedOrders,exportUnsettledList,settle,importSettle,rolloverList,rolloverDetail,cancelRollover,getAllUnsettledIds,settleAll
}