<template>
	<div>
		<div v-if="!queryname" class="empty-tip">
			<el-empty description="请先选择一个表格分类" />
		</div>
		
		<el-table v-else :data="fieldList" border stripe style="width: 100%">
			<el-table-column prop="ffield" label="字段代码" width="150" />
			<el-table-column prop="title" label="列标题" min-width="150" />
			<el-table-column prop="width" label="列宽度" width="120" />
			<el-table-column prop="findex" label="排序" width="100" />
			<el-table-column label="格式化" width="120">
				<template #default="{ row }">
					<el-tag v-if="row.formatter" type="info" size="small">{{ row.formatter }}</el-tag>
					<span v-else class="text-muted">-</span>
				</template>
			</el-table-column>
			<el-table-column label="对齐" width="100">
				<template #default="{ row }">
					{{ row.align || 'left' }}
				</template>
			</el-table-column>
			<el-table-column label="操作" width="100" fixed="right">
				<template #default="{ row }">
					<el-button type="danger" link size="small" @click="handleDelete(row)">
						<el-icon><Delete /></el-icon> 删除
					</el-button>
				</template>
			</el-table-column>
		</el-table>
	</div>
</template>

<script setup>
import { ref, watch } from 'vue';
import { Delete } from '@element-plus/icons-vue';

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

const emit = defineEmits(['refresh', 'delete']);

const fieldList = ref([]);

// 监听 fields 变化，更新本地列表
watch(() => props.fields, (newVal) => {
	fieldList.value = newVal || [];
}, { immediate: true, deep: true });

// 删除字段
const handleDelete = (row) => {
	emit('delete', row);
};
</script>

<style scoped>
.empty-tip {
	padding: 40px 0;
}

.text-muted {
	color: #909399;
	font-size: 12px;
}
</style>
