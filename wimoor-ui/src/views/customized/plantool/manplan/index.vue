<template>
<div class="main-sty">
	<div class="con-header">
		<el-row>
			<el-space :size="6">
				<GroupSelect @change="groupChange" defaultValue="only" />
				<el-select v-model="queryParams.marketplaceid" placeholder="站点" clearable @change="handleQuery" style="width:120px;">
				<el-option v-for="item in marketplaceList" :key="item.marketplaceid" :label="item.name" :value="item.marketplaceid" />
			</el-select>
				<el-input v-model="queryParams.search" v-debounce-input="handleQuery" placeholder="搜索SKU或名称" clearable style="width:200px;" />
				<el-button type="primary" @click="handleRefresh" :loading="refreshLoading">
					<el-icon><Refresh /></el-icon>
					<span>刷新数据</span>
				</el-button>
			</el-space>
			<div class='rt-btn-group'>
				<el-space :size="16">
					<span class="font-base">
						<span class="font-base-nine">总工时:</span>{{ totalWorkHours }}h
					</span>
					<span class="font-base">
						<span class="font-base-nine">总人数:</span>{{ totalPersonNum }}
					</span>
				</el-space>
			</div>
		</el-row>
	</div>
	<GlobalTable ref="globalTableRef"
		:tableData="tableData"
		height="calc(100vh - 230px)"
		:defaultSort="{ prop: 'planqty', order: 'descending' }"
		@loadTable="loadTableData"
		:stripe="true"
		style="width: 100%;margin-bottom:16px;">
		<template #field>
			<el-table-column prop="image" label="图片" width="65">
				<template #default="scope">
					<el-image :src="scope.row.image" style="width:40px;height:40px;" />
				</template>
			</el-table-column>
			<el-table-column prop="sku" label="SKU" width="140" sortable="custom" show-overflow-tooltip />
			<el-table-column prop="name" label="产品名称" min-width="180" show-overflow-tooltip />
			<el-table-column prop="groupname" label="店铺" width="100" show-overflow-tooltip />
			<el-table-column prop="marketname" label="站点" width="70" />
			<el-table-column prop="planqty" label="计划数量" width="100" sortable="custom" align="right" />
			<el-table-column label="产品工时(h)" width="130" align="center">
				<template #default="scope">
					<el-input-number v-model="scope.row.proworkhours" :min="0" :max="9999" :precision="2" size="small" controls-position="right"
						@change="handleProWorkHoursChange(scope.row)" style="width:100px;" />
				</template>
			</el-table-column>
			<el-table-column label="单位工时(h)" width="130" align="center">
				<template #default="scope">
					<el-input-number v-model="scope.row.unitstime" :min="0" :max="9999" :precision="2" size="small" controls-position="right"
						@change="handleUnitsTimeChange(scope.row)" style="width:100px;" />
				</template>
			</el-table-column>
			<el-table-column prop="totalworkhours" label="总工时(h)" width="100" sortable="custom" align="right">
				<template #default="scope">
					{{ scope.row.totalworkhours || 0 }}
				</template>
			</el-table-column>
			<el-table-column prop="personnum" label="所需人数" width="100" sortable="custom" align="right">
				<template #default="scope">
					<span :class="{ 'text-warning': scope.row.personnum > 0 }">{{ scope.row.personnum || 0 }}</span>
				</template>
			</el-table-column>
			<el-table-column prop="shopworktime" label="日工作时长(h)" width="120" align="right">
				<template #default="scope">
					{{ scope.row.shopworktime || 8 }}
				</template>
			</el-table-column>
		</template>
	</GlobalTable>
</div>
</template>
<script>
export default { name: "人力计划" };
</script>
<script setup>
import { reactive, toRefs, ref, onMounted } from 'vue';
import { Refresh } from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';
import GroupSelect from '@/components/header/group_select.vue';
import marketApi from '@/api/amazon/market/marketApi.js';
import plantoolApi from '../api/plantoolApi.js';

const globalTableRef = ref();

const state = reactive({
	queryParams: {
		search: '',
		groupid: '',
		marketplaceid: ''
	},
	marketplaceList: [],
	tableData: { records: [], total: 0 },
	refreshLoading: false,
	totalWorkHours: 0,
	totalPersonNum: 0
});
const { queryParams, marketplaceList, tableData, refreshLoading, totalWorkHours, totalPersonNum } = toRefs(state);

function groupChange(val) {
	state.queryParams.groupid = val;
	handleQuery();
}

function handleQuery() {
	globalTableRef.value?.loadTable(state.queryParams);
}

function loadTableData(params) {
	plantoolApi.manPlanList(params).then(res => {
		state.tableData.records = res.data?.records || [];
		state.tableData.total = res.data?.total || 0;
		// 计算汇总
		let workHours = 0;
		let personNum = 0;
		state.tableData.records.forEach(item => {
			workHours += Number(item.totalworkhours || 0);
			personNum += Number(item.personnum || 0);
		});
		state.totalWorkHours = workHours.toFixed(2);
		state.totalPersonNum = Math.ceil(personNum);
	});
}

function handleRefresh() {
	state.refreshLoading = true;
	plantoolApi.refreshManPlan(state.queryParams).then(() => {
		ElMessage.success('刷新成功');
		handleQuery();
	}).finally(() => {
		state.refreshLoading = false;
	});
}

function handleProWorkHoursChange(row) {
	plantoolApi.updateProWorkHours({
		id: row.id,
		proworkhours: row.proworkhours
	}).then(() => {
		ElMessage.success('更新成功');
		handleQuery();
	});
}

function handleUnitsTimeChange(row) {
	plantoolApi.updateUnitsTime({
		id: row.id,
		unitstime: row.unitstime
	}).then(() => {
		ElMessage.success('更新成功');
		handleQuery();
	});
}

onMounted(() => {
	// 加载站点列表并默认选中第一个
	marketApi.getMarketAll().then(res => {
		state.marketplaceList = res.data || [];
		if (state.marketplaceList.length > 0) {
			state.queryParams.marketplaceid = state.marketplaceList[0].marketplaceid;
		}
	});
});
</script>
<style scoped>
.text-warning {
	color: #e6a23c;
	font-weight: bold;
}
</style>
