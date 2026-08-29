<template>
  <el-drawer :title="carrierName + ' - 明细'" v-model="visible" size="80%">
    <el-tabs v-model="activeTab" @tab-change="handleTabChange">
      <!-- 发货明细页签 -->
      <el-tab-pane label="发货明细" name="shipment">
        <!-- 发货明细表格 -->
        <el-table
          v-loading="shipmentLoading"
          :data="shipmentData"
          style="width: 100%"
          stripe
          show-summary
          :summary-method="getSummaries"
          max-height="calc(100vh - 200px)"
        >
          <el-table-column label="货件编码" prop="shipmentid" min-width="150" sortable="custom">
            <template #default="{ row }">
              <div>{{ row.shipmentid || '-' }}</div>
            </template>
          </el-table-column>
          <el-table-column label="物流渠道" prop="channame" min-width="150">
            <template #default="{ row }">
              <div>{{ row.channame || '-' }}</div>
              <div class="font-extraSmall">
                <span>{{ row.subarea }}</span>-<span>{{ row.channelname }}</span>-<span>{{ row.transtype }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="计划发货" prop="totalqty" width="100" align="right" sortable="custom" />
          <el-table-column label="实际发货" prop="totalout" width="100" align="right" sortable="custom" />
          <el-table-column label="实际接收" prop="totalrec" width="100" align="right" sortable="custom" />
          <el-table-column label="接收差值" prop="lessrec" width="100" align="right" sortable="custom">
            <template #default="{ row }">
              <span :class="{ 'text-red': row.lessrec > 0 }">{{ row.lessrec || 0 }}</span>
            </template>
          </el-table-column>
          <el-table-column label="待发货" prop="needout" width="90" align="right" sortable="custom" />
          <el-table-column label="待接收" prop="needrec" width="90" align="right" sortable="custom" />
          <el-table-column label="发货货值" prop="worth" width="120" align="right" sortable="custom">
            <template #default="{ row }">
              <span class="amount">{{ formatNumber(row.worth) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="预估重量(KG)" prop="readweight" width="130" align="right" sortable="custom" />
          <el-table-column label="运输重量(KG)" prop="transweight_kg" width="130" align="right" sortable="custom" />
          <el-table-column label="运输费用" prop="shipfee" width="110" align="right" sortable="custom">
            <template #default="{ row }">
              <span class="amount paid">{{ formatNumber(row.shipfee) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="关税/其他费用" prop="totalotherfee" width="130" align="right" sortable="custom">
            <template #default="{ row }">
              <span class="amount">{{ formatNumber(row.totalotherfee) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="货件箱数" prop="totalbox" width="100" align="right" sortable="custom" />
          <el-table-column label="平均时效(天)" prop="avgtime" width="120" align="right" sortable="custom" />
        </el-table>

        <!-- 分页 -->
        <el-pagination
          v-if="shipmentTotal > 0"
          class="mt16"
          :current-page="queryParams.currentpage"
          :page-size="queryParams.pagesize"
          :total="shipmentTotal"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </el-tab-pane>

      <!-- 发票记录页签 -->
      <el-tab-pane label="发票记录" name="invoice">
        <el-table
          v-loading="invoiceLoading"
          :data="invoiceData"
          style="width: 100%"
          stripe
        >
          <el-table-column label="发票号码" min-width="180" show-overflow-tooltip>
            <template #default="{ row }">
              <span v-if="row.digitalInvoiceNo">{{ row.digitalInvoiceNo }}</span>
              <span v-else>{{ row.invoiceCode ? row.invoiceCode + ' ' : '' }}{{ row.invoiceNo }}</span>
            </template>
          </el-table-column>
          <el-table-column label="开票日期" prop="invoiceDate" width="110">
            <template #default="{ row }">
              {{ row.invoiceDate || '-' }}
            </template>
          </el-table-column>
          <el-table-column label="供应商名称" prop="sellerName" min-width="180" show-overflow-tooltip />
          <el-table-column label="价税合计" prop="amountWithTax" width="120" align="right">
            <template #default="{ row }">
              <span class="amount paid">{{ formatNumber(row.amountWithTax) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="入账状态" prop="postingStatus" width="100" align="center">
            <template #default="{ row }">
              <el-tag :type="row.postingStatus === 1 ? 'success' : 'info'" size="small">
                {{ row.postingStatus === 1 ? '已入账' : '未入账' }}
              </el-tag>
            </template>
          </el-table-column>
        </el-table>

        <!-- 发票汇总 -->
        <el-descriptions :column="3" border size="small" class="mt16" v-if="invoiceData.length > 0">
          <el-descriptions-item label="发票总数">{{ invoiceData.length }} 张</el-descriptions-item>
          <el-descriptions-item label="价税合计">{{ formatNumber(invoiceTotalAmount) }}</el-descriptions-item>
          <el-descriptions-item label="已入账金额">{{ formatNumber(invoiceInvoicedAmount) }}</el-descriptions-item>
        </el-descriptions>
      </el-tab-pane>
    </el-tabs>
  </el-drawer>
</template>

<script setup name="CarrierDetailDrawer">
import { ref, reactive, computed } from 'vue'
import reportApi from '@/api/amazon/inbound/reportV2Api.js'
import { getInvoiceList } from '@/api/finance/invoiceLedger'
import finStore from "@/hooks/store/useFinanceStore.js"

const visible = ref(false)
const activeTab = ref('shipment')
const carrierName = ref('')

// 发货明细相关
const shipmentLoading = ref(false)
const shipmentData = ref([])
const shipmentTotal = ref(0)
const summaryData = ref({})

// 发票记录相关
const invoiceLoading = ref(false)
const invoiceData = ref([])

// 发票汇总
const invoiceTotalAmount = computed(() => {
  return invoiceData.value.reduce((sum, item) => sum + (Number(item.amountWithTax) || 0), 0)
})
const invoiceInvoicedAmount = computed(() => {
  return invoiceData.value
    .filter(item => item.postingStatus === 1)
    .reduce((sum, item) => sum + (Number(item.amountWithTax) || 0), 0)
})

// 保存外部传入的筛选条件
const parentParams = reactive({
  rowCompanyid: '',
  fromDate: '',
  toDate: '',
  reconcileMonth: ''
})

const queryParams = reactive({
  currentpage: 1,
  pagesize: 20
})

// 打开抽屉
function open(row, searchForm) {
  visible.value = true
  carrierName.value = row.logitics || '-'
  queryParams.currentpage = 1
  activeTab.value = 'shipment'

  // 保存父页面的筛选条件
  parentParams.rowCompanyid = row.companyid || ''
  parentParams.fromDate = searchForm.fromDate || ''
  parentParams.toDate = searchForm.toDate || ''
  parentParams.reconcileMonth = searchForm.reconcileMonth || ''

  loadShipmentData()
}

// 切换页签
function handleTabChange(tab) {
  if (tab === 'shipment' && shipmentData.value.length === 0) {
    loadShipmentData()
  } else if (tab === 'invoice' && invoiceData.value.length === 0) {
    loadInvoiceData()
  }
}

// 加载发货明细数据
function loadShipmentData() {
  shipmentLoading.value = true
  const params = {
    companyid: parentParams.rowCompanyid,
    fromDate: parentParams.fromDate,
    toDate: parentParams.toDate,
    datetype: 'deliverydate',
    type: 'logitics',
    groupby: ['shipmentid'],
    currentpage: queryParams.currentpage,
    pagesize: queryParams.pagesize
  }
  reportApi.getShipmentReportByLoistics(params).then((res) => {
    shipmentData.value = res.data.records || []
    shipmentTotal.value = res.data.total || 0
    if (queryParams.currentpage === 1 && res.data.total > 0 && res.data.records[0]?.summary) {
      summaryData.value = res.data.records[0].summary
    } else if (queryParams.currentpage === 1) {
      summaryData.value = {}
    }
  }).catch((error) => {
    console.error('获取发货明细失败:', error)
  }).finally(() => {
    shipmentLoading.value = false
  })
}

// 加载发票记录数据
async function loadInvoiceData() {
  invoiceLoading.value = true
  try {
    const groupid = await finStore.getCurrentTenantId()
    const res = await getInvoiceList({
      groupid,
      pageNum: 1,
      pageSize: 9999
    })
    const allInvoices = res?.rows || []
    // 筛选当前承运商的发票，且开票日期在对账月份内
    invoiceData.value = allInvoices.filter(invoice => {
      if (!invoice.carrierId || invoice.sellerName !== carrierName.value) return false
      // 如果有对账月份筛选，按开票月份过滤
      if (parentParams.reconcileMonth && invoice.invoiceDate) {
        return invoice.invoiceDate.substring(0, 7) === parentParams.reconcileMonth
      }
      return true
    })
  } catch (error) {
    console.error('获取发票记录失败:', error)
  } finally {
    invoiceLoading.value = false
  }
}

// 分页
function handleSizeChange(size) {
  queryParams.pagesize = size
  queryParams.currentpage = 1
  loadShipmentData()
}

function handleCurrentChange(page) {
  queryParams.currentpage = page
  loadShipmentData()
}

// 格式化数字
function formatNumber(num) {
  if (!num || num === 0) return '0.00'
  return Number(num).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

// 合计行（使用后端返回的全量汇总）
function getSummaries({ columns, data }) {
  const arr = ['合计']
  const sumFields = [
    'totalqty', 'totalout', 'totalrec', 'lessrec', 'needout', 'needrec',
    'worth', 'readweight', 'transweight_kg', 'shipfee', 'totalotherfee', 'totalbox'
  ]
  const fmtFields = ['worth', 'readweight', 'transweight_kg', 'shipfee', 'totalotherfee']
  columns.forEach((item, index) => {
    if (index < 2) return
    const prop = item.property
    if (prop && sumFields.includes(prop) && summaryData.value[prop] !== undefined) {
      arr[index] = fmtFields.includes(prop) ? formatNumber(summaryData.value[prop]) : summaryData.value[prop]
    }
    if (prop === 'avgtime' && summaryData.value.avgtime !== undefined) {
      arr[index] = Number(summaryData.value.avgtime).toFixed(1)
    }
  })
  return arr
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

.text-red {
  color: #f56c6c;
}

.mb8 {
  margin-bottom: 8px;
}

.mt16 {
  margin-top: 16px;
}
</style>
