<template>
	<div class="main-sty">
		<el-tabs v-model="activeTab" @tab-change="handleTabChange">
			<!-- 商品列表 -->
			<el-tab-pane label="商品列表" name="goods">
				<el-form :model="goodsForm" label-width="100px" :inline="true">
					<el-form-item label="货品名称：">
						<el-input v-model="goodsForm.productName" placeholder="请输入货品名称" style="width: 200px;"></el-input>
					</el-form-item>
					<el-form-item label="SKC外部编码：">
						<el-input v-model="goodsForm.extCode" placeholder="请输入SKC外部编码" style="width: 200px;"></el-input>
					</el-form-item>
					<el-form-item>
						<el-button type="primary" @click="loadGoodsData">查询</el-button>
						<el-button @click="resetGoodsForm">重置</el-button>
					</el-form-item>
				</el-form>

				<el-table :data="goodsTableData" style="width: 100%" border v-loading="goodsLoading">
					<el-table-column prop="productSkcId" label="SKC ID" width="120"></el-table-column>
					<el-table-column prop="productName" label="货品名称" min-width="200" show-overflow-tooltip></el-table-column>
					<el-table-column prop="extCode" label="SKC外部编码" width="150"></el-table-column>
					<el-table-column label="主图" width="80">
						<template #default="scope">
							<el-image v-if="scope.row.mainImageUrl" :src="scope.row.mainImageUrl" style="width: 50px; height: 50px;" fit="cover"></el-image>
						</template>
					</el-table-column>
					<el-table-column label="类目" min-width="150">
						<template #default="scope">
							{{ scope.row.leafCatName || scope.row.cat3Name || scope.row.cat2Name || scope.row.cat1Name || '-' }}
						</template>
					</el-table-column>
					<el-table-column label="状态" width="100">
						<template #default="scope">
							<el-tag v-if="scope.row.skcSiteStatus === 1" type="success">已加入站点</el-tag>
							<el-tag v-else type="info">未加入站点</el-tag>
						</template>
					</el-table-column>
					<el-table-column label="创建时间" width="160">
						<template #default="scope">
							{{ formatTimestamp(scope.row.createdAt) }}
						</template>
					</el-table-column>
				</el-table>

				<el-pagination
					v-if="goodsPagination.total > 0"
					@size-change="handleGoodsSizeChange"
					@current-change="handleGoodsCurrentChange"
					:current-page="goodsPagination.page"
					:page-sizes="[10, 20, 50, 100]"
					:page-size="goodsPagination.pageSize"
					layout="total, sizes, prev, pager, next, jumper"
					:total="goodsPagination.total"
					style="margin-top: 20px;"
				></el-pagination>
			</el-tab-pane>

			<!-- SKU列表 -->
			<el-tab-pane label="SKU列表" name="sku">
				<el-form :model="skuForm" label-width="100px" :inline="true">
					<el-form-item label="货品名称：">
						<el-input v-model="skuForm.productName" placeholder="请输入货品名称" style="width: 200px;"></el-input>
					</el-form-item>
					<el-form-item label="SKU货号：">
						<el-input v-model="skuForm.extCode" placeholder="请输入SKU货号" style="width: 200px;"></el-input>
					</el-form-item>
					<el-form-item>
						<el-button type="primary" @click="loadSkuData">查询</el-button>
						<el-button @click="resetSkuForm">重置</el-button>
					</el-form-item>
				</el-form>

				<el-table :data="skuTableData" style="width: 100%" border v-loading="skuLoading">
					<el-table-column prop="productSkuId" label="SKU ID" width="120"></el-table-column>
					<el-table-column prop="productSkcId" label="SKC ID" width="120"></el-table-column>
					<el-table-column prop="productName" label="货品名称" min-width="200" show-overflow-tooltip></el-table-column>
					<el-table-column prop="extCode" label="SKU货号" width="150"></el-table-column>
					<el-table-column label="体积(mm)" width="180">
						<template #default="scope">
							{{ scope.row.volumeLen && scope.row.volumeWidth && scope.row.volumeHeight
								? scope.row.volumeLen + '×' + scope.row.volumeWidth + '×' + scope.row.volumeHeight
								: '-' }}
						</template>
					</el-table-column>
					<el-table-column label="重量" width="100">
						<template #default="scope">
							{{ scope.row.weightValue ? scope.row.weightValue + 'mg' : '-' }}
						</template>
					</el-table-column>
					<el-table-column prop="virtualStock" label="虚拟库存" width="100"></el-table-column>
					<el-table-column label="是否易损" width="90">
						<template #default="scope">
							<el-tag v-if="scope.row.isFragile" type="warning" size="small">是</el-tag>
							<span v-else>-</span>
						</template>
					</el-table-column>
					<el-table-column label="敏感类型" width="100">
						<template #default="scope">
							<el-tag v-if="scope.row.isSensitive === 1" type="danger" size="small">敏感品</el-tag>
							<span v-else>-</span>
						</template>
					</el-table-column>
					<el-table-column label="操作" width="80" fixed="right">
						<template #default="scope">
							<el-button type="primary" link @click="showSkuDetail(scope.row)">详情</el-button>
						</template>
					</el-table-column>
				</el-table>

				<el-pagination
					v-if="skuPagination.total > 0"
					@size-change="handleSkuSizeChange"
					@current-change="handleSkuCurrentChange"
					:current-page="skuPagination.page"
					:page-sizes="[10, 20, 50, 100]"
					:page-size="skuPagination.pageSize"
					layout="total, sizes, prev, pager, next, jumper"
					:total="skuPagination.total"
					style="margin-top: 20px;"
				></el-pagination>
			</el-tab-pane>
		</el-tabs>

		<!-- SKU详情弹窗 -->
		<el-dialog v-model="skuDetailVisible" title="SKU详情" width="600px">
			<el-descriptions :column="2" border v-if="currentSku">
				<el-descriptions-item label="SKU ID">{{ currentSku.productSkuId }}</el-descriptions-item>
				<el-descriptions-item label="SKC ID">{{ currentSku.productSkcId }}</el-descriptions-item>
				<el-descriptions-item label="SKU货号">{{ currentSku.extCode || '-' }}</el-descriptions-item>
				<el-descriptions-item label="虚拟库存">{{ currentSku.virtualStock ?? '-' }}</el-descriptions-item>
				<el-descriptions-item label="发货模式">{{ currentSku.skuShippingMode ?? '-' }}</el-descriptions-item>
				<el-descriptions-item label="子销售模式">{{ currentSku.skuSubSellMode ?? '-' }}</el-descriptions-item>
				<el-descriptions-item label="体积(mm)">{{ currentSku.volumeLen && currentSku.volumeWidth && currentSku.volumeHeight ? currentSku.volumeLen + '×' + currentSku.volumeWidth + '×' + currentSku.volumeHeight : '-' }}</el-descriptions-item>
				<el-descriptions-item label="重量">{{ currentSku.weightValue ? currentSku.weightValue + 'mg' : '-' }}</el-descriptions-item>
				<el-descriptions-item label="WMS体积(mm)">{{ currentSku.wmsVolumeLen && currentSku.wmsVolumeWidth && currentSku.wmsVolumeHeight ? currentSku.wmsVolumeLen + '×' + currentSku.wmsVolumeWidth + '×' + currentSku.wmsVolumeHeight : '-' }}</el-descriptions-item>
				<el-descriptions-item label="WMS重量">{{ currentSku.wmsWeightValue ? currentSku.wmsWeightValue + 'mg' : '-' }}</el-descriptions-item>
				<el-descriptions-item label="是否超长边">{{ currentSku.isSideOverLength ? '是' : '否' }}</el-descriptions-item>
				<el-descriptions-item label="是否超大体积">{{ currentSku.isVolumeOverSize ? '是' : '否' }}</el-descriptions-item>
				<el-descriptions-item label="是否易损品">{{ currentSku.isFragile ? '是' : '否' }}</el-descriptions-item>
				<el-descriptions-item label="是否敏感品">{{ currentSku.isSensitive === 1 ? '是' : '否' }}</el-descriptions-item>
				<el-descriptions-item label="条码" :span="2">{{ currentSku.barCodes || '-' }}</el-descriptions-item>
				<el-descriptions-item label="规格" :span="2">{{ currentSku.skuSpecList || '-' }}</el-descriptions-item>
				<el-descriptions-item label="JIT模式信息" :span="2">{{ currentSku.jitModeInfo || '-' }}</el-descriptions-item>
			</el-descriptions>
		</el-dialog>
	</div>
