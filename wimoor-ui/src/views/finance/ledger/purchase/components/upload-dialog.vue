<template>
  <el-dialog title="导入对账单" v-model="visible" width="500px" append-to-body>
    <el-alert
      title="请按照模板格式上传对账单"
      type="info"
      :closable="false"
      show-icon
      class="mb16"
    >
      <template #default>
        <p>1. 支持 .xlsx、.xls 格式文件</p>
        <p>2. 单次最多导入 1000 条记录</p>
        <p>3. 
          <el-link type="primary" @click="handleDownloadTemplate">下载模板</el-link>
        </p>
      </template>
    </el-alert>

    <el-form :model="form" ref="formRef" :rules="rules" label-width="100px">
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
      <el-form-item label="对账日期" prop="reconcileDate">
        <el-date-picker
          v-model="form.reconcileDate"
          type="date"
          placeholder="选择对账日期"
          value-format="YYYY-MM-DD"
          style="width: 100%"
        />
      </el-form-item>
      <el-form-item label="对账单文件" prop="file">
        <el-upload
          ref="uploadRef"
          class="upload-demo"
          :auto-upload="false"
          :limit="1"
          :on-exceed="handleExceed"
          :on-change="handleChange"
          :on-remove="handleRemove"
          accept=".xlsx,.xls"
        >
          <template #trigger>
            <el-button type="primary" plain>选择文件</el-button>
          </template>
          <template #tip>
            <div class="el-upload__tip">只能上传 xlsx/xls 文件</div>
          </template>
        </el-upload>
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="visible = false">取 消</el-button>
      <el-button type="primary" :loading="loading" @click="handleSubmit">开始导入</el-button>
    </template>
  </el-dialog>
</template>

<script setup name="UploadDialog">
import { ref, reactive } from 'vue'
import { getCurrentInstance } from '@vue/runtime-core'
import { uploadPurchaseLedgerReconcile } from '@/api/finance/purchaseLedger'
import faccountApi from '@/api/erp/finances/faccountApi'

const { proxy } = getCurrentInstance()
const emit = defineEmits(['success'])

const visible = ref(false)
const loading = ref(false)
const accountList = ref([])
const formRef = ref()
const uploadRef = ref()

const form = reactive({
  accountId: null,
  reconcileDate: '',
  file: null
})

const rules = {
  accountId: [{ required: true, message: '请选择采购账户', trigger: 'change' }],
  reconcileDate: [{ required: true, message: '请选择对账日期', trigger: 'change' }],
  file: [{ required: true, message: '请上传对账单文件', trigger: 'change' }]
}

// 打开弹窗
function open() {
  visible.value = true
  form.accountId = null
  form.reconcileDate = ''
  form.file = null
  
  getAccountList()
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

// 文件变更
function handleChange(file) {
  form.file = file.raw
}

// 移除文件
function handleRemove() {
  form.file = null
}

// 超出限制
function handleExceed() {
  proxy.$message.warning('只能上传一个文件')
}

// 下载模板
function handleDownloadTemplate() {
  // 创建模板数据
  const headers = ['订单号', '供应商', '物料SKU', '对账金额', '备注']
  const rows = [
    ['PO20240101001', '示例供应商', 'SKU001', '1000.00', '示例数据']
  ]
  
  const csvContent = [headers.join(','), ...rows.map(row => row.join(','))].join('\n')
  const blob = new Blob(['\uFEFF' + csvContent], { type: 'text/csv;charset=utf-8;' })
  const link = document.createElement('a')
  link.href = URL.createObjectURL(blob)
  link.download = '采购对账单导入模板.csv'
  link.click()
}

// 提交
async function handleSubmit() {
  await formRef.value.validate()
  
  if (!form.file) {
    proxy.$message.warning('请上传对账单文件')
    return
  }
  
  loading.value = true
  try {
    const formData = new FormData()
    formData.append('file', form.file)
    formData.append('accountId', form.accountId)
    formData.append('reconcileDate', form.reconcileDate)
    
    await uploadPurchaseLedgerReconcile(formData)
    proxy.$message.success('导入成功')
    visible.value = false
    emit('success')
  } catch (error) {
    proxy.$message.error('导入失败')
  } finally {
    loading.value = false
  }
}

defineExpose({ open })
</script>

<style scoped>
.mb16 {
  margin-bottom: 16px;
}

.upload-demo {
  width: 100%;
}
</style>
