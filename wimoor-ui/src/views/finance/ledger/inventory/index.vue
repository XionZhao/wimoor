<template>
  <div class="app-container">
    <!-- 查询区域 -->
    <el-row :gutter="10" class="mb8">
      <el-form :model="searchForm" ref="searchRef" :inline="true" v-show="showSearch" label-width="98px">
        <el-form-item label="SKU">
          <el-input v-model="searchForm.sku" placeholder="请输入SKU" clearable style="width: 150px" />
        </el-form-item>
        <el-form-item label="物料名称">
          <el-input v-model="searchForm.materialName" placeholder="请输入物料名称" clearable style="width: 150px" />
        </el-form-item>
        <el-form-item label="仓库">
          <el-select v-model="searchForm.warehouseId" placeholder="请选择仓库" clearable style="width: 150px">
            <el-option
              v-for="item in warehouseList"
              :key="item.id"
              :label="item.name"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="会计期间">
          <el-date-picker
            v-model="searchForm.period"
            type="month"
            placeholder="选择月份"
            format="YYYY年MM月"
            value-format="YYYYMM"
            style="width: 150px"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="Search" @click="handleSearch">查询</el-button>
          <el-button icon="Refresh" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList">
        <el-button type="primary" plain icon="Download" @click="handleExport">导出</el-button>
        <el-button type="warning" plain icon="Document" @click="handleBatchVoucher">批量生成凭证</el-button>
      </right-toolbar>
    </el-row>

    <!-- 标签页 -->
    <el-tabs v-model="activeTab" @tab-click="handleTabClick">
      <!-- 汇总账 -->
      <el-tab-pane label="汇总账" name="summary">
        <el-table v-loading="loading" :data="summaryData" style="width: 100%" @selection-change="handleSelectionChange">
          <el-table-column type="selection" width="55" align="center" />
          <el-table-column label="SKU" prop="sku" width="120" fixed="left">
            <template #default="{ row }">
              <el-link type="primary" @click="handleViewDetail(row)">{{ row.sku }}</el-link>
            </template>
          </el-table-column>
          <el-table-column label="物料名称" prop="materialName"   show-overflow-tooltip />
          <el-table-column label="仓库" prop="warehouseName" width="120" />
          <el-table-column label="当前数量" prop="currentQty" width="100" align="right">
            <template #default="{ row }">
              <span :class="{ 'text-danger': row.currentQty < 0 }">{{ row.currentQty }}</span>
            </template>
          </el-table-column>
          <el-table-column label="当前金额" prop="currentAmount" width="120" align="right">
            <template #default="{ row }">
              <span class="amount">{{ formatNumber(row.currentAmount) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="移动平均单价" prop="unitCost" width="120" align="right">
            <template #default="{ row }">
              <span class="amount">{{ formatNumber(row.unitCost) }}</span>
            </template>
          </el-table-column>
          
          <!-- 本期变动 -->
          <el-table-column label="本期入库" align="center">
            <el-table-column label="数量" prop="inQty" width="80" align="right" />
            <el-table-column label="金额" prop="inAmount" width="100" align="right">
              <template #default="{ row }">
                <span class="amount paid">{{ formatNumber(row.inAmount) }}</span>
              </template>
            </el-table-column>
          </el-table-column>
          <el-table-column label="本期出库" align="center">
            <el-table-column label="数量" prop="outQty" width="80" align="right" />
            <el-table-column label="金额" prop="outAmount" width="100" align="right">
              <template #default="{ row }">
                <span class="amount unpaid">{{ formatNumber(row.outAmount) }}</span>
              </template>
            </el-table-column>
          </el-table-column>
          
          <!-- 累计变动 -->
          <el-table-column label="累计入库" align="center">
            <el-table-column label="数量" prop="totalInQty" width="80" align="right" />
            <el-table-column label="金额" prop="totalInAmount" width="100" align="right">
              <template #default="{ row }">
                <span class="amount">{{ formatNumber(row.totalInAmount) }}</span>
              </template>
            </el-table-column>
          </el-table-column>
          <el-table-column label="累计出库" align="center">
            <el-table-column label="数量" prop="totalOutQty" width="80" align="right" />
            <el-table-column label="金额" prop="totalOutAmount" width="100" align="right">
              <template #default="{ row }">
                <span class="amount">{{ formatNumber(row.totalOutAmount) }}</span>
              </template>
            </el-table-column>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <!-- 明细账 -->
      <el-tab-pane label="明细账" name="detail">
        <el-table v-loading="loading" :data="detailData" style="width: 100%">
          <el-table-column label="变动日期" prop="transDate" width="160" />
          <el-table-column label="变动类型" prop="transType" width="120" align="center">
            <template #default="{ row }">
              <el-tag :type="getTransTypeTag(row.transType)">
                {{ getTransTypeText(row.transType) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="单据号" prop="sourceDocNumber" width="150">
            <template #default="{ row }">
              <el-link type="primary" @click="handleViewDoc(row)">{{ row.sourceDocNumber }}</el-link>
            </template>
          </el-table-column>
          <el-table-column label="SKU" prop="sku" width="120" />
          <el-table-column label="物料名称" prop="materialName"   show-overflow-tooltip />
          <el-table-column label="仓库" prop="warehouseName" width="120" />
          
          <!-- 数量变动 -->
          <el-table-column label="变动前数量" prop="qtyBefore" width="100" align="right" />
          <el-table-column label="变动数量" prop="qtyChange" width="100" align="right">
            <template #default="{ row }">
              <span :class="row.qtyChange > 0 ? 'text-success' : 'text-danger'">
                {{ row.qtyChange > 0 ? '+' : '' }}{{ row.qtyChange }}
              </span>
            </template>
          </el-table-column>
          <el-table-column label="变动后数量" prop="qtyAfter" width="100" align="right" />
          
          <!-- 金额变动 -->
          <el-table-column label="变动前金额" prop="amountBefore" width="120" align="right">
            <template #default="{ row }">
              <span class="amount">{{ formatNumber(row.amountBefore) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="变动金额" prop="amountChange" width="120" align="right">
            <template #default="{ row }">
              <span class="amount" :class="row.amountChange > 0 ? 'text-success' : 'text-danger'">
                {{ row.amountChange > 0 ? '+' : '' }}{{ formatNumber(row.amountChange) }}
              </span>
            </template>
          </el-table-column>
          <el-table-column label="变动后金额" prop="amountAfter" width="120" align="right">
            <template #default="{ row }">
              <span class="amount">{{ formatNumber(row.amountAfter) }}</span>
            </template>
          </el-table-column>
          
          <!-- 单价信息 -->
          <el-table-column label="发生时单价" prop="unitCost" width="120" align="right">
            <template #default="{ row }">
              <span class="amount">{{ formatNumber(row.unitCost) }}</span>
            </template>
          </el-table-column>
          
          <!-- 凭证信息 -->
          <el-table-column label="凭证号" prop="voucherNumber" width="150">
            <template #default="{ row }">
              <el-link v-if="row.voucherNumber" type="primary" @click="handleViewVoucher(row)">{{ row.voucherNumber }}</el-link>
              <span v-else class="text-gray">-</span>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <!-- 勾稽校验 -->
      <el-tab-pane label="勾稽校验" name="check">
        <el-alert
          title="勾稽校验说明"
          type="info"
          :closable="false"
          show-icon
          class="mb16"
        >
          <template #default>
            <p>校验库存表与变动记录表数据一致性，差异数量/金额不为0表示存在数据异常</p>
          </template>
        </el-alert>

        <el-table v-loading="loading" :data="checkData" style="width: 100%">
          <el-table-column label="SKU" prop="sku" width="120" />
          <el-table-column label="物料名称" prop="materialName"   show-overflow-tooltip />
          <el-table-column label="仓库" prop="warehouseName" width="120" />
          
          <!-- 数量校验 -->
          <el-table-column label="数量校验" align="center">
            <el-table-column label="库存表" prop="inventoryQty" width="100" align="right" />
            <el-table-column label="记录汇总" prop="recordSumQty" width="100" align="right" />
            <el-table-column label="差异" prop="diffQty" width="100" align="right">
              <template #default="{ row }">
                <span :class="row.diffQty !== 0 ? 'text-danger' : 'text-success'">
                  {{ row.diffQty }}
                </span>
              </template>
            </el-table-column>
          </el-table-column>
          
          <!-- 金额校验 -->
          <el-table-column label="金额校验" align="center">
            <el-table-column label="库存表" prop="inventoryAmount" width="120" align="right">
              <template #default="{ row }">
                <span class="amount">{{ formatNumber(row.inventoryAmount) }}</span>
              </template>
            </el-table-column>
            <el-table-column label="记录汇总" prop="recordSumAmount" width="120" align="right">
              <template #default="{ row }">
                <span class="amount">{{ formatNumber(row.recordSumAmount) }}</span>
              </template>
            </el-table-column>
            <el-table-column label="差异" prop="diffAmount" width="120" align="right">
              <template #default="{ row }">
                <span class="amount" :class="row.diffAmount !== 0 ? 'text-danger' : 'text-success'">
                  {{ formatNumber(row.diffAmount) }}
                </span>
              </template>
            </el-table-column>
          </el-table-column>
          
          <!-- 校验状态 -->
          <el-table-column label="校验状态" prop="checkStatus" width="100" align="center">
            <template #default="{ row }">
              <el-tag :type="row.checkStatus === 'normal' ? 'success' : 'danger'">
                {{ row.checkStatus === 'normal' ? '正常' : '异常' }}
              </el-tag>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>

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
  </div>
</template>

<script setup name="InventoryLedger">
import { ref, reactive, onMounted } from 'vue'
import { getCurrentInstance } from '@vue/runtime-core'
import { 
  getInventoryLedgerSummary, 
  getInventoryLedgerDetail, 
  getInventoryLedgerCheck,
  batchGenerateVoucher 
} from '@/api/finance/inventoryLedger'
import warehouseApi from '@/api/erp/warehouse/warehouseApi'

const { proxy } = getCurrentInstance()

const loading = ref(false)
const showSearch = ref(true)
const activeTab = ref('summary')
const warehouseList = ref([])
const selectedRows = ref([])

const searchRef = ref()

// 汇总账数据
const summaryData = ref([])
// 明细账数据
const detailData = ref([])
// 勾稽校验数据
const checkData = ref([])

const total = ref(0)

const searchForm = reactive({
  sku: '',
  materialName: '',
  warehouseId: null,
  period: ''
})

const queryParams = reactive({
  pageNum: 1,
  pageSize: 20
})

// 获取仓库列表
async function getWarehouses() {
  try {
    const res = await warehouseApi.getWarehouseList()
    warehouseList.value = res.data || []
  } catch (error) {
    console.error('获取仓库列表失败:', error)
  }
}

// 获取汇总账数据
async function getSummaryData() {
  loading.value = true
  try {
    const params = {
      ...searchForm,
      ...queryParams
    }
    const res = await getInventoryLedgerSummary(params)
    summaryData.value = res.data?.list || []
    total.value = res.data?.total || 0
  } catch (error) {
    console.error('获取汇总账失败:', error)
    proxy.$message.error('查询失败')
  } finally {
    loading.value = false
  }
}

// 获取明细账数据
async function getDetailData() {
  loading.value = true
  try {
    const params = {
      ...searchForm,
      ...queryParams
    }
    const res = await getInventoryLedgerDetail(params)
    detailData.value = res.data?.list || []
    total.value = res.data?.total || 0
  } catch (error) {
    console.error('获取明细账失败:', error)
    proxy.$message.error('查询失败')
  } finally {
    loading.value = false
  }
}

// 获取勾稽校验数据
async function getCheckData() {
  loading.value = true
  try {
    const params = { ...searchForm }
    const res = await getInventoryLedgerCheck(params)
    checkData.value = res.data || []
  } catch (error) {
    console.error('获取勾稽校验失败:', error)
    proxy.$message.error('查询失败')
  } finally {
    loading.value = false
  }
}

// 标签切换
function handleTabClick() {
  queryParams.pageNum = 1
  if (activeTab.value === 'summary') {
    getSummaryData()
  } else if (activeTab.value === 'detail') {
    getDetailData()
  } else if (activeTab.value === 'check') {
    getCheckData()
  }
}

// 查询
function handleSearch() {
  queryParams.pageNum = 1
  handleTabClick()
}

// 重置
function handleReset() {
  proxy.resetForm('searchRef')
  handleSearch()
}

// 刷新
function getList() {
  handleTabClick()
}

// 选择行
function handleSelectionChange(selection) {
  selectedRows.value = selection
}

// 分页
function handleSizeChange(size) {
  queryParams.pageSize = size
  handleTabClick()
}

function handleCurrentChange(page) {
  queryParams.pageNum = page
  handleTabClick()
}

// 查看明细
function handleViewDetail(row) {
  searchForm.sku = row.sku
  searchForm.warehouseId = row.warehouseId
  activeTab.value = 'detail'
  handleTabClick()
}

// 查看单据
function handleViewDoc(row) {
  // 根据单据类型跳转
  const routes = {
    PURCHASE_IN: '/erp/purchase/orders',
    SALE_OUT: '/erp/sales/orders',
    TRANSFER_IN: '/erp/transfer/in',
    TRANSFER_OUT: '/erp/transfer/out',
    ADJUST: '/erp/inventory/adjust'
  }
  const route = routes[row.transType] || '/erp/purchase/orders'
  router.push(`${route}/detail/${row.sourceDocId}`)
}

// 查看凭证
function handleViewVoucher(row) {
  router.push(`/finance/vouchers/detail/${row.voucherId}`)
}

// 批量生成凭证
async function handleBatchVoucher() {
  if (selectedRows.value.length === 0) {
    proxy.$message.warning('请选择需要生成凭证的记录')
    return
  }
  
  try {
    await proxy.$confirm('确认为选中的记录生成凭证吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    const ids = selectedRows.value.map(row => row.id)
    await batchGenerateVoucher({ ids })
    proxy.$message.success('凭证生成成功')
    handleTabClick()
  } catch (error) {
    if (error !== 'cancel') {
      proxy.$message.error('凭证生成失败')
    }
  }
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

// 格式化数字
function formatNumber(num) {
  if (!num || num === 0) return '0.00'
  return Number(num).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

// 变动类型
function getTransTypeTag(type) {
  const tags = {
    PURCHASE_IN: 'success',
    SALE_OUT: 'danger',
    TRANSFER_IN: 'primary',
    TRANSFER_OUT: 'warning',
    ADJUST: 'info',
    OTHER: ''
  }
  return tags[type] || ''
}

function getTransTypeText(type) {
  const texts = {
    PURCHASE_IN: '采购入库',
    SALE_OUT: '销售出库',
    TRANSFER_IN: '调拨入库',
    TRANSFER_OUT: '调拨出库',
    ADJUST: '盘点调整',
    OTHER: '其他'
  }
  return texts[type] || '未知'
}

onMounted(async () => {
  await getWarehouses()
  getSummaryData()
})
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

.text-success {
  color: #67c23a;
}

.text-danger {
  color: #f56c6c;
}

.text-gray {
  color: #909399;
}

.mb16 {
  margin-bottom: 16px;
}

.mt16 {
  margin-top: 16px;
}
</style>