</template>
<script>
export default {
	name: "Temu商品SKU列表"
}
</script>
<script setup>
import { ref, reactive, onMounted } from 'vue';
import goodsApi from "@/api/temu/goodsApi.js";

const activeTab = ref('goods');

// ========== 商品列表 ==========
const goodsLoading = ref(false);
const goodsTableData = ref([]);
const goodsPagination = reactive({ page: 1, pageSize: 10, total: 0 });
const goodsForm = reactive({ productName: '', extCode: '' });

function formatTimestamp(ts) {
	if (!ts) return '-';
	return new Date(ts).toLocaleString('zh-CN');
}

async function loadGoodsData() {
	goodsLoading.value = true;
	try {
		const res = await goodsApi.listGoods({
			page: goodsPagination.page,
			pageSize: goodsPagination.pageSize,
			productName: goodsForm.productName || undefined,
			extCode: goodsForm.extCode || undefined
		});
		if (res.data) {
			goodsTableData.value = res.data.list || [];
			goodsPagination.total = Number(res.data.total) || 0;
		}
	} catch (error) {
		console.error('查询失败:', error);
	} finally {
		goodsLoading.value = false;
	}
}

function resetGoodsForm() {
	goodsForm.productName = '';
	goodsForm.extCode = '';
	goodsPagination.page = 1;
	loadGoodsData();
}

