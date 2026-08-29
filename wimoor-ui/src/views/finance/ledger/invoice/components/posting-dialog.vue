<template>
  <el-dialog title="发票入账" v-model="visible" width="600px" append-to-body>
    <el-alert
      title="入账后将自动生成凭证"
      type="warning"
      :closable="false"
      show-icon
      class="mb16"
    />

    <el-descriptions :column="2" border class="mb16">
      <el-descriptions-item label="发票数量">{{ invoices.length }} 张</el-descriptions-item>
      <el-descriptions-item label="价税合计">
        <span class="amount">{{ formatNumber(totalAmount) }}</span>
      </el-descriptions-item>
    </el-descriptions>

    <el-table :data="invoices" style="width: 100%" size="small" class="mb16">
      <el-table-column label="发票号码" prop="invoiceNo" width="180" />
      <el-table-column label="供应商" prop="supplierName" show-overflow-tooltip />
      <el-table-column label="价税合计" prop="amountWithTax" width="120" align="right">
        <template #default="{ row }">
          <span class="amount">{{ formatNumber(row.amountWithTax) }}</span>
        </template>
      </el-table-column>
    </el-table>

    <el-divider content-position="left">凭证预览</el-divider>
    
    <el-descriptions :column="1" border size="small">
      <el-descriptions-item label="借方科目">
        <span>预付账款-ERP在途发票</span>
      </el-descriptions-item>
      <el-descriptions-item label="借方金额">
        <span class="amount debit">{{ formatNumber(totalAmount) }}</span>
      </el-descriptions-item>
      <el-descriptions-item label="贷方科目">
        <span>预付账款-ERP采购供应商</span>
      </el-descriptions-item>
      <el-descriptions-item label="贷方金额">
        <span class="amount credit">{{ formatNumber(totalAmount) }}</span>
      </el-descriptions-item>
    </el-descriptions>

    <template #footer>
      <el-button @click="visible = false">取 消</el-button>
      <el-button type="primary" :loading="loading" @click="handleSubmit">确认入账</el-button>
    </template>
  </el-dialog>
</template>

<script setup name="PostingDialog">
import { ref, computed } from 'vue'
import { getCurrentInstance } from '@vue/runtime-core'
import { postingInvoices } from '@/api/finance/invoiceLedger'

const { proxy } = getCurrentInstance()
const emit = defineEmits(['success'])

const visible = ref(false)
const loading = ref(false)
const invoices = ref([])

// 计算总金额
const totalAmount = computed(() => {
  return invoices.value.reduce((sum, item) => sum + (item.amountWithTax || 0), 0)
})

// 打开弹窗
function open(rows) {
  visible.value = true
  invoices.value = rows
}

// 格式化数字
function formatNumber(num) {
  if (!num || num === 0) return '0.00'
  return Number(num).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

// 提交
async function handleSubmit() {
  loading.value = true
  try {
    const ids = invoices.value.map(item => item.id)
    await postingInvoices({ ids })
    proxy.$message.success('入账成功')
    visible.value = false
    emit('success')
  } catch (error) {
    proxy.$message.error('入账失败')
  } finally {
    loading.value = false
  }
}

defineExpose({ open })
</script>

<style scoped>
.amount {
  font-family: 'DIN Alternate', monospace;
}

.amount.debit {
  color: #f56c6c;
}

.amount.credit {
  color: #67c23a;
}

.mb16 {
  margin-bottom: 16px;
}
</style>
