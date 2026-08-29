<template>
	<el-dialog 
	width="480px"
	:title="title" 
	:rules="rules"
	v-model="visible"
	>
	<el-form :model="form" label-width="80px">
		<el-form-item label="账户名称" prop="name">
			<el-input v-model="form.name"></el-input>
		</el-form-item>
		<el-form-item label="账户类型" v-if="showPayType">
			<el-radio-group v-model="form.paymeth" class="ml-4">
				<el-radio :label="item.id" v-for="item in payMethodList">{{item.name}}</el-radio>
			</el-radio-group>
		</el-form-item>
	</el-form>
	<template #footer>
		<span class="dialog-footer">
			<el-button @click="visible = false">取消</el-button>
			<el-button type="primary" @click="handleConfirm">确认</el-button>
		</span>
	</template>
	</el-dialog>
</template>

<script setup>
import { ref, reactive, toRefs, watch } from 'vue'
import { ElMessage } from 'element-plus'
import faccountApi from '@/api/erp/finances/faccountApi.js'

const props = defineProps({
	payMethodList: { type: Array, default: () => [] },
})

const emit = defineEmits(['refresh'])

const visible = ref(false)
const showPayType = ref(false)
const title = ref('账户编辑')
const form = ref({
	name: '',
	paymeth: '',
})

function show(item) {
	if (item) {
		// 重命名
		title.value = '重命名'
		showPayType.value = false
		form.value = { ...item }
	} else {
		// 新增
		title.value = '添加账户'
		showPayType.value = true
		form.value = { name: '', paymeth: '' }
	}
	visible.value = true
}

function handleConfirm() {
	if (!form.value.name) {
		ElMessage.error('请输入账户名称')
		return
	}
	if (form.value.id) {
		faccountApi.updateAccountName(form.value).then(res => {
			visible.value = false
			ElMessage.success('修改成功')
			emit('refresh')
		})
	} else {
		faccountApi.saveAccount(form.value).then(res => {
			visible.value = false
			ElMessage.success('添加成功')
			emit('refresh')
		})
	}
}

defineExpose({
	show,
})
</script>
