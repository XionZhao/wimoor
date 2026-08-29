<template>
<div>
	<!-- 账户卡片选择 -->
	<div class="card-div acct-cards">
		<el-scrollbar>
			<div class="scrollbar-flex-content">
				<el-card
					v-for="item in acctOptions"
					:key="item.id"
					:shadow="item.id === selectedAcctId ? 'always' : 'hover'"
					:class="['acct-card', item.id === selectedAcctId ? 'acct-card-active' : '']"
					@click="handleAcctChange(item.id)"
				>
					<div class="acct-card-body">
						<div class="acct-card-name">{{ item.name }}</div>
						<el-tag type="warning" size="small" class="acct-card-tag">账期</el-tag>
					</div>
					<div class="acct-card-balance">
						<el-tooltip content="账户当前结余金额" placement="top">
							<span>结余金额：¥ {{ formatMoney(item.balance || 0) }}</span>
						</el-tooltip>
					</div>
				</el-card>
			</div>
		</el-scrollbar>
	</div>
	<!-- 采购付款明细 -->
		<div style="margin-top:16px;">
			<div class="summary-header">
				<span class="summary-title">采购付款明细</span>
				<el-space>
					<el-button type="primary" @click="handleSettle" :disabled="selectedRows.length === 0">
						结转<span v-if="selectedRows.length > 0">({{ selectedRows.length }})</span>
					</el-button>
					<el-button type="warning" @click="handleSettleAll" :disabled="!selectedAcctId">全部结转</el-button>
					<el-button @click="handleImportSettle">导入结转</el-button>
				</el-space>
			</div>
			<PaymentRecord ref="paymentDetailRef" :inForm="true" :selectable="true" :hideAcct="true" :key="'detail-' + selectedAcctId" @selection-change="handlePaymentSelectionChange">
		</PaymentRecord>
		</div>
	<!-- 已结转记录 -->
	<el-card shadow="never" class="main-card" style="margin-top:16px;">
		<div class="summary-header">
			<span class="summary-title">已结转记录</span>
			<span class="font-base"><span class="font-base-nine">已结转金额总计:</span>￥{{ formatMoney(summaryData.settledAmount) }}</span>
		</div>
		<SettledRecord ref="settledRecordRef" :key="'settled-' + selectedAcctId" @refresh="handleSettledRefresh"></SettledRecord>
	</el-card>
	<!-- 1688账单明细 -->
	<el-card shadow="never" class="main-card" style="margin-top:16px;">
		<div class="summary-header">
			<span class="summary-title">1688账单明细</span>
			<el-space>
				<el-button @click="openUpload">
					<span title="导入后可以搜索买家账单确认收货时间 已保证系统付款与1688账单对应">1688账单明细导入</span>
				</el-button>
				<Datepicker longtime="ok" ref="datepickers" :shortIndex="3" @changedate="changedate" />
			</el-space>
		</div>
		<div style="text-align:right;margin-bottom:16px;">
			<span class="font-base">
				<span class="font-base-nine">支付金额:</span>￥{{ formatMoney(orderSummaryData.totalPayAmount) }}
				<span class="font-base-nine" style="margin-left:16px;">确认收货金额:</span>￥{{ formatMoney(orderSummaryData.totalConfirmAmount) }}
				<span class="font-base-nine" style="margin-left:16px;">退款金额:</span>￥{{ formatMoney(orderSummaryData.totalReturnAmount) }}
			</span>
		</div>
		<el-tabs v-model="activeName" class="demo-tabs" @tab-change="handleClick">
			<el-tab-pane label="账单明细" name="order">
				<OrderRecord ref="orderRecordRef" :inForm="true" :key="'order-' + selectedAcctId" @change="orderChange"></OrderRecord>
			</el-tab-pane>
			<el-tab-pane label="还款记录" name="pay">
				<PayRecord ref="payRecordRef" :inForm="true" :key="'pay-' + selectedAcctId"></PayRecord>
			</el-tab-pane>
			<el-tab-pane label="退款记录" name="returnOrder">
				<ReturnOrderRecord ref="returnOrderRecordRef" :inForm="true" :key="'returnOrder-' + selectedAcctId"></ReturnOrderRecord>
			</el-tab-pane>
			<el-tab-pane label="充退记录" name="returnPay">
				<ReturnPayRecord ref="returnPayRecordRef" :inForm="true" :key="'returnPay-' + selectedAcctId"></ReturnPayRecord>
			</el-tab-pane>
			<el-tab-pane label="付款明细" name="record">
			<PaymentRecord v-if="activeName === 'record'" ref="paymentRecordRef" :inForm="true" :hideAcct="true" :key="'record-' + selectedAcctId"></PaymentRecord>
		</el-tab-pane>
		</el-tabs>
	</el-card>
