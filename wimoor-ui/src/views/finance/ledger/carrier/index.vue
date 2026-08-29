<template>
  <div class="app-container">
    <!-- 查询区域 -->
    <el-row :gutter="10" class="mb8">
      <el-form :model="searchForm" ref="searchRef" :inline="true" v-show="showSearch" label-width="98px">
        <el-form-item label="对账月份">
          <Datepicker datetype="month" ref="datepickers" @changedate="changedate" />
        </el-form-item>
        <el-form-item label="承运商名称">
          <el-select v-model="searchForm.companyid" placeholder="选择承运商" clearable style="width: 180px" @change="handleCarrierChange">
            <el-option v-for="item in carrierList" :value="item.id" :key="item.id" :label="item.name" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="Search" @click="handleSearch">查询</el-button>
          <el-button icon="Refresh" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="loadData">
      </right-toolbar>
    </el-row>

    <!-- 统计卡片 -->
    <el-row :gutter="16" class="mb8">
      <el-col :span="4">
        <el-card shadow="never" class="stat-card">
          <div class="stat-card-content">
            <div class="stat-card-info">
              <div class="stat-card-label">实际发货</div>
              <div class="stat-card-value success">{{ summaryData.totalout || 0 }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="5">
        <el-card shadow="never" class="stat-card">
          <div class="stat-card-content">
            <div class="stat-card-info">
              <div class="stat-card-label">运输重量(KG)</div>
              <div class="stat-card-value">{{ formatNumber(summaryData.transweight_kg) }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="4">
        <el-card shadow="never" class="stat-card">
          <div class="stat-card-content">
            <div class="stat-card-info">
              <div class="stat-card-label">货件票数</div>
              <div class="stat-card-value">{{ summaryData.shipmentnum || 0 }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="5">
        <el-card shadow="never" class="stat-card">
          <div class="stat-card-content">
            <div class="stat-card-info">
              <div class="stat-card-label">运输费用</div>
              <div class="stat-card-value danger">{{ formatNumber(summaryData.shipfee) }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="5">
        <el-card shadow="never" class="stat-card">
          <div class="stat-card-content">
            <div class="stat-card-info">
              <div class="stat-card-label">关税/其他费用</div>
              <div class="stat-card-value warning">{{ formatNumber(summaryData.totalotherfee) }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 表格区域 -->
    <el-table
      v-loading="loading"
      :data="tableData"
      style="width: 100%"
      row-key="logitics"
      show-summary
      :summary-method="getSummaries"
      height="calc(100vh - 240px)"
      :default-sort="{ prop: 'shipfee', order: 'descending' }"
      @sort-change="handleSortChange"
      :stripe="true"
      :border="false"
    >
      <el-table-column label="物流承运商" prop="logitics" fixed="left" width="400" sortable="custom">
        <template #default="{ row }">
          <el-link type="primary" @click="handleViewDetail(row)">{{ row.logitics || '-' }}</el-link>
        </template>
      </el-table-column>
      <el-table-column label="实际发货" prop="totalout" min-width="110" align="right" sortable="custom">
        <template #header>
          <el-tooltip placement="top" content="已发货出库的货件上的发货数量.(等于 实际接收数量 + 待接收数量 +接收差值)">
            <span>实际发货</span>
          </el-tooltip>
        </template>
        <template #default="{ row }">
          <div>{{ row.totalout || 0 }}</div>
          <div class="font-extraSmall" v-if="row.lessrec !== undefined">接收差值: {{ row.lessrec || 0 }}</div>
        </template>
      </el-table-column>
      <el-table-column label="运输重量(KG)" prop="transweight_kg" min-width="130" align="right" sortable="custom">
        <template #header>
          <el-tooltip placement="top" content="货件计费重量">
            <span>运输重量(KG)</span>
          </el-tooltip>
        </template>
      </el-table-column>
      <el-table-column label="货件票数" prop="shipmentnum" min-width="110" align="right" sortable="custom">
        <template #default="{ row }">
          <div>{{ row.shipmentnum || 0 }}</div>
          <div class="font-extraSmall" v-if="row.totalbox !== undefined">箱数: {{ row.totalbox || 0 }}</div>
        </template>
      </el-table-column>
      <el-table-column label="运输费用" prop="shipfee" min-width="120" align="right" sortable="custom">
        <template #header>
          <el-tooltip placement="top" content="货件运费">
            <span>运输费用</span>
          </el-tooltip>
        </template>
        <template #default="{ row }">
          <span class="amount paid">{{ formatNumber(row.shipfee) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="关税/其他费用" prop="totalotherfee" min-width="130" align="right" sortable="custom">
        <template #header>
          <el-tooltip placement="top" content="货件的税费和其它费用">
            <span>关税/其他费用</span>
          </el-tooltip>
        </template>
        <template #default="{ row }">
          <span class="amount">{{ formatNumber(row.totalotherfee) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="已开票金额" prop="invoicedAmount" min-width="110" align="right" sortable="custom">
        <template #header>
          <el-tooltip placement="top" content="已开具发票的金额">
            <span>已开票金额</span>
          </el-tooltip>
        </template>
        <template #default="{ row }">
          <span class="amount paid">{{ formatNumber(row.invoicedAmount) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="未开票金额" prop="uninvoicedAmount" min-width="110" align="right" sortable="custom">
        <template #header>
          <el-tooltip placement="top" content="尚未开具发票的金额">
            <span>未开票金额</span>
          </el-tooltip>
        </template>
        <template #default="{ row }">
          <span class="amount unpaid">{{ formatNumber(row.uninvoicedAmount) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="150" fixed="right" align="center">
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
      :current-page="currentPage"
      :page-size="pageSize"
      :total="total"
      :page-sizes="[10, 20, 50, 100]"
      layout="total, sizes, prev, pager, next, jumper"
      @size-change="handleSizeChange"
      @current-change="handleCurrentChange"
    />

    <!-- 发货明细抽屉 -->
    <detail-drawer ref="detailDrawerRef" />

    <!-- 对账详情弹窗 -->
    <el-dialog title="对账详情" v-model="reconcileDetailVisible" width="700px">
      <div v-loading="reconcileDetailLoading">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="承运商名称">{{ reconcileDetail.carrierName }}</el-descriptions-item>
          <el-descriptions-item label="公司名">{{ reconcileDetail.companyName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="对账月份">{{ reconcileDetail.reconcileMonth || '-' }}</el-descriptions-item>
          <el-descriptions-item label="对账时间">{{ reconcileDetail.reconcileTime || '-' }}</el-descriptions-item>
        </el-descriptions>

        <el-divider content-position="left">发货汇总</el-divider>
        <el-descriptions :column="3" border>
          <el-descriptions-item label="计划发货">{{ reconcileDetail.totalPlanQty || 0 }}</el-descriptions-item>
          <el-descriptions-item label="实际发货">{{ reconcileDetail.totalActualQty || 0 }}</el-descriptions-item>
          <el-descriptions-item label="实际接收">{{ reconcileDetail.totalReceivedQty || 0 }}</el-descriptions-item>
        </el-descriptions>

        <el-divider content-position="left">费用汇总</el-divider>
        <el-descriptions :column="2" border>
          <el-descriptions-item label="运输费用">{{ formatNumber(reconcileDetail.totalShipFee) }}</el-descriptions-item>
          <el-descriptions-item label="关税/其他费用">{{ formatNumber(reconcileDetail.totalOtherFee) }}</el-descriptions-item>
          <el-descriptions-item label="发货货值">{{ formatNumber(reconcileDetail.totalWorth) }}</el-descriptions-item>
          <el-descriptions-item label="货件票数">{{ reconcileDetail.totalShipmentNum || 0 }}</el-descriptions-item>
        </el-descriptions>
      </div>
      <template #footer>
        <el-button @click="reconcileDetailVisible = false">关 闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="CarrierLedger">
import { ref, reactive, onMounted } from 'vue'
import { getCurrentInstance } from '@vue/runtime-core'
import transportationApi from '@/api/erp/ship/transportationApi.js'
import { reconcileCarrier, getCarrierReconcileDetail, getCarrierLedgerSummary } from '@/api/finance/carrierLedger'
import { getInvoiceList, getInvoiceStatistics } from '@/api/finance/invoiceLedger'
import Datepicker from '@/components/header/datepicker.vue'
import DetailDrawer from './components/detail-drawer.vue'
import finStore from "@/hooks/store/useFinanceStore.js"

const { proxy } = getCurrentInstance()

const loading = ref(false)
const showSearch = ref(true)
const tableData = ref([])
const total = ref(0)
const summaryData = ref({})
const carrierList = ref([])

const searchRef = ref()
const datepickers = ref()
const detailDrawerRef = ref()
const reconcileDetailVisible = ref(false)
const reconcileDetailLoading = ref(false)
const reconcileDetail = ref({})
const currentPage = ref(1)
const pageSize = ref(20)

const searchForm = reactive({
  companyid: '',
  fromDate: '',
  toDate: '',
  reconcileMonth: ''
})

// 获取承运商列表
function loadCarrierList() {
  transportationApi.getTranlist().then((res) => {
    carrierList.value = res.data || []
  })
}

// 承运商切换
function handleCarrierChange(val) {
  handleSearch()
}

// 日期改变
function changedate(dates) {
  searchForm.fromDate = dates.start || ''
  searchForm.toDate = dates.end || ''
  searchForm.reconcileMonth = dates.month || ''
  handleSearch()
}

// 加载数据
async function loadData() {
  loading.value = true
  try {
    const params = {
      ...searchForm
    }
    const [res, invoiceRes] = await Promise.all([
      getCarrierLedgerSummary(params),
      getInvoiceListByCarrier()
    ])

    const records = res.data || []
    const invoiceMap = invoiceRes || {}

    // 合并发票数据
    records.forEach(row => {
      const carrierName = row.logitics || ''
      const invoiceData = invoiceMap[carrierName] || {}
      row.invoicedAmount = invoiceData.invoicedAmount || 0
      // 未开票金额 = 运输费用 - 已开票金额
      row.uninvoicedAmount = (Number(row.shipfee) || 0) - (invoiceData.invoicedAmount || 0)
    })

    tableData.value = records
    total.value = records.length

    if (records.length > 0) {
      const summary = {
        shipmentnum: records.reduce((sum, row) => sum + (Number(row.shipmentnum) || 0), 0),
        totalqty: records.reduce((sum, row) => sum + (Number(row.totalqty) || 0), 0),
        totalout: records.reduce((sum, row) => sum + (Number(row.totalout) || 0), 0),
        transweight_kg: records.reduce((sum, row) => sum + (Number(row.transweight_kg) || 0), 0),
        shipfee: records.reduce((sum, row) => sum + (Number(row.shipfee) || 0), 0),
        totalotherfee: records.reduce((sum, row) => sum + (Number(row.totalotherfee) || 0), 0),
        invoicedAmount: records.reduce((sum, row) => sum + (Number(row.invoicedAmount) || 0), 0),
        uninvoicedAmount: records.reduce((sum, row) => sum + (Number(row.uninvoicedAmount) || 0), 0)
      }
      summaryData.value = summary
    } else {
      summaryData.value = {}
    }
  } catch (error) {
    console.error('获取承运商台账失败:', error)
  } finally {
    loading.value = false
  }
}

// 获取承运商发票列表
async function getInvoiceListByCarrier() {
  try {
    const groupid = await finStore.getCurrentTenantId()
    const res = await getInvoiceList({
      groupid,
      pageNum: 1,
      pageSize: 9999
    })
    const invoiceList = res?.rows || []
    
    // 统计承运商类型发票数量
    const carrierInvoices = invoiceList.filter(invoice => invoice.carrierId)

    const invoiceMap = {}

    // 只统计承运商类型的发票（carrierId不为空）
    carrierInvoices.forEach(invoice => {
      const carrierName = invoice.sellerName || ''
      if (!carrierName) return

      // 使用carrierId作为key避免重复
      const carrierId = invoice.carrierId
      if (!invoiceMap[carrierId]) {
        invoiceMap[carrierId] = {
          carrierName,
          invoicedAmount: 0
        }
      }

      // 已开票金额 = 所有承运商发票的价税合计
      const amount = invoice.amountWithTax || 0
      invoiceMap[carrierId].invoicedAmount += amount
    })

    // 转换为以carrierName为key的map
    const result = {}
    Object.values(invoiceMap).forEach(item => {
      result[item.carrierName] = {
        invoicedAmount: item.invoicedAmount
      }
    })

    return result
  } catch (error) {
    console.error('获取发票数据失败:', error)
    return {}
  }
}

// 查询
function handleSearch() {
  currentPage.value = 1
  loadData()
}

// 重置
function handleReset() {
  searchForm.companyid = ''
  searchForm.fromDate = ''
  searchForm.toDate = ''
  searchForm.reconcileMonth = ''
  datepickers.value?.reset()
  currentPage.value = 1
  loadData()
}

// 分页
function handleSizeChange(size) {
  pageSize.value = size
  currentPage.value = 1
}

function handleCurrentChange(page) {
  currentPage.value = page
}

// 排序
function handleSortChange({ prop, order }) {
  // 可根据需要实现排序逻辑
}

// 查看明细 - 打开明细抽屉
function handleViewDetail(row) {
  detailDrawerRef.value.open(row, searchForm)
}

// 对账
async function handleReconcile(row) {
  try {
    await proxy.$confirm('确认对该承运商进行对账操作？', '确认对账', { type: 'warning' })
    const params = {
      carrierId: row.companyid || '',
      companyName: finStore.currentTenantName.value || '',
      reconcileMonth: searchForm.reconcileMonth,
      carrierName: row.logitics || '',
      totalPlanQty: row.totalqty || 0,
      totalActualQty: row.totalout || 0,
      totalReceivedQty: row.totalrec || 0,
      totalShipFee: row.shipfee || 0,
      totalOtherFee: row.totalotherfee || 0,
      totalWorth: row.worth || 0,
      totalShipmentNum: row.shipmentnum || 0
    }
    const res = await reconcileCarrier(params)
    if (res.code === 200) {
      proxy.$message.success('对账成功')
      setTimeout(() => {
        loadData()
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
    const res = await getCarrierReconcileDetail({
      carrierId: row.companyid || '',
      reconcileMonth: searchForm.reconcileMonth
    })
    const record = res.data || {}
    reconcileDetail.value = {
      carrierName: record.carrierName || row.logitics || '-',
      companyName: record.companyName || finStore.currentTenantName.value || '-',
      reconcileMonth: record.reconcileMonth || '-',
      reconcileTime: record.reconcileTime || '-',
      totalPlanQty: record.totalPlanQty || 0,
      totalActualQty: record.totalActualQty || 0,
      totalReceivedQty: record.totalReceivedQty || 0,
      totalShipFee: record.totalShipFee || 0,
      totalOtherFee: record.totalOtherFee || 0,
      totalWorth: record.totalWorth || 0,
      totalShipmentNum: record.totalShipmentNum || 0
    }
  } catch (error) {
    console.error('获取对账详情失败:', error)
  } finally {
    reconcileDetailLoading.value = false
  }
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

// 格式化数字
function formatNumber(num) {
  if (!num || num === 0) return '0.00'
  return Number(num).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

// 合计行
function getSummaries({ columns, data }) {
  const arr = ['合计']
  const summaryFields = [
    'totalout', 'transweight_kg', 'shipmentnum', 'shipfee', 'totalotherfee', 'invoicedAmount', 'uninvoicedAmount'
  ]
  columns.forEach((item, index) => {
    if (index < 1) return
    const prop = item.property
    if (prop && summaryFields.includes(prop) && summaryData.value[prop] !== undefined) {
      if (['transweight_kg', 'shipfee', 'totalotherfee', 'invoicedAmount', 'uninvoicedAmount'].includes(prop)) {
        arr[index] = formatNumber(summaryData.value[prop])
      } else {
        arr[index] = summaryData.value[prop] || 0
      }
    }
  })
  return arr
}

onMounted(async () => {
  loadCarrierList()
  // 获取当前租户ID
  const tenantId = await finStore.getCurrentTenantId()
  if (tenantId) {
    searchForm.groupid = tenantId
  }
  // Datepicker会在mounted时触发changedate事件，由changedate调用handleSearch加载数据
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

.text-red {
  color: #f56c6c;
}

.mt16 {
  margin-top: 16px;
}

.mb8 {
  margin-bottom: 8px;
}
</style>
