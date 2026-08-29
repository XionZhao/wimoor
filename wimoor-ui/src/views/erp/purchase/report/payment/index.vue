<template>
    <div :class="inForm?'onclass':'main-sty'">
        <div class="con-header">
        <el-row>
            <el-space :size="6">
            				 <slot name="extra-filters"></slot>
            				 <GroupSelect  @change="changeGroup" defaultValue="all" />
				 <Warehouse    @changeware="wareChange" defaultValue="all" />
				  <Supplier  v-if="!hideSupplier" ref="supplierRef"   @change="changeSupplier" />
				  <div class="date-picker-group" >
				  	<el-select style="width:130px;" v-model="queryParams.datetype" @change="handleQuery">
				  		<el-option v-for="item in dateOptions" :label="item.label" :value="item.value"></el-option>
				  	</el-select>
				  	<Datepicker  longtime="ok"
				  	ref="datepickers"
				  	 @changedate="changedate" />
				  </div>
					<el-select v-if="!hideAcct" v-model="queryParams.acct" placeholder="付款账户" @change="handleQuery" style="width:140px;">
				 <el-option  v-for="item in payAccountList"   :key="item.id"  :label="item.name" :value="item.id"  ></el-option>
			</el-select>
					<el-select v-model="queryParams.projectid"  @change="handleQuery" placeholder="费用类型" clearable style="width:130px;">
						<el-option  v-for="item in finlist"   :key="item.id"  :label="item.name" :value="item.id"  ></el-option>
					</el-select>
                <el-input  v-model="queryParams.search" @clear="handleQuery" placeholder="请输入" class="input-with-select" clearable style="width:300px;">
                    <template #prepend>
                        <el-select v-model="queryParams.searchtype" @change='handleQuery'  placeholder="SKU" style="width: 110px">
                            <el-option label="SKU" value="sku"></el-option>
                            <el-option label="订单编号" value="number"></el-option>
                            <el-option label="付款备注" value="remark"></el-option>
							<el-option v-if="queryParams.settlementid" label="1688订单" value="orderid"></el-option>
                        </el-select>
                    </template>
                    <template #append>
                        <el-button @click="handleQuery">
                            <el-icon style="font-size: 16px;align-itmes:center">
                                <search />
                            </el-icon>
                        </el-button>
                    </template>
                </el-input>
            </el-space>
        </el-row>
		<el-row>
			<el-button @click="downLoadExcel">
			    <span>导出</span>
			</el-button>
			<div class='rt-btn-group'>
				<el-space :size="16">
				<span class="font-base"><span class="font-base-nine">付款金额总计:</span>￥{{totalamount}}</span>
			   </el-space>
			</div>
		</el-row>
        </div>
        <!--表单-->
        <el-row>
			<GlobalTable ref="globalTable"
			 :tableData="tableData"  
			 height="calc(100vh - 250px)" 
			 :defaultSort="{ prop: 'opttime', order: 'descending' }"  
			 @loadTable="loadTableData" 
			 @selection-change="handleSelectionChange"
			 :stripe="true"  
			 style="width: 100%;margin-bottom:16px;">
				<template #field>
				<el-table-column v-if="selectable" type="selection" width="55" />
			    <el-table-column prop="number"  label="订单编号" width="220" sortable="custom" show-overflow-tooltip>
					<template #default="scope">
					   <div >{{scope.row.number}}  
					   <el-tag v-if="scope.row.paystatus==1" type="success">已付款</el-tag>
					   <el-tag v-if="scope.row.paystatus==0" type="info">未付款</el-tag></div>
					   <div class='font-extraSmall'>{{scope.row.wname}}</div>
					 </template>
					</el-table-column>
				<el-table-column prop="image" label="图片" width="65" >
				   <template #default="scope">
				    <el-image :src="scope.row.image"   style="width:40px;height:40px;"  ></el-image>
				  </template>
				</el-table-column>
			    <el-table-column prop="sku" label="名称/SKU"  sortable="custom" show-overflow-tooltip>
		       <template #default="scope">
		          <div class='mname'>{{scope.row.mname}}</div>
		          <div class='sku'>{{scope.row.sku}} <span v-if="scope.row.groupname" class="font-extraSmall">[{{scope.row.groupname}}]</span> </div>
		      </template>
		    </el-table-column>
				 <el-table-column prop="payment_method"  width="280" label="供应商" sortable="custom" >
				 <template #default="scope">
						<div >{{scope.row.cname}}</div>
						<div class='font-extraSmall' v-if="scope.row.payment_method">
						账户类型:{{scope.row.acctname||''}}<span v-if="scope.row.payment_method !== getPayTypeLabel(scope.row.payment_method)">[{{scope.row.payment_method}}]</span>
						<span> - </span>
						<span :style="{color:getPayTypeColor(scope.row.payment_method)}">{{getPayTypeLabel(scope.row.payment_method)}}</span>
					</div>
					</template>
				 </el-table-column>
				<el-table-column prop="purchases" width="120" label="采购数量"  sortable="custom" >
					<template #default="scope">
					    <div >{{scope.row.purchases}}</div>
					    <div class='font-extraSmall' v-if="scope.row.totalin">入库:{{scope.row.totalin}}  </div>
					</template>
				</el-table-column>
				 <el-table-column prop="orderprice"  width="120" label="采购金额" sortable="custom" >
				<template #default="scope">
					    <div >{{scope.row.orderprice}}</div>
					    <div class='font-extraSmall' v-if="scope.row.totalpay">已付:{{scope.row.totalpay}}  </div>
					</template>
				</el-table-column>
			<el-table-column prop="fee_type"  width="180" label="费用类型" sortable="custom" >
			<template #default="scope">
				    <div >{{scope.row.fee_type}}</div>
					    <div class='font-extraSmall'><span >付款日期:{{dateFormat(scope.row.opttime)}}</span></div>
				</template>
			</el-table-column>
					 <el-table-column prop="orderprice"  width="150" label="付款金额" sortable="custom" >
					 <template #default="scope">
					 	    <div v-if="scope.row.payprice<0" class="text-red">{{scope.row.payprice}}:退款</div>
							<div v-else>￥{{scope.row.payprice}}</div>
					 	   <div class='font-extraSmall'>操作人:{{scope.row.name}}</div>
					 	</template>
					 </el-table-column>
				
				
				   <el-table-column prop="remark"  label="备注"  sortable="custom" />
			</template>
			</GlobalTable>
        </el-row>

    </div>
