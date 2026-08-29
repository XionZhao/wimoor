<template>
	<el-dialog top="3vh" class="mypaymentdailog" 
	lock-scroll
	:title="operateType.dialogTitle" 
	v-model="dialogVisable" width="1300px">
		<el-scrollbar height="calc(100vh - 180px)" :always="true" :native="true">
		<el-row  class="bg-full">
			<el-col :span="10">
				<el-card shadow="never" class="bg-full">
					<div  class="product-box">
						<el-image v-if="queryParams.entry.image" :src="queryParams.entry.image" class="img-40"  width="40" height="40"  ></el-image>
						<el-image v-else :src="$require('empty/noimage40.png')"  class="img-40"  width="40" height="40"  ></el-image>
						<div>
							<p class="name">{{queryParams.entry.mname}}</p>
							<p class="sku">{{queryParams.entry.sku}}</p>
						</div>
					</div>
					<div class="flex-center-between " v-if="operateType.tabsType=='付款'">
					<el-space :spacer="spacer" :size="24" >
					 <span ><span class="font-bold">{{payMap.payneed}}</span><p class="font-extraSmall">待付款</p></span>
					 <span><span class="font-bold">{{payMap.totalcost}}</span><p class="font-extraSmall">已付款</p></span>
					 <span><span class="font-bold">{{payMap.totalship}}</span><p class="font-extraSmall">已付运费</p></span>
					 <span><span class="font-bold">{{totalamount}}</span><p class="font-extraSmall">总付费</p></span>
            <span><span class="font-bold">{{formatFloat(payMap.weight)}}</span><p class="font-extraSmall">重量kg</p></span>
					</el-space>
					<el-tag v-if="queryParams.paystatus==0" type="warning" size="large">待付款</el-tag>
					<el-tag v-if="queryParams.paystatus==1" type="success" size="large">付款完成</el-tag>
					</div>
					<el-divider class="divider-cell"></el-divider>
					<el-form label-width="100px" label-position="top">
						 <div >
						<el-form-item label="">
						   <el-table :data="tableFeeData" border size="small">
							   <el-table-column type="index">
								   <template #header>
									   <el-link :underline="false" @click="handleAdd">
										   <el-icon class="ic-cen font-large"><Plus /></el-icon>
									   </el-link>
								   </template>
							   </el-table-column>
							   <el-table-column label="费用名称">
								   <template #header>
									   <div  class="flex-center">
									   <span>费用名称</span>
									   <span class="table-edit-flex">
										 <el-icon @click.stop="gotoFinPage" class="ic-cen"><Edit/></el-icon>  
									   </span>
									   </div>
								   </template>
								   <template #default='scope'>
								       <template v-if="scope.row.isFixed">
								           <div>
								               <span>{{ scope.row.name }}</span>
								           </div>
								       </template>
								       <el-select v-else v-model="scope.row.objectid" size="small" style="width: 150px;">
										   <el-option  v-for="item in finlist"   :key="item.id"  :label="item.name" :value="item.id"  ></el-option>
									   </el-select>
								   </template>
							   </el-table-column>
							   <el-table-column label="金额">
								   <template #default='scope'>
								       <template v-if="scope.row.isFixed">
								           <el-input size="small" v-model="scope.row.amount" clearable @input="handleFixedFeeChange(scope.row.field, $event)"></el-input>
								       </template>
								       <template v-else>
								           <el-input size="small" v-model="scope.row.amount" clearable @input="scope.row.amount=CheckInputFloat(scope.row.amount)"></el-input>
								       </template>
								   </template>
							   </el-table-column>
							   <el-table-column label="操作" width="60">
								   <template #default='scope'>
								       <template v-if="scope.row.isFixed">
								           <span></span>
								       </template>
								       <template v-else>
								           <el-link :underline="false" @click="handleDelete(scope.$index)">
								              <el-icon class="ic-cen font-medium"><Close /></el-icon>
								           </el-link>
								       </template>
								  </template>
							   </el-table-column>
						   </el-table>
						</el-form-item>
						</div>
						<el-form-item label="预计到货日期">
							<el-radio-group v-model="formData.datetype" >
							      <el-radio label="1" >
									 <el-date-picker
									        style="width:176px"
									         v-model="payMap.delivery_cycledate"
									         type="date"
									         placeholder="请选择"
									       />
								  </el-radio>
							      <el-radio label="2" >
									  <div>{{dateFormat(payMap.deliverydate)}}
									  <span class="font-extraSmall">旧预计到货日期</span>
									  </div>
								  </el-radio>
							    </el-radio-group>
						</el-form-item>
						<el-row :gutter="24">
						 <el-col :span="24">

			<el-form-item    class="pointer pay-method-item" label="账户">
				<template #label>
					<div class="pointer  flex-between " >
            <div class="flex-center">
              <span class="font-bold" style="margin-right: 8px;">账户:</span>
              <el-radio-group v-model="formData.paymethod" @change="loadPaymentAccount(formData.paymethod)" class="pay-method-radio">
                <el-radio v-for="item in payMethodList" :key="item.id" :label="item.id">{{ item.name }}</el-radio>
              </el-radio-group>
            </div>
           <div style="padding-top:4px;padding-left:2px;"> <el-icon @click.stop="showPayIndexDialog"  class="font-extraSmall"><Sort /></el-icon></div>

					</div>
				</template>
				<el-select v-model="formData.payacc" @change="onPayAccChange" style="width:260px">
					 <el-option  v-for="item in payAccList"   :key="item.id"  :label="item.name" :value="item.id"  >
					 </el-option>
				</el-select>
        <el-icon @click.stop="showPayaccIndexDialog"  style="padding-left:2px" ><Sort /></el-icon>
        <div v-if="isCashType && selectedAccountBalance !== null" style=" padding-left:6px;" class="font-extraSmall">￥{{ formatFloat(selectedAccountBalance) }}</div>
			</el-form-item>
			</el-col>
						</el-row>
						<el-form-item label="备注">
							<el-input v-model="formData.remark"  :rows="2"
								type="textarea"></el-input>
						</el-form-item>
						<el-form-item >
						<el-button type="primary" v-if="queryParams.entry.paystatus==1" disabled >{{operateType.tabsType}}</el-button>
						<el-button type="primary" v-else @click.stop="payment" >{{operateType.tabsType}}</el-button>
						<el-button type="info" v-if="queryParams.entry.paystatus==1" disabled >申请{{operateType.tabsType}}</el-button>
						<el-button type="info" v-else @click.stop="applypayment" style="background-color: #fff; color: #606266; border-color: #dcdfe6;">申请{{operateType.tabsType}}</el-button>
						</el-form-item>
					</el-form>
				</el-card>	
			</el-col>
			<el-col :span="9">
				<el-card shadow="never" class="bg-full">
					<OrderRecord ref="orderRecordRef" @noauth="layoutChange" @change="handleAlibabaOrderFee"/>
				</el-card>
			</el-col>
			<el-col :span="5" class="record-box">
					 <PayRecord ref="recordRef" @change="loadRecord()" @loadpay="loadTotalPay" />
			</el-col>
   </el-row>	
		  </el-scrollbar>
		  <template #footer class="footerbg">
			  <el-button @click="dialogVisable=false">关闭</el-button>
			  <el-button type="primary"  v-if="queryParams.entry.paystatus==0"  @click.stop="stopPayment" plain>结束付款</el-button>
			  <el-button type="primary" v-if="queryParams.entry.paystatus==1" @click="startPayment" plain>继续付款</el-button>
		  </template>
	</el-dialog>
	<FinItem ref="finItemRef" @change="loadFacProject()"></FinItem>
	<PaymethodIndex ref="paymentIndexRef" @change="loadPaymentMethod"></PaymethodIndex>
	<PayaccIndex ref="payaccIndexRef" @change="refreshPayAccList"></PayaccIndex>
</template>

<script setup>
	import {h, ref,reactive,onMounted,watch,inject,toRefs,nextTick,computed} from 'vue'
	import { ElDivider } from 'element-plus'
    import {Close,Plus,Edit,Sort} from '@element-plus/icons-vue';
	import PayRecord from "./pay_record.vue"
	import OrderRecord from "./order_record.vue";
	import purchaselistApi from '@/api/erp/purchase/form/listApi.js';
	import faccountApi from '@/api/erp/finances/faccountApi.js';
	import FinItem from '@/views/erp/finance/account/components/finItem.vue';
	import PaymethodIndex from "@/views/erp/finance/account/components/paymethod_index_dialog.vue";
	import PayaccIndex from "@/views/erp/finance/account/components/payacc_index_dialog.vue";
	import { ElMessage, ElMessageBox } from 'element-plus';
	import {CheckInputFloat,CheckInputInt,dateFormat,dateTimesFormat,formatFloat} from '@/utils/index.js';
	import {useRouter } from 'vue-router'
	const emitter = inject("emitter");
	const emit = defineEmits(['change']);
	const spacer = h(ElDivider, { direction: 'vertical'});
	const recordRef=ref();
	const finItemRef=ref();
	const orderRecordRef=ref();
	const paymentIndexRef=ref();
	const payaccIndexRef=ref();
	const router = useRouter();
	const state = reactive({
		operateType:{
			dialogTitle:'采购付款',
			tabsType:'付款'
		},
		dialogVisable:false,
		// 查询参数
		queryParams: {
			entry:{},
		} ,
		payMap:{
			paylist:[],
			entry:{},
		},
		otherFeeData:[],
		formData:{
			datetype:'1',
			cost:'',
			ship:'',
			payacc:"",
			paymethod:null,
			remark:"",
		},
		layout:{left:10,right:14},
		finlist:[],
		payMethodList:[],
		payAccList:[],
		totalamount:0,
	})
	const {
		dialogVisable,
		queryParams,
		operateType,
		otherFeeData,
		layout,
		payMap,
		formData,
		finlist,
		payMethodList,
		payAccList,
		totalamount,
	}=toRefs(state)

	// 计算表格数据，包含货物费用、运费和其他费用
	const tableFeeData = computed(() => {
		const fixedRows = [
			{ name: '货物费用', amount: state.formData.cost, isFixed: true, field: 'cost' },
			{ name: '运费', amount: state.formData.ship, isFixed: true, field: 'ship' }
		];
		const otherRows = state.otherFeeData.map(item => ({
			...item,
			isFixed: false
		}));
		return [...fixedRows, ...otherRows];
	})

	// 判断当前选中的账户类型是否为现金
	const isCashType = computed(() => {
		const selectedMethod = state.payMethodList.find(item => item.id === state.formData.paymethod);
		return selectedMethod && selectedMethod.name === '现金';
	})

	// 获取当前选中支付账户的余额
	const selectedAccountBalance = computed(() => {
		if (!state.formData.payacc) return null;
		const selectedAccount = state.payAccList.find(item => item.id === state.formData.payacc);
		return selectedAccount ? selectedAccount.balance : null;
	})

	function gotoFinPage(){
		//跳转至fin页面
		finItemRef.value.show();
	}
	function handleFixedFeeChange(field, value) {
		const checkedValue = value === '' ? '' : CheckInputFloat(value);
		if (field === 'cost') {
			state.formData.cost = checkedValue;
		} else if (field === 'ship') {
			state.formData.ship = checkedValue;
		}
		// 同步更新tableFeeData中的固定行数据
		tableFeeData.value.forEach(item => {
			if (item.isFixed && item.field === field) {
				item.amount = checkedValue;
			}
		});
	}
	function layoutChange(){
		state.layout={left:23,right:1};
	}
	function showPayIndexDialog(){
		paymentIndexRef.value.show();
	}
	function showPayaccIndexDialog(){
		payaccIndexRef.value.show();
	}
	function refreshPayAccList(){
		// 清空缓存，重新加载支付账户列表
		state.payAccList=[];
		loadPaymentAccount(state.formData.paymethod);
	}

	function onPayAccChange(accId){
		// 选择账户时，反向联动更新账户类型
		const selectedAcc = state.payAccList.find(item => item.id === accId);
		if(selectedAcc && selectedAcc.paymeth){
			state.formData.paymethod = selectedAcc.paymeth;
		}
	}

	function handleAdd(){
		state.otherFeeData.push({
			name:'',
			amount:'',
			objectid:'',
		})
	}
    
	function handleDelete(index){
		// 减去系统固定行数量，得到otherFeeData中的实际索引
		const sysCount = tableFeeData.value.filter(item => item.isFixed).length;
		const actualIndex = index - sysCount;
		if (actualIndex >= 0) {
			state.otherFeeData.splice(actualIndex, 1);
		}
	}
	function loadFacProject(){
		faccountApi.getProject().then((res)=>{
			if(res.data && res.data.length>0){
				// 下拉列表：非系统的费用项（包含默认项和用户手动添加项）
				state.finlist = res.data.filter(item => !item.issys);
				// 非系统默认项自动填充到表格（仅首次加载时）
				if(!state._defaultFeeLoaded){
					const defaultNonSys = res.data.filter(item => !item.issys && item.isdefault);
					defaultNonSys.forEach(item => {
						state.otherFeeData.push({
							name: item.name,
							amount: '',
							objectid: item.id,
						});
					});
					state._defaultFeeLoaded = true;
				}
			}
		});
	}
	function loadPaymentMethod(){
		faccountApi.getPaymentMethod().then((res)=>{
			if(res.data && res.data.length>0){
				state.payMethodList=res.data;
				state.formData.paymethod=res.data[0].id;
				loadPaymentAccount(res.data[0].id);
			}
		});
	}
	function loadPaymentAccount(paymethod){
		if(state.payAccList!=null&&state.payAccList.length>0){
			var defaultid="";
			var normalDefault="";
			state.payAccList.forEach(item=>{
				if(item.isdefault&&item.paymeth==paymethod){
					defaultid=item.id;
				}
				if(normalDefault==""&&item.isdefault){
					normalDefault=item.id;
				}
				
			});
			state.formData.payacc=defaultid==""?normalDefault:defaultid;
		}else{
			faccountApi.getPaymentAccount({"paymethod":paymethod}).then((res)=>{
				if(res.data && res.data.length>0){
					state.payAccList=res.data;
					var defaultid="";
					var normalDefault="";
					state.payAccList.forEach(item=>{
						if(item.isdefault&&item.paymeth==paymethod){
							defaultid=item.id;
						}
						if(normalDefault==""&&item.isdefault){
							normalDefault=item.id;
						}
						
					});
					state.formData.payacc=defaultid==""?normalDefault:defaultid;
					 
				}else{
					state.payAccList=[];
					state.formData.payacc="";
				}
			});
		}
		
	}
	 
	function applypayment(){
		if(state.queryParams.entry.paystatus==1){
			ElMessage.error( '当前付款状态已完结！' );
			return;
		}
		var data={};
		var sumpay=0;
		data.paymethod=state.formData.paymethod;
    data.payacc=state.formData.payacc;
		data.logisiter=null;
		data.status="0";
		data.payid=null;
		if(state.formData.datetype=="1"){
			data.deliverydate=payMap.delivery_cycledate;
		}else{
			data.deliverydatestr=payMap.deliverydate;
		}
		data.remark=state.formData.remark;
		// 动态获取系统费用项金额
		const costRow = tableFeeData.value.find(item => item.isFixed && item.field === 'cost');
		const shipRow = tableFeeData.value.find(item => item.isFixed && item.field === 'ship');
		data.costamount = costRow && costRow.amount ? parseFloat(costRow.amount) : 0;
		data.shipamount = shipRow && shipRow.amount ? parseFloat(shipRow.amount) : 0;

		if(data.costamount > 0) sumpay += data.costamount;
		if(data.shipamount > 0) sumpay += data.shipamount;
		data.entryid=state.queryParams.entry.id;
		if(state.operateType.tabsType=="付款"){
			data.paytype="out";
		}else if(state.operateType.tabsType=="退款"){
			data.paytype="in";
		}
		if(data.paytype=="in"){
			if(data.shipamount > 0) data.shipamount = (-1) * data.shipamount;
			if(data.costamount > 0) data.costamount = (-1) * data.costamount;
		}
		if(tableFeeData.value && tableFeeData.value.length>0){
			var feeList=[];
			tableFeeData.value.forEach(function(item){
				if(!item.isFixed && (item.amount!=undefined && item.amount!="" && parseFloat(item.amount)>0) && item.objectid){
				    sumpay=sumpay+parseFloat(item.amount);
				    var feeItem = {...item};
					if(data.paytype=="in"){
						feeItem.amount=(-1)*parseFloat(item.amount);
					}
					feeList.push(JSON.stringify(feeItem));
				}
			});
			data.feelist=feeList.toString();
		}
		if(sumpay<=0.000001){
			ElMessage.error('费用不能小于等于0');
			return;
		}
		purchaselistApi.paymentApply(data).then((res)=>{
			if(res.data){
				ElMessage.success('申请'+state.operateType.tabsType+'成功');
				emit("change");
				state.queryParams.entry.paystatus=3;
			}
		});
	}
	function payment(){
		if(state.queryParams.entry.paystatus==3){
			ElMessage.error('当前存在已请款的费用，请跳转至请款单页面处理后再操作付款！');
			return;
		}
		if(state.queryParams.entry.paystatus==1){
			ElMessage.error('当前付款状态已完结！');
			return;
		}
		var data={};
		var sumpay=0;
		data.paymethod=state.formData.paymethod;
		data.payacc=state.formData.payacc;
		data.logisiter=null;
		data.status="0";
		data.payid=null;
		if(state.formData.datetype=="1"){
			data.deliverydate=payMap.delivery_cycledate;
		}else{
			data.deliverydatestr=payMap.deliverydate;
		}
		data.remark=state.formData.remark;
		// 动态获取系统费用项金额
		const costRow = tableFeeData.value.find(item => item.isFixed && item.field === 'cost');
		const shipRow = tableFeeData.value.find(item => item.isFixed && item.field === 'ship');
		data.costamount = costRow && costRow.amount ? parseFloat(costRow.amount) : 0;
		data.shipamount = shipRow && shipRow.amount ? parseFloat(shipRow.amount) : 0;

		if(data.costamount > 0) sumpay += data.costamount;
		if(data.shipamount > 0) sumpay += data.shipamount;
		data.entryid=state.queryParams.entry.id;
		if(state.operateType.tabsType=="付款"){
			data.paytype="out";
		}else if(state.operateType.tabsType=="退款"){
			data.paytype="in";
		}
		if(data.paytype=="in"){
			if(data.shipamount > 0) data.shipamount = (-1) * data.shipamount;
			if(data.costamount > 0) data.costamount = (-1) * data.costamount;
		}
		if(tableFeeData.value && tableFeeData.value.length>0){
			var feeList=[];
			tableFeeData.value.forEach(function(item){
				if(!item.isFixed && (item.amount!=undefined && item.amount!="" && parseFloat(item.amount)>0) && item.objectid){
				    sumpay=sumpay+parseFloat(item.amount);
				    var feeItem = {...item};
					if(data.paytype=="in"){
						feeItem.amount=(-1)*parseFloat(item.amount);
					}
					feeList.push(JSON.stringify(feeItem));
				}
			});
			data.feelist=feeList.toString();
		}

		if(sumpay<=0.000001){
			ElMessage.error('费用不能小于等于0');
			return;
		}
		purchaselistApi.payment(data).then((res)=>{
			if(res.data){
				ElMessage.success('付款成功');
				loadRecord();
				state.payMap=res.data;
				state.queryParams.entry.totalpay=res.data.entry.totalpay;
				state.queryParams.entry.paystatus=res.data.entry.paystatus;
				state.queryParams.entry.auditstatus=res.data.entry.auditstatus;
				emit("change");
				emitter.emit("removeCache", "采购记账");
				//if(res.data.entry.paystatus==1){
					//state.dialogVisable=false;
				//}
			}
		});
	}
	function stopPayment(){
		if(state.queryParams.entry.paystatus==3){
			ElMessage.error('当前存在已请款的费用，请跳转至请款单页面处理后再操作束付款！');
			return;
		}
		ElMessageBox.confirm(
			'请确认是否结束付款？',
			{
			  confirmButtonText: '确认',
			  cancelButtonText: '取消',
			  type: 'warning',
			  callback:(action)=>{
				 if(action=="confirm"){
					 var data={};
					 data.entryid=state.queryParams.entry.id;
					 data.paytype="out";
					 data.costamount=0;
					 data.shipamount=0;
					 if(state.formData.datetype=="1"){
					 	data.deliverydate=payMap.delivery_cycledate;
					 }else{
					 	data.deliverydatestr=payMap.deliverydate;
					 }
					 data.payid=null;
					 data.status="1";
					 data.remark=state.formData.remark;
					 data.logisiter=null;
					 data.feelist="";
					 data.paymethod=state.formData.paymethod;
					 purchaselistApi.payment(data).then((res)=>{
					 	if(res.data){
					 		ElMessage.success('操作成功');
					 		state.payMap=res.data;
							state.queryParams.entry.totalpay=res.data.entry.totalpay;
							state.queryParams.entry.paystatus=res.data.entry.paystatus;
							state.queryParams.entry.auditstatus=res.data.entry.auditstatus;
					 		emit("change");
					 	}
					 });
				 }
			  }
			}
		  )
		
	}
	function startPayment(){
		var data={};
		data.entryid=state.queryParams.entry.id;
		data.paytype="out";
		data.costamount=0;
		data.shipamount=0;
		if(state.formData.datetype=="1"){
			data.deliverydate=payMap.delivery_cycledate;
		}else{
			data.deliverydatestr=payMap.deliverydate;
		}
		data.payid=null;
		data.status="2";
		data.remark=state.formData.remark;
		data.logisiter=null;
		data.feelist="";
		data.paymethod=state.formData.paymethod;
		purchaselistApi.payment(data).then((res)=>{
			if(res.data){
				ElMessage.success('操作成功');
				state.payMap=res.data;
				state.queryParams.entry.totalpay=res.data.entry.totalpay;
				state.queryParams.entry.paystatus=res.data.entry.paystatus;
				state.queryParams.entry.auditstatus=res.data.entry.auditstatus;
				emit("change");
			}
		});
		//state.dialogVisable=false;
	}
	function toPaymentPage(){
		router.push({
			path:"/finance/account",
			query:{
				title:'采购记账',
				path:"/finance/account",
			},
		})
	}
	function handleAlibabaOrderFee(fee){
		if(fee.price){
			state.formData.cost=fee.price;
		}
		if(fee.ship){
			state.formData.ship=fee.ship;
		}
		// 同步更新tableFeeData中的固定行数据
		tableFeeData.value.forEach(item => {
			if (item.isFixed && item.field === 'cost') {
				item.amount = state.formData.cost;
			} else if (item.isFixed && item.field === 'ship') {
				item.amount = state.formData.ship;
			}
		});
	}
	
	function show(type,entry){
		state.queryParams.entry=entry;
		state.otherFeeData = [];
		state._defaultFeeLoaded = false;
		if(type=="pay"){
			state.operateType.dialogTitle = "采购付款"
			state.operateType.tabsType  ="付款"
		}else{
			state.operateType.dialogTitle = "采购退款"
			state.operateType.tabsType  ="退款"
		}
		loadRecord();
		loadFacProject();
		loadPaymentMethod();
		state.dialogVisable = true;
		nextTick(()=>{
	     	orderRecordRef.value.show(entry);
		})
		
	}
	function loadTotalPay(totalamounts){
		state.totalamount=totalamounts;
	}
	
	
	function loadRecord(){
			  purchaselistApi.getRecdetail({"id":state.queryParams.entry.id,"ftype":"pay","actiontype":"all"}).then((res)=>{
				if(res.data){
					state.payMap=res.data;
					nextTick(()=>{
						recordRef.value.show(state.payMap.paylist);
					})
				    
				}
			});
			
	}
	
	defineExpose({
		show,
	})
