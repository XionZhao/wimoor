<template>
  <el-dialog :title="dialogTitle" v-model="visible" width="650px" append-to-body>
    <el-alert
      :title="alertMessage"
      type="warning"
      :closable="false"
      show-icon
      class="mb16"
    />

    <!-- 发票汇总信息 -->
    <el-descriptions :column="2" border class="mb16">
      <el-descriptions-item label="发票数量">{{ invoices.length }} 张</el-descriptions-item>
      <el-descriptions-item label="价税合计">
        <span class="amount">{{ formatNumber(totalAmount) }}</span>
      </el-descriptions-item>
    </el-descriptions>

    <!-- 凭证设置表单 -->
    <el-form ref="formRef" :model="form" :rules="rules" label-width="100px" class="mb16">
      <el-form-item label="凭证日期" prop="voucherDate">
        <el-date-picker
          v-model="form.voucherDate"
          type="date"
          placeholder="选择凭证日期"
          value-format="YYYY-MM-DD"
          style="width: 100%"
        />
      </el-form-item>
      <el-form-item label="凭证类型" prop="voucherType">
        <el-select v-model="form.voucherType" placeholder="选择凭证类型" style="width: 100%">
          <el-option label="普通凭证" value="1" />
          <el-option label="调整凭证" value="2" />
        </el-select>
      </el-form-item>
      <el-form-item label="凭证状态" prop="voucherStatus">
        <el-select v-model="form.voucherStatus" placeholder="选择凭证状态" style="width: 100%">
          <el-option label="草稿" :value="1" />
          <el-option label="已过账" :value="3" />
        </el-select>
      </el-form-item>
      <el-form-item label="摘要" prop="summary">
        <el-input v-model="form.summary" placeholder="请输入凭证摘要" />
      </el-form-item>
    </el-form>

    <!-- 发票明细列表 -->
    <el-divider content-position="left">选中发票</el-divider>
    <el-table :data="invoices" style="width: 100%" size="small" class="mb16" max-height="200">
      <el-table-column label="发票号码" width="180" show-overflow-tooltip>
        <template #default="{ row }">
          <span v-if="row.digitalInvoiceNo">{{ row.digitalInvoiceNo }}</span>
          <span v-else>{{ row.invoiceCode ? row.invoiceCode + ' ' : '' }}{{ row.invoiceNo }}</span>
        </template>
      </el-table-column>
      <el-table-column label="供应商" prop="sellerName" show-overflow-tooltip />
      <el-table-column label="开票日期" prop="invoiceDate" width="110" />
      <el-table-column label="价税合计" prop="amountWithTax" width="120" align="right">
        <template #default="{ row }">
          <span class="amount">{{ formatNumber(row.amountWithTax) }}</span>
        </template>
      </el-table-column>
    </el-table>

    <!-- 映射规则预览 -->
    <el-divider content-position="left">凭证分录预览</el-divider>
    <div v-if="mappingList.length > 0">
      <div v-for="(mapping, index) in mappingList" :key="index" class="mapping-preview">
        <el-descriptions :column="1" border size="small">
          <el-descriptions-item label="摘要">{{ mapping.summary }}</el-descriptions-item>
          <el-descriptions-item label="借方科目">{{ mapping.debitSubjectName }}</el-descriptions-item>
          <el-descriptions-item label="借方金额">
            <span class="amount debit">{{ formatNumber(totalAmount) }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="贷方科目">{{ mapping.creditSubjectName }}</el-descriptions-item>
          <el-descriptions-item label="贷方金额">
            <span class="amount credit">{{ formatNumber(totalAmount) }}</span>
          </el-descriptions-item>
        </el-descriptions>
      </div>
    </div>
    <el-empty v-else description="未配置映射规则，请先在财务配置中设置" :image-size="60" />

    <template #footer>
      <el-button @click="visible = false">取 消</el-button>
      <el-button type="primary" :loading="loading" :disabled="mappingList.length === 0" @click="handleSubmit">
        确认生成凭证
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup name="InvoicePostingDialog">
import { ref, reactive, computed } from 'vue'
import { getCurrentInstance } from '@vue/runtime-core'
import { listClosingTemplateInvoice } from '@/api/finance/closing_template_invoice'
import { generateVoucherFromInvoice } from '@/api/finance/closing_template_invoice'
import finStore from '@/hooks/store/useFinanceStore.js'

const { proxy } = getCurrentInstance()
const emit = defineEmits(['success'])

const visible = ref(false)
const loading = ref(false)
const invoices = ref([])
const mappingList = ref([])
const subjectOptions = ref([])
const invoiceType = ref(null) // 0=采购发票, 1=承运商发票

const today = new Date()
const year = today.getFullYear()
const month = String(today.getMonth() + 1).padStart(2, '0')
const day = String(today.getDate()).padStart(2, '0')

const form = reactive({
  voucherDate: `${year}-${month}-${day}`,
  voucherType: '1',
  voucherStatus: 1,
  summary: '发票入账'
})

const rules = {
  voucherDate: [{ required: true, message: '凭证日期不能为空', trigger: 'change' }],
  voucherType: [{ required: true, message: '凭证类型不能为空', trigger: 'change' }],
  voucherStatus: [{ required: true, message: '凭证状态不能为空', trigger: 'change' }]
}

// 计算总金额
const totalAmount = computed(() => {
  return invoices.value.reduce((sum, item) => sum + (Number(item.amountWithTax) || 0), 0)
})

// 对话框标题
const dialogTitle = computed(() => {
  return invoiceType.value === 1 ? '生成承运商凭证设置' : '生成采购凭证设置'
})

// 提示信息
const alertMessage = computed(() => {
  return invoiceType.value === 1 
    ? '根据承运商发票凭证映射规则，将选中的发票转换为会计凭证'
    : '根据采购发票凭证映射规则，将选中的发票转换为会计凭证'
})

// 格式化数字
function formatNumber(num) {
  if (!num || num === 0) return '0.00'
  return Number(num).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

// 打开弹窗
async function open(rows, subjectOpts, type) {
  visible.value = true
  invoices.value = rows || []
  subjectOptions.value = subjectOpts || []
  invoiceType.value = type
  await loadMappingRules()
}

// 加载映射规则
async function loadMappingRules() {
  try {
    const groupid = await finStore.getCurrentTenantId()
    const params = { groupid }
    // 如果指定了发票类型，则按类型过滤
    if (invoiceType.value !== null && invoiceType.value !== undefined) {
      params.invoiceType = invoiceType.value
    }
    const response = await listClosingTemplateInvoice(params)
    const rows = response?.rows || response?.data?.rows || response?.data || []
    // 填充科目名称
    mappingList.value = rows.map(row => {
      const debitSubject = subjectOptions.value.find(s => s.subjectId === row.debitSubjectId)
      const creditSubject = subjectOptions.value.find(s => s.subjectId === row.creditSubjectId)
      return {
        ...row,
        debitSubjectName: debitSubject ? `${debitSubject.subjectCode} ${debitSubject.subjectName}` : row.debitSubjectId,
        creditSubjectName: creditSubject ? `${creditSubject.subjectCode} ${creditSubject.subjectName}` : row.creditSubjectId
      }
    })
  } catch (error) {
    console.error('加载映射规则失败:', error)
    mappingList.value = []
  }
}

// 提交
async function handleSubmit() {
  const valid = await proxy.$refs.formRef?.validate().catch(() => false)
  if (!valid) return

  if (mappingList.value.length === 0) {
    proxy.$message.warning('未配置映射规则，请先在财务配置中设置')
    return
  }

  loading.value = true
  try {
    const groupid = await finStore.getCurrentTenantId()
    const invoiceIds = invoices.value.map(item => item.id)
    const data = {
      groupid,
      invoiceIds,
      voucherDate: form.voucherDate,
      voucherType: form.voucherType,
      voucherStatus: form.voucherStatus,
      summary: form.summary,
      invoiceType: invoiceType.value
    }
    const res = await generateVoucherFromInvoice(data)
    proxy.$message.success(res.msg || '生成凭证成功')
    visible.value = false
    emit('success')
  } catch (error) {
    proxy.$message.error(error.message || '生成凭证失败')
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

.mapping-preview {
  margin-bottom: 12px;
}

.mapping-preview:last-child {
  margin-bottom: 0;
}
</style>
