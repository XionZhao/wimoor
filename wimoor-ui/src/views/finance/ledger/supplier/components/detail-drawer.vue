<template>
  <el-drawer :title="supplierName + ' - 供应商明细'" v-model="visible" size="80%">
    <el-tabs v-model="activeTab">
      <el-tab-pane label="采购订单" name="orders">
        <el-table :data="orderList" style="width: 100%" v-loading="loading" stripe show-summary>
          <el-table-column prop="number" label="订单编号" width="150" show-overflow-tooltip>
            <template #default="{ row }">
              <div>{{ row.orderNumber }}</div>
            </template>
          </el-table-column>
          <el-table-column prop="createdate" label="下单日期" width="100">
            <template #default="{ row }">
              {{ dateFormat(row.orderDate) }}
            </template>
          </el-table-column>
          <el-table-column prop="image" label="图片" width="65">
            <template #default="{ row }">
              <el-image v-if="row.image" :src="row.image" style="width:40px;height:40px;" />
              <el-image v-else :src="$require('empty/noimage40.png')" style="width:40px;height:40px;" />
            </template>
          </el-table-column>
          <el-table-column prop="sku" label="SKU" width="140" show-overflow-tooltip />
          <el-table-column prop="mname" label="物料名称" width="150" show-overflow-tooltip>
            <template #default="{ row }">
              <div>{{ row.materialName }}</div>
              <div class="font-extraSmall">{{ row.sku }}</div>
            </template>
          </el-table-column>
          <el-table-column prop="itemprice" width="100" label="采购单价">
            <template #default="{ row }">
              {{ row.itemprice ? formatNumber(row.itemprice) : '-' }}
            </template>
          </el-table-column>
          <el-table-column prop="amount" width="100" label="采购数量">
            <template #default="{ row }">
              <div>{{ row.amount }}</div>
              <div class="font-extraSmall"><span>预估重量：</span>{{ row.weight || 0 }} kg</div>
            </template>
          </el-table-column>
          <el-table-column prop="orderprice" width="120" label="采购金额">
            <template #default="{ row }">
              <div>{{ formatNumber(row.orderprice) }}</div>
              <div class="font-extraSmall">待付:
                <span v-if="row.orderprice >= row.totalpay && row.paystatus == 0">
                  {{ formatFloat(row.orderprice - row.totalpay) }}
                </span>
                <span v-else>0</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="totalpay" width="120" label="已付款金额">
            <template #default="{ row }">
              <div>{{ formatNumber(row.totalpay) }}</div>
              <el-tag size="small" v-if="row.paystatus == 1" type="success">已付款</el-tag>
              <el-tag size="small" v-if="row.paystatus == 0" type="info">未付款</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="shipfee" width="90" label="运费">
            <template #default="{ row }">
              <span v-if="row.shipfee">{{ formatNumber(row.shipfee) }}</span>
              <span v-else>-</span>
            </template>
          </el-table-column>
          <el-table-column prop="totalin" label="入库数量" width="110">
            <template #default="{ row }">
              <div>{{ row.totalin }}</div>
              <el-tag size="small" v-if="row.inwhstatus == 1" type="success">已收货</el-tag>
              <el-tag size="small" v-if="row.inwhstatus == 0" type="info">未收货</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="otherfee" label="其它费用" width="110">
            <template #default="{ row }">
              <span v-if="row.otherfee">{{ formatNumber(row.otherfee) }}</span>
              <span v-else>-</span>
            </template>
          </el-table-column>
          <el-table-column prop="deliverydate" width="120" label="预计到货时间">
            <template #default="{ row }">
              <div>{{ dateFormat(row.deliverydate) }}</div>
            </template>
          </el-table-column>
          <el-table-column prop="skuRemark" label="备注">
            <template #default="{ row }">
              <span v-if="row.skuRemark">{{ row.skuRemark }}</span>
              <span v-else>-</span>
            </template>
          </el-table-column>
        </el-table>
        <el-pagination
          v-if="orderTotal > 0"
          class="mt16"
          :current-page="orderQuery.pageNum"
          :page-size="orderQuery.pageSize"
          :page-sizes="pageSizes"
          :total="orderTotal"
          layout="total, sizes, prev, pager, next, jumper"
          @current-change="handleOrderPageChange"
          @size-change="handleOrderSizeChange"
        />
      </el-tab-pane>

      <el-tab-pane label="付款记录" name="payments">
        <el-table :data="paymentList" style="width: 100%" v-loading="loading" stripe>
          <el-table-column prop="number" label="订单编号" width="180" show-overflow-tooltip>
            <template #default="{ row }">
              <div>{{ row.orderNumber }}</div>
              <el-tag size="small" v-if="row.paystatus == 1" type="success">已付款</el-tag>
              <el-tag size="small" v-if="row.paystatus == 0" type="info">未付款</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="image" label="图片" width="65">
            <template #default="{ row }">
              <el-image v-if="row.image" :src="row.image" style="width:40px;height:40px;" />
              <el-image v-else :src="$require('empty/noimage40.png')" style="width:40px;height:40px;" />
            </template>
          </el-table-column>
          <el-table-column prop="sku" label="名称/SKU" show-overflow-tooltip>
            <template #default="{ row }">
              <div class="mname">{{ row.materialName }}</div>
              <div class="sku">{{ row.sku }}</div>
            </template>
          </el-table-column>
          <el-table-column prop="purchases" width="100" label="采购数量">
            <template #default="{ row }">
              <div>{{ row.amount }}</div>
              <div class="font-extraSmall" v-if="row.totalin">入库:{{ row.totalin }}</div>
            </template>
          </el-table-column>
          <el-table-column prop="orderprice" width="120" label="采购金额">
            <template #default="{ row }">
              <div>{{ formatNumber(row.orderprice) }}</div>
              <div class="font-extraSmall" v-if="row.totalpay">已付:{{ formatNumber(row.totalpay) }}</div>
            </template>
          </el-table-column>
          <el-table-column prop="fee_type" width="150" label="费用类型">
            <template #default="{ row }">
              <div>{{ row.projectName || '-' }}</div>
              <div class="font-extraSmall">付款日期:{{ dateFormat(row.payTime) }}</div>
            </template>
          </el-table-column>
          <el-table-column prop="payprice" width="150" label="付款金额">
            <template #default="{ row }">
              <div v-if="row.payprice < 0" class="text-red">{{ row.payprice }}:退款</div>
              <div v-else>￥{{ formatNumber(row.payprice) }}</div>
              <div class="font-extraSmall">操作人:{{ row.operator }}</div>
            </template>
          </el-table-column>
          <el-table-column prop="remark" label="备注" show-overflow-tooltip />
        </el-table>
        <el-pagination
          v-if="paymentTotal > 0"
          class="mt16"
          :current-page="paymentQuery.pageNum"
          :page-size="paymentQuery.pageSize"
          :page-sizes="pageSizes"
          :total="paymentTotal"
          layout="total, sizes, prev, pager, next, jumper"
          @current-change="handlePaymentPageChange"
          @size-change="handlePaymentSizeChange"
        />
      </el-tab-pane>

      <el-tab-pane label="发票记录" name="invoices">
        <el-table :data="invoiceList" style="width: 100%" v-loading="loading">
          <el-table-column label="发票号码" prop="invoiceNo">
            <template #default="{ row }">
              <el-link type="primary" @click="handleViewInvoice(row)">{{ row.invoiceNo }}</el-link>
            </template>
          </el-table-column>
          <el-table-column label="发票类型" prop="invoiceType" align="center">
            <template #default="{ row }">
              <el-tag :type="getInvoiceTypeTag(row.invoiceType)" size="small">
                {{ getInvoiceTypeText(row.invoiceType) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="开票日期" prop="invoiceDate" />
          <el-table-column label="价税合计" prop="amountWithTax" align="right">
            <template #default="{ row }">
              <span class="amount">{{ formatNumber(row.amountWithTax) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="状态" prop="status" align="center">
            <template #default="{ row }">
              <el-tag :type="getInvoiceStatusType(row.status)" size="small">
                {{ getInvoiceStatusText(row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="入账状态" prop="postingStatus" align="center">
            <template #default="{ row }">
              <el-tag :type="row.postingStatus === 1 ? 'success' : 'info'" size="small">
                {{ row.postingStatus === 1 ? '已入账' : '未入账' }}
              </el-tag>
            </template>
          </el-table-column>
        </el-table>
        <el-pagination
          v-if="invoiceTotal > 0"
          class="mt16"
          :current-page="invoiceQuery.pageNum"
          :page-size="invoiceQuery.pageSize"
          :page-sizes="pageSizes"
          :total="invoiceTotal"
          layout="total, sizes, prev, pager, next, jumper"
          @current-change="handleInvoicePageChange"
          @size-change="handleInvoiceSizeChange"
        />
      </el-tab-pane>
    </el-tabs>

    <!-- 对账确认弹窗 -->
    <el-dialog title="确认对账" v-model="reconcileVisible" width="400px" append-to-body>
      <p>确认标记该供应商为已对账状态？</p>
      <template #footer>
        <el-button @click="reconcileVisible = false">取 消</el-button>
        <el-button type="primary" :loading="reconcileLoading" @click="handleReconcileSubmit">确 认</el-button>
      </template>
    </el-dialog>
  </el-drawer>
</template>

<script setup name="SupplierDetailDrawer">
import { ref, reactive, nextTick, watch } from 'vue'
import { getCurrentInstance } from '@vue/runtime-core'
import { 
  getSupplierLedgerOrders, 
  getSupplierLedgerPayments, 
  getSupplierLedgerInvoices,
  reconcileSupplier 
} from '@/api/finance/supplierLedger'

const { proxy } = getCurrentInstance()
const emit = defineEmits(['reconcileSuccess'])

const visible = ref(false)
const loading = ref(false)
const activeTab = ref('orders')
const supplierId = ref(null)
const supplierName = ref('')
const reconcileVisible = ref(false)
const reconcileLoading = ref(false)
const currentGroupid = ref('')
const currentStartDate = ref('')
const currentEndDate = ref('')
const currentReconcileMonth = ref('')
const currentCompanyName = ref('')

// 订单数据
const orderList = ref([])
const orderTotal = ref(0)
const orderQuery = reactive({ pageNum: 1, pageSize: 10 })

// 付款数据
const paymentList = ref([])
const paymentTotal = ref(0)
const paymentQuery = reactive({ pageNum: 1, pageSize: 10 })

// 发票数据
const invoiceList = ref([])
const invoiceTotal = ref(0)
const invoiceQuery = reactive({ pageNum: 1, pageSize: 10 })

// 每页条数选项
const pageSizes = [10, 20, 50, 100]

// 打开抽屉
function open(id, name, tab, groupid, startDate, endDate, reconcileMonth, companyName) {
  visible.value = true
  supplierId.value = id
  supplierName.value = name
  currentGroupid.value = groupid || ''
  currentStartDate.value = startDate || ''
  currentEndDate.value = endDate || ''
  currentReconcileMonth.value = reconcileMonth || ''
  currentCompanyName.value = companyName || ''
  // 设置activeTab会触发watch，自动加载对应数据
  activeTab.value = tab || 'orders'
}

// 标签切换 - 使用watch监听activeTab变化
watch(activeTab, (newTab) => {
  if (!visible.value) return
  if (newTab === 'orders') {
    loadOrders()
  } else if (newTab === 'payments') {
    loadPayments()
  } else if (newTab === 'invoices') {
    loadInvoices()
  } else if (newTab === 'reconcile') {
    reconcileVisible.value = true
  }
})

// 加载订单
async function loadOrders() {
  loading.value = true
  try {
    const res = await getSupplierLedgerOrders({
      supplierId: supplierId.value,
      groupid: currentGroupid.value,
      ...orderQuery
    })
    orderList.value = res.data || []
    orderTotal.value = res.total || 0
  } catch (error) {
    console.error('获取订单列表失败:', error)
  } finally {
    loading.value = false
  }
}

// 加载付款
async function loadPayments() {
  loading.value = true
  try {
    const res = await getSupplierLedgerPayments({
      supplierId: supplierId.value,
      groupid: currentGroupid.value,
      ...paymentQuery
    })
    paymentList.value = res.data || []
    paymentTotal.value = res.total || 0
  } catch (error) {
    console.error('获取付款列表失败:', error)
  } finally {
    loading.value = false
  }
}

// 加载发票
async function loadInvoices() {
  loading.value = true
  try {
    const res = await getSupplierLedgerInvoices({
      supplierId: supplierId.value,
      groupid: currentGroupid.value,
      ...invoiceQuery
    })
    invoiceList.value = res.data || []
    invoiceTotal.value = res.total || 0
  } catch (error) {
    console.error('获取发票列表失败:', error)
  } finally {
    loading.value = false
  }
}

// 分页
function handleOrderPageChange(page) {
  orderQuery.pageNum = page
  loadOrders()
}

function handleOrderSizeChange(size) {
  orderQuery.pageSize = size
  orderQuery.pageNum = 1
  loadOrders()
}

function handlePaymentPageChange(page) {
  paymentQuery.pageNum = page
  loadPayments()
}

function handlePaymentSizeChange(size) {
  paymentQuery.pageSize = size
  paymentQuery.pageNum = 1
  loadPayments()
}

function handleInvoicePageChange(page) {
  invoiceQuery.pageNum = page
  loadInvoices()
}

function handleInvoiceSizeChange(size) {
  invoiceQuery.pageSize = size
  invoiceQuery.pageNum = 1
  loadInvoices()
}

// 查看订单详情
function handleViewOrder(row) {
  router.push(`/erp/purchase/orders/detail/${row.orderId}`)
}

// 查看发票详情
function handleViewInvoice(row) {
  router.push(`/finance/invoice-ledger/detail/${row.id}`)
}

// 对账
function handleConfirmReconcile() {
  reconcileVisible.value = true
}

async function handleReconcileSubmit() {
  reconcileLoading.value = true
  try {
    const res = await reconcileSupplier({
      supplierId: supplierId.value,
      groupid: currentGroupid.value,
      startDate: currentStartDate.value,
      endDate: currentEndDate.value,
      reconcileMonth: currentReconcileMonth.value,
      companyName: currentCompanyName.value || ''
    })
    if (res.code === 200) {
      proxy.$message.success('对账成功')
      reconcileVisible.value = false
      // 通知父组件刷新列表
      emit('reconcileSuccess')
    } else {
      proxy.$message.error(res.msg || '对账失败')
    }
  } catch (error) {
    console.error('对账失败:', error)
    // 如果 error.response 存在，说明 request.js 拦截器已处理，不再重复提示
    if (!error.response) {
      proxy.$message.error('对账失败，请稍后重试')
    }
  } finally {
    reconcileLoading.value = false
  }
}

// 格式化数字
function formatNumber(num) {
  if (!num || num === 0) return '0.00'
  return Number(num).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

// 格式化浮点数
function formatFloat(num) {
  if (!num || num === 0) return '0.00'
  return Number(num).toFixed(2)
}

// 日期格式化
function dateFormat(date) {
  if (!date) return ''
  const d = new Date(date)
  const year = d.getFullYear()
  const month = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

// 订单状态
function getStatusType(status) {
  const types = { 0: 'info', 1: 'warning', 2: 'success', 3: '' }
  return types[status] || 'info'
}

function getStatusText(status) {
  const texts = { 0: '草稿', 1: '待审核', 2: '审核通过', 3: '已完成' }
  return texts[status] || '未知'
}

// 发票类型
function getInvoiceTypeTag(type) {
  if (!type) return 'info'
  if (type.includes('SPECIAL') || type.includes('专用') || type.includes('专票')) return ''
  if (type.includes('NORMAL') || type.includes('普通') || type.includes('普票')) return 'success'
  if (type.includes('MOTOR') || type.includes('机动车')) return 'warning'
  if (type.includes('运输')) return 'warning'
  return 'info'
}

function getInvoiceTypeText(type) {
  if (!type) return '未知'
  const typeMap = {
    'VAT_SPECIAL': '增值税专用发票',
    'VAT_NORMAL': '增值税普通发票',
    'MOTOR': '机动车发票',
    'VAT_ELECTRONIC': '电子发票',
    'VAT_SPECIAL_ELECTRONIC': '电子专票'
  }
  return typeMap[type] || type
}

// 发票状态
function getInvoiceStatusType(status) {
  const types = { NORMAL: 'success', CANCELLED: 'danger', RED_ALL: 'danger', RED_PART: 'warning', ABNORMAL: 'danger' }
  return types[status] || 'info'
}

function getInvoiceStatusText(status) {
  const texts = { NORMAL: '正常', CANCELLED: '作废', RED_ALL: '红冲全部', RED_PART: '红冲部分', ABNORMAL: '异常' }
  return texts[status] || '未知'
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

.mt16 {
  margin-top: 16px;
}
</style>
