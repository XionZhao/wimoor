<template>
<div class="main-sty">
	<div class="con-header">
		<el-row>
			<el-space :size="6">
				<GroupSelect @change="groupChange" defaultValue="only" />
				<el-input v-model="queryParams.search" v-debounce-input="handleQuery" placeholder="搜索单号或供应商" clearable style="width:200px;" />
			</el-space>
			<div class='rt-btn-group'>
				<el-space :size="16">
					<span class="font-base" v-if="activeTab === 'submit'">
						<span class="font-base-nine">待提交:</span>{{ approveNums.submitNum || 0 }}
					</span>
					<span class="font-base" v-if="activeTab === 'approve'">
						<span class="font-base-nine">待审批:</span>{{ approveNums.approveNum || 0 }}
					</span>
				</el-space>
			</div>
		</el-row>
	</div>
	<el-tabs v-model="activeTab" class="demo-tabs" @tab-change="handleTabChange">
		<el-tab-pane label="待提交" name="submit">
			<GlobalTable ref="submitTableRef"
				:tableData="submitTableData"
				height="calc(100vh - 280px)"
				:defaultSort="{ prop: 'createtime', order: 'descending' }"
				@loadTable="loadSubmitTableData"
				@selection-change="handleSubmitSelectionChange"
				:stripe="true"
				style="width: 100%;margin-bottom:16px;">
				<template #field>
					<el-table-column type="selection" width="55" />
					<el-table-column prop="formno" label="单号" width="180" sortable="custom" show-overflow-tooltip />
					<el-table-column prop="suppliername" label="供应商" width="150" show-overflow-tooltip />
					<el-table-column prop="groupname" label="店铺" width="120" show-overflow-tooltip />
					<el-table-column prop="totalamount" label="总金额" width="120" sortable="custom" align="right">
						<template #default="scope">
							{{ scope.row.currency || '￥' }}{{ scope.row.totalamount }}
						</template>
					</el-table-column>
					<el-table-column prop="itemcount" label="物料数量" width="100" align="right" />
					<el-table-column prop="createtime" label="创建时间" width="160" sortable="custom" />
					<el-table-column prop="remark" label="备注" min-width="150" show-overflow-tooltip />
					<el-table-column label="操作" width="120" fixed="right">
						<template #default="scope">
							<el-button type="primary" link @click="handleSubmit(scope.row)">提交</el-button>
						</template>
					</el-table-column>
				</template>
			</GlobalTable>
			<div class="table-footer" v-if="submitSelectRows.length > 0">
				<el-button type="primary" @click="handleBatchSubmit">批量提交 ({{ submitSelectRows.length }})</el-button>
			</div>
		</el-tab-pane>
		<el-tab-pane label="待审批" name="approve">
			<GlobalTable ref="approveTableRef"
				:tableData="approveTableData"
				height="calc(100vh - 280px)"
				:defaultSort="{ prop: 'submittime', order: 'descending' }"
				@loadTable="loadApproveTableData"
				@selection-change="handleApproveSelectionChange"
				:stripe="true"
				style="width: 100%;margin-bottom:16px;">
				<template #field>
					<el-table-column type="selection" width="55" />
					<el-table-column prop="formno" label="单号" width="180" sortable="custom" show-overflow-tooltip />
					<el-table-column prop="suppliername" label="供应商" width="150" show-overflow-tooltip />
					<el-table-column prop="groupname" label="店铺" width="120" show-overflow-tooltip />
					<el-table-column prop="totalamount" label="总金额" width="120" sortable="custom" align="right">
						<template #default="scope">
							{{ scope.row.currency || '￥' }}{{ scope.row.totalamount }}
						</template>
					</el-table-column>
					<el-table-column prop="itemcount" label="物料数量" width="100" align="right" />
					<el-table-column prop="submitter" label="提交人" width="100" />
					<el-table-column prop="submittime" label="提交时间" width="160" sortable="custom" />
					<el-table-column prop="remark" label="备注" min-width="150" show-overflow-tooltip />
					<el-table-column label="操作" width="160" fixed="right">
						<template #default="scope">
							<el-button type="success" link @click="handleApprove(scope.row)">审批通过</el-button>
							<el-button type="danger" link @click="handleReject(scope.row)">驳回</el-button>
						</template>
					</el-table-column>
				</template>
			</GlobalTable>
			<div class="table-footer" v-if="approveSelectRows.length > 0">
				<el-button type="success" @click="handleBatchApprove">批量通过 ({{ approveSelectRows.length }})</el-button>
			</div>
		</el-tab-pane>
		<el-tab-pane label="历史记录" name="history">
			<GlobalTable ref="historyTableRef"
				:tableData="historyTableData"
				height="calc(100vh - 280px)"
				:defaultSort="{ prop: 'audittime', order: 'descending' }"
				@loadTable="loadHistoryTableData"
				:stripe="true"
				style="width: 100%;margin-bottom:16px;">
				<template #field>
					<el-table-column prop="formno" label="单号" width="180" sortable="custom" show-overflow-tooltip />
					<el-table-column prop="suppliername" label="供应商" width="150" show-overflow-tooltip />
					<el-table-column prop="groupname" label="店铺" width="120" show-overflow-tooltip />
					<el-table-column prop="totalamount" label="总金额" width="120" sortable="custom" align="right">
						<template #default="scope">
							{{ scope.row.currency || '￥' }}{{ scope.row.totalamount }}
						</template>
					</el-table-column>
					<el-table-column label="状态" width="100" align="center">
						<template #default="scope">
							<el-tag :type="scope.row.auditstatus === 'approved' ? 'success' : 'danger'" size="small">
								{{ scope.row.auditstatus === 'approved' ? '已通过' : '已驳回' }}
							</el-tag>
						</template>
					</el-table-column>
					<el-table-column prop="auditor" label="审批人" width="100" />
					<el-table-column prop="audittime" label="审批时间" width="160" sortable="custom" />
					<el-table-column prop="remark" label="备注" min-width="150" show-overflow-tooltip />
				</template>
			</GlobalTable>
		</el-tab-pane>
	</el-tabs>
