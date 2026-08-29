<template>
  <div class="voucher-log-container" style="padding: 10px;">
    <!-- 筛选栏 -->
    <el-form :inline="true" :model="queryParams" class="search-form">
      <el-form-item label="凭证类型">
        <el-select v-model="queryParams.voucherType" placeholder="全部" clearable style="width: 160px" @change="handleQuery">
          <el-option label="付款凭证" value="payment" />
          <el-option label="在途库存凭证" value="inventory_transit" />
          <el-option label="入库库存凭证" value="inventory_inbound" />
          <el-option label="采购发票凭证" value="invoice" />
          <el-option label="承运商发票凭证" value="invoice_carrier" />
        </el-select>
      </el-form-item>
      <el-form-item label="同步状态">
        <el-select v-model="queryParams.syncStatus" placeholder="全部" clearable style="width: 140px" @change="handleQuery">
          <el-option label="待同步" :value="0" />
          <el-option label="已同步" :value="1" />
          <el-option label="已变更" :value="2" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="handleQuery">查询</el-button>
        <el-button @click="resetQuery">重置</el-button>
        <el-button type="success" @click="openGenerateDialog">批量生成凭证</el-button>
        <el-button type="danger" @click="handleClearCurrentPage" :disabled="tableData.length === 0">清空当前页</el-button>
      </el-form-item>
    </el-form>

    <!-- 数据表格 -->
    <el-table
      v-loading="loading"
      :data="tableData"
      border
      stripe
      style="width: 100%"
      row-key="id"
      @expand-change="handleExpand"
    >
      <el-table-column type="expand">
        <template #default="{ row }">
          <div class="expand-content">
            <el-descriptions :column="2" border size="small" :title="row.voucherType === 'invoice' || row.voucherType === 'invoice_carrier' ? '发票信息' : '订单信息'">
              <el-descriptions-item :label="row.voucherType === 'invoice' || row.voucherType === 'invoice_carrier' ? '发票ID' : '订单ID'">{{ row.orderId }}</el-descriptions-item>
              <el-descriptions-item :label="row.voucherType === 'invoice' || row.voucherType === 'invoice_carrier' ? '发票号码' : '订单编号'">{{ row.orderNumber }}</el-descriptions-item>
              <el-descriptions-item :label="row.voucherType === 'invoice' || row.voucherType === 'invoice_carrier' ? '销方名称' : '供应商'">{{ row.supplierName }}</el-descriptions-item>
              <el-descriptions-item v-if="row.voucherType !== 'invoice' && row.voucherType !== 'invoice_carrier'" label="仓库">{{ row.warehouseName }}</el-descriptions-item>
              <el-descriptions-item label="数据指纹(MD5)">{{ row.dataHash }}</el-descriptions-item>
              <el-descriptions-item label="创建人">{{ row.createBy }}</el-descriptions-item>
              <el-descriptions-item label="创建时间">{{ row.createdTime }}</el-descriptions-item>
              <el-descriptions-item label="更新时间">{{ row.updatedTime }}</el-descriptions-item>
            </el-descriptions>
            <!-- 付款明细 -->
            <div v-if="row.voucherType === 'payment'" class="payment-section">
              <h4 class="section-title">付款明细</h4>
              <div v-if="row._paymentRecords === undefined" class="payment-empty">展开后加载中...</div>
              <div v-else-if="row._paymentRecords === null" class="payment-empty">加载中...</div>
              <div v-else-if="row._paymentRecords.length === 0" class="payment-empty">暂无付款记录</div>
              <el-table v-else :data="row._paymentRecords" border size="small" class="payment-table">
                <el-table-column label="付款日期" prop="paymentDate" width="110" align="center">
                  <template #default="{ row: pr }">
                    {{ pr.paymentDate || '--' }}
                  </template>
                </el-table-column>
                <el-table-column label="付款金额" width="120" align="right">
                  <template #default="{ row: pr }">
                    {{ formatMoney(pr.amount) }}
                  </template>
                </el-table-column>
                <el-table-column label="费用类型" prop="feeTypeName" min-width="120" show-overflow-tooltip />
                <el-table-column label="采购账户" prop="accountName" min-width="120" show-overflow-tooltip />
                <el-table-column label="状态" width="90" align="center">
                  <template #default="{ row: pr }">
                    <el-tag v-if="pr.paymentStatus === 0" type="danger" size="small">已取消</el-tag>
                    <el-tag v-else type="success" size="small">已付款</el-tag>
                  </template>
                </el-table-column>
                <el-table-column label="同步状态" width="100" align="center">
                  <template #default="{ row: pr }">
                    <el-tooltip :content="pr.syncStatus === 0 ? '新记录，等待生成凭证' : pr.syncStatus === 1 ? '已生成凭证' : pr.syncStatus === 2 ? '数据已变更，需重新生成凭证' : '已删除'" placement="top">
                      <el-tag :type="pr.syncStatus === 0 ? 'info' : pr.syncStatus === 1 ? 'success' : pr.syncStatus === 2 ? 'warning' : 'danger'" size="small">
                        {{ pr.syncStatus === 0 ? '待同步' : pr.syncStatus === 1 ? '已同步' : pr.syncStatus === 2 ? '已变更' : '已删除' }}
                      </el-tag>
                    </el-tooltip>
                  </template>
                </el-table-column>
              </el-table>
            </div>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="单据编号" prop="orderNumber" min-width="160" show-overflow-tooltip />
      <el-table-column label="凭证类型" width="130" align="center">
        <template #default="{ row }">
          <el-tag :type="voucherTypeTag(row.voucherType)" size="small">
            {{ voucherTypeLabel(row.voucherType) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="凭证号" width="160" align="center">
        <template #default="{ row }">
          <el-link v-if="row.voucherId" type="primary" @click="handleViewVoucher(row)">
            {{ row.voucherWord || '--' }}-{{ row.voucherNumber || '--' }}
          </el-link>
          <span v-else class="text-muted">--</span>
        </template>
      </el-table-column>
      <el-table-column label="金额" width="120" align="right">
        <template #default="{ row }">
          {{ formatMoney(row.totalAmount) }}
        </template>
      </el-table-column>
      <el-table-column label="同步状态" width="100" align="center">
        <template #header>
          <el-tooltip content="待同步=新记录等待处理 | 已同步=已生成凭证 | 已变更=数据变更需重新处理" placement="top">
            <span>同步状态 <el-icon style="vertical-align: middle"><QuestionFilled /></el-icon></span>
          </el-tooltip>
        </template>
        <template #default="{ row }">
          <el-tooltip :content="row.syncStatus === 0 ? '新记录，等待生成凭证' : row.syncStatus === 1 ? '已生成凭证' : row.syncStatus === 2 ? '数据已变更，需重新生成凭证' : ''" placement="top">
            <el-tag :type="syncStatusTag(row.syncStatus)" size="small">
              {{ syncStatusLabel(row.syncStatus) }}
            </el-tag>
          </el-tooltip>
        </template>
      </el-table-column>
      <el-table-column label="同步时间" width="170" align="center">
        <template #default="{ row }">
          {{ row.syncTime || '--' }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="80" align="center" fixed="right">
        <template #default="{ row }">
          <el-popconfirm title="确定删除此记录吗？删除后关联的凭证也将被清除。" @confirm="handleDelete(row)">
            <template #reference>
              <el-link type="danger">删除</el-link>
            </template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页 -->
    <el-pagination
      v-model:current-page="pageNum"
      v-model:page-size="pageSize"
      :page-sizes="[10, 20, 50, 100]"
      :total="total"
      layout="total, sizes, prev, pager, next, jumper"
      @size-change="handleQuery"
      @current-change="handleQuery"
    />

    <!-- 批量生成凭证弹框 -->
    <el-dialog
      v-model="generateDialogVisible"
      :title="'批量生成' + generateTypeLabel"
      width="600px"
      :close-on-click-modal="false"
      @closed="handleDialogClosed"
    >
      <el-form :model="generateForm" label-width="100px">
        <el-form-item label="凭证类型">
          <el-radio-group v-model="generateForm.generateType">
            <el-radio value="payment">采购付款凭证</el-radio>
            <el-radio value="journal">手动记账凭证</el-radio>
            <el-radio value="inventory">采购入库凭证</el-radio>
            <el-radio value="invoice">发票凭证</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="日期区间">
          <el-date-picker
            v-model="generateForm.dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="说明">
          <div class="form-tip">
            {{ generateTypeTip }}
          </div>
        </el-form-item>
      </el-form>

      <!-- 生成结果 -->
      <div v-if="generateResult" class="generate-result">
        <el-divider />
        <div class="result-summary">
          <el-tag type="success" size="large">处理 {{ generateResult.totalDays }} 天</el-tag>
          <el-tag type="primary" size="large">共 {{ generateResult.totalOrders }} 个订单</el-tag>
          <el-tag type="warning" size="large">生成 {{ generateResult.totalVouchers }} 个凭证</el-tag>
          <el-tag v-if="generateResult.errorDays > 0" type="danger" size="large">{{ generateResult.errorDays }} 天失败</el-tag>
        </div>
        <el-table :data="generateResult.dailyResults" size="small" max-height="300" style="margin-top: 12px">
          <el-table-column prop="date" label="日期" width="120" />
          <el-table-column prop="orderCount" label="订单数" width="80" />
          <el-table-column prop="voucherCount" label="凭证数" width="80" />
          <el-table-column label="新增/更新" width="120">
            <template #default="{ row }">
              <span v-if="row.newCount !== undefined">新增{{ row.newCount }} / 更新{{ row.updateCount }}</span>
              <span v-else>--</span>
            </template>
          </el-table-column>
          <el-table-column label="结果" min-width="200" show-overflow-tooltip>
            <template #default="{ row }">
              <span :style="{ color: row.error || row.orderCount === 0 && row.voucherCount === 0 ? '#f56c6c' : '' }">{{ row.error || row.message }}</span>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <template #footer>
        <el-button @click="generateDialogVisible = false" :disabled="generating">关闭</el-button>
        <el-button type="primary" @click="handleGenerate" :loading="generating" :disabled="!generateForm.dateRange">
          {{ generating ? '生成中...' : '开始生成' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { listMappingVouchersSource, generateVoucherByDateRange, generateJournalVoucherByDateRange, generateInventoryVoucherByDateRange, generateInvoiceVoucherByDateRange, getPaymentRecords, deleteMappingVouchersSource, deleteMappingVouchersSourceByIds } from '@/api/finance/mappingVouchersSource'
import finStore from '@/hooks/store/useFinanceStore.js'
import { ElMessage } from 'element-plus'
import { QuestionFilled } from '@element-plus/icons-vue'

const router = useRouter()

const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(20)

const queryParams = reactive({
  voucherType: '',
  syncStatus: ''
})

// 批量生成凭证相关
const generateDialogVisible = ref(false)
const generating = ref(false)
const generateResult = ref(null)
const generateForm = reactive({
  generateType: 'payment',
  dateRange: []
})

// 凭证类型标签和提示
const generateTypeLabel = computed(() => {
  const map = { payment: '采购付款凭证', journal: '手动记账凭证', inventory: '采购入库凭证', invoice: '发票凭证' }
  return map[generateForm.generateType] || '凭证'
})
const generateTypeTip = computed(() => {
  const map = {
    payment: '系统将为区间内每一天单独处理：从ERP获取当天完成付款的订单，生成采购付款凭证。已生成过的凭证会自动更新。',
    journal: '系统将为区间内每一天单独处理：从ERP台账获取当天的手动记账记录（支出/收入），生成对应凭证。已取消的记录会同步撤销凭证。',
    inventory: '系统将为区间内每一天单独处理：从ERP获取当天完成入库的订单，生成采购入库凭证。已生成过的凭证会自动更新。',
    invoice: '系统将为区间内每一天单独处理：获取当天的发票数据，生成发票凭证。已生成过的凭证会自动更新。'
  }
  return map[generateForm.generateType] || ''
})

// 凭证类型标签
const voucherTypeMap = {
  'payment': '付款凭证',
  'inventory_transit': '在途库存',
  'inventory_inbound': '入库库存',
  'invoice': '采购发票',
  'invoice_carrier': '承运商发票'
}
const voucherTypeTagMap = {
  'payment': 'primary',
  'inventory_transit': 'warning',
  'inventory_inbound': 'success',
  'invoice': '',
  'invoice_carrier': 'danger'
}
const voucherTypeLabel = (type) => voucherTypeMap[type] || type
const voucherTypeTag = (type) => voucherTypeTagMap[type] || 'info'

// 同步状态标签
const syncStatusMap = {
  0: '待同步',
  1: '已同步',
  2: '已变更'
}
const syncStatusTagMap = {
  0: 'info',
  1: 'success',
  2: 'warning'
}
const syncStatusLabel = (status) => syncStatusMap[status] || '未知'
const syncStatusTag = (status) => syncStatusTagMap[status] || 'info'

const formatMoney = (val) => {
  if (val === null || val === undefined) return '--'
  return '¥' + Number(val).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

const handleQuery = async () => {
  loading.value = true
  try {
    const groupid = await finStore.getCurrentTenantId()
    const res = await listMappingVouchersSource({
      groupid,
      voucherType: queryParams.voucherType || undefined,
      syncStatus: queryParams.syncStatus !== '' ? queryParams.syncStatus : undefined,
      pageNum: pageNum.value,
      pageSize: pageSize.value
    })
    tableData.value = res.rows || []
    total.value = res.total || 0
  } finally {
    loading.value = false
  }
}

const resetQuery = () => {
  queryParams.voucherType = ''
  queryParams.syncStatus = ''
  pageNum.value = 1
  handleQuery()
}

const handleExpand = async (row, expandedRows) => {
  // 仅付款凭证类型需要加载付款明细
  if (row.voucherType !== 'payment') return
  // 判断当前行是否展开
  const isExpanded = expandedRows.some(r => r.id === row.id)
  if (!isExpanded) return
  // 避免重复加载
  if (row._paymentRecords !== undefined) return
  // 标记为加载中
  row._paymentRecords = null
  try {
    const groupid = await finStore.getCurrentTenantId()
    const res = await getPaymentRecords({ groupid, orderId: row.orderId })
    row._paymentRecords = res.data || []
  } catch {
    row._paymentRecords = []
  }
}

// 获取订单编号显示文本
const getOrderNumberLabel = (row) => {
  if (row.voucherType === 'invoice' || row.voucherType === 'invoice_carrier') {
    return '发票号码'
  }
  return '订单编号'
}

// 查看凭证详情
const handleViewVoucher = (row) => {
  router.push({
    path: '/fin/voucher/create',
    query: {
      title: '凭证录入',
      path: '/fin/voucher/create',
      voucherId: row.voucherId
    }
  })
}

// 打开批量生成弹框
const openGenerateDialog = () => {
  generateResult.value = null
  generateForm.generateType = 'payment'
  generateForm.dateRange = []
  generateDialogVisible.value = true
}

// 关闭弹框时的处理
const handleDialogClosed = () => {
  generateResult.value = null
  generateForm.generateType = 'payment'
  generateForm.dateRange = []
}

// 根据类型获取对应的API函数
const getGenerateApi = (type) => {
  const apiMap = {
    payment: generateVoucherByDateRange,
    journal: generateJournalVoucherByDateRange,
    inventory: generateInventoryVoucherByDateRange,
    invoice: generateInvoiceVoucherByDateRange
  }
  return apiMap[type] || generateVoucherByDateRange
}

// 执行批量生成
const handleGenerate = async () => {
  if (!generateForm.dateRange || generateForm.dateRange.length !== 2) {
    ElMessage.warning('请选择日期区间')
    return
  }

  generating.value = true
  generateResult.value = null

  try {
    const groupid = await finStore.getCurrentTenantId()
    const apiFn = getGenerateApi(generateForm.generateType)
    const res = await apiFn({
      groupid,
      startDate: generateForm.dateRange[0],
      endDate: generateForm.dateRange[1]
    })
    generateResult.value = res.data
    ElMessage.success('批量生成' + generateTypeLabel.value + '完成')
    // 刷新列表
    handleQuery()
  } catch (error) {
    ElMessage.error('批量生成凭证失败：' + (error.message || '未知错误'))
  } finally {
    generating.value = false
  }
}

// 删除单条记录
const handleDelete = async (row) => {
  try {
    await deleteMappingVouchersSource(row.id)
    ElMessage.success('删除成功')
    handleQuery()
  } catch (error) {
    ElMessage.error('删除失败：' + (error.message || '未知错误'))
  }
}

// 清空当前页所有记录
const handleClearCurrentPage = async () => {
  if (tableData.value.length === 0) return
  const ids = tableData.value.map(row => row.id)
  try {
    await deleteMappingVouchersSourceByIds(ids)
    ElMessage.success('成功清空 ' + ids.length + ' 条记录')
    handleQuery()
  } catch (error) {
    ElMessage.error('清空失败：' + (error.message || '未知错误'))
  }
}

onMounted(() => {
  handleQuery()
})
</script>

<style scoped>
.voucher-log-container {
  padding: 0;
}

.search-form {
  padding: 10px 0;
}

.expand-content {
  padding: 12px 20px;
  background: #fafafa;
}

.text-muted {
  color: #999;
}

:deep(.el-table .el-table__expanded-cell) {
  padding: 0 !important;
}

.form-tip {
  color: #909399;
  font-size: 13px;
  line-height: 1.6;
}

.generate-result {
  margin-top: 8px;
}

.result-summary {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  margin-bottom: 8px;
}

.payment-section {
  margin-top: 16px;
}

.section-title {
  margin: 0 0 10px 0;
  font-size: 14px;
  font-weight: 600;
  color: #303133;
}

.payment-empty {
  padding: 20px;
  text-align: center;
  color: #999;
  font-size: 13px;
}

.payment-table {
  margin-top: 8px;
}
</style>