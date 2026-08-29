<template>
  <el-dialog title="同步发票" v-model="visible" width="500px" append-to-body>
    <el-alert
      title="从税局API同步发票数据"
      type="info"
      :closable="false"
      show-icon
      class="mb16"
    >
      <template #default>
        <p>1. 请确保已配置税局API凭证</p>
        <p>2. 同步时间范围建议不超过3个月</p>
        <p>3. 重复发票将自动去重</p>
      </template>
    </el-alert>

    <el-form :model="form" ref="formRef" :rules="rules" label-width="100px">
      <el-form-item label="同步方式" prop="syncType">
        <el-radio-group v-model="form.syncType">
          <el-radio value="auto">自动匹配</el-radio>
          <el-radio value="manual">手动指定</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="日期范围" prop="dateRange">
        <el-date-picker
          v-model="form.dateRange"
          type="daterange"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          value-format="YYYY-MM-DD"
          style="width: 100%"
        />
      </el-form-item>
      <el-form-item label="发票类型" v-if="form.syncType === 'manual'">
        <el-select v-model="form.invoiceType" placeholder="请选择" clearable style="width: 100%">
          <el-option label="数电专票" value="DIGITAL_VAT" />
          <el-option label="数电普票" value="DIGITAL_NORMAL" />
          <el-option label="传统发票" value="TRADITIONAL" />
        </el-select>
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="visible = false">取 消</el-button>
      <el-button type="primary" :loading="loading" @click="handleSubmit">开始同步</el-button>
    </template>
  </el-dialog>
</template>

<script setup name="SyncDialog">
import { ref, reactive } from 'vue'
import { getCurrentInstance } from '@vue/runtime-core'
import { syncInvoices } from '@/api/finance/invoiceLedger'

const { proxy } = getCurrentInstance()
const emit = defineEmits(['success'])

const visible = ref(false)
const loading = ref(false)
const formRef = ref()

const form = reactive({
  syncType: 'auto',
  dateRange: [],
  invoiceType: null
})

const rules = {
  syncType: [{ required: true, message: '请选择同步方式', trigger: 'change' }],
  dateRange: [{ required: true, message: '请选择日期范围', trigger: 'change' }]
}

// 打开弹窗
function open() {
  visible.value = true
  form.syncType = 'auto'
  form.dateRange = []
  form.invoiceType = null
}

// 提交
async function handleSubmit() {
  await formRef.value.validate()
  
  loading.value = true
  try {
    await syncInvoices({
      syncType: form.syncType,
      startDate: form.dateRange[0],
      endDate: form.dateRange[1],
      invoiceType: form.invoiceType
    })
    proxy.$message.success('同步成功')
    visible.value = false
    emit('success')
  } catch (error) {
    proxy.$message.error('同步失败')
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
</style>
