<template>
  <div class="app-container">
    <el-tabs v-model="mainTab">
      <!-- 发票列表 Tab -->
      <el-tab-pane label="发票列表" name="list">
        <!-- 操作栏 -->
        <el-row :gutter="10" class="mb8">
          <el-col :span="1.5">
            <el-button type="primary" plain icon="Document" :disabled="selectedInvoices.length === 0" @click="handleGeneratePurchaseVoucher">
              生成采购凭证
            </el-button>
          </el-col>
          <el-col :span="1.5">
            <el-button type="success" plain icon="Document" :disabled="selectedInvoices.length === 0" @click="handleGenerateCarrierVoucher">
              生成承运商凭证
            </el-button>
          </el-col>
          <el-col :span="1.5">
            <el-button type="warning" plain icon="Upload" @click="handleSync">同步发票</el-button>
          </el-col>
          <el-col :span="1.5">
            <el-button type="warning" plain icon="Upload" @click="handleImport">导入发票</el-button>
          </el-col>
          <el-col :span="1.5">
            <el-button type="success" plain icon="Plus" @click="handleAddInvoice">新增发票</el-button>
          </el-col>
          <el-col :span="1.5">
            <el-button type="danger" plain icon="Delete" :disabled="selectedInvoices.length === 0" @click="handleDelete">
              删除
            </el-button>
          </el-col>
          <right-toolbar v-model:showSearch="showSearch" @queryTable="getInvoiceList"></right-toolbar>
        </el-row>

        <!-- 搜索条件 -->
        <el-card v-show="showSearch" class="search-card" shadow="never">
          <el-form :model="queryParams" ref="queryRef" :inline="true" label-width="80px">
            <el-form-item label="发票号码" prop="invoiceNo">
              <el-input v-model="queryParams.invoiceNo" placeholder="请输入发票号码" clearable style="width: 160px" />
            </el-form-item>
            <el-form-item label="供应商" prop="sellerName">
              <el-input v-model="queryParams.sellerName" placeholder="请输入供应商名称" clearable style="width: 160px" />
            </el-form-item>
            <el-form-item label="入账状态" prop="postingStatus">
              <el-select v-model="queryParams.postingStatus" placeholder="全部" clearable style="width: 120px">
                <el-option label="未入账" :value="0" />
                <el-option label="已入账" :value="1" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
              <el-button icon="Refresh" @click="resetQuery">重置</el-button>
            </el-form-item>
          </el-form>
        </el-card>

        <!-- 统计信息 -->
        <el-row :gutter="16" class="mb8" v-if="statistics">
          <el-col :span="6">
            <el-statistic title="发票总数" :value="statistics.totalCount || 0" />
          </el-col>
          <el-col :span="6">
            <el-statistic title="未入账" :value="statistics.unpostedCount || 0" />
          </el-col>
          <el-col :span="6">
            <el-statistic title="已入账" :value="statistics.postedCount || 0" />
          </el-col>
          <el-col :span="6">
            <el-statistic title="价税合计">
              <template #default>
                <span class="amount">{{ formatNumber(statistics.totalAmountWithTax) }}</span>
              </template>
            </el-statistic>
          </el-col>
        </el-row>

        <!-- 发票列表 -->
        <el-table
          v-loading="invoiceLoading"
          :data="invoiceList"
          border
          stripe
          style="width: 100%"
          @selection-change="handleSelectionChange"
          max-height="calc(100vh - 400px)"
        >
          <el-table-column type="selection" width="55" align="center" />
          <el-table-column label="发票号码" width="220" show-overflow-tooltip>
            <template #default="{ row }">
              <span v-if="row.digitalInvoiceNo">{{ row.digitalInvoiceNo }}</span>
              <span v-else>{{ row.invoiceCode ? row.invoiceCode + ' ' : '' }}{{ row.invoiceNo }}</span>
            </template>
          </el-table-column>
          <el-table-column label="发票类型" prop="invoiceType" width="150">
            <template #default="{ row }">
              <el-tag :type="getInvoiceTypeTag(row.invoiceType)" size="small">
                {{ getInvoiceTypeText(row.invoiceType) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="供应商" prop="sellerName" min-width="150" show-overflow-tooltip />
          <el-table-column label="供应商类型" width="100" align="center">
            <template #default="{ row }">
              <el-tag v-if="row.supplierId" type="primary" size="small">供应商</el-tag>
              <el-tag v-else-if="row.carrierId" type="warning" size="small">承运商</el-tag>
              <el-tag v-else type="info" size="small">未知</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="开票日期" prop="invoiceDate" width="110" />
          <el-table-column label="价税合计" prop="amountWithTax" width="120" align="right">
            <template #default="{ row }">
              <span class="amount">{{ formatNumber(row.amountWithTax) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="不含税金额" prop="amountWithoutTax" width="120" align="right">
            <template #default="{ row }">
              <span class="amount">{{ formatNumber(row.amountWithoutTax) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="税额" prop="taxAmount" width="100" align="right">
            <template #default="{ row }">
              <span class="amount">{{ formatNumber(row.taxAmount) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="入账状态" prop="postingStatus" width="100" align="center">
            <template #default="{ row }">
              <el-tag :type="row.postingStatus === 1 ? 'success' : 'info'" size="small">
                {{ row.postingStatus === 1 ? '已入账' : '未入账' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="100" align="center" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" icon="View" @click="handleDetail(row)">详情</el-button>
            </template>
          </el-table-column>
        </el-table>

        <!-- 分页 -->
        <el-pagination
          v-if="invoiceTotal > 0"
          class="mt16"
          :total="invoiceTotal"
          v-model:current-page="queryParams.pageNum"
          v-model:page-size="queryParams.pageSize"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="getInvoiceList"
          @current-change="getInvoiceList"
        />
      </el-tab-pane>

      <!-- 导入税控文件 Tab -->
      <el-tab-pane label="导入税控文件" name="import">
        <!-- 操作栏 -->
        <el-row :gutter="10" class="mb8">
          <el-col :span="1.5">
            <el-button type="warning" plain icon="Upload" @click="handleImportTaxFile">导入税控文件</el-button>
          </el-col>
          <el-col :span="1.5">
            <el-button type="danger" plain icon="Delete" :disabled="!importedData" @click="handleClear">清空数据</el-button>
          </el-col>
        </el-row>

        <!-- 未导入时的空状态 -->
        <el-empty v-if="!importedData" description="暂无数据，请导入税控平台文件">
          <el-button type="primary" icon="Upload" @click="handleImportTaxFile">导入文件</el-button>
        </el-empty>

        <!-- 导入后显示数据 -->
        <div v-else>
          <el-alert
            :title="'已导入 ' + sheetNames.length + ' 个页签，共 ' + totalRows + ' 条数据'"
            type="success"
            :closable="false"
            show-icon
            class="mb16"
          />
          <el-tabs v-model="activeTab" type="border-card">
            <el-tab-pane
              v-for="sheetName in sheetNames"
              :key="sheetName"
              :label="sheetName"
              :name="sheetName"
            >
              <div class="table-container">
                <el-table
                  :data="currentSheetData.rows"
                  border
                  stripe
                  style="width: 100%"
                  max-height="calc(100vh - 380px)"
                  :header-cell-style="{ background: '#f5f7fa', color: '#606266', fontWeight: '600' }"
                >
                  <el-table-column
                    v-for="header in currentSheetData.headers"
                    :key="header"
                    :label="header"
                    :prop="header"
                    min-width="120"
                    show-overflow-tooltip
                  />
                </el-table>
              </div>
              <div class="row-count">
                共 {{ currentSheetData.rows.length }} 条记录
              </div>
            </el-tab-pane>
          </el-tabs>
        </div>
      </el-tab-pane>
    </el-tabs>

    <!-- 导入弹窗 -->
    <import-dialog ref="importDialogRef" @imported="handleImported" />

    <!-- 发票详情抽屉 -->
    <detail-drawer ref="detailDrawerRef" />

    <!-- 生成凭证设置对话框 -->
    <invoice-posting-dialog ref="postingDialogRef" @success="handlePostingSuccess" />

    <!-- 新增发票对话框 -->
    <add-invoice-dialog ref="addInvoiceDialogRef" @success="handleAddInvoiceSuccess" />
  </div>
</template>

<script setup name="InvoiceLedger">
import { ref, computed, reactive } from 'vue'
import { getCurrentInstance } from '@vue/runtime-core'
import { getInvoiceList as apiGetInvoiceList, getInvoiceStatistics, exportInvoices, importInvoicesFromJson } from '@/api/finance/invoiceLedger'
import { listAll } from '@/api/finance/subjects'
import finStore from '@/hooks/store/useFinanceStore.js'
import ImportDialog from './components/import-dialog.vue'
import DetailDrawer from './components/detail-drawer.vue'
import InvoicePostingDialog from './components/invoice-posting-dialog.vue'
import AddInvoiceDialog from './components/add-invoice-dialog.vue'

const { proxy } = getCurrentInstance()

// 主Tab
const mainTab = ref('list')

// 发票列表相关
const queryRef = ref()
const invoiceList = ref([])
const invoiceTotal = ref(0)
const invoiceLoading = ref(false)
const selectedInvoices = ref([])
const showSearch = ref(true)
const statistics = ref(null)
const subjectOptions = ref([])

const queryParams = reactive({
  pageNum: 1,
  pageSize: 20,
  invoiceNo: '',
  sellerName: '',
  postingStatus: null
})

// 税控导入相关
const importDialogRef = ref()
const detailDrawerRef = ref()
const postingDialogRef = ref()
const addInvoiceDialogRef = ref()
const importedData = ref(null)
const activeTab = ref('')

// 页签名称列表
const sheetNames = computed(() => {
  if (!importedData.value) return []
  return Object.keys(importedData.value)
})

// 当前页签数据
const currentSheetData = computed(() => {
  if (!importedData.value || !activeTab.value) {
    return { headers: [], rows: [] }
  }
  return importedData.value[activeTab.value] || { headers: [], rows: [] }
})

// 总行数
const totalRows = computed(() => {
  if (!importedData.value) return 0
  return Object.values(importedData.value).reduce((sum, sheet) => sum + sheet.rows.length, 0)
})

// 格式化数字
function formatNumber(num) {
  if (!num || num === 0) return '0.00'
  return Number(num).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

// 票种编码到中文名称的映射
const invoiceTypeMap = {
  'VAT_SPECIAL': '增值税专用发票',
  'VAT_NORMAL': '增值税普通发票',
  'MOTOR': '机动车销售统一发票',
  'FREIGHT': '货物运输业增值税专用发票',
  'TOLL': '通行费电子发票',
  'RAILWAY': '铁路电子客票'
}

// 票种编码到标签颜色的映射
const invoiceTypeColorMap = {
  'VAT_SPECIAL': '',
  'VAT_NORMAL': 'success',
  'MOTOR': 'warning',
  'FREIGHT': 'warning',
  'TOLL': 'warning',
  'RAILWAY': 'warning'
}

// 发票类型标签颜色
function getInvoiceTypeTag(type) {
  if (!type) return 'info'
  return invoiceTypeColorMap[type] ?? 'info'
}

// 发票类型显示文本
function getInvoiceTypeText(type) {
  if (!type) return '未知'
  return invoiceTypeMap[type] || type
}

/** 查询发票列表 */
async function getInvoiceList() {
  invoiceLoading.value = true
  try {
    const groupid = await finStore.getCurrentTenantId()
    console.log('获取到的groupid:', groupid)
    const params = {
      pageNum: queryParams.pageNum,
      pageSize: queryParams.pageSize,
      invoiceNo: queryParams.invoiceNo,
      sellerName: queryParams.sellerName,
      postingStatus: queryParams.postingStatus,
      groupid
    }
    console.log('查询发票列表，完整参数:', JSON.stringify(params))
    const response = await apiGetInvoiceList(params)
    console.log('查询发票列表，响应:', response)
    invoiceList.value = response?.rows || []
    invoiceTotal.value = response?.total || 0
  } catch (error) {
    console.error('查询发票列表失败:', error)
    invoiceList.value = []
    invoiceTotal.value = 0
  } finally {
    invoiceLoading.value = false
  }
}

/** 查询统计数据 */
async function getStatistics() {
  try {
    const groupid = await finStore.getCurrentTenantId()
    const response = await getInvoiceStatistics({ groupid })
    statistics.value = response?.data || {}
  } catch (error) {
    console.error('查询统计数据失败:', error)
  }
}

/** 加载会计科目选项 */
async function loadSubjectOptions() {
  try {
    const groupid = await finStore.getCurrentTenantId()
    const response = await listAll({ groupid, status: 1 })
    subjectOptions.value = response?.data || []
  } catch (error) {
    console.error('加载会计科目失败:', error)
  }
}

/** 搜索 */
function handleQuery() {
  queryParams.pageNum = 1
  getInvoiceList()
}

/** 重置 */
function resetQuery() {
  proxy.resetForm("queryRef")
  handleQuery()
}

/** 选择变更 */
function handleSelectionChange(selection) {
  selectedInvoices.value = selection
}

/** 生成采购凭证 */
function handleGeneratePurchaseVoucher() {
  if (selectedInvoices.value.length === 0) {
    proxy.$message.warning('请先选择需要生成凭证的发票')
    return
  }
  const postedInvoices = selectedInvoices.value.filter(item => item.postingStatus === 1)
  if (postedInvoices.length > 0) {
    proxy.$message.warning(`选中有 ${postedInvoices.length} 张已入账的发票，将自动跳过`)
  }
  postingDialogRef.value.open(selectedInvoices.value, subjectOptions.value, 0)
}

/** 生成承运商凭证 */
function handleGenerateCarrierVoucher() {
  if (selectedInvoices.value.length === 0) {
    proxy.$message.warning('请先选择需要生成凭证的发票')
    return
  }
  const postedInvoices = selectedInvoices.value.filter(item => item.postingStatus === 1)
  if (postedInvoices.length > 0) {
    proxy.$message.warning(`选中有 ${postedInvoices.length} 张已入账的发票，将自动跳过`)
  }
  postingDialogRef.value.open(selectedInvoices.value, subjectOptions.value, 1)
}

/** 生成凭证成功回调 */
function handlePostingSuccess() {
  selectedInvoices.value = []
  getInvoiceList()
  getStatistics()
}

/** 新增发票成功回调 */
function handleAddInvoiceSuccess() {
  getInvoiceList()
  getStatistics()
}

/** 同步发票 */
function handleSync() {
  proxy.$message.info('同步发票功能开发中')
}

/** 导入发票 */
function handleImport() {
  proxy.$message.info('导入发票功能开发中')
}

/** 新增发票 */
function handleAddInvoice() {
  addInvoiceDialogRef.value.open()
}

/** 删除发票 */
function handleDelete() {
  proxy.$message.info('删除发票功能开发中')
}

/** 查看详情 */
function handleDetail(row) {
  detailDrawerRef.value.open(row.id)
}

/** 导入税控文件 */
function handleImportTaxFile() {
  importDialogRef.value.open()
}

/** 清理单行数据：去空值，转字符串 */
function cleanRow(row) {
  const cleaned = {}
  for (const [key, value] of Object.entries(row)) {
    if (value !== null && value !== undefined && value !== '') {
      cleaned[key] = String(value)
    }
  }
  return cleaned
}

/** 导入完成回调：按页签分组发送给后端，后端按页签分别处理主表/明细/扩展 */
async function handleImported(sheets) {
  importedData.value = sheets
  const names = Object.keys(sheets)
  if (names.length > 0) {
    activeTab.value = names[0]
  }

  // 调试：打印每个页签的解析结果
  console.log('[税控导入] 共解析到', names.length, '个页签:', names)
  for (const sheetName of names) {
    const sd = sheets[sheetName]
    console.log(`[税控导入] 页签"${sheetName}"：表头=[${sd?.headers?.join(', ')}]，数据行数=${sd?.rows?.length || 0}`)
    if (sd?.rows?.length > 0) {
      console.log(`[税控导入] 页签"${sheetName}"第一行数据:`, sd.rows[0])
    }
  }

  // 按页签分组清理数据，保留页签名称
  const sheetsData = {}
  let totalCount = 0
  for (const sheetName of names) {
    const sheetData = sheets[sheetName]
    if (sheetData && sheetData.rows && sheetData.rows.length > 0) {
      const cleaned = sheetData.rows.map(cleanRow).filter(row => Object.keys(row).length > 0)
      if (cleaned.length > 0) {
        sheetsData[sheetName] = cleaned
        totalCount += cleaned.length
      }
    }
  }

  if (totalCount === 0) {
    proxy.$message.warning('没有有效的发票数据')
    return
  }

  try {
    const groupid = await finStore.getCurrentTenantId()
    if (!groupid) {
      proxy.$message.error('请先选择账套')
      return
    }
    console.log('[税控导入] 按页签发送到后端:', Object.keys(sheetsData).map(k => `${k}:${sheetsData[k].length}条`).join(', '))
    const res = await importInvoicesFromJson(sheetsData, groupid)
    proxy.$message.success(res.msg || '导入成功')
    // 刷新发票列表
    getInvoiceList()
    getStatistics()
    // 切换到发票列表Tab
    mainTab.value = 'list'
  } catch (error) {
    console.error('导入发票失败:', error)
    console.error('错误响应:', error.response)
    console.error('错误数据:', error.response?.data)
    const errMsg = error.response?.data?.msg || error.message || '未知错误'
    proxy.$message.error('保存发票数据失败：' + errMsg)
  }
}

/** 清空数据 */
function handleClear() {
  proxy.$confirm('确认清空已导入的数据？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    importedData.value = null
    activeTab.value = ''
    proxy.$message.success('已清空')
  }).catch(() => {})
}

// 初始化
loadSubjectOptions().then(() => {
  getInvoiceList()
  getStatistics()
})
</script>

<style scoped>
.mb8 {
  margin-bottom: 8px;
}

.mb16 {
  margin-bottom: 16px;
}

.mt16 {
  margin-top: 16px;
}

.search-card {
  margin-bottom: 8px;
}

.search-card :deep(.el-card__body) {
  padding-bottom: 0;
}

.amount {
  font-family: 'DIN Alternate', monospace;
}

.table-container {
  overflow-x: auto;
}

.row-count {
  margin-top: 12px;
  text-align: right;
  font-size: 13px;
  color: #909399;
}
</style>
