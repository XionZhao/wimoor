<template>
  <div class="app-container">
    <!-- 查询区域 -->
    <el-row :gutter="10" class="mb8">
      <el-form :model="searchForm" ref="searchRef" :inline="true" v-show="showSearch" label-width="98px">
        <el-form-item label="对账月份">
          <Datepicker datetype="month" ref="datepickers" @changedate="changedate" />
        </el-form-item>
        <el-form-item label="供应商名称">
          <el-input v-model="searchForm.supplierName" placeholder="请输入供应商名称" clearable style="width: 180px" />
        </el-form-item>
        <el-form-item label="联系人">
          <el-input v-model="searchForm.contactPerson" placeholder="请输入联系人" clearable style="width: 130px" />
        </el-form-item>
        <el-form-item label="对账状态">
          <el-select v-model="searchForm.reconcileStatus" placeholder="请选择" clearable style="width: 120px" @change="handleSearch">
            <el-option label="未对账" :value="0" />
            <el-option label="已对账" :value="1" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="Search" @click="handleSearch">查询</el-button>
          <el-button icon="Refresh" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList">
      </right-toolbar>
    </el-row>

    <!-- 统计卡片 -->
    <el-row :gutter="16" class="mb8">
      <el-col :span="4">
        <el-card shadow="never" class="stat-card">
          <div class="stat-card-content">
            <div class="stat-card-info">
              <div class="stat-card-label">供应商总数</div>
              <div class="stat-card-value">{{ statistics.totalSuppliers || 0 }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="4">
        <el-card shadow="never" class="stat-card">
          <div class="stat-card-content">
            <div class="stat-card-info">
              <div class="stat-card-label">订单总额</div>
              <div class="stat-card-value">{{ formatNumber(statistics.totalorderprice) }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="4">
        <el-card shadow="never" class="stat-card">
          <div class="stat-card-content">
            <div class="stat-card-info">
              <div class="stat-card-label">已付总额</div>
              <div class="stat-card-value success">{{ formatNumber(statistics.totalcostfee) }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="4">
        <el-card shadow="never" class="stat-card">
          <div class="stat-card-content">
            <div class="stat-card-info">
              <div class="stat-card-label">未付总额</div>
              <div class="stat-card-value danger">{{ formatNumber(statistics.waitPay) }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="4">
        <el-card shadow="never" class="stat-card">
          <div class="stat-card-content">
            <div class="stat-card-info">
              <div class="stat-card-label">已开票总额</div>
              <div class="stat-card-value success">{{ formatNumber(statistics.totalInvoicedAmount) }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="4">
        <el-card shadow="never" class="stat-card">
          <div class="stat-card-content">
            <div class="stat-card-info">
              <div class="stat-card-label">未开票总额</div>
              <div class="stat-card-value warning">{{ formatNumber(statistics.totalUninvoicedAmount) }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 表格区域（参照采购统计，补全付款/入库信息） -->
    <el-table
      v-loading="loading"
      :data="tableData"
      style="width: 100%"
      row-key="supplierId"
      show-summary
      :summary-method="getSummaries"
      height="calc(100vh - 280px)"
      :default-sort="{ prop: 'totalorderprice', order: 'descending' }"
      @sort-change="handleSortChange"
    >
      <el-table-column label="供应商名称" prop="supplierName" fixed="left" min-width="160" sortable="custom">
        <template #default="{ row }">
          <el-link type="primary" @click="handleViewDetail(row)">{{ row.supplierName }}</el-link>
        </template>
      </el-table-column>
      <el-table-column label="联系人" prop="contactPerson" min-width="90" />
      <el-table-column label="联系电话" prop="phone" min-width="120" />

      <!-- 采购汇总 -->
      <el-table-column label="采购汇总" align="center">
        <el-table-column label="订单数" prop="orderCount" width="90" align="right" sortable="custom" />
        <el-table-column label="订单总额" prop="totalorderprice" width="120" align="right" sortable="custom">
          <template #default="{ row }">
            <span class="amount">{{ formatNumber(row.totalorderprice) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="已收货" prop="totalin" width="90" align="right" sortable="custom" />
      </el-table-column>

      <!-- 付款汇总 -->
      <el-table-column label="付款汇总" align="center">
        <el-table-column label="已付总额" prop="totalcostfee" width="120" align="right" sortable="custom">
          <template #default="{ row }">
            <span class="amount paid">{{ formatNumber(row.totalcostfee) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="未付总额" prop="waitPay" width="120" align="right" sortable="custom">
          <template #default="{ row }">
            <span class="amount unpaid">{{ formatNumber(row.waitPay) }}</span>
          </template>
        </el-table-column>
      </el-table-column>

      <!-- 发票汇总 -->
      <el-table-column label="发票汇总" align="center">
        <el-table-column label="已开票" prop="totalInvoicedAmount" width="120" align="right" sortable="custom">
          <template #default="{ row }">
            <span class="amount paid">{{ formatNumber(row.totalInvoicedAmount) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="未开票" prop="totalUninvoicedAmount" width="120" align="right" sortable="custom">
          <template #default="{ row }">
            <span class="amount unpaid">{{ formatNumber(row.totalUninvoicedAmount) }}</span>
          </template>
        </el-table-column>
      </el-table-column>

      <!-- 对账状态 -->
      <el-table-column label="对账状态" prop="reconcileStatus" width="100" align="center">
        <template #default="{ row }">
          <el-tag :type="getReconcileStatusType(row.reconcileStatus)">
            {{ getReconcileStatusText(row.reconcileStatus) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="最后对账日期" prop="lastReconcileDate" width="120">
        <template #default="{ row }">
          <el-link v-if="row.lastReconcileDate" type="primary" @click="handleViewReconcile(row)">{{ row.lastReconcileDate }}</el-link>
          <span v-else>-</span>
        </template>
      </el-table-column>

      <el-table-column label="操作" width="180" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" link icon="View" @click="handleViewDetail(row)">明细</el-button>
          <el-button type="success" link icon="Check" @click="handleReconcile(row)">对账</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页 -->
    <el-pagination
      v-if="total > 0"
      class="mt16"
      :current-page="queryParams.pageNum"
      :page-size="queryParams.pageSize"
      :total="total"
      :page-sizes="[10, 20, 50, 100]"
      layout="total, sizes, prev, pager, next, jumper"
      @size-change="handleSizeChange"
      @current-change="handleCurrentChange"
    />

    <!-- 供应商明细抽屉 -->
    <detail-drawer ref="detailDrawerRef" @reconcileSuccess="getList" />

    <!-- 对账详情弹窗 -->
    <el-dialog title="对账详情" v-model="reconcileDetailVisible" width="700px">
      <div v-loading="reconcileDetailLoading">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="供应商名称">{{ reconcileDetail.supplierName }}</el-descriptions-item>
          <el-descriptions-item label="公司名">{{ reconcileDetail.companyName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="对账月份">{{ reconcileDetail.reconcileMonth || '-' }}</el-descriptions-item>
          <el-descriptions-item label="对账时间">{{ reconcileDetail.reconcileTime || '-' }}</el-descriptions-item>
        </el-descriptions>

        <el-divider content-position="left">采购汇总</el-divider>
        <el-descriptions :column="3" border>
          <el-descriptions-item label="订单数">{{ reconcileDetail.orderCount || 0 }}</el-descriptions-item>
          <el-descriptions-item label="订单总额">{{ formatNumber(reconcileDetail.totalorderprice) }}</el-descriptions-item>
          <el-descriptions-item label="已收货">{{ reconcileDetail.totalin || 0 }}</el-descriptions-item>
        </el-descriptions>

        <el-divider content-position="left">付款汇总</el-divider>
        <el-descriptions :column="2" border>
          <el-descriptions-item label="已付总额">{{ formatNumber(reconcileDetail.totalcostfee) }}</el-descriptions-item>
          <el-descriptions-item label="未付总额">{{ formatNumber(reconcileDetail.waitPay) }}</el-descriptions-item>
        </el-descriptions>

        <el-divider content-position="left">发票汇总</el-divider>
        <el-descriptions :column="2" border>
          <el-descriptions-item label="已开票总额">{{ formatNumber(reconcileDetail.totalInvoicedAmount) }}</el-descriptions-item>
          <el-descriptions-item label="未开票总额">{{ formatNumber(reconcileDetail.totalUninvoicedAmount) }}</el-descriptions-item>
        </el-descriptions>
      </div>
      <template #footer>
        <el-button @click="reconcileDetailVisible = false">关 闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="SupplierLedger">
import { ref, reactive, onMounted } from 'vue'
import { getCurrentInstance } from '@vue/runtime-core'
import { getSupplierLedgerSummary, getSupplierLedgerStatistics, getSupplierReconcileDetail, reconcileSupplier } from '@/api/finance/supplierLedger'
import { formatFloat } from '@/utils/index.js'
import Datepicker from '@/components/header/datepicker.vue'
import DetailDrawer from './components/detail-drawer.vue'
import finStore from "@/hooks/store/useFinanceStore.js"

const { proxy } = getCurrentInstance()

const loading = ref(false)
const showSearch = ref(true)
const tableData = ref([])
const total = ref(0)
const statistics = ref({})
const summary = ref({})

const searchRef = ref()
const datepickers = ref()
const detailDrawerRef = ref()
const reconcileDetailVisible = ref(false)
const reconcileDetailLoading = ref(false)
const reconcileDetail = ref({})

const searchForm = reactive({
  groupid: '',
  supplierName: '',
  contactPerson: '',
  reconcileStatus: null,
  reconcileMonth: ''
})

const queryParams = reactive({
  pageNum: 1,
  pageSize: 20
})

// 带重试的请求函数
async function requestWithRetry(fn, retries = 2, delay = 1000) {
  for (let i = 0; i < retries; i++) {
    try {
      return await fn()
    } catch (error) {
      if (i < retries - 1) {
        await new Promise(resolve => setTimeout(resolve, delay))
      } else {
        throw error
      }
    }
  }
}

// 获取统计数据
async function getStatistics() {
  if (!searchForm.groupid) return
  try {
    const params = { ...searchForm }
    const res = await requestWithRetry(() => getSupplierLedgerStatistics(params))
    statistics.value = res.data || res || {}
  } catch (error) {
    console.error('获取统计数据失败:', error)
  }
}

// 获取列表数据
async function getList() {
  if (!searchForm.groupid) return
  loading.value = true
  try {
    const params = {
      ...searchForm,
      ...queryParams
    }
    const res = await requestWithRetry(() => getSupplierLedgerSummary(params))
    tableData.value = res.data || []
    total.value = res.total || 0
    // 从第一条数据中提取合计行（参照采购统计的summary模式）
    if (tableData.value.length > 0 && tableData.value[0].summary) {
      summary.value = tableData.value[0].summary
    } else {
      summary.value = {}
    }
  } catch (error) {
    console.error('获取供应商台账失败:', error)
    // 不显示错误消息，让 loading 状态处理
  } finally {
    loading.value = false
  }
}

// 查询
async function handleSearch() {
  // groupid未设置时跳过查询（Datepicker比父组件先mounted，会提前触发）
  if (!searchForm.groupid) {
    return
  }
  queryParams.pageNum = 1
  await Promise.allSettled([getStatistics(), getList()])
}

// 重置
function handleReset() {
  proxy.resetForm('searchRef')
  // 重置日期选择器到当月
  datepickers.value?.reset()
  // 重新设置groupid（重置表单会清空groupid）
  const tenantId = finStore.currentTenantId.value
  if (tenantId) {
    searchForm.groupid = tenantId
  }
  handleSearch()
}

// 分页
function handleSizeChange(size) {
  queryParams.pageSize = size
  getList()
}

function handleCurrentChange(page) {
  queryParams.pageNum = page
  getList()
}

function changedate(dates) {
  searchForm.startDate = dates.start
  searchForm.endDate = dates.end
  searchForm.reconcileMonth = dates.month || ''
  handleSearch()
}

// 排序
function handleSortChange({ prop, order }) {
  // 可根据需要实现排序逻辑
}

// 查看明细
function handleViewDetail(row) {
  detailDrawerRef.value.open(row.supplierId, row.supplierName, 'orders', searchForm.groupid, null, null, searchForm.reconcileMonth, finStore.currentTenantName.value)
}

// 对账
async function handleReconcile(row) {
  try {
    await proxy.$confirm('确认对该供应商进行对账操作？', '确认对账', { type: 'warning' })
    const params = {
      groupid: searchForm.groupid,
      supplierId: row.supplierId,
      companyName: finStore.currentTenantName.value || '',
      reconcileMonth: searchForm.reconcileMonth,
      orderCount: row.orderCount || 0,
      totalOrderAmount: row.totalorderprice || 0,
      totalReceived: row.totalin || 0,
      totalPaidAmount: row.totalcostfee || 0,
      totalUnpaidAmount: row.waitPay || 0,
      totalInvoicedAmount: row.totalInvoicedAmount || 0,
      totalUninvoicedAmount: row.totalUninvoicedAmount || 0
    }
    console.log('对账请求参数:', params)
    const res = await reconcileSupplier(params)
    console.log('对账响应:', res)
    if (res.code === 200) {
      proxy.$message.success('对账成功')
      // 延迟刷新列表，避免请求冲突
      setTimeout(() => {
        getList()
        getStatistics()
      }, 500)
    } else {
      proxy.$message.error(res.msg || '对账失败')
    }
  } catch (e) {
    if (e !== 'cancel' && e !== 'close') {
      console.error('对账失败详情:', e)
      if (!e.response) {
        proxy.$message.error('对账失败，请稍后重试')
      }
    }
  }
}

// 查看对账详情
async function handleViewReconcile(row) {
  reconcileDetailVisible.value = true
  reconcileDetailLoading.value = true
  try {
    const res = await getSupplierReconcileDetail({
      supplierId: row.supplierId,
      groupid: searchForm.groupid,
      reconcileMonth: searchForm.reconcileMonth
    })
    const record = res.data || {}
    reconcileDetail.value = {
      supplierName: record.supplierName || row.supplierName,
      companyName: record.companyName || finStore.currentTenantName.value || '-',
      reconcileMonth: record.reconcileMonth || '-',
      reconcileTime: record.reconcileTime || '-',
      orderCount: record.orderCount || 0,
      totalorderprice: record.totalOrderAmount || 0,
      totalin: record.totalReceived || 0,
      totalcostfee: record.totalPaidAmount || 0,
      waitPay: record.totalUnpaidAmount || 0,
      totalInvoicedAmount: record.totalInvoicedAmount || 0,
      totalUninvoicedAmount: record.totalUninvoicedAmount || 0
    }
  } catch (error) {
    console.error('获取对账详情失败:', error)
  } finally {
    reconcileDetailLoading.value = false
  }
}

// 格式化数字
function formatNumber(num) {
  if (!num || num === 0) return '0.00'
  return Number(num).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

// 对账状态
function getReconcileStatusType(status) {
  const types = { 0: 'info', 1: 'success' }
  return types[status] || 'info'
}

function getReconcileStatusText(status) {
  const texts = { 0: '未对账', 1: '已对账' }
  return texts[status] || '未知'
}

// 合计行（参照采购统计）
function getSummaries({ columns }) {
  const arr = ['合计']
  columns.forEach((item, index) => {
    if (index < 3) return
    const prop = item.property
    if (prop && summary.value[prop] !== undefined) {
      if (['itemprice', 'pprice', 'weight'].includes(prop)) {
        arr[index] = formatFloat(summary.value[prop])
      } else if (['orderamount', 'actual_totalin', 'totalin', 'needin', 'totalre', 'lessrec'].includes(prop)) {
        arr[index] = summary.value[prop] || 0
      } else {
        arr[index] = formatNumber(summary.value[prop])
      }
    }
  })
  return arr
}

onMounted(async () => {
  // 获取当前租户ID（groupid）
  const tenantId = await finStore.getCurrentTenantId()
  if (tenantId) {
    searchForm.groupid = tenantId
    console.log('设置groupid:', tenantId)
  } else {
    console.warn('未获取到tenantId')
  }
  // 并行请求，互不影响
  await Promise.allSettled([getStatistics(), getList()])
})
</script>

<style scoped>
.app-container {
  padding: 10px;
}

.stat-card {
  height: 80px;
}

.stat-card .stat-card-content {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
}

.stat-card .stat-card-info {
  text-align: center;
}

.stat-card .stat-card-label {
  font-size: 12px;
  color: #909399;
  margin-bottom: 8px;
}

.stat-card .stat-card-value {
  font-size: 20px;
  font-weight: 600;
  color: #303133;
  font-family: 'DIN Alternate', monospace;
}

.stat-card .stat-card-value.success {
  color: #67c23a;
}

.stat-card .stat-card-value.danger {
  color: #f56c6c;
}

.stat-card .stat-card-value.warning {
  color: #e6a23c;
}

.amount {
  font-family: 'DIN Alternate', monospace;
}

.amount.paid {
  color: #67c23a;
}

.amount.unpaid {
  color: #f56c6c;
}

.mt16 {
  margin-top: 16px;
}

.con-header {
  margin-bottom: 8px;
}
</style>
