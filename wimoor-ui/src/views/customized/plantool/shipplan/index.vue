<template>
<div class="main-sty">
	<div class="con-header">
		<el-row>
			<el-space :size="6">
				<GroupSelect @change="groupChange" defaultValue="only" />
				<el-select v-model="queryParams.marketplaceid" placeholder="站点" clearable @change="handleQuery" style="width:120px;">
				<el-option v-for="item in marketplaceList" :key="item.marketplaceid" :label="item.name" :value="item.marketplaceid" />
			</el-select>
				<el-select v-model="queryParams.warehouseid" placeholder="仓库" clearable @change="handleQuery" style="width:140px;">
					<el-option v-for="item in warehouseList" :key="item.id" :label="item.name" :value="item.id" />
				</el-select>
				<el-input v-model="queryParams.search" v-debounce-input="handleQuery" placeholder="搜索SKU或名称" clearable style="width:200px;" />
				<el-button type="primary" @click="handleRefresh" :loading="refreshLoading">
					<el-icon><Refresh /></el-icon>
					<span>刷新数据</span>
				</el-button>
			</el-space>
			<div class='rt-btn-group'>
				<el-space :size="16">
					<el-button type="success" @click="handleGenerateShip" :disabled="selectRows.length === 0">
						生成发货单 ({{ selectRows.length }})
					</el-button>
				</el-space>
			</div>
		</el-row>
	</div>
	<GlobalTable ref="globalTableRef"
		:tableData="tableData"
		height="calc(100vh - 230px)"
		:defaultSort="{ prop: 'saleqty', order: 'descending' }"
		@loadTable="loadTableData"
		@selection-change="handleSelectionChange"
		:stripe="true"
		style="width: 100%;margin-bottom:16px;">
		<template #field>
			<el-table-column type="selection" width="55" />
			<el-table-column prop="image" label="图片" width="65">
				<template #default="scope">
					<el-image :src="scope.row.image" style="width:40px;height:40px;" />
				</template>
			</el-table-column>
			<el-table-column prop="sku" label="SKU" width="140" sortable="custom" show-overflow-tooltip />
			<el-table-column prop="name" label="产品名称" min-width="180" show-overflow-tooltip />
			<el-table-column prop="groupname" label="店铺" width="100" show-overflow-tooltip />
			<el-table-column prop="marketname" label="站点" width="70" />
			<el-table-column prop="warehousename" label="仓库" width="100" show-overflow-tooltip />
			<el-table-column prop="fbaqty" label="FBA库存" width="90" sortable="custom" align="right" />
			<el-table-column prop="localqty" label="本地库存" width="90" sortable="custom" align="right" />
			<el-table-column prop="saleqty" label="月销量" width="90" sortable="custom" align="right" />
			<el-table-column prop="avgsaleqty" label="日均销量" width="90" sortable="custom" align="right" />
			<el-table-column label="发货周期" width="120" align="center">
				<template #default="scope">
					<el-input-number v-model="scope.row.deliverycycle" :min="0" :max="365" size="small" controls-position="right"
						@change="handleDeliveryCycleChange(scope.row)" style="width:80px;" />
				</template>
			</el-table-column>
			<el-table-column label="备货周期" width="120" align="center">
				<template #default="scope">
					<el-input-number v-model="scope.row.stockcycle" :min="0" :max="365" size="small" controls-position="right"
						@change="handleStockCycleChange(scope.row)" style="width:80px;" />
				</template>
			</el-table-column>
			<el-table-column prop="planqty" label="建议发货量" width="100" sortable="custom" align="right">
				<template #default="scope">
					<span :class="{ 'text-primary': scope.row.planqty > 0 }">{{ scope.row.planqty }}</span>
				</template>
			</el-table-column>
		</template>
	</GlobalTable>
</div>
</template>
<script>
export default { name: "出货计划" };
</script>
<script setup>
import { reactive, toRefs, ref, onMounted } from 'vue';
import { Refresh } from '@element-plus/icons-vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import GroupSelect from '@/components/header/group_select.vue';
import marketApi from '@/api/amazon/market/marketApi.js';
import plantoolApi from '../api/plantoolApi.js';

const globalTableRef = ref();

const state = reactive({
	queryParams: {
		search: '',
		groupid: '',
		marketplaceid: '',
		warehouseid: ''
	},
	marketplaceList: [],
	warehouseList: [],
	tableData: { records: [], total: 0 },
	selectRows: [],
	refreshLoading: false
});
const { queryParams, marketplaceList, warehouseList, tableData, selectRows, refreshLoading } = toRefs(state);

function groupChange(val) {
	state.queryParams.groupid = val;
	loadWarehouseList();
	handleQuery();
}

function handleQuery() {
	globalTableRef.value?.loadTable(state.queryParams);
}

function loadTableData(params) {
	plantoolApi.shipPlanList(params).then(res => {
		state.tableData.records = res.data?.records || [];
		state.tableData.total = res.data?.total || 0;
	});
}

function handleSelectionChange(selection) {
	state.selectRows = selection;
}

function loadWarehouseList() {
	plantoolApi.selectWareHouseList({ groupid: state.queryParams.groupid }).then(res => {
		state.warehouseList = res.data || [];
	});
}

function handleRefresh() {
	state.refreshLoading = true;
	plantoolApi.refreshShipPlan(state.queryParams).then(() => {
		ElMessage.success('刷新成功');
		handleQuery();
	}).finally(() => {
		state.refreshLoading = false;
	});
}

function handleDeliveryCycleChange(row) {
	plantoolApi.updateDeliverycycle({
		id: row.id,
		deliverycycle: row.deliverycycle
	}).then(() => {
		ElMessage.success('更新成功');
	});
}

function handleStockCycleChange(row) {
	plantoolApi.updateStockcycle({
		id: row.id,
		stockcycle: row.stockcycle
	}).then(() => {
		ElMessage.success('更新成功');
	});
}

function handleGenerateShip() {
	if (state.selectRows.length === 0) {
		ElMessage.warning('请选择要生成发货单的数据');
		return;
	}
	ElMessageBox.confirm('确认生成发货单?', '提示', {
		confirmButtonText: '确定',
		cancelButtonText: '取消',
		type: 'warning'
	}).then(() => {
		const ids = state.selectRows.map(row => row.id);
		plantoolApi.saveToPlanItem({ ids }).then(() => {
			ElMessage.success('生成成功');
			handleQuery();
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
	loadWarehouseList();
});
</script>
<style scoped>
.text-primary {
	color: #409eff;
	font-weight: bold;
}
</style>
