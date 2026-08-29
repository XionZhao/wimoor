<template>
    <div>
        <div class="con-header">
		<el-row>
			<el-space>
		<div class="date-picker-group">
			<el-date-picker
				v-model="dateValue"
				type="daterange"
				:clearable="false"
				range-separator="至"
				start-placeholder="开始日期"
				end-placeholder="结束日期"
				:shortcuts="shortcuts"
				@change="dateChange"
			/>
		</div>
		</el-space>
		</el-row>
        </div>
        <el-row :gutter="16">
			<el-col :span="10">
				<GlobalTable ref="globalTable"
				 :tableData="tableData"  height="calc(100vh - 350px)" 
				 :defaultSort="{ prop: 'id', order: 'ascending' }"  @loadTable="loadTableData"
				 highlight-current-row @row-click="handleRowClick"
				 style="width: 100%;margin-bottom:16px;">
					<template #field>
				<el-table-column prop="id" label="结转单号" width="80" show-overflow-tooltip>
					<template #default="scope">
						<div>{{scope.$index + 1}}</div>
					</template>
				</el-table-column>
				<el-table-column prop="relation_count" label="关联订单" width="80" align="center">
					<template #default="scope">
						<el-tag type="info" size="small">{{scope.row.relation_count || 0}}</el-tag>
					</template>
				</el-table-column>
				<el-table-column prop="total_amount" label="结转总金额" width="120" align="right">
					<template #default="scope">
						<div class="text-primary">￥{{formatMoney(scope.row.total_amount)}}</div>
					</template>
				</el-table-column>
				<el-table-column prop="opttime" label="操作时间" width="160" sortable="custom">
					<template #default="scope">
						<div>{{scope.row.opttime}}</div>
					</template>
				</el-table-column>
				<el-table-column label="操作" width="100" fixed="right">
					<template #default="scope">
						<el-button type="danger" link @click.stop="handleCancel(scope.row)">撤销</el-button>
					</template>
				</el-table-column>
			</template>
				</GlobalTable>
			</el-col>
			<el-col :span="14">
				<div class="detail-panel" v-loading="detailLoading">
					<div class="detail-header" v-if="currentRow">
						<span>结转单号：{{currentRowIndex + 1}}</span>
						<span style="margin-left: 20px;">结转总金额：<span class="text-primary">￥{{formatMoney(currentRow.total_amount)}}</span></span>
						<span style="margin-left: 20px;">操作人：{{currentRow.operator_name}}</span>
						<span style="margin-left: 20px;">操作时间：{{currentRow.opttime}}</span>
					</div>
					<el-table :data="detailData" border stripe max-height="calc(100vh - 400px)">
						<el-table-column prop="number" label="订单编号" width="180" show-overflow-tooltip>
							<template #default="scope">
								<div>{{scope.row.number}}</div>
							</template>
						</el-table-column>
						<el-table-column prop="image" label="图片" width="55">
							<template #default="scope">
								<el-image v-if="scope.row.image" :src="scope.row.image" style="width:35px;height:35px;"></el-image>
							</template>
						</el-table-column>
						<el-table-column prop="sku" label="名称/SKU" show-overflow-tooltip>
							<template #default="scope">
								<div class='mname'>{{scope.row.mname}}</div>
								<div class='sku'>{{scope.row.sku}} <span v-if="scope.row.groupname" class="font-extraSmall">[{{scope.row.groupname}}]</span></div>
							</template>
						</el-table-column>
						<el-table-column prop="cname" width="150" label="供应商">
							<template #default="scope">
								<div>{{scope.row.cname}}</div>
							</template>
						</el-table-column>
						<el-table-column prop="orderprice" width="100" label="采购金额" align="right">
							<template #default="scope">
								<div>{{scope.row.orderprice}}</div>
							</template>
						</el-table-column>
						<el-table-column prop="fee_type" width="100" label="费用类型">
							<template #default="scope">
								<div>{{scope.row.fee_type}}</div>
							</template>
						</el-table-column>
						<el-table-column prop="payprice" width="100" label="付款金额" align="right">
							<template #default="scope">
								<div v-if="scope.row.payprice<0" class="text-red">{{scope.row.payprice}}:退款</div>
								<div v-else>￥{{scope.row.payprice}}</div>
							</template>
						</el-table-column>
						<el-table-column prop="operator_name" width="80" label="操作人">
							<template #default="scope">
								<div>{{scope.row.operator_name}}</div>
							</template>
						</el-table-column>
						<el-table-column prop="remark" label="备注" show-overflow-tooltip />
					</el-table>
					<el-empty v-if="!detailLoading && detailData.length === 0" description="请选择一条结转记录查看详情" />
				</div>
			</el-col>
        </el-row>
    </div>