function handleGoodsSizeChange(val) {
	goodsPagination.pageSize = val;
	goodsPagination.page = 1;
	loadGoodsData();
}

function handleGoodsCurrentChange(val) {
	goodsPagination.page = val;
	loadGoodsData();
}

// ========== SKU列表 ==========
const skuLoading = ref(false);
const skuTableData = ref([]);
const skuPagination = reactive({ page: 1, pageSize: 10, total: 0 });
const skuForm = reactive({ productName: '', extCode: '' });

const skuDetailVisible = ref(false);
const currentSku = ref(null);

async function loadSkuData() {
	skuLoading.value = true;
	try {
		const res = await goodsApi.listSku({
			page: skuPagination.page,
			pageSize: skuPagination.pageSize,
			productName: skuForm.productName || undefined,
			extCode: skuForm.extCode || undefined
		});
		if (res.data) {
			skuTableData.value = res.data.list || [];
			skuPagination.total = Number(res.data.total) || 0;
		}
	} catch (error) {
		console.error('查询SKU失败:', error);
	} finally {
		skuLoading.value = false;
	}
}

function resetSkuForm() {
	skuForm.productName = '';
	skuForm.extCode = '';
	skuPagination.page = 1;
	loadSkuData();
}

function handleSkuSizeChange(val) {
	skuPagination.pageSize = val;
	skuPagination.page = 1;
	loadSkuData();
}

function handleSkuCurrentChange(val) {
	skuPagination.page = val;
	loadSkuData();
}

function showSkuDetail(row) {
	currentSku.value = row;
	skuDetailVisible.value = true;
}

function handleTabChange(tab) {
	if (tab === 'goods' && goodsTableData.value.length === 0) {
		loadGoodsData();
	} else if (tab === 'sku' && skuTableData.value.length === 0) {
		loadSkuData();
	}
}

onMounted(() => {
	loadGoodsData();
});
</script>
<style scoped>
.main-sty {
	padding: 20px;
	background-color: #ffffff;
	border-radius: 8px;
}
</style>
