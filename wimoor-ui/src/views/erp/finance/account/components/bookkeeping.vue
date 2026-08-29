<template>
<div class="bookkeeping-container">
	<el-row :gutter="16">
		<el-col :span="8">
			<el-card shadow="never" class="bookkeeping-card">
				<template #header>
					<div class="flex-center-between">
						<span class="font-bold">新建记账</span>
						<span v-if="activeItem.id" class="font-extraSmall">当前账户：{{ activeItem.name }}</span>
					</div>
				</template>
				<el-form :model="bookForm" label-width="100px" label-position="top">
					<el-form-item label="店铺" required>
						<GroupSelect :value="bookForm.groupid" defaultValue="only" @change="handleBookGroupChange" />
					</el-form-item>
					<el-form-item label="账户" required>
						<el-select v-model="bookForm.acct" placeholder="请选择账户" style="width: 100%">
							<el-option v-for="item in accountOptions" :key="item.id" :label="item.name" :value="item.id"/>
						</el-select>
					</el-form-item>
					<el-form-item label="记账类型" required>
						<el-radio-group v-model="bookForm.ftype">
							<el-radio label="in">收入</el-radio>
							<el-radio label="out">支出</el-radio>
						</el-radio-group>
					</el-form-item>
					<el-form-item label="收支项目" required>
					<div class="flex-center" style="width:100%">
						<el-select v-model="bookForm.projectid" placeholder="请选择收支项目" style="flex:1">
							<el-option v-for="item in feeTypeList" :key="item.id" :label="item.name" :value="item.id"/>
						</el-select>
						<el-button type="primary" link style="margin-left:8px" @click="handleFeeItem"><el-icon><Edit /></el-icon> 编辑</el-button>
					</div>
				</el-form-item>
					<el-form-item label="金额" required>
						<el-input v-model="bookForm.amount" placeholder="请输入金额">
							<template #prefix>￥</template>
						</el-input>
					</el-form-item>
					<el-form-item label="备注">
						<el-input v-model="bookForm.remark" type="textarea" :rows="2" placeholder="请输入备注信息"/>
					</el-form-item>
					<el-form-item>
						<el-button type="primary" @click="handleBookSubmit">生成记账</el-button>
						<el-button @click="resetBookForm">重置</el-button>
					</el-form-item>
				</el-form>
			</el-card>
		</el-col>
		<el-col :span="16">
			<el-card shadow="never" class="bookkeeping-card">
				<template #header>
					<div class="flex-center-between">
						<span class="font-bold">记账记录</span>
						<div class="flex-center">
							<Datepicker style="width: 240px; margin-right: 16px;" shortIndex="1" ref="datepickersRef" @changedate="changedate" />
							<div class="summary-wrap">
								<span>收入：<b class="text-success">{{summaryData.inTotal}}</b></span>
								<span style="margin-left:12px">支出：<b class="text-danger">{{summaryData.outTotal}}</b></span>
							</div>
						</div>
					</div>
				</template>
				<div class="bookkeeping-filter">
					<el-form :inline="true" :model="bookFilter" size="default">
						<el-form-item label="店铺">
							<GroupSelect :value="bookFilter.groupid" defaultValue="all" @change="handleGroupChange" />
						</el-form-item>
						<el-form-item label="账户">
							<el-select v-model="bookFilter.acct" placeholder="全部" clearable style="width: 140px">
								<el-option v-for="item in accountOptions" :key="item.id" :label="item.name" :value="item.id"/>
							</el-select>
						</el-form-item>
						<el-form-item label="类型">
							<el-select v-model="bookFilter.ftype" placeholder="全部" clearable style="width: 100px">
								<el-option label="收入" value="in"/>
								<el-option label="支出" value="out"/>
							</el-select>
						</el-form-item>
						<el-form-item label="收支项目">
						<el-select v-model="bookFilter.projectid" placeholder="全部" clearable style="width: 130px">
							<el-option v-for="item in feeTypeList" :key="item.id" :label="item.name" :value="item.id"/>
						</el-select>
					</el-form-item>
					<el-form-item>
						<el-button type="primary" @click="handleQuery">查询</el-button>
						<el-button @click="resetBookFilter">重置</el-button>
					</el-form-item>
					</el-form>
				</div>
				<el-table :data="bookRecords" border stripe style="width: 100%" height="calc(100vh - 350px)">
					<el-table-column label="日期" prop="createtime" width="160"/>
					<el-table-column label="店铺" prop="groupName" width="120"/>
					<el-table-column label="账户" prop="acctName" width="180"/>
					<el-table-column label="记账类型" width="90">
						<template #default="scope">
							<el-tag v-if="scope.row.ftype==='in'" type="success" size="small">收入</el-tag>
							<el-tag v-else type="danger" size="small">支出</el-tag>
						</template>
					</el-table-column>
					<el-table-column label="收支项目" prop="name" width="240"/>
					<el-table-column label="金额(￥)" prop="amount" width="100"/>
					<el-table-column label="备注" prop="remark" show-overflow-tooltip/>
					<el-table-column label="操作" width="80" align="center">
						<template #default="scope">
							<el-button link type="primary" size="small" @click="cancelBookItem(scope.row)">撤销</el-button>
						</template>
					</el-table-column>
				</el-table>
			<div class="pagination-wrap">
				<el-pagination
					v-model:current-page="pagination.currentpage"
					v-model:page-size="pagination.pagesize"
					:page-sizes="[10, 20, 50, 100]"
					:total="pagination.total"
					layout="total, sizes, prev, pager, next, jumper"
					@size-change="handlePageChange"
					@current-change="handlePageChange"
				/>
			</div>
		</el-card>
		</el-col>
	</el-row>
