<template>
<div class="main-sty">
	<div class="con-header">
		<el-row>
			<el-space :size="6">
				<GroupSelect @change="groupChange" defaultValue="only" />
				<el-select v-model="queryParams.marketplaceid" placeholder="站点" clearable @change="handleQuery" style="width:120px;">
				<el-option v-for="item in marketplaceList" :key="item.marketplaceid" :label="item.name" :value="item.marketplaceid" />
			</el-select>
				<el-select v-model="queryParams.supplierid" placeholder="供应商" clearable filterable @change="handleQuery" style="width:150px;">
					<el-option v-for="item in supplierList" :key="item.id" :label="item.name" :value="item.id" />
				</el-select>
				<el-input v-model="queryParams.search" v-debounce-input="handleQuery" placeholder="搜索物料SKU或名称" clearable style="width:200px;" />
				<el-button type="primary" @click="handleRefresh" :loading="refreshLoading">
					<el-icon><Refresh /></el-icon>
					<span>刷新数据</span>
				</el-button>
			</el-space>
			<div class='rt-btn-group'>
				<el-space :size="16">
					<el-button type="success" @click="handleGeneratePurchase" :disabled="selectRows.length === 0">
						生成采购单 ({{ selectRows.length }})
					</el-button>
				</el-space>
			</div>
		</el-row>
	</div>
	<GlobalTable ref="globalTableRef"
		:tableData="tableData"
		height="calc(100vh - 230px)"
		:defaultSort="{ prop: 'needqty', order: 'descending' }"
		@loadTable="loadTableData"
		@selection-change="handleSelectionChange"
		:stripe="true"
		style="width: 100%;margin-bottom:16px;">
		<template #field>
			<el-table-column type="selection" width="55" />
			<el-table-column type="expand" width="40">
				<template #default="scope">
					<div class="expand-container">
						<el-table :data="scope.row.weekDetails || []" border size="small" style="width:100%;">
							<el-table-column prop="weeklabel" label="周" width="100" />
							<el-table-column prop="needqty" label="需求量" width="100" align="right" />
							<el-table-column prop="stockqty" label="库存量" width="100" align="right" />
							<el-table-column prop="shortqty" label="缺口量" width="100" align="right">
								<template #default="weekScope">
									<span :class="{ 'text-danger': weekScope.row.shortqty > 0 }">{{ weekScope.row.shortqty }}</span>
								</template>
							</el-table-column>
							<el-table-column prop="remark" label="备注" min-width="150" />
						</el-table>
					</div>
				</template>
			</el-table-column>
			<el-table-column prop="image" label="图片" width="65">
				<template #default="scope">
					<el-image :src="scope.row.image" style="width:40px;height:40px;" />
				</template>
			</el-table-column>
			<el-table-column prop="sku" label="物料SKU" width="140" sortable="custom" show-overflow-tooltip />
			<el-table-column prop="name" label="物料名称" min-width="180" show-overflow-tooltip />
			<el-table-column prop="suppliername" label="供应商" width="120" show-overflow-tooltip />
			<el-table-column prop="groupname" label="店铺" width="100" show-overflow-tooltip />
			<el-table-column prop="needqty" label="需求量" width="90" sortable="custom" align="right" />
			<el-table-column prop="stockqty" label="库存量" width="90" sortable="custom" align="right" />
			<el-table-column prop="shortqty" label="缺口量" width="90" sortable="custom" align="right">
				<template #default="scope">
					<span :class="{ 'text-danger': scope.row.shortqty > 0 }">{{ scope.row.shortqty }}</span>
				</template>
			</el-table-column>
			<el-table-column label="采购天数" width="120" align="center">
				<template #default="scope">
					<el-input-number v-model="scope.row.purchasedaynum" :min="0" :max="365" size="small" controls-position="right"
						@change="handlePurchasedaynumChange(scope.row)" style="width:80px;" />
				</template>
			</el-table-column>
			<el-table-column prop="purchasenum" label="建议采购量" width="110" sortable="custom" align="right">
				<template #default="scope">
					<span :class="{ 'text-primary': scope.row.purchasenum > 0 }">{{ scope.row.purchasenum }}</span>
				</template>
			</el-table-column>
		</template>
	</GlobalTable>