</template>
<script>
    export default{ name:"采购付款明细" };
</script>
<script setup>
    import {MenuUnfold,Plus,SettingTwo,Help,Copy,MoreOne} from '@icon-park/vue-next';
    import { ref,reactive,onMounted,toRefs} from 'vue'
    import {Search,ArrowDown,} from '@element-plus/icons-vue'
	import listApi from '@/api/erp/purchase/form/listApi.js';
	import warehouseApi from '@/api/erp/warehouse/warehouseApi.js';
	import { ElMessageBox,ElMessage } from 'element-plus';
	import Warehouse from '@/components/header/warehouse.vue';
	import Datepicker from '@/components/header/datepicker.vue';
	import {dateFormat} from '@/utils/index.js';
	import Supplier from '@/components/header/supplier.vue';
    import faccountApi from '@/api/erp/finances/faccountApi.js';
    import GroupSelect from '@/components/header/group_select.vue';
	 
	 let globalTable=ref();

	 const state = reactive({
	 		tableData:{records:[],total:0},
	 		queryParams:{
	 			searchtype:"sku",
	 			search:"",
				datetype:"paydate",
	 		},
			
			dateOptions:[{label:"付款时间",value:"paydate"}
			            ,{label:"确认收货时间",value:"recdate"}
						,{label:"账单日",value:"countdate"}],
			payAccountList:[],
			finlist:[],
			totalamount:0,
	 });
	 const { queryParams,tableData,totalamount,payAccountList,finlist,dateOptions, } = toRefs(state);
	 let props = defineProps({
  	                      inForm:undefined,
						  selectable:{
						  type:Boolean,
						  default:false,
					  },
					  hideSupplier:{
						  type:Boolean,
						  default:false,
					  },
					  hideAcct:{
						  type:Boolean,
						  default:false,
					  },
                        });
 const { inForm,selectable,hideSupplier,hideAcct} = toRefs(props);
 const emit = defineEmits(['selection-change']);
		  function handleQuery(){
			  if (!globalTable.value) {
			    return;
			  }
			  globalTable.value.loadTable(state.queryParams);
		  }
	  function show(param){
	  if(param.settlementid !== undefined) state.queryParams.settlementid=param.settlementid;
	  if(param.search !== undefined) state.queryParams.search=param.search;
	  if(param.searchtype !== undefined) state.queryParams.searchtype=param.searchtype;
	  if(param.acct !== undefined) state.queryParams.acct=param.acct;
	  if(param.acctType !== undefined) state.queryParams.acctType=param.acctType;
	  if(param.supplierid !== undefined) state.queryParams.supplierid=param.supplierid;
	  if(param.showSettled !== undefined) state.queryParams.showSettled=param.showSettled;
	  else state.queryParams.showSettled=true;
		  // 如果下拉框数据还没有加载，则重新加载
		  if(state.payAccountList.length === 0){
			  loadPaymentAccount();
		  }
		  if(state.finlist.length === 0){
			  loadFacProject();
		  }
		   handleQuery();
	  }
  function handleSelectionChange(selection){
	  emit('selection-change', selection);
  }
	   defineExpose({ show, queryParams: state.queryParams, getQueryParams: () => state.queryParams });
	  // 根据账户类型匹配记录
	  function matchAcctType(row, acctType) {
		  if (!acctType) return true;
		  const acctname = (row.acctname || '').toString();
		  const paymentMethod = (row.payment_method || '').toString();
		  if (acctType.includes('诚e')) {
			  return acctname.includes('诚e') || paymentMethod.includes('诚e');
		  }
		  return acctname.includes(acctType) || paymentMethod.includes(acctType);
	  }
	  function loadTableData(params){
		  		  listApi.getPaymentReport(params).then(res=>{
						 let records = res.data.records || [];
						 state.tableData.records=records;
						 state.tableData.total=res.data.total || 0;
						 if(params.currentpage==1){
								 if(records.length>0){
									  state.totalamount=records[0].totalpayprice;
								 }else{
									 state.totalamount=0;
								 }
						 }
						 
		  		  }).catch(err => {
		  		  	  console.error('获取采购付款明细失败:', err);
		  		  	  state.tableData.records=[];
		  		  	  state.tableData.total=0;
		  		  	  state.totalamount=0;
		  		  })
		  }
	 function downLoadExcel(){
	 	listApi.getPaymentReportExcel(state.queryParams) 
	 }
	 function changeSupplier(value,type){
		 state.queryParams.supplierid=value;
         if(type!="load"){
		    handleQuery();
		 }
	 }
	 function changeGroup(value,type){
		 state.queryParams.groupid=value;
         if(type!="load"){
		    handleQuery();
		 }
	 }
	 function loadPaymentAccount(){
	 	faccountApi.getPaymentAccount().then((res)=>{
	 		if(res.data && res.data.length>0){
	 			res.data.push({"id":"","name":"全部付款账户"});
	 			state.payAccountList=res.data;
	 			// 不再重置queryParams.acct，保留外部传入的值
	 		}else{
				state.payAccountList=[];
			}
	 	});
	 }
	 function loadFacProject(){
	 	faccountApi.getProject().then((res)=>{
		    res.data.push({"id":"","name":"全部费用类型"});
	 		if(res.data && res.data.length>0){
	 			state.finlist=res.data;
	 		}else{
				state.finlist=[];
				state.queryParam.projectid="";
			}
	 	});
	 }
	 function wareChange(val){
		 state.queryParams.warehouseid=val;
		 handleQuery();
	 }
	 //日期改变
	 function changedate(datestr,timedate,type){
	 	state.queryParams.fromDate=datestr.start;
	 	state.queryParams.toDate=datestr.end;
		if(type!="load"){
	 	 handleQuery();
		}
	 }
	 
	 // 根据付款方式名称判断支付类型标签
function getPayTypeLabel(paymentMethod) {
    if (!paymentMethod) return ''
    const name = paymentMethod.toString()
    // 账期类：账期(1688)、诚e赊(1688)
    if (name.includes('账期') || name.includes('诚e赊')) {
        return '账期'
    }
    return '现金'
}
// 根据付款方式名称获取颜色
function getPayTypeColor(paymentMethod) {
    if (!paymentMethod) return ''
    const name = paymentMethod.toString()
    if (name.includes('账期') || name.includes('诚e赊')) {
        return '#e6a23c' // 橙色
    }
    return '#67c23a' // 绿色
}

onMounted(()=>{
			 loadPaymentAccount();
			 loadFacProject();
		 })
	 
</script>
<style>
</style>