<template>
<div class="account"> 
<div class="account-card el-white-bg" ref="leftdiv" :style="{minHeight: leftheight}">
	 
	<h4>我的账户
	<el-button style="float:right;" @click.stop="showDeleteDailog" type="info" link size="mini">查看删除列表</el-button>
</h4>
<el-card :class="item.active?'m-t-16 active':'m-t-16' " class="pointer " v-for="item in DataList"  @click="handleActive(item)">
	<div class="card-header flex-between">
		<span>
			<el-space direction="vertical" :size="2" alignment="left">
				<span @click.stop="showPayIndexDialog">{{item.name}}</span>
				<span class="font-extraSmall">{{item.paymethName}}</span>
			</el-space>&nbsp;
			<el-tag v-if="item.isdefault" :title="item.name" size="small" effect="plain" type="danger">默认</el-tag>
		</span>
		<el-dropdown v-if="item.id" trigger="click">
			<el-icon class="font-large text-gray pointer"><MoreFilled /></el-icon>
			<template #dropdown>
				<el-dropdown-menu>
				<el-dropdown-item v-if="item.isdefault" @click.stop="handleCancelDefault(item)">取消该支付方式默认卡</el-dropdown-item>
				<el-dropdown-item v-else @click.stop="handleDefault(item)">设为该支付方式默认卡</el-dropdown-item>
				<el-dropdown-item @click.stop="handleRename(item)">重命名</el-dropdown-item>
				<el-dropdown-item @click.stop="handleDelect(item)">删除</el-dropdown-item>
			</el-dropdown-menu>
			</template>
		</el-dropdown>
	</div>
	<div class="card-body">
		<span :class="item.balance>0?'text-primary':''">￥
			<span class="ft-24" v-if="item.balance">{{formatFloat(item.balance)}}</span>
			<span class="ft-24" v-else>0.00</span>
		</span>
	</div>
</el-card>
	<el-card @click="handleAdd" class="add-account pointer" shadow="hover">
		<div class="flex-vertical">
		<el-icon class="text-center"><Plus /></el-icon>
		<span class="text-gray">添加账户</span>
		</div>
	</el-card>
	 
</div>
<div class="account-chart" ref="rightdiv">
	<el-row :gutter="16">
		<el-col :span="16">
			<LineChart ref="lineChartRef"/>
		</el-col>
		<el-col :span="8">
			<PieChart ref="pieChartRef"/>
		</el-col>
	</el-row>
	<el-row>
		<Table  ref="tableRef" @changeData="refreshData"/>
	</el-row>
</div>
<AccountEditDialog ref="editDialogRef" :payMethodList="payMethodList" @refresh="refreshData"/>
<AccountDeleteDialog ref="deleteDialogRef" @refresh="refreshData"/>
<PaymethodIndex ref="paymentIndexRef" @change="handlePaymethodChange"/>
</div>
</template>

<script setup>
import { ref, reactive, toRefs, onMounted, nextTick } from 'vue'
import { MoreFilled, Plus } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { formatFloat } from '@/utils/index.js'
import LineChart from "./lineChart.vue"
import PieChart from "./pieChart.vue"
import Table from "./table.vue"
import AccountEditDialog from "./account_edit_dialog.vue"
import AccountDeleteDialog from "./account_delete_dialog.vue"
import PaymethodIndex from "./paymethod_index_dialog.vue"
import faccountApi from '@/api/erp/finances/faccountApi.js'

const state = reactive({
	DataList: [],
	activeItem: {},
	leftheight: '600px',
	payMethodList: [],
})
const { DataList, activeItem, leftheight, payMethodList } = toRefs(state)

const pieChartRef = ref()
const tableRef = ref()
const lineChartRef = ref()
const leftdiv = ref()
const rightdiv = ref()
const editDialogRef = ref()
const deleteDialogRef = ref()
const paymentIndexRef = ref()

function loadPaymentMethod() {
	faccountApi.getPaymentMethod().then((res) => {
		if (res.data && res.data.length > 0) {
			state.payMethodList = res.data
		}
	})
}

function loadMyAccount(isInit) {
	faccountApi.getAccountAll().then(res => {
		state.DataList = res.data
		state.DataList.sort((a, b) => {
			return (a.findex || 0) - (b.findex || 0)
		})
		var summary = 0.00
		state.DataList.forEach(item => {
			summary += item.balance
		})
		state.DataList.unshift({ paymethName: "总账户", isdefault: false, balance: summary, id: "" })
		if (isInit) {
			handleActive(state.DataList[0])
		} else {
			handleActive(state.activeItem)
		}
	})
}

function handleActive(item) {
	state.DataList.forEach(row => {
		row.active = (row.id == item.id)
	})
	state.activeItem = JSON.parse(JSON.stringify(item))
	loadRightData(item)
}

function loadRightData(item) {
	pieChartRef.value.show(item)
	tableRef.value.show(item)
	lineChartRef.value.show(item)
}

function refreshData() {
	loadMyAccount(false)
	loadRightData(state.activeItem)
}

function handleRename(item) {
	editDialogRef.value.show(item)
}

function handleAdd() {
	editDialogRef.value.show(null)
}

function showDeleteDailog() {
	deleteDialogRef.value.show()
}

function showPayIndexDialog() {
	paymentIndexRef.value.show()
}

function handlePaymethodChange() {
	loadPaymentMethod()
	refreshData()
}

function handleDelect(item) {
	faccountApi.updateAccountDelete(item).then(res => {
		ElMessage.success('删除成功')
		refreshData()
	})
}

function handleDefault(item) {
	faccountApi.updateAccountDefault(item).then(res => {
		ElMessage.success('设置成功')
		refreshData()
	})
}

function handleCancelDefault(item) {
	faccountApi.cancelAccountDefault(item).then(res => {
		ElMessage.success('设置成功')
		refreshData()
	})
}

onMounted(() => {
	loadPaymentMethod()
	loadMyAccount(true)
	nextTick(() => {
		if (rightdiv.value) {
			state.leftheight = rightdiv.value.offsetHeight + 'px'
		}
	})
})

defineExpose({
	getDataList() { return state.DataList },
	getActiveItem() { return state.activeItem },
	getPayMethodList() { return state.payMethodList },
})
</script>

<style scoped>
.account{
	display: flex;
}
.active{
	background:#ff8000;
	border: 1px solid #ff8000 !important;
	box-shadow: 0 2px 12px 0 rgba(255,107,14,0.2);
}
.active,.active .text-primary,.active .text-gray{
	color: #fff!important;
}
.active .font-extraSmall{
	color:rgba(255,255,255,0.6)
}
.active .el-tag--plain.el-tag--danger{
	background-color: transparent;
	color: #fff;
	border-color:rgba(255,255,255,0.6) ;
}
.account-card{
	padding:16px;
	width:280px;
	overflow-y:auto;
}
.account-card h4{
	margin-bottom: 16px;
}
.account-chart{
	padding:16px;
	flex: 1;
	background-color: var(--el-color-info-lighter);
}
.dark .account-chart{
	background-color:#0e0e0e;
}
.m-t-16{
	margin-bottom: 16px;
}
.account-card .card-body{
	margin-top: 24px;
}
.ft-24{
	font-size: 24px;
	font-family:DIN Alternate,Helvetica Neue,Helvetica,Arial, SF Pro Display;
	font-weight: 700;
}
.text-primary{
	color: var(--el-color-primary);
}
.text-gray{
	color: #999;
}
.add-account{
	border: 2px dashed #DEDEDE;
}
</style>
