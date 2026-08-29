import request from "@/utils/request.js";

function syncGoodsList(data){ 
	 return request({
		 url: '/temu/api/v1/temu/goods/sync',
		 method: 'post',
		 data: data,
	   });
}

function listGoods(params){ 
	 return request({
		 url: '/temu/api/v1/temu/goods/list',
		 method: 'get',
		 params: params,
	   });
}

function listSku(params){ 
	 return request({
		 url: '/temu/api/v1/temu/goods/sku/list',
		 method: 'get',
		 params: params,
	   });
}

export default{
	syncGoodsList,
	listGoods,
	listSku
}