<template>
<el-dialog
width="480px"
title="已删除账户列表" 
v-model="visible"
>
<el-table :data="tableData">
	<el-table-column prop="paymethName" label="账户类型" width="180" />
	<el-table-column prop="name" label="账户名" width="180" />
	<el-table-column prop="name" label="操作" >
		<template #default="scope">
			<el-button link type="primary" @click.stop="recoverItem(scope.row)">恢复</el-button>
		</template>
	</el-table-column>
</el-table>
<template #footer>
	<span class="dialog-footer">
		<el-button @click="visible = false">取消</el-button>
	</span>
</template>
</el-dialog>
</template>

<script setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import faccountApi from '@/api/erp/finances/faccountApi.js'

const emit = defineEmits(['refresh'])

const visible = ref(false)
const tableData = ref([])

function show() {
	visible.value = true
	loadData()
}

function loadData() {
	faccountApi.findAccountArchiveAll().then((res) => {
		tableData.value = res.data
	})
}

function recoverItem(row) {
	var data = { id: row.id }
	faccountApi.recoverAccountDelete(data).then((res) => {
		if (res.data) {
			ElMessage.success('恢复成功')
			visible.value = false
			emit('refresh')
		}
	})
}

defineExpose({
	show,
})
</script>