</div>
<UploadDialog ref="uploadDialogRef" @upload="handleUpload"></UploadDialog>
</template>

<script setup>
import { ref, reactive, onMounted, nextTick, toRefs } from 'vue'
import { Delete } from '@element-plus/icons-vue'
import Datepicker from '@/components/header/datepicker.vue'
import PaymentRecord from '@/views/erp/purchase/report/payment/index.vue'
import UploadDialog from '@/components/Upload/uploadDialog.vue'
import OrderRecord from '@/views/erp/finance/alibabaSettlement/components/order.vue'
import PayRecord from '@/views/erp/finance/alibabaSettlement/components/pay.vue'
import ReturnOrderRecord from '@/views/erp/finance/alibabaSettlement/components/returnorder.vue'
import ReturnPayRecord from '@/views/erp/finance/alibabaSettlement/components/returnpay.vue'
import SettledRecord from '@/views/erp/finance/alibabaSettlement/components/settled.vue'
import purchaseAlibabaSettlementApi from '@/api/erp/finances/purchaseAlibabaSettlementApi.js'
import faccountApi from '@/api/erp/finances/faccountApi.js'
import { ElMessage, ElMessageBox } from 'element-plus'
import { dateFormat, dateTimesFormat } from '@/utils/index.js'

const uploadDialogRef = ref()
const orderRecordRef = ref()
const payRecordRef = ref()
const returnOrderRecordRef = ref()
const returnPayRecordRef = ref()
const paymentRecordRef = ref()
const paymentDetailRef = ref()
const settledRecordRef = ref()
const datepickers = ref()

const state = reactive({
	acctOptions: [],
	selectedAcctId: '',
	selectedAcctType: '',
	activeSettlementId: '',
	activeName: 'order',
	settlementList: [],
	queryParams: { search: '', acct: '' },
	acctAmounts: {},
	selectedRows: [],
	importType: 'payDate',
	summaryData: {
		settledCount: 0,
		settledAmount: 0,
		settledPaid: 0,
		unsettledCount: 0,
		unsettledAmount: 0,
		unsettledUnpaid: 0
	},
	orderSummaryData: {
		totalPayAmount: 0,
		totalConfirmAmount: 0,
		totalReturnAmount: 0
	}
})

const { acctOptions, selectedAcctId, selectedAcctType, activeSettlementId, activeName, settlementList, queryParams, summaryData, orderSummaryData, acctAmounts, selectedRows } = toRefs(state)

