<template>
  <el-drawer title="关联单据" v-model="visible" size="60%">
    <el-tabs v-model="activeTab">
      <el-tab-pane label="采购订单" name="orders">
        <el-table :data="relations.orders || []" style="width: 100%" v-loading="loading">
          <el-table-column label="订单号" prop="orderNumber" width="150">
            <template #default="{ row }">
              <el-link type="primary" @click="handleViewOrder(row)">{{ row.orderNumber }}</el-link>
            </template>
          </el-table-column>
          <el-table-column label="供应商" prop="supplierName" width="150" />
          <el-table-column label="订单金额" prop="orderAmount" width="120" align="right">
            <template #default="{ row }">
              <span class="amount">{{ formatNumber(row.orderAmount) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="已付金额" prop="paidAmount" width="120" align="right">
            <template #default="{ row }">
              <span class="amount paid">{{ formatNumber(row.paidAmount) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="状态" prop="status" width="100" align="center">
            <template #default="{ row }">
              <el-tag :type="row.status === 3 ? 'success' : 'warning'">
                {{ row.status === 3 ? '已完成' : '进行中' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="创建时间" prop="createdate" width="160" />
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="付款记录" name="payments">
        <el-table :data="relations.payments || []" style="width: 100%" v-loading="loading">
          <el-table-column label="付款单号" prop="paymentId" width="150" />
          <el-table-column label="付款金额" prop="payAmount" width="120" align="right">
            <template #default="{ row }">
              <span class="amount">{{ formatNumber(row.payAmount) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="采购账户" prop="accountName" width="150" />
          <el-table-column label="费用类型" prop="projectName" width="120" />
          <el-table-column label="付款时间" prop="payDate" width="160" />
          <el-table-column label="凭证号" prop="voucherNumber" width="150">
            <template #default="{ row }">
              <el-link v-if="row.voucherNumber" type="primary">{{ row.voucherNumber }}</el-link>
              <span v-else class="text-gray">-</span>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>
  </el-drawer>
</template>

<script setup name="RelationDrawer">
import { ref, reactive } from 'vue'
import { getInvoiceRelations } from '@/api/finance/invoiceLedger'

const visible = ref(false)
const loading = ref(false)
const activeTab = ref('orders')
const relations = reactive({
  orders: [],
  payments: []
})

// 打开抽屉
async function open(id) {
  visible.value = true
  loading.value = true
  
  try {
    const res = await getInvoiceRelations({ id })
    relations.orders = res.data?.orders || []
    relations.payments = res.data?.payments || []
  } catch (error) {
    console.error('获取关联单据失败:', error)
  } finally {
    loading.value = false
  }
}

// 查看订单详情
function handleViewOrder(row) {
  // 跳转到采购订单详情
  router.push(`/erp/purchase/orders/detail/${row.orderId}`)
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

.text-gray {
  color: #909399;
}
</style>