</div>
</template>
<script>
export default { name: "提货付款" };
</script>
<script setup>
import { reactive, toRefs, ref, onMounted } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import GroupSelect from '@/components/header/group_select.vue';
import plantoolApi from '../api/plantoolApi.js';

const submitTableRef = ref();
const approveTableRef = ref();
const historyTableRef = ref();

const state = reactive({
	activeTab: 'submit',
	queryParams: {
		search: '',
		groupid: ''
	},
	approveNums: {
		submitNum: 0,
		approveNum: 0
	},
	submitTableData: { records: [], total: 0 },
	approveTableData: { records: [], total: 0 },
	historyTableData: { records: [], total: 0 },
	submitSelectRows: [],
	approveSelectRows: []
});
const { activeTab, queryParams, approveNums, submitTableData, approveTableData, historyTableData, submitSelectRows, approveSelectRows } = toRefs(state);

function groupChange(val) {
	state.queryParams.groupid = val;
	loadApproveNums();
	handleQuery();
}

function handleQuery() {
	if (state.activeTab === 'submit') {
		submitTableRef.value?.loadTable(state.queryParams);
	} else if (state.activeTab === 'approve') {
		approveTableRef.value?.loadTable(state.queryParams);
	} else if (state.activeTab === 'history') {
		historyTableRef.value?.loadTable(state.queryParams);
	}
}

function handleTabChange() {
	handleQuery();
}

function loadApproveNums() {
	plantoolApi.loadApproveNums(state.queryParams).then(res => {
		state.approveNums = res.data || { submitNum: 0, approveNum: 0 };
	});
}

function loadSubmitTableData(params) {
	plantoolApi.pickPayList({ ...params, status: 'submit' }).then(res => {
		state.submitTableData.records = res.data?.records || [];
		state.submitTableData.total = res.data?.total || 0;
	});
}

function loadApproveTableData(params) {
	plantoolApi.pickPayList({ ...params, status: 'approve' }).then(res => {
		state.approveTableData.records = res.data?.records || [];
		state.approveTableData.total = res.data?.total || 0;
	});
}

function loadHistoryTableData(params) {
	plantoolApi.pickPayList({ ...params, status: 'history' }).then(res => {
		state.historyTableData.records = res.data?.records || [];
		state.historyTableData.total = res.data?.total || 0;
	});
}

function handleSubmitSelectionChange(selection) {
	state.submitSelectRows = selection;
}

function handleApproveSelectionChange(selection) {
	state.approveSelectRows = selection;
}

function handleSubmit(row) {
	ElMessageBox.confirm('确认提交?', '提示', {
		confirmButtonText: '确定',
		cancelButtonText: '取消',
		type: 'warning'
	}).then(() => {
		plantoolApi.submitPickPay({ ids: [row.id] }).then(() => {
			ElMessage.success('提交成功');
			loadApproveNums();
			handleQuery();
		});
	}).catch(() => {});
}

function handleBatchSubmit() {
	if (state.submitSelectRows.length === 0) {
		ElMessage.warning('请选择要提交的数据');
		return;
	}
	ElMessageBox.confirm(`确认提交${state.submitSelectRows.length}条数据?`, '提示', {
		confirmButtonText: '确定',
		cancelButtonText: '取消',
		type: 'warning'
	}).then(() => {
		const ids = state.submitSelectRows.map(row => row.id);
		plantoolApi.submitPickPay({ ids }).then(() => {
			ElMessage.success('提交成功');
			loadApproveNums();
			handleQuery();
		});
	}).catch(() => {});
}

function handleApprove(row) {
	ElMessageBox.confirm('确认审批通过?', '提示', {
		confirmButtonText: '确定',
		cancelButtonText: '取消',
		type: 'warning'
	}).then(() => {
		plantoolApi.approvePickPay({ ids: [row.id], action: 'approve' }).then(() => {
			ElMessage.success('审批成功');
			loadApproveNums();
			handleQuery();
		});
	}).catch(() => {});
}

function handleReject(row) {
	ElMessageBox.prompt('请输入驳回原因', '驳回', {
		confirmButtonText: '确定',
		cancelButtonText: '取消',
		inputPattern: /\S+/,
		inputErrorMessage: '请输入驳回原因'
	}).then(({ value }) => {
		plantoolApi.approvePickPay({ ids: [row.id], action: 'reject', remark: value }).then(() => {
			ElMessage.success('已驳回');
			loadApproveNums();
			handleQuery();
		});
	}).catch(() => {});
}

function handleBatchApprove() {
	if (state.approveSelectRows.length === 0) {
		ElMessage.warning('请选择要审批的数据');
		return;
	}
	ElMessageBox.confirm(`确认审批通过${state.approveSelectRows.length}条数据?`, '提示', {
		confirmButtonText: '确定',
		cancelButtonText: '取消',
		type: 'warning'
	}).then(() => {
		const ids = state.approveSelectRows.map(row => row.id);
		plantoolApi.approvePickPay({ ids, action: 'approve' }).then(() => {
			ElMessage.success('审批成功');
			loadApproveNums();
			handleQuery();
		});
	}).catch(() => {});
}

onMounted(() => {
	loadApproveNums();
});
</script>
<style scoped>
.table-footer {
	padding: 10px 0;
	text-align: right;
}
</style>
