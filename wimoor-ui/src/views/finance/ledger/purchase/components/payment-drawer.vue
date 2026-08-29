<template>
  <el-drawer title="付款明细" v-model="visible" size="60%">
    <el-descriptions :column="2" border class="mb16">
      <el-descriptions-item label="订单号">{{ orderInfo.orderNumber }}</el-descriptions-item>
      <el-descriptions-item label="供应商">{{ orderInfo.supplierName }}</el-descriptions-item>
      <el-descriptions-item label="订单金额">
        <span class="amount">{{ formatNumber(orderInfo.orderAmount) }}</span>
      </el-descriptions-item>
      <el-descriptions-item label="已付金额">
        <span class="amount paid">{{ formatNumber(orderInfo.paidAmount) }}</span>
      </el-descriptions-item>
      <el-descriptions-item label="未付金额">
        <span class="amount unpaid">{{ formatNumber(orderInfo.unpaidAmount) }}</span>
      </el-descriptions-item>
    </el-descriptions>

    <el-table :data="paymentList" style="width: 100%" v-loading="loading">
      <el-table-column label="付款单号" prop="paymentId" width="150" />
      <el-table-column label="付款金额" prop="payAmount" width="120" align="right">
        <template #default="{ row }">
          <span class="amount">{{ formatNumber(row.payAmount) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="采购账户" prop="accountName" width="150" />
      <el-table-column label="费用类型" prop="projectName" width="120" />
      <el-table-column label="付款时间" prop="payDate" width="180" />
      <el-table-column label="凭证号" prop="voucherNumber" width="150">
        <template #default="{ row }">
          <el-link v-if="row.voucherNumber" type="primary">{{ row.voucherNumber }}</el-link>
          <span v-else class="text-gray">未生成</span>
        </template>
      </el-table-column>
      <el-table-column label="备注" prop="remark" show-overflow-tooltip />
    </el-table>
  </el-drawer>
</template>

<script setup name="PaymentDrawer">
import { ref, reactive } from 'vue'
import { getPurchaseLedgerPayments } from '@/api/finance/purchaseLedger'

const visible = ref(false)
const loading = ref(false)
const paymentList = ref([])
const orderInfo = reactive({
  orderNumber: '',
  supplierName: '',
  orderAmount: 0,
  paidAmount: 0,
  unpaidAmount: 0
})

// 打开抽屉
async function open(orderId) {
  visible.value = true
  loading.value = true
  
  try {
    const res = await getPurchaseLedgerPayments({ orderId })
    paymentList.value = res.data?.list || []
    if (res.data?.orderInfo) {
      Object.assign(orderInfo, res.data.orderInfo)
    }
  } catch (error) {
    console.error('获取付款明细失败:', error)
  } finally {
    loading.value = false
  }
}

// 格式化数字
function formatNumber(num) {
  if (!num || num === 0) return '0.00'
  return Number(num).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

defineExpose({ open })
</script>

<style scoped>
.amount {
  font-family: 'DIN Alternate', monospace;
}

.amount.paid {
  color: #67c23a;
}

.amount.unpaid {
  color: #f56c6c;
}

.text-gray {
  color: #909399;
}

.mb16 {
  margin-bottom: 16px;
}
</style>
