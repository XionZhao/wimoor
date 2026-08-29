<template>
  <el-dialog title="台账付款" v-model="visible" width="600px" append-to-body>
    <el-form :model="form" ref="formRef" :rules="rules" label-width="100px">
      <el-form-item label="订单号">
        <el-input v-model="form.orderNumber" disabled />
      </el-form-item>
      <el-form-item label="供应商">
        <el-input v-model="form.supplierName" disabled />
      </el-form-item>
      <el-form-item label="订单金额">
        <el-input v-model="form.orderAmount" disabled>
          <template #append>元</template>
        </el-input>
      </el-form-item>
      <el-form-item label="未付金额">
        <el-input v-model="form.unpaidAmount" disabled>
          <template #append>元</template>
        </el-input>
      </el-form-item>
      <el-form-item label="付款金额" prop="payAmount">
        <el-input-number v-model="form.payAmount" :min="0.01" :max="form.unpaidAmount" :precision="2" style="width: 100%" />
      </el-form-item>
      <el-form-item label="采购账户" prop="accountId">
        <el-select v-model="form.accountId" placeholder="请选择采购账户" style="width: 100%">
          <el-option
            v-for="item in accountList"
            :key="item.id"
            :label="item.name"
            :value="item.id"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="费用类型" prop="projectId">
        <el-select v-model="form.projectId" placeholder="请选择费用类型" style="width: 100%">
          <el-option
            v-for="item in projectList"
            :key="item.id"
            :label="item.name"
            :value="item.id"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="备注">
        <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="请输入备注" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="visible = false">取 消</el-button>
      <el-button type="primary" :loading="loading" @click="handleSubmit">确 定</el-button>
    </template>
  </el-dialog>
</template>

<script setup name="PayDialog">
import { ref, reactive } from 'vue'
import { getCurrentInstance } from '@vue/runtime-core'
import { payPurchaseLedger } from '@/api/finance/purchaseLedger'
import faccountApi from '@/api/erp/finances/faccountApi'
import { getProject } from '@/api/erp/finance/project'

const { proxy } = getCurrentInstance()
const emit = defineEmits(['success'])

const visible = ref(false)
const loading = ref(false)
const accountList = ref([])
const projectList = ref([])
const formRef = ref()

const form = reactive({
  orderId: null,
  orderNumber: '',
  supplierName: '',
  orderAmount: 0,
  unpaidAmount: 0,
  payAmount: 0,
  accountId: null,
  projectId: null,
  remark: ''
})

const rules = {
  payAmount: [{ required: true, message: '请输入付款金额', trigger: 'blur' }],
  accountId: [{ required: true, message: '请选择采购账户', trigger: 'change' }],
  projectId: [{ required: true, message: '请选择费用类型', trigger: 'change' }]
}

// 打开弹窗
function open(row) {
  visible.value = true
  form.orderId = row.orderId
  form.orderNumber = row.orderNumber
  form.supplierName = row.supplierName
  form.orderAmount = row.orderAmount
  form.unpaidAmount = row.unpaidAmount
  form.payAmount = row.unpaidAmount
  form.accountId = row.accountId || null
  form.projectId = null
  form.remark = ''
  
  getAccountList()
  getProjectList()
}

// 批量付款
function openBatch(rows) {
  visible.value = true
  form.orderId = rows.map(r => r.orderId).join(',')
  form.orderNumber = rows.map(r => r.orderNumber).join(',')
  form.supplierName = rows[0]?.supplierName
  form.orderAmount = rows.reduce((sum, r) => sum + (r.orderAmount || 0), 0)
  form.unpaidAmount = rows.reduce((sum, r) => sum + (r.unpaidAmount || 0), 0)
  form.payAmount = form.unpaidAmount
  form.accountId = rows[0]?.accountId || null
  form.projectId = null
  form.remark = ''
  
  getAccountList()
  getProjectList()
}

// 获取账户列表
async function getAccountList() {
  try {
    const res = await faccountApi.getAccountAll()
    accountList.value = res.data || []
  } catch (error) {
    console.error('获取账户列表失败:', error)
  }
}

// 获取费用类型列表
async function getProjectList() {
  try {
    const res = await getProject()
    projectList.value = res.data || []
  } catch (error) {
    console.error('获取费用类型失败:', error)
  }
}

// 提交
async function handleSubmit() {
  await formRef.value.validate()
  
  loading.value = true
  try {
    await payPurchaseLedger(form)
    proxy.$message.success('付款成功')
    visible.value = false
    emit('success')
  } catch (error) {
    proxy.$message.error('付款失败')
  } finally {
    loading.value = false
  }
}

defineExpose({ open, openBatch })
</script>