</script>
<style>

	.mypaymentdailog .el-dialog__footer{
		background:#f5f5f5;
		border-bottom-right-radius:2px;
		border-bottom-left-radius:2px;
		text-align:center;
	}
	.dark .mypaymentdailog .el-dialog__footer{
		background:#1b1b1b;
		border-bottom-right-radius:2px;
		border-bottom-left-radius:2px;
		text-align:center;
	}
	 .mypaymentdailog .el-dialog__body{
		 padding:1px 0px;
		 background-color:#f5f5f5;
	 }
	 .mypaymentdailog {
		 margin: var(--el-dialog-margin-top,15vh) auto 10px;
	 }
</style>
<style scoped="scoped">
	.bg-full{
		height:100%;
	}
	.divider-cell{
		margin-top:16px!important;
		margin-bottom:16px !important;
	}
	.record-box{
		padding:12px;
	}
.el-progress-bar{
	width: 200px;
}
.flex-grow{
	flex:1;
}
.product-box{
	display: flex;
	margin-bottom:16px;
}	
.product-box .el-image{
	margin-right: 16px;
}
.product-box .name{
font-size: 12px;
margin-bottom:8px;
}
.product-box .sku{
font-size: 12px;
color:var(--el-color-blue)
}
.m-t-32{
	margin-top: 32px;
}

.img-40{width: 40px;
height: 40px;flex: none;
margin-right: 8px;}
.payacc-label {
	display: flex;
	align-items: center;
	justify-content: space-between;
	width: 100%;
}
.balance-text {
	font-size: 12px;
	color: #999;
}
.pay-method-item :deep(.el-form-item__label) {
	padding-bottom: 0 !important;
}
.pay-method-radio {
	margin-left: 4px;
}
.pay-method-radio :deep(.el-radio) {
	margin-right: 8px;
	height: 24px;
	line-height: 24px;
}
.pay-method-radio :deep(.el-radio__label) {
	padding-left: 4px;
	font-size: 13px;
}
</style>
