<template>
<div class="main-sty">
	<div class="con-header">
		<el-row>
			<el-space :size="6">
				<GroupSelect @change="groupChange" defaultValue="only" />
				<el-date-picker v-model="queryParams.month" type="month" placeholder="选择月份" @change="monthChange" value-format="YYYY-MM" style="width:140px;" />
				<el-select v-model="queryParams.marketplaceid" placeholder="站点" clearable @change="handleQuery" style="width:120px;">
				<el-option v-for="item in marketplaceList" :key="item.marketplaceid" :label="item.name" :value="item.marketplaceid" />
			</el-select>
				<el-select v-model="queryParams.categoryid" placeholder="分类" clearable @change="handleQuery" style="width:120px;">
					<el-option v-for="item in categoryList" :key="item.id" :label="item.name" :value="item.id" />
				</el-select>
				<el-select v-model="queryParams.owner" placeholder="负责人" clearable @change="handleQuery" style="width:120px;">
					<el-option v-for="item in ownerList" :key="item.id" :label="item.name" :value="item.id" />
				</el-select>
				<el-input v-model="queryParams.search" v-debounce-input="handleQuery" placeholder="搜索SKU或名称" clearable style="width:200px;" />
			</el-space>
			<div class='rt-btn-group'>
				<el-space :size="16">
					<el-button @click="downLoadExcel">
						<el-icon><Download /></el-icon>
						<span>导出Excel</span>
					</el-button>
				</el-space>
			</div>
		</el-row>
	</div>
	<el-tabs v-model="activeTab" class="demo-tabs" @tab-change="handleTabChange">
		<el-tab-pane label="SKU维度" name="sku">
			<GlobalTable ref="skuTableRef"
				:tableData="skuTableData"
				height="calc(100vh - 280px)"
				:defaultSort="{ prop: 'saleqty', order: 'descending' }"
				@loadTable="loadSkuTableData"
				:stripe="true"
				style="width: 100%;margin-bottom:16px;">
				<template #field>
					<el-table-column prop="image" label="图片" width="65">
						<template #default="scope">
							<el-image :src="scope.row.image" style="width:40px;height:40px;" />
						</template>
					</el-table-column>
					<el-table-column prop="sku" label="SKU" width="160" sortable="custom" show-overflow-tooltip />
					<el-table-column prop="name" label="产品名称" min-width="200" show-overflow-tooltip />
					<el-table-column prop="groupname" label="店铺" width="120" show-overflow-tooltip />
					<el-table-column prop="marketname" label="站点" width="80" />
					<el-table-column prop="catename" label="分类" width="100" show-overflow-tooltip />
					<el-table-column prop="saleqty" label="销量" width="100" sortable="custom" align="right" />
					<el-table-column prop="saleamount" label="销售额" width="120" sortable="custom" align="right">
						<template #default="scope">
							{{ scope.row.currency || '' }}{{ scope.row.saleamount }}
						</template>
					</el-table-column>
					<el-table-column prop="avgqty" label="日均销量" width="100" sortable="custom" align="right" />
				</template>
			</GlobalTable>
		</el-tab-pane>
		<el-tab-pane label="分类维度" name="category">
			<GlobalTable ref="cateTableRef"
				:tableData="cateTableData"
				height="calc(100vh - 280px)"
				:defaultSort="{ prop: 'saleqty', order: 'descending' }"
				@loadTable="loadCateTableData"
				:stripe="true"
				style="width: 100%;margin-bottom:16px;">
				<template #field>
					<el-table-column prop="catename" label="分类名称" min-width="200" show-overflow-tooltip />
					<el-table-column prop="groupname" label="店铺" width="120" show-overflow-tooltip />
					<el-table-column prop="marketname" label="站点" width="80" />
					<el-table-column prop="skucount" label="SKU数量" width="100" sortable="custom" align="right" />
					<el-table-column prop="saleqty" label="销量" width="120" sortable="custom" align="right" />
					<el-table-column prop="saleamount" label="销售额" width="140" sortable="custom" align="right">
						<template #default="scope">
							{{ scope.row.currency || '' }}{{ scope.row.saleamount }}
						</template>
					</el-table-column>
					<el-table-column prop="avgqty" label="日均销量" width="100" sortable="custom" align="right" />
				</template>
			</GlobalTable>
		</el-tab-pane>
		<el-tab-pane label="汇总维度" name="summary">
			<GlobalTable ref="summaryTableRef"
				:tableData="summaryTableData"
				height="calc(100vh - 280px)"
				:defaultSort="{ prop: 'saleqty', order: 'descending' }"
				@loadTable="loadSummaryTableData"
				:stripe="true"
				style="width: 100%;margin-bottom:16px;">
				<template #field>
					<el-table-column prop="groupname" label="店铺" min-width="150" show-overflow-tooltip />
					<el-table-column prop="marketname" label="站点" width="80" />
					<el-table-column prop="skucount" label="SKU数量" width="100" sortable="custom" align="right" />
					<el-table-column prop="catcount" label="分类数量" width="100" sortable="custom" align="right" />
					<el-table-column prop="saleqty" label="总销量" width="120" sortable="custom" align="right" />
					<el-table-column prop="saleamount" label="总销售额" width="140" sortable="custom" align="right">
						<template #default="scope">
							{{ scope.row.currency || '' }}{{ scope.row.saleamount }}
						</template>
					</el-table-column>
					<el-table-column prop="avgqty" label="日均销量" width="100" sortable="custom" align="right" />
				</template>
			</GlobalTable>
		</el-tab-pane>
	</el-tabs>
