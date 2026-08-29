<template>
	<div class="main-sty">
		<el-row :gutter="20">
			<el-col :span="24">
				<div class="con-header">
					<el-form :model="form" label-width="120px">
						<el-form-item label="货品名称：">
							<el-input v-model="form.productName" placeholder="请输入货品名称" style="width: 300px;"></el-input>
						</el-form-item>
						<el-form-item label="SKC外部编码：">
							<el-input v-model="form.extCode" placeholder="请输入SKC外部编码" style="width: 300px;"></el-input>
						</el-form-item>
						<el-form-item label="创建时间：">
							<el-date-picker v-model="form.dateRange" type="daterange" range-separator="至" start-placeholder="开始日期" end-placeholder="结束日期" value-format="x" style="width: 300px;"></el-date-picker>
						</el-form-item>
						<el-form-item>
							<el-button type="primary" @click="onSubmit" :loading="loading">同步商品列表</el-button>
							<el-button @click="resetForm">重置</el-button>
						</el-form-item>
					</el-form>
				</div>
			</el-col>
		</el-row>
	</div>
</template>
<script>
export default {
	name: "Temu商品同步"
}
</script>
<script setup>
import { ref, reactive } from 'vue';
import goodsApi from "@/api/temu/goodsApi.js";
import { ElMessage } from 'element-plus';

const loading = ref(false);

const form = reactive({
	productName: '',
	extCode: '',
	dateRange: []
});

async function onSubmit() {
	loading.value = true;
	try {
		const params = {};
		if (form.productName) {
			params.productName = form.productName;
		}
		if (form.extCode) {
			params.extCode = form.extCode;
		}
		if (form.dateRange && form.dateRange.length === 2) {
			params.createdAtStart = form.dateRange[0];
			params.createdAtEnd = form.dateRange[1];
		}
		const res = await goodsApi.syncGoodsList(params);
		ElMessage.success('同步成功，共同步' + (res.data?.syncedCount || 0) + '条');
	} catch (error) {
		console.error('同步失败:', error);
		ElMessage.error('同步失败，请重试');
	} finally {
		loading.value = false;
	}
}

function resetForm() {
	form.productName = '';
	form.extCode = '';
	form.dateRange = [];
}
</script>
<style scoped>
.con-header {
	padding: 20px;
	background-color: #ffffff;
	border-radius: 8px;
	box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
	margin-bottom: 20px;
}
.main-sty {
	padding: 20px;
}
</style>
