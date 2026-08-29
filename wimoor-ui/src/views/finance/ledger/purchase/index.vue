<template>
  <div class="app-container">
    <!-- 查询区域 -->
    <el-row :gutter="10" class="mb8">
      <el-form :model="searchForm" ref="searchRef" :inline="true" v-show="showSearch" label-width="98px">
        <el-form-item label="采购账户">
          <el-select v-model="searchForm.accountId" placeholder="请选择采购账户" clearable style="width: 200px">
            <el-option
              v-for="item in accountList"
              :key="item.id"
              :label="item.name"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="供应商">
          <el-select v-model="searchForm.supplierId" placeholder="请选择供应商" clearable filterable style="width: 200px">
            <el-option
              v-for="item in supplierList"
              :key="item.id"
              :label="item.name"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="日期范围">
          <el-date-picker
            v-model="searchForm.dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
            style="width: 240px"
          />
        </el-form-item>
        <el-form-item label="订单状态">
          <el-select v-model="searchForm.orderStatus" placeholder="请选择" clearable style="width: 120px">
            <el-option label="草稿" :value="0" />
            <el-option label="待审核" :value="1" />
            <el-option label="审核通过" :value="2" />
            <el-option label="已完成" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item label="付款状态">
          <el-select v-model="searchForm.payStatus" placeholder="请选择" clearable style="width: 120px">
            <el-option label="未付款" :value="0" />
            <el-option label="部分付款" :value="1" />
            <el-option label="已付款" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="Search" @click="handleSearch">查询</el-button>
          <el-button icon="Refresh" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList">
        <el-button type="primary" plain icon="Download" @click="handleExport">导出</el-button>
        <el-button type="success" plain icon="Upload" @click="handleUpload">导入对账单</el-button>
      </right-toolbar>
    </el-row>

    <!-- 账户卡片区域 -->
    <el-row :gutter="16" class="mb8">
      <el-col :span="6" v-for="item in accountCards" :key="item.id">
        <el-card shadow="hover" :class="{ 'active-card': searchForm.accountId === item.id }" @click="handleAccountClick(item)">
          <div class="account-card">
            <div class="account-header">
              <span class="account-name">{{ item.name }}</span>
              <el-tag :type="item.accountType === 1 ? 'success' : 'warning'" size="small">
                {{ item.accountType === 1 ? '现金类' : '账期类' }}
              </el-tag>
            </div>
            <div class="account-balance">
              <span class="label">余额</span>
              <span class="value">{{ formatNumber(item.balance) }}</span>
            </div>
            <div class="account-stats">
              <div class="stat-item">
                <span class="stat-label">订单总额</span>
                <span class="stat-value">{{ formatNumber(item.totalOrderAmount) }}</span>
              </div>
              <div class="stat-item">
                <span class="stat-label">已付总额</span>
                <span class="stat-value paid">{{ formatNumber(item.totalPaidAmount) }}</span>
              </div>
              <div class="stat-item">
                <span class="stat-label">未付总额</span>
                <span class="stat-value unpaid">{{ formatNumber(item.totalUnpaidAmount) }}</span>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 统计卡片 -->
    <el-row :gutter="16" class="mb8">
      <el-col :span="6">
        <el-card shadow="never" class="stat-card">
          <div class="stat-card-content">
            <div class="stat-card-icon" style="background: #409eff">
              <el-icon><Document /></el-icon>
            </div>
            <div class="stat-card-info">
              <div class="stat-card-label">订单总数</div>
              <div class="stat-card-value">{{ statistics.totalOrders || 0 }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="never" class="stat-card">
          <div class="stat-card-content">
            <div class="stat-card-icon" style="background: #67c23a">
              <el-icon><Money /></el-icon>
            </div>
            <div class="stat-card-info">
              <div class="stat-card-label">订单总额</div>
              <div class="stat-card-value">{{ formatNumber(statistics.totalOrderAmount) }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="never" class="stat-card">
          <div class="stat-card-content">
            <div class="stat-card-icon" style="background: #e6a23c">
              <el-icon><Check /></el-icon>
            </div>
            <div class="stat-card-info">
              <div class="stat-card-label">已付总额</div>
              <div class="stat-card-value">{{ formatNumber(statistics.totalPaidAmount) }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="never" class="stat-card">
          <div class="stat-card-content">
            <div class="stat-card-icon" style="background: #f56c6c">
              <el-icon><Warning /></el-icon>
            </div>
            <div class="stat-card-info">
              <div class="stat-card-label">未付总额</div>
              <div class="stat-card-value">{{ formatNumber(statistics.totalUnpaidAmount) }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 表格区域 -->
    <el-table v-loading="loading" :data="tableData" style="width: 100%" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="订单号" prop="orderNumber" width="150" fixed="left">
        <template #default="{ row }">
          <el-link type="primary" @click="handleOrderDetail(row)">{{ row.orderNumber }}</el-link>
        </template>
      </el-table-column>
      <el-table-column label="供应商" prop="supplierName"   show-overflow-tooltip />
      <el-table-column label="物料名称" prop="materialName" width="150" show-overflow-tooltip />
      <el-table-column label="SKU" prop="materialSku" width="120" />
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
      <el-table-column label="未付金额" prop="unpaidAmount" width="120" align="right">
        <template #default="{ row }">
          <span class="amount unpaid">{{ formatNumber(row.unpaidAmount) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="订单状态" prop="orderStatus" width="100" align="center">
        <template #default="{ row }">
          <el-tag :type="getOrderStatusType(row.orderStatus)">
            {{ getOrderStatusText(row.orderStatus) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="付款状态" prop="payStatus" width="100" align="center">
        <template #default="{ row }">
          <el-tag :type="getPayStatusType(row.payStatus)">
            {{ getPayStatusText(row.payStatus) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="创建时间" prop="createdate" width="160" />
      <el-table-column label="交货日期" prop="deliverydate" width="120" />
      <el-table-column label="操作" width="180" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" link icon="View" @click="handleViewPayments(row)">付款明细</el-button>
          <el-button type="success" link icon="Money" @click="handlePay(row)" v-if="row.unpaidAmount > 0">付款</el-button>
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

    <!-- 付款弹窗 -->
    <pay-dialog ref="payDialogRef" @success="handleSearch" />

    <!-- 付款明细抽屉 -->
    <payment-drawer ref="paymentDrawerRef" />

    <!-- 批量上传弹窗 -->
    <upload-dialog ref="uploadDialogRef" @success="handleSearch" />
  </div>
</template>

<script setup name="PurchaseLedger">
import { ref, reactive, onMounted } from 'vue'
import { getCurrentInstance } from '@vue/runtime-core'
import { Document, Money, Check, Warning } from '@element-plus/icons-vue'
import { getPurchaseLedgerList, getPurchaseLedgerStatistics, getPurchaseLedgerAccounts } from '@/api/finance/purchaseLedger'
import customerApi from '@/api/erp/material/customerApi.js'
import PayDialog from './components/pay-dialog.vue'
import PaymentDrawer from './components/payment-drawer.vue'
import UploadDialog from './components/upload-dialog.vue'

const { proxy } = getCurrentInstance()

const loading = ref(false)
const showSearch = ref(true)
const tableData = ref([])
const total = ref(0)
const accountList = ref([])
const accountCards = ref([])
const supplierList = ref([])
const statistics = ref({})
const selectedRows = ref([])

const searchRef = ref()
const payDialogRef = ref()
const paymentDrawerRef = ref()
const uploadDialogRef = ref()

const searchForm = reactive({
  accountId: null,
  supplierId: null,
  dateRange: [],
  orderStatus: null,
  payStatus: null
})

const queryParams = reactive({
  pageNum: 1,
  pageSize: 20
})

// 获取账户卡片数据
async function getAccountCards() {
  try {
    const res = await getPurchaseLedgerAccounts()
    accountCards.value = res.data || []
  } catch (error) {
    console.error('获取账户卡片失败:', error)
  }
}

// 获取统计数据
async function getStatistics() {
  try {
    const params = { ...searchForm }
    const res = await getPurchaseLedgerStatistics(params)
    statistics.value = res.data || {}
  } catch (error) {
    console.error('获取统计数据失败:', error)
  }
}

// 获取列表数据
async function getList() {
  loading.value = true
  try {
    const params = {
      ...searchForm,
      ...queryParams,
      startDate: searchForm.dateRange?.[0] || null,
      endDate: searchForm.dateRange?.[1] || null
    }
    delete params.dateRange

    const res = await getPurchaseLedgerList(params)
    tableData.value = res.data?.list || []
    total.value = res.data?.total || 0
  } catch (error) {
    console.error('获取台账列表失败:', error)
    proxy.$message.error('查询失败')
  } finally {
    loading.value = false
  }
}

// 查询
function handleSearch() {
  queryParams.pageNum = 1
  getList()
  getStatistics()
}

// 重置
function handleReset() {
  proxy.resetForm('searchRef')
  handleSearch()
}

// 点击账户卡片
function handleAccountClick(account) {
  searchForm.accountId = searchForm.accountId === account.id ? null : account.id
  handleSearch()
}

// 选择行
function handleSelectionChange(selection) {
  selectedRows.value = selection
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

// 查看订单详情
function handleOrderDetail(row) {
  // 跳转到采购订单详情
  router.push(`/erp/purchase/orders/detail/${row.orderId}`)
}

// 查看付款明细
function handleViewPayments(row) {
  paymentDrawerRef.value.open(row.orderId)
}

// 付款
function handlePay(row) {
  payDialogRef.value.open(row)
}

// 批量付款
function handleBatchPay() {
  if (selectedRows.value.length === 0) {
    proxy.$message.warning('请选择需要付款的订单')
    return
  }
  payDialogRef.value.openBatch(selectedRows.value)
}

// 导出
async function handleExport() {
  try {
    const params = { ...searchForm }
    proxy.$message.success('导出成功')
  } catch (error) {
    proxy.$message.error('导出失败')
  }
}

// 导入对账单
function handleUpload() {
  uploadDialogRef.value.open()
}

// 格式化数字
function formatNumber(num) {
  if (!num || num === 0) return '0.00'
  return Number(num).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

// 订单状态
function getOrderStatusType(status) {
  const types = { 0: 'info', 1: 'warning', 2: 'success', 3: '' }
  return types[status] || 'info'
}

function getOrderStatusText(status) {
  const texts = { 0: '草稿', 1: '待审核', 2: '审核通过', 3: '已完成' }
  return texts[status] || '未知'
}

// 付款状态
function getPayStatusType(status) {
  const types = { 0: 'danger', 1: 'warning', 2: 'success' }
  return types[status] || 'info'
}

function getPayStatusText(status) {
  const texts = { 0: '未付款', 1: '部分付款', 2: '已付款' }
  return texts[status] || '未知'
}

onMounted(async () => {
  await getAccountCards()
  await getStatistics()
  getList()
  loadSuppliers()
})

// 加载供应商列表
async function loadSuppliers() {
  const res = await customerApi.listAll()
  supplierList.value = (res.data || []).map(item => ({
    ...item,
    value: item.id,
    label: item.name
  }))
}
</script>

<style scoped>
.account-card {
  padding: 10px;
}

.account-card .account-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.account-card .account-name {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.account-card .account-balance {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 0;
  border-bottom: 1px solid #ebeef5;
  margin-bottom: 12px;
}

.account-card .account-balance .label {
  font-size: 14px;
  color: #909399;
}

.account-card .account-balance .value {
  font-size: 20px;
  font-weight: 600;
  color: #409eff;
  font-family: 'DIN Alternate', monospace;
}

.account-card .account-stats {
  display: flex;
  justify-content: space-between;
}

.account-card .stat-item {
  text-align: center;
}

.account-card .stat-label {
  font-size: 12px;
  color: #909399;
  display: block;
}

.account-card .stat-value {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
}

.account-card .stat-value.paid {
  color: #67c23a;
}

.account-card .stat-value.unpaid {
  color: #f56c6c;
}

.active-card {
  border-color: #409eff;
  background: #ecf5ff;
}

.stat-card {
  height: 100px;
}

.stat-card .stat-card-content {
  display: flex;
  align-items: center;
  height: 100%;
}

.stat-card .stat-card-icon {
  width: 50px;
  height: 50px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 16px;
}

.stat-card .stat-card-icon .el-icon {
  font-size: 24px;
  color: #fff;
}

.stat-card .stat-card-info {
  flex: 1;
}

.stat-card .stat-card-label {
  font-size: 14px;
  color: #909399;
  margin-bottom: 8px;
}

.stat-card .stat-card-value {
  font-size: 24px;
  font-weight: 600;
  color: #303133;
  font-family: 'DIN Alternate', monospace;
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
</style>
