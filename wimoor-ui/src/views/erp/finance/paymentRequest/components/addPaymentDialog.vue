<template>
	<el-dialog
		v-model="dialogVisible"
		title="手动新增请款单"
		width="700px"
		:before-close="handleClose"
	>
		<el-form :model="formData" :rules="rules" ref="formRef" label-width="100px" label-position="top">
			<el-form-item label="店铺" prop="groupid">
				<GroupSelect :value="formData.groupid" defaultValue="only" @change="changeGroup" style="width: 100%"/>
			</el-form-item>
			<el-form-item label="支付方式" prop="paymethod">
				<el-radio-group v-model="formData.paymethod" @change="loadPaymentAccount(formData.paymethod)">
					<el-radio v-for="item in payMethodList" :key="item.id" :label="item.id">{{ item.name }}</el-radio>
				</el-radio-group>
			</el-form-item>
			<el-form-item label="支付账号" v-if="payAccList && payAccList.length > 0">
				<el-select v-model="formData.payacc" style="width: 100%">
					<el-option v-for="item in payAccList" :key="item.id" :label="item.name" :value="item.id"/>
				</el-select>
			</el-form-item>
			<el-form-item label="费用明细">
				<el-table :data="feeTableData" border size="small">
					<el-table-column type="index" width="50">
						<template #header>
							<el-link :underline="false" @click="handleAddFee">
								<el-icon class="ic-cen font-large"><Plus /></el-icon>
							</el-link>
						</template>
					</el-table-column>
					<el-table-column label="费用项目">
						<template #default="scope">
							<el-select v-model="scope.row.projectid" size="small" style="width: 100%">
								<el-option v-for="item in finlist" :key="item.id" :label="item.name" :value="item.id"/>
							</el-select>
						</template>
					</el-table-column>
					<el-table-column label="金额(￥)" width="180">
						<template #default="scope">
							<el-input size="small" v-model="scope.row.amount" @input="scope.row.amount = CheckInputFloat(scope.row.amount)"/>
						</template>
					</el-table-column>
					<el-table-column label="操作" width="60">
						<template #default="scope">
							<el-link :underline="false" @click="handleDeleteFee(scope.$index)">
								<el-icon class="ic-cen font-medium"><Close /></el-icon>
							</el-link>
						</template>
					</el-table-column>
				</el-table>
			</el-form-item>
			<el-form-item label="备注">
				<el-input v-model="formData.remark" type="textarea" :rows="2" placeholder="请输入备注"/>
			</el-form-item>
		</el-form>
		<template #footer>
			<el-button @click="dialogVisible = false">取消</el-button>
			<el-button type="primary" @click="handleSubmit">确认提交</el-button>
		</template>
	</el-dialog>
</template>

<script setup>
import { ref, reactive, toRefs } from 'vue'
import { Plus, Close } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { CheckInputFloat } from '@/utils/index.js'
import faccountApi from '@/api/erp/finances/faccountApi.js'
import purchaseFinlistApi from '@/api/erp/purchase/finance/listApi.js'
import GroupSelect from '@/components/header/group_select.vue'

const emit = defineEmits(['refresh'])
const formRef = ref()

const state = reactive({
	dialogVisible: false,
	formData: {
		groupid: '',
		paymethod: null,
		payacc: '',
		remark: '',
	},
	feeTableData: [{ projectid: '', amount: '' }],
	payMethodList: [],
	payAccList: [],
	finlist: [],
})

const {
	dialogVisible,
	formData,
	feeTableData,
	payMethodList,
	payAccList,
	finlist,
} = toRefs(state)

const rules = reactive({
	groupid: [{ required: true, message: '请选择店铺', trigger: 'change' }],
	paymethod: [{ required: true, message: '请选择支付方式', trigger: 'change' }],
})

function changeGroup(val) {
	state.formData.groupid = val
}

function handleAddFee() {
	state.feeTableData.push({ projectid: '', amount: '' })
}

function handleDeleteFee(index) {
	if (state.feeTableData.length <= 1) {
		ElMessage.warning('至少保留一条费用明细')
		return
	}
	state.feeTableData.splice(index, 1)
}

function loadPaymentMethod() {
	faccountApi.getPaymentMethod().then(res => {
		if (res.data && res.data.length > 0) {
			state.payMethodList = res.data
			state.formData.paymethod = res.data[0].id
			loadPaymentAccount(res.data[0].id)
		}
	})
}

function loadPaymentAccount(paymethod) {
	faccountApi.getPaymentAccount({ paymethod: paymethod }).then(res => {
		if (res.data && res.data.length > 0) {
			state.payAccList = res.data
			var defaultid = ''
			state.payAccList.forEach(item => {
				if (item.isdefault) {
					defaultid = item.id
				}
			})
			state.formData.payacc = defaultid || res.data[0].id
		} else {
			state.payAccList = []
			state.formData.payacc = ''
		}
	})
}

function loadProject() {
	faccountApi.getProject().then(res => {
		if (res.data && res.data.length > 0) {
			state.finlist = res.data.filter(item => !item.issys)
		}
	})
}

function handleSubmit() {
	formRef.value.validate((valid) => {
		if (!valid) return

		// 校验费用明细
		var feeList = []
		var totalAmount = 0
		state.feeTableData.forEach(item => {
			if (item.projectid && item.amount && parseFloat(item.amount) > 0) {
				feeList.push({
					objectid: item.projectid,
					amount: parseFloat(item.amount),
				})
				totalAmount += parseFloat(item.amount)
			}
		})

		if (feeList.length === 0) {
			ElMessage.error('请至少添加一条有效的费用明细')
			return
		}

		var data = {
			groupid: state.formData.groupid,
			paymethod: state.formData.paymethod ? state.formData.paymethod.toString() : '',
			payacc: state.formData.payacc,
			remark: state.formData.remark,
			feelist: feeList.map(item => JSON.stringify(item)).join(','),
		}

		purchaseFinlistApi.saveManual(data).then(res => {
			if (res.data) {
				ElMessage.success('新增请款单成功')
				dialogVisible.value = false
				emit('refresh')
				resetForm()
			}
		})
	})
}

function resetForm() {
	state.formData = {
		groupid: '',
		paymethod: null,
		payacc: '',
		remark: '',
	}
	state.feeTableData = [{ projectid: '', amount: '' }]
	state.payAccList = []
}

function handleClose(done) {
	resetForm()
	done()
}

function show() {
	dialogVisible.value = true
	// 延迟加载，确保 GroupSelect 组件已挂载
	setTimeout(() => {
		loadPaymentMethod()
		loadProject()
	}, 100)
}

defineExpose({
	show,
})
</script>
