<template>
    <div>
        <div class="con-header">
		<el-row>
			<el-space>
			<el-input  v-model="queryParams.search" @clear="handleQuery" placeholder="请输入订单编号" class="input-with-select" clearable>
			    <template #append>
			        <el-button @click="handleQuery">
			            <el-icon style="font-size: 16px;align-items:center">
			                <search />
			            </el-icon>
			        </el-button>
			    </template>
			</el-input>
			<el-input  v-model="queryParams.remark" @clear="handleQuery" placeholder="请输入备注" class="input-with-select" clearable>
			    <template #append>
			        <el-button @click="handleQuery">
			            <el-icon style="font-size: 16px;align-items:center">
			                <search />
			            </el-icon>
			        </el-button>
			    </template>
			</el-input>
			<el-button @click="handleExport" :loading="exporting">
				<el-icon><Download /></el-icon>导出
			</el-button>
			</el-space>
		</el-row>
        </div>
        <el-row>
			<GlobalTable ref="globalTable"
			 :tableData="tableData"  height="calc(100vh - 350px)" 
			 :defaultSort="{ prop: 'paytime', order: 'descending' }"  @loadTable="loadTableData" :stripe="true"  
			 style="width: 100%;margin-bottom:16px;">
				<template #field>
			<el-table-column prop="number" label="订单编号" width="220" sortable="custom" show-overflow-tooltip>
				<template #default="scope">
					<div>{{scope.row.number}}
					<el-tag v-if="scope.row.paystatus==1" type="success">已付款</el-tag>
					<el-tag v-if="scope.row.paystatus==0" type="info">未付款</el-tag></div>
					<div class='font-extraSmall'>{{scope.row.wname}}</div>
				</template>
			</el-table-column>
			<el-table-column prop="image" label="图片" width="65">
				<template #default="scope">
					<el-image :src="scope.row.image" style="width:40px;height:40px;"></el-image>
				</template>
			</el-table-column>
			<el-table-column prop="sku" label="名称/SKU" sortable="custom" show-overflow-tooltip>
				<template #default="scope">
					<div class='mname'>{{scope.row.mname}}</div>
					<div class='sku'>{{scope.row.sku}} <span v-if="scope.row.groupname" class="font-extraSmall">[{{scope.row.groupname}}]</span></div>
				</template>
			</el-table-column>
			<el-table-column prop="cname" width="220" label="供应商" sortable="custom">
				<template #default="scope">
					<div>{{scope.row.cname}}</div>
					<div class='font-extraSmall' v-if="scope.row.payment_method">付款方式:{{scope.row.payment_method}}</div>
				</template>
			</el-table-column>
			<el-table-column prop="purchases" width="120" label="采购数量" sortable="custom">
				<template #default="scope">
					<div>{{scope.row.purchases}}</div>
					<div class='font-extraSmall' v-if="scope.row.totalin">入库:{{scope.row.totalin}}</div>
				</template>
			</el-table-column>
			<el-table-column prop="orderprice" width="120" label="采购金额" sortable="custom">
				<template #default="scope">
					<div>{{scope.row.orderprice}}</div>
					<div class='font-extraSmall' v-if="scope.row.totalpay">已付:{{scope.row.totalpay}}</div>
				</template>
			</el-table-column>
			<el-table-column prop="fee_type" width="180" label="费用类型" sortable="custom">
				<template #default="scope">
					<div>{{scope.row.fee_type}}</div>
					<div class='font-extraSmall'><span>付款日期:{{dateFormat(scope.row.opttime)}}</span></div>
				</template>
			</el-table-column>
			<el-table-column prop="payprice" width="150" label="付款金额" sortable="custom">
				<template #default="scope">
					<div v-if="scope.row.payprice<0" class="text-red">{{scope.row.payprice}}:退款</div>
					<div v-else>￥{{scope.row.payprice}}</div>
					<div class='font-extraSmall'>操作人:{{scope.row.name}}</div>
				</template>
			</el-table-column>
			<el-table-column prop="remark" label="备注" sortable="custom" />
		</template>
			</GlobalTable>
        </el-row>
    </div>
</template>
<script setup>
    import { ref,reactive,toRefs} from 'vue'
    import {Search,Download} from '@element-plus/icons-vue'
	import {dateFormat} from '@/utils/index.js';
    import purchaseAlibabaSettlementApi from '@/api/erp/finances/purchaseAlibabaSettlementApi.js';
    import downloadhandler from '@/utils/download-handler.js';
	 
	 let globalTable=ref();
	 const state = reactive({
	 		tableData:{records:[],total:0},
	 		queryParams:{
	 			search:"",
	 		},
			exporting: false,
	 });
	 const { queryParams,tableData,exporting } = toRefs(state);
	  function handleQuery(){
	 	  globalTable.value.loadTable(state.queryParams);
	  }
	  function handleExport(){
			state.exporting = true;
			purchaseAlibabaSettlementApi.exportUnsettledList(state.queryParams, () => {
				state.exporting = false;
			});
	  }
	  function show(param){
		  state.queryParams=param;
		  handleQuery();
	  }
	   defineExpose({ show});
	  function loadTableData(params){
	  		  purchaseAlibabaSettlementApi.unsettledList(params).then(res=>{
	  				 state.tableData.records=res.data.records;
	  				 state.tableData.total=res.data.total;
	  		  })
	  }
</script>
<style>
</style>