function formatMoney(val) {
	if (!val && val !== 0) return '0.00'
	return Number(val).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

function loadAcctOptions() {
		faccountApi.getAccountAll().then(res => {
			const allAccts = res.data || []
			// 过滤账期账户：paymeth==2 表示账期
			state.acctOptions = allAccts.filter(item => item.paymeth == 2)
			if (state.acctOptions.length > 0) {
				// 默认选中"诚e赊(1688)"卡片
				const defaultAcct = state.acctOptions.find(item => item.name && item.name.includes('诚e'))
				if (defaultAcct) {
					state.selectedAcctId = defaultAcct.id
				} else {
					state.selectedAcctId = state.acctOptions[0].id
				}
				state.queryParams.acct = state.selectedAcctId
				// 从账户名称派生acctType
				const selectedAcct = state.acctOptions.find(item => item.id === state.selectedAcctId)
				state.selectedAcctType = selectedAcct ? selectedAcct.name : ''
				state.queryParams.acctType = state.selectedAcctType
				loadSettlementList()
				loadSummary()
				// 组件因key变化会被重新创建，需要等挂载完成后再调用show
				nextTick(() => {
					nextTick(() => {
						if (paymentDetailRef.value) {
							paymentDetailRef.value.show({ acct: state.selectedAcctId, acctType: state.selectedAcctType, showSettled: false })
						}
						if (settledRecordRef.value) {
							settledRecordRef.value.show(state.queryParams)
						}
					})
				})
			}
		})
	}

function handleAcctChange(val) {
	state.selectedAcctId = val
	state.queryParams.acct = val
	// 从账户名称派生acctType
	const selectedAcct = state.acctOptions.find(item => item.id === val)
	state.selectedAcctType = selectedAcct ? selectedAcct.name : ''
	state.queryParams.acctType = state.selectedAcctType
	state.activeSettlementId = ''
	state.settlementList = []
	state.selectedRows = []
	loadSettlementList()
	loadSummary()
	// 组件因key变化会被重新创建，需要等挂载完成后再调用show
	nextTick(() => {
		nextTick(() => {
			if (paymentDetailRef.value) {
				paymentDetailRef.value.show({ acct: val, acctType: state.selectedAcctType, showSettled: false })
			}
			if (settledRecordRef.value) {
				settledRecordRef.value.show(state.queryParams)
			}
			// 刷新当前活动的1688账单明细tab
			handleClick()
		})
	})
}

// 付款记录选择变更
function handlePaymentSelectionChange(selection) {
	state.selectedRows = selection
}

// 结转选中的付款记录
function handleSettle() {
	if (state.selectedRows.length === 0) {
		ElMessage.warning('请先选择要结转的付款记录')
		return
	}
	ElMessageBox.confirm(`确定要将选中的 ${state.selectedRows.length} 条付款记录进行结转吗?`, {
		confirmButtonText: '确认',
		cancelButtonText: '取消',
		type: 'warning',
	}).then(() => {
		const ids = state.selectedRows.map(row => row.id)
		purchaseAlibabaSettlementApi.settle({ ids: ids, acct: state.selectedAcctId }).then(() => {
			ElMessage.success('结转成功')
			state.selectedRows = []
			// 刷新付款明细和已结转记录
			nextTick(() => {
				if (paymentDetailRef.value) {
					paymentDetailRef.value.show({ acct: state.selectedAcctId, acctType: state.selectedAcctType, showSettled: false })
				}
				if (settledRecordRef.value) {
					settledRecordRef.value.show(state.queryParams)
				}
			})
		})
	}).catch(() => {})
}

// 全部结转
function handleSettleAll() {
	if (!state.selectedAcctId) {
		ElMessage.warning('请先选择一个账期账户')
		return
	}
	// 获取PaymentRecord组件的完整筛选条件（与getPaymentReport完全一致）
	const getQueryParams = paymentDetailRef.value?.getQueryParams
	const paymentParams = getQueryParams ? getQueryParams() : (paymentDetailRef.value?.queryParams || {})
	// 构建与PaymentReportDTO一致的请求参数（去除分页参数）
	const params = {
		acct: state.selectedAcctId,
		fromDate: paymentParams.fromDate || '',
		toDate: paymentParams.toDate || '',
		datetype: paymentParams.datetype || 'paydate',
		search: paymentParams.search || '',
		searchtype: paymentParams.searchtype || 'sku',
		warehouseid: paymentParams.warehouseid || '',
		supplierid: paymentParams.supplierid || '',
		paymethod: paymentParams.paymethod || '',
		projectid: paymentParams.projectid || '',
		groupid: paymentParams.groupid || '',
		settlementid: paymentParams.settlementid || ''
	}
	console.log('[全部结转] 发送给后端的参数:', JSON.stringify(params, null, 2))
	// 先获取未结转记录数量
	purchaseAlibabaSettlementApi.getAllUnsettledIds(params).then(res => {
		const count = res.data || 0
		if (count === 0) {
			ElMessage.info('当前筛选条件下没有未结转的付款记录')
			return
		}
		ElMessageBox.confirm(
			`确定要将当前筛选条件下的 ${count} 条未结转付款记录进行全部结转吗？<br/><br/>` +
			`<span style="color: #f56c6c; font-weight: bold;">此操作不可撤销，请确认！</span>`,
			'全部结转确认',
			{
				confirmButtonText: '确认结转',
				cancelButtonText: '取消',
				type: 'warning',
				dangerouslyUseHTMLString: true,
			}
		).then(() => {
			// 调用全部结转接口（传递相同的筛选条件）
			purchaseAlibabaSettlementApi.settleAll(params).then(() => {
				ElMessage.success('全部结转成功')
				state.selectedRows = []
				// 刷新付款明细和已结转记录
				nextTick(() => {
					if (paymentDetailRef.value) {
						paymentDetailRef.value.show({ acct: state.selectedAcctId, acctType: state.selectedAcctType, showSettled: false })
					}
					if (settledRecordRef.value) {
						settledRecordRef.value.show(state.queryParams)
					}
				})
			})
		}).catch(() => {})
	})
}

// 导入结转数据
function handleImportSettle() {
	if (!state.selectedAcctId) {
		ElMessage.warning('请先选择一个账期账户')
		return
	}
	state.importType = 'settle'
	uploadDialogRef.value.show({ 'template': 'PurchaseAlibabaSettlement', 'title': '导入结转数据', 'action': '/erp/api/v1/purchase/alibaba/entry/purchaseAlibabaSettlement/downExcelTemp', 'desc': '使用说明：先从采购付款明细导出数据，然后仅保留订单号和SKU列，导入后会自动生成结转记录' })
}

// 1688账单明细导入
function openUpload() {
	if (!state.selectedAcctId) {
		ElMessage.warning('请先选择一个账期账户')
		return
	}
	state.importType = 'payDate'
	uploadDialogRef.value.show({ 'template': 'PurchaseAlibabaPayDate', 'title': '阿里巴巴订单实际付款时间' })
}

function handleUpload(formDatas) {
	uploadDialogRef.value.loading()
	formDatas.append('acct', state.selectedAcctId)
	if (state.importType === 'payDate') {
		purchaseAlibabaSettlementApi.uploadPayDateFile(formDatas, () => {
			loadSettlementList()
			loadSummary()
			nextTick(() => {
				if (settledRecordRef.value) {
					settledRecordRef.value.show(state.queryParams)
				}
			})
			uploadDialogRef.value.hide()
		})
	} else {
		purchaseAlibabaSettlementApi.importSettle(formDatas, (res) => {
			if (res && res.code === 200) {
				ElMessage.success(res.data || '导入成功')
			} else {
				ElMessage.warning(res?.msg || '导入完成')
			}
			nextTick(() => {
				if (settledRecordRef.value) {
					settledRecordRef.value.show(state.queryParams)
				}
			})
			uploadDialogRef.value.hide()
		})
	}
}

function handleSettlementChange(item) {
	state.activeSettlementId = item.id
	handleQuery()
}

function orderChange(value) {
	state.queryParams.settlementid = state.activeSettlementId
	state.queryParams.search = value
	state.queryParams.searchtype = 'orderid'
	state.activeName = 'record'
}

function handleClick() {
	if (state.activeName == 'record') {
		nextTick(() => {
			if (paymentRecordRef.value) paymentRecordRef.value.show(state.queryParams)
		})
	}
	if (state.activeName == 'order') {
		nextTick(() => {
			if (orderRecordRef.value) {
				state.queryParams.search = ''
				state.queryParams.searchtype = ''
				orderRecordRef.value.show(state.queryParams)
			}
		})
	}
	if (state.activeName == 'pay') {
		nextTick(() => {
			if (payRecordRef.value) payRecordRef.value.show(state.queryParams)
		})
	}
	if (state.activeName == 'returnOrder') {
		nextTick(() => {
			if (returnOrderRecordRef.value) returnOrderRecordRef.value.show(state.queryParams)
		})
	}
	if (state.activeName == 'returnPay') {
		nextTick(() => {
			if (returnPayRecordRef.value) returnPayRecordRef.value.show(state.queryParams)
		})
	}
}

function changedate(dates) {
	state.queryParams.fromDate = dates.start
	state.queryParams.toDate = dates.end
	// 如果acct还没设置（Datepicker比loadAcctOptions先触发），跳过加载
	if (!state.selectedAcctId) return
	loadSettlementList()
	loadSummary()
	// 切换日期时刷新当前活动的1688 tab
	handleQuery()
}

function handleQuery() {
	if (state.activeSettlementId) {
		handleClick()
	}
}

function deleteSettlement(value) {
	ElMessageBox.confirm('你确定要删除此账期导入的全部数据吗?', {
		confirmButtonText: '确认',
		cancelButtonText: '取消',
		type: 'warning',
		callback: (action) => {
			if (action == 'confirm') {
				purchaseAlibabaSettlementApi.deleteSettlement({ settlementid: value }).then(res => {
					ElMessage.success('删除成功')
					loadSettlementList()
					loadSummary()
				})
			}
		}
	})
}

function loadSettlementList() {
	purchaseAlibabaSettlementApi.list(state.queryParams).then(res => {
		state.settlementList = res.data || []
		if (state.settlementList.length > 0) {
			state.activeSettlementId = state.settlementList[0].id
			state.queryParams.settlementid = state.activeSettlementId
		} else {
			state.activeSettlementId = ''
			state.queryParams.settlementid = ''
		}
		loadOrderSummary()
		// 只有有结算记录时才刷新订单列表
		if (state.activeSettlementId) {
			handleQuery()
		}
	})
}

function loadSummary() {
	purchaseAlibabaSettlementApi.summary(state.queryParams).then(res => {
		if (res.data) {
			state.summaryData = {
				settledCount: res.data.settledCount || 0,
				settledAmount: res.data.settledAmount || 0,
				settledPaid: res.data.settledPaid || 0,
				unsettledCount: res.data.unsettledCount || 0,
				unsettledAmount: res.data.unsettledAmount || 0,
				unsettledUnpaid: res.data.unsettledUnpaid || 0
			}
			// 计算已付金额 = 已结转已付金额 + 未结转已付金额
			state.acctAmounts[state.selectedAcctId] = {
				paidAmount: (res.data.settledPaid || 0) + (res.data.unsettledAmount || 0),
				transferredAmount: res.data.settledAmount || 0
			}
		}
	})
}

function loadOrderSummary() {
	purchaseAlibabaSettlementApi.orderSummary(state.queryParams).then(res => {
		if (res.data) {
			state.orderSummaryData = {
				totalPayAmount: res.data.totalPayAmount || 0,
				totalConfirmAmount: res.data.totalConfirmAmount || 0,
				totalReturnAmount: res.data.totalReturnAmount || 0
			}
		}
	})
}

// 撤销结转后刷新付款明细列表
function handleSettledRefresh() {
	loadSummary()
	loadOrderSummary()
	// 刷新采购付款明细
	nextTick(() => {
		if (paymentDetailRef.value) {
			paymentDetailRef.value.show({ acct: state.selectedAcctId, acctType: state.selectedAcctType, showSettled: false })
		}
	})
}

onMounted(() => {
	loadAcctOptions()
})
</script>

<style scoped>
.m-b-16 {
	margin-bottom: 16px;
}
.main-card {
	min-height: 300px;
}
.summary-header {
	display: flex;
	align-items: center;
	justify-content: space-between;
	margin-bottom: 16px;
}
.summary-title {
	font-size: 16px;
	font-weight: 600;
	color: #303133;
}
.summary-body {
	margin-bottom: 16px;
}
.summary-label {
	font-size: 13px;
	color: #909399;
	margin-bottom: 4px;
}
.summary-value {
	font-size: 18px;
	font-weight: 600;
}
.text-primary {
	color: #409eff;
}
.text-success {
	color: #67c23a;
}
.text-warning {
	color: #e6a23c;
}
.text-danger {
	color: #f56c6c;
}
.scrollbar-flex-content {
	display: flex;
	margin-top: 20px;
	margin-bottom: 20px;
}
.acct-cards {
	margin-bottom: 0;
}
.acct-card {
	min-width: 180px;
	margin-right: 12px;
	cursor: pointer;
	transition: all 0.3s;
}
.acct-card:hover {
	border-color: var(--el-color-primary-light-5);
}
.acct-card-active {
	border-color: var(--el-color-primary);
	background-color: var(--el-color-primary-light-9);
}
.acct-card-body {
	display: flex;
	align-items: center;
	justify-content: space-between;
}
.acct-card-name {
	font-size: 14px;
	font-weight: 600;
	color: #303133;
	margin-right: 8px;
}
.acct-card-tag {
	flex-shrink: 0;
}
.acct-card-balance {
	margin-top: 8px;
	font-size: 13px;
	color: #606266;
}
.acct-card-amount {
	margin-top: 4px;
	font-size: 13px;
	color: #606266;
}
.settlement-list {
	max-height: 200px;
	overflow-y: auto;
	margin-bottom: 16px;
}
.settlement-item {
	padding: 10px 12px;
	border: 1px solid #e4e7ed;
	border-radius: 4px;
	margin-bottom: 8px;
	cursor: pointer;
	transition: all 0.2s;
}
.settlement-item:hover {
	border-color: var(--el-color-primary-light-5);
}
.settlement-item.active {
	border-color: var(--el-color-primary);
	background-color: var(--el-color-primary-light-9);
}
.settlement-item-header {
	display: flex;
	justify-content: space-between;
	align-items: center;
	margin-bottom: 6px;
}
.settlement-date {
	font-weight: 600;
	color: #303133;
}
.settlement-item-body {
	display: flex;
	gap: 16px;
	font-size: 13px;
	color: #606266;
}
.alibabasettlement .active {
	--el-tag-bg-color: var(--el-color-primary-light-9);
	--el-tag-border-color: var(--el-color-primary-light-8);
	--el-tag-hover-color: var(--el-color-primary);
	--el-tag-text-color: var(--el-color-primary);
	background-color: var(--el-tag-bg-color);
	border-color: var(--el-tag-border-color);
	color: var(--el-tag-text-color);
}
.summary-card {
	margin-bottom: 0;
}
</style>
<style>
.alibabasettlement .el-descriptions__body {
	background-color: unset !important;
}
</style>
