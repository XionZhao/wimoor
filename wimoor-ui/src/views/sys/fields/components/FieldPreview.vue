<template>
	<div class="preview-container">
		<div v-if="!queryname" class="preview-empty">
			<el-empty description="请先选择一个表格查看预览" />
		</div>
		
		<template v-else>
			<div class="preview-tip">
				<el-alert 
					title="预览说明" 
					description="下面是表格的模拟预览，点击「打开列配置」按钮可体验列配置功能。" 
					type="info" 
					show-icon 
					:closable="false"
				/>
			</div>
			
			<div class="preview-table-wrapper">
				<table class="preview-table">
					<thead>
						<tr>
							<th v-for="field in sortedFields" :key="field.ffield" :style="{ width: field.width + 'px' }">
								{{ field.title }}
							</th>
						</tr>
					</thead>
					<tbody>
						<tr v-for="(row, index) in previewData" :key="index">
							<td v-for="field in sortedFields" :key="field.ffield" :style="{ textAlign: field.align || 'left' }">
								{{ row[field.ffield] || '-' }}
							</td>
						</tr>
					</tbody>
				</table>
			</div>
			
			<div class="preview-footer">
				<el-tag type="info">共 {{ fields.length }} 列</el-tag>
				<el-tag type="success">模拟数据 3 行</el-tag>
			</div>
		</template>
	</div>
</template>

<script setup>
import { computed } from 'vue';

const props = defineProps({
	queryname: {
		type: String,
		default: ''
	},
	fields: {
		type: Array,
		default: () => []
	}
});

// 按排序字段排序
const sortedFields = computed(() => {
	if (!props.fields) return [];
	return [...props.fields].sort((a, b) => (a.findex || 0) - (b.findex || 0));
});

// 生成模拟预览数据
const previewData = computed(() => {
	if (!props.fields || props.fields.length === 0) return [];
	
	// 生成3行模拟数据
	const data = [];
	for (let i = 1; i <= 3; i++) {
		const row = {};
		props.fields.forEach(field => {
			// 根据字段名生成模拟数据
			if (field.ffield.includes('id') || field.ffield.includes('Id')) {
				row[field.ffield] = `${1000 + i}`;
			} else if (field.ffield.includes('name') || field.ffield.includes('Name')) {
				row[field.ffield] = `示例数据${i}`;
			} else if (field.ffield.includes('price') || field.ffield.includes('amount') || field.ffield.includes('Price') || field.ffield.includes('Amount')) {
				row[field.ffield] = `${(Math.random() * 1000).toFixed(2)}`;
			} else if (field.ffield.includes('date') || field.ffield.includes('time') || field.ffield.includes('Date') || field.ffield.includes('Time')) {
				row[field.ffield] = '2026-01-01';
			} else if (field.ffield.includes('status') || field.ffield.includes('Status')) {
				row[field.ffield] = '正常';
			} else {
				row[field.ffield] = `值${i}`;
			}
		});
		data.push(row);
	}
	return data;
});
</script>

<style scoped>
.preview-container {
	padding: 10px;
}

.preview-empty {
	padding: 40px 0;
}

.preview-tip {
	margin-bottom: 16px;
}

.preview-table-wrapper {
	overflow-x: auto;
	border: 1px solid #e4e7ed;
	border-radius: 4px;
}

.preview-table {
	width: 100%;
	border-collapse: collapse;
	font-size: 14px;
}

.preview-table th,
.preview-table td {
	border: 1px solid #e4e7ed;
	padding: 10px 12px;
	text-align: left;
}

.preview-table th {
	background: #f5f7fa;
	color: #909399;
	font-weight: 500;
	white-space: nowrap;
}

.preview-table tr:hover td {
	background: #f5f7fa;
}

.preview-footer {
	margin-top: 16px;
	display: flex;
	gap: 10px;
}
</style>