</div>
<FinItem ref="finItemRef" @change="loadFeeTypes"></FinItem>
</template>

<script setup>
import { ref, reactive, toRefs, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Edit } from '@element-plus/icons-vue'
import journalApi from '@/api/erp/finances/journalApi.js'
import faccountApi from '@/api/erp/finances/faccountApi.js'
import FinItem from './finItem.vue'
import Datepicker from '@/components/header/datepicker.vue'
import GroupSelect from '@/components/header/group_select.vue'
import groupApi from '@/api/amazon/group/groupApi.js'

const DataList = ref([])
const feeTypeList = ref([])
const activeItem = ref({})
const finItemRef = ref()
const datepickersRef = ref()

const state = reactive({
	bookForm: {
		acct: '',
		ftype: 'out',
		projectid: '',
		amount: '',
		remark: '',
		groupid: '',
	},
	bookFilter: {
		fromDate: '',
		toDate: '',
		acct: '',
		ftype: '',
		projectid: '',
		groupid: '',
	},
})
const bookRecords = ref([])
const allRecords = ref([])
const pagination = reactive({
	currentpage: 1,
	pagesize: 10,
	total: 0,
})
const summaryData = reactive({
	inTotal: '0.00',
	outTotal: '0.00',
	netTotal: '0.00',
})
const { bookForm, bookFilter } = toRefs(state)

const accountOptions = computed(() => {
	return DataList.value.filter(item => item.id)
})

function loadMyAccount() {
	faccountApi.getAccountAll().then(res => {
		DataList.value = res.data || []
		DataList.value.sort((a, b) => (a.findex || 0) - (b.findex || 0))
	})
}

function loadFeeTypes() {
	faccountApi.getProject().then((res) => {
		feeTypeList.value = res.data || []
	})
}

function handleFeeItem() {
	finItemRef.value.show()
}

function handleBookGroupChange(val) {
	state.bookForm.groupid = val || ''
}

function handleGroupChange(val) {
	state.bookFilter.groupid = val || ''
	loadBookRecords()
}

function changedate(dates) {
	state.bookFilter.fromDate = dates.start
	state.bookFilter.toDate = dates.end
	loadBookRecords()
}

function handleBookSubmit() {
	if (!state.bookForm.groupid) {
		ElMessage.error('请选择店铺！')
		return
	}
	if (!state.bookForm.acct) {
		ElMessage.error('请选择一个具体的账户！')
		return
	}
	if (!state.bookForm.projectid) {
		ElMessage.error('请选择一个具体的费用类型！')
		return
	}
	if (!state.bookForm.amount) {
		ElMessage.error('请输入金额！')
		return
	}
	if (parseFloat(state.bookForm.amount) < 0) {
		ElMessage.error('请输入金额必须为正数！')
		return
	}
	var data = {
		acct: state.bookForm.acct,
		ftype: state.bookForm.ftype,
		projectid: state.bookForm.projectid,
		amount: parseFloat(state.bookForm.amount),
		remark: state.bookForm.remark,
		groupid: state.bookForm.groupid,
	}
	journalApi.save(data).then((res) => {
		if (res.data && res.data.msg) {
			if (res.data.msg.includes('成功')) {
				ElMessage.success('记账成功！')
				resetBookForm()
				loadBookRecords()
			} else {
				ElMessage.error(res.data.msg)
			}
		} else {
			ElMessage.success('记账成功！')
			resetBookForm()
			loadBookRecords()
		}
	}).catch(() => {
		ElMessage.error('记账失败，请重试')
	})
}