</div>
</template>
<script>
export default { name: "销售月报" };
</script>
<script setup>
import { reactive, toRefs, ref, onMounted } from 'vue';
import { Download } from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';
import GroupSelect from '@/components/header/group_select.vue';
import marketApi from '@/api/amazon/market/marketApi.js';
import materialApi from '@/api/erp/material/materialApi.js';
import plantoolApi from '../api/plantoolApi.js';

const skuTableRef = ref();
const cateTableRef = ref();
const summaryTableRef = ref();

const state = reactive({
	activeTab: 'sku',
	queryParams: {
		search: '',
		groupid: '',
		marketplaceid: '',
		categoryid: '',
		owner: '',
		month: '',
		nowdate: '',
		olddate: '',
		searchtype: 'sname'
	},
	marketplaceList: [],
	categoryList: [],
	ownerList: [],
	skuTableData: { records: [], total: 0 },
	cateTableData: { records: [], total: 0 },
	summaryTableData: { records: [], total: 0 }
});
const { activeTab, queryParams, marketplaceList, categoryList, ownerList, skuTableData, cateTableData, summaryTableData } = toRefs(state);

function monthChange() {
	updateMonthParams();
	handleQuery();
}

function groupChange(val) {
	state.queryParams.groupid = val;
	handleQuery();
}

function handleQuery() {
	if (state.activeTab === 'sku') {
		skuTableRef.value?.loadTable(state.queryParams);
	} else if (state.activeTab === 'category') {
		cateTableRef.value?.loadTable(state.queryParams);
	} else if (state.activeTab === 'summary') {
		summaryTableRef.value?.loadTable(state.queryParams);
	}
}

function handleTabChange() {
	handleQuery();
}

function loadSkuTableData(params) {
	plantoolApi.saleMonthList(params).then(res => {
		state.skuTableData.records = res.data?.records || [];
		state.skuTableData.total = res.data?.total || 0;
	});
}

function loadCateTableData(params) {
	plantoolApi.saleMonthCateList(params).then(res => {
		state.cateTableData.records = res.data?.records || [];
		state.cateTableData.total = res.data?.total || 0;
	});
}

function loadSummaryTableData(params) {
	plantoolApi.saleMonthSummaryList(params).then(res => {
		state.summaryTableData.records = res.data?.records || [];
		state.summaryTableData.total = res.data?.total || 0;
	});
}

function downLoadExcel() {
	// 导出Excel功能
	ElMessage.info('导出功能开发中');
}

onMounted(() => {
	// 默认选中当前月份
	const now = new Date();
	const year = now.getFullYear();
	const month = String(now.getMonth() + 1).padStart(2, '0');
	state.queryParams.month = `${year}-${month}`;
	// 计算nowdate和olddate
	updateMonthParams();
	// 加载站点列表并默认选中第一个
	marketApi.getMarketAll().then(res => {
		state.marketplaceList = res.data || [];
		if (state.marketplaceList.length > 0) {
			state.queryParams.marketplaceid = state.marketplaceList[0].marketplaceid;
		}
	});
	// 加载分类列表
	materialApi.getCategory().then(res => {
		state.categoryList = res.data || [];
	});
	// 加载负责人列表
	materialApi.getOwnerList().then(res => {
		state.ownerList = res.data || [];
	});
});

function updateMonthParams() {
	if (state.queryParams.month) {
		state.queryParams.nowdate = state.queryParams.month + '-01';
		// 计算上一个月
		const [y, m] = state.queryParams.month.split('-').map(Number);
		const prevMonth = m === 1 ? 12 : m - 1;
		const prevYear = m === 1 ? y - 1 : y;
		state.queryParams.olddate = `${prevYear}-${String(prevMonth).padStart(2, '0')}-01`;
	}
}
</script>
<style scoped>
</style>