</div>
</template>
<script>
export default { name: "物料需求" };
</script>
<script setup>
import { reactive, toRefs, ref, onMounted } from 'vue';
import { Refresh } from '@element-plus/icons-vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import GroupSelect from '@/components/header/group_select.vue';
import marketApi from '@/api/amazon/market/marketApi.js';
import customerApi from '@/api/erp/material/customerApi.js';
import plantoolApi from '../api/plantoolApi.js';

const globalTableRef = ref();

const state = reactive({
	queryParams: {
		search: '',
		groupid: '',
		marketplaceid: '',
		supplierid: ''
	},
	marketplaceList: [],
	supplierList: [],
	tableData: { records: [], total: 0 },
	selectRows: [],
	refreshLoading: false
});
const { queryParams, marketplaceList, supplierList, tableData, selectRows, refreshLoading } = toRefs(state);

function groupChange(val) {
	state.queryParams.groupid = val;
	handleQuery();
}

function handleQuery() {
	globalTableRef.value?.loadTable(state.queryParams);
}

function loadTableData(params) {
	plantoolApi.purchasePlanList(params).then(res => {
		state.tableData.records = res.data?.records || [];
		state.tableData.total = res.data?.total || 0;
	});
}

function handleSelectionChange(selection) {
	state.selectRows = selection;
}

function handleExpandChange(row, expanded) {
	if (expanded && (!row.weekDetails || row.weekDetails.length === 0)) {
		plantoolApi.getWeekDetail({ id: row.id }).then(res => {
			row.weekDetails = res.data || [];
		});
	}
}

function handleRefresh() {
	state.refreshLoading = true;
	plantoolApi.refreshPurchasePlan(state.queryParams).then(() => {
		ElMessage.success('刷新成功');
		handleQuery();
	}).finally(() => {
		state.refreshLoading = false;
	});
}

function handlePurchasedaynumChange(row) {
	plantoolApi.updatePurchasedaynum({
		id: row.id,
		purchasedaynum: row.purchasedaynum
	}).then(() => {
		ElMessage.success('更新成功');
		handleQuery();
	});
}

function handleGeneratePurchase() {
	if (state.selectRows.length === 0) {
		ElMessage.warning('请选择要生成采购单的物料');
		return;
	}
	ElMessageBox.confirm('确认生成采购单?', '提示', {
		confirmButtonText: '确定',
		cancelButtonText: '取消',
		type: 'warning'
	}).then(() => {
		const ids = state.selectRows.map(row => row.id);
		plantoolApi.checkMaterial({ ids }).then(res => {
			if (res.data && res.data.length > 0) {
				ElMessage.warning('部分物料信息不完整，请检查');
			} else {
				plantoolApi.savePurchaseForm({ ids }).then(() => {
					ElMessage.success('生成成功');
					handleQuery();
				});
			}
		});
	}).catch(() => {});
}

onMounted(() => {
	// 加载站点列表并默认选中第一个
	marketApi.getMarketAll().then(res => {
		state.marketplaceList = res.data || [];
		if (state.marketplaceList.length > 0) {
			state.queryParams.marketplaceid = state.marketplaceList[0].marketplaceid;
		}
	});
	// 加载供应商列表
	customerApi.listAll().then(res => {
		state.supplierList = res.data || [];
	});
});
</script>
<style scoped>
.expand-container {
	padding: 10px 50px;
}
.text-primary {
	color: #409eff;
	font-weight: bold;
}
.text-danger {
	color: #f56c6c;
	font-weight: bold;
}
</style>