</template>
<script setup>
    import { ref,reactive,toRefs,onMounted,nextTick} from 'vue'
    import { ElMessage, ElMessageBox } from 'element-plus'
    import { dateFormat } from '@/utils/index.js'
    import purchaseAlibabaSettlementApi from '@/api/erp/finances/purchaseAlibabaSettlementApi.js';
	 
	 let globalTable=ref();
	 const dateValue = ref([]);
	 const shortcuts = [
	   {
	     text: '近1个月',
	     value: () => {
	       const end = new Date()
	       const start = new Date()
	       start.setTime(start.getTime() - 3600 * 1000 * 24 * 30)
	       return [start, end]
	     },
	   },
	   {
	     text: '近3个月',
	     value: () => {
	       const end = new Date()
	       const start = new Date()
	       start.setTime(start.getTime() - 3600 * 1000 * 24 * 90)
	       return [start, end]
	     },
	   },
	   {
	     text: '近6个月',
	     value: () => {
	       const end = new Date()
	       const start = new Date()
	       start.setTime(start.getTime() - 3600 * 1000 * 24 * 180)
	       return [start, end]
	     },
	   },
	   {
	     text: '近1年',
	     value: () => {
	       const end = new Date()
	       const start = new Date()
	       start.setTime(start.getTime() - 3600 * 1000 * 24 * 365)
	       return [start, end]
	     },
	   },
	 ];
	 const state = reactive({
	 		tableData:{records:[],total:0},
	 		queryParams:{
	 			acct:"",
	 			datetype:"paydate",
	 		},
			detailData: [],
			detailLoading: false,
			currentRow: null,
			currentRowIndex: 0,
	 });
	 const { queryParams,tableData,detailData,detailLoading,currentRow,currentRowIndex } = toRefs(state);
	 
	 function formatMoney(val) {
	 	if (!val && val !== 0) return '0.00'
	 	return Number(val).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
	 }
	 
	  function dateChange(val){
		  if(val && val.length === 2){
			  state.queryParams.fromDate = dateFormat(val[0]);
			  state.queryParams.toDate = dateFormat(val[1]) + " 23:59:59";
			  handleQuery();
		  }
	  }
	  function handleQuery(){
	 	  globalTable.value.loadTable(state.queryParams);
	  }
	  function show(param){
		  Object.assign(state.queryParams, param);
		  handleQuery();
	  }
	  onMounted(()=>{
		  dateValue.value = shortcuts[0].value();
		  state.queryParams.fromDate = dateFormat(dateValue.value[0]);
		  state.queryParams.toDate = dateFormat(dateValue.value[1]) + " 23:59:59";
	  })
	  
	  function loadTableData(params){
  		  purchaseAlibabaSettlementApi.rolloverList(params).then(res=>{
  			  state.tableData.records=res.data.records || [];
  			  state.tableData.total=res.data.total || 0;
  			  // 默认选中第一行
  			  nextTick(() => {
  				  if(state.tableData.records.length > 0) {
  					  selectFirstRow();
  				  }
  			  });
  		  })
	  }
	  
	  // 选中第一行
	  function selectFirstRow(){
		  if(globalTable.value && state.tableData.records.length > 0) {
			  const firstRow = state.tableData.records[0];
			  globalTable.value.setCurrentRow(firstRow);
			  handleRowClick(firstRow);
		  }
	  }
	  
	  // 点击行查看详情
	  function handleRowClick(row){
		  if(!row) return;
		  state.currentRow = row;
		  state.currentRowIndex = state.tableData.records.indexOf(row);
		  state.detailLoading = true;
		  purchaseAlibabaSettlementApi.rolloverDetail({ rolloverId: row.id }).then(res=>{
			  state.detailData = res.data || [];
			  state.detailLoading = false;
		  }).catch(() => {
			  state.detailLoading = false;
		  })
	  }
	  
	  // 撤销结转
	  function handleCancel(row){
		  ElMessageBox.confirm(`确定要撤销结转单吗？撤销后相关付款记录将恢复为未结转状态。`, {
			  confirmButtonText: '确认',
			  cancelButtonText: '取消',
			  type: 'warning',
		  }).then(() => {
			  purchaseAlibabaSettlementApi.cancelRollover({ rolloverId: row.id }).then(res=>{
				  ElMessage.success('撤销成功')
				  handleQuery();
				  emit('refresh');
			  })
		  }).catch(() => {})
	  }
	  
	  const emit = defineEmits(['refresh']);
	  defineExpose({ show});
</script>
<style scoped>
.date-picker-group {
	display: flex;
	align-items: center;
}
.text-primary {
	color: #409eff;
}
.text-red {
	color: #f56c6c;
}
.font-extraSmall {
	font-size: 12px;
	color: #909399;
}
.mname {
	font-weight: 500;
}
.sku {
	font-size: 12px;
	color: #909399;
}
.detail-panel {
	border: 1px solid #e4e7ed;
	border-radius: 4px;
	padding: 12px;
	height: calc(100vh - 350px);
	overflow: auto;
}
.detail-header {
	padding: 8px 0 12px;
	border-bottom: 1px solid #e4e7ed;
	margin-bottom: 12px;
	font-size: 14px;
}
</style>