function resetBookForm() {
	state.bookForm.acct = ''
	state.bookForm.ftype = 'out'
	state.bookForm.projectid = ''
	state.bookForm.amount = ''
	state.bookForm.remark = ''
	state.bookForm.groupid = ''
}

function loadBookRecords() {
	var param = {}
	if (state.bookFilter.fromDate) {
		param.fromDate = state.bookFilter.fromDate
	}
	if (state.bookFilter.toDate) {
		param.toDate = state.bookFilter.toDate
	}
	if (state.bookFilter.acct) {
		param.acc = state.bookFilter.acct
	}
	if (state.bookFilter.projectid) {
		param.project = state.bookFilter.projectid
	}
	if (state.bookFilter.groupid) {
		param.groupid = state.bookFilter.groupid
	}
	// 获取店铺列表用于映射名称
	groupApi.getAmazonGroup().then(groupRes => {
		var groupMap = {}
		if (groupRes.data) {
			groupRes.data.forEach(function (item) {
				groupMap[item.id] = item.name
			})
		}
		journalApi.findDetailList(param).then(res => {
			if (res.data) {
				var acctMap = {}
				DataList.value.forEach(function (item) {
					if (item.id) {
						acctMap[item.id] = item.name
					}
				})
				var records = Array.isArray(res.data) ? res.data : (res.data.records || [])
				// 只显示t_erp_fin_journalaccount表的记录（isable=1为手动记账，0为采购付款）
				records = records.filter(function (item) {
					return item.isable === 1 || item.isable === '1'
				})
				records.forEach(function (item) {
					item.acctName = acctMap[item.acct] || item.acct || ''
					item.groupName = groupMap[item.groupid] || item.groupid || ''
				})
				if (state.bookFilter.ftype) {
					records = records.filter(function (item) {
						return item.ftype === state.bookFilter.ftype
					})
				}
				allRecords.value = records
				pagination.total = records.length
				calculateSummary()
				applyPagination()
			} else {
				allRecords.value = []
				bookRecords.value = []
				pagination.total = 0
				summaryData.inTotal = '0.00'
				summaryData.outTotal = '0.00'
				summaryData.netTotal = '0.00'
			}
		}).catch(() => { })
	}).catch(() => { })
}

function handleQuery() {
	pagination.currentpage = 1
	loadBookRecords()
}

function handlePageChange() {
	applyPagination()
}

function applyPagination() {
	var start = (pagination.currentpage - 1) * pagination.pagesize
	var end = start + pagination.pagesize
	bookRecords.value = allRecords.value.slice(start, end)
}

function calculateSummary() {
	var inTotal = 0
	var outTotal = 0
	allRecords.value.forEach(function (item) {
		var amount = parseFloat(item.amount) || 0
		if (item.ftype === 'in') {
			inTotal += amount
		} else {
			outTotal += amount
		}
	})
	summaryData.inTotal = inTotal.toFixed(2)
	summaryData.outTotal = outTotal.toFixed(2)
	summaryData.netTotal = (inTotal - outTotal).toFixed(2)
}

function resetBookFilter() {
	state.bookFilter.fromDate = ''
	state.bookFilter.toDate = ''
	state.bookFilter.acct = ''
	state.bookFilter.ftype = ''
	state.bookFilter.projectid = ''
	state.bookFilter.groupid = ''
	pagination.currentpage = 1
	if (datepickersRef.value) {
		datepickersRef.value.reset()
	}
}

function cancelBookItem(row) {
	ElMessageBox.confirm(
		'你确定要撤销该笔记账吗?',
		{
			confirmButtonText: '确认',
			cancelButtonText: '取消',
			type: 'warning',
			callback: (action) => {
				if (action == "confirm") {
					journalApi.cancel({ "id": row.id }).then(() => {
						ElMessage.success('撤销成功')
						loadBookRecords()
					})
				}
			}
		}
	)
}

onMounted(() => {
	loadMyAccount()
	loadFeeTypes()
})
</script>

<style scoped>
.bookkeeping-container{
	padding-top: 0px;
}
.bookkeeping-card{
	border-radius: var(--el-border-radius-base);
}
.bookkeeping-filter{
	margin-bottom: 12px;
	padding-bottom: 4px;
	border-bottom: 1px solid var(--el-border-color-lighter);
}
.bookkeeping-filter :deep(.el-form-item){
	margin-bottom: 10px;
}
.pagination-wrap{
	display: flex;
	justify-content: flex-end;
	margin-top: 12px;
}
.summary-wrap{
	font-size: 13px;
}
.text-success{
	color: var(--el-color-success);
}
.text-danger{
	color: var(--el-color-danger);
}
</style>
