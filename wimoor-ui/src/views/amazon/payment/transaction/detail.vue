<template>
  <div class="flex-between">
    <el-space style="margin-bottom: 16px;">
      <Group @change="groupChange" defaultValue="only" />
      <Datepicker ref="datepickers" :shortIndex="1" @changedate="changedate" />
      <el-select v-model="queryParams.searchType" style="width: 110px;" @change="handleSearch">
        <el-option label="订单号" value="orderId" />
        <el-option label="SKU" value="sku" />
      </el-select>
      <el-input v-model="queryParams.searchValue" placeholder="请输入" clearable style="width: 200px;"
        @clear="handleSearch" @keyup.enter="handleSearch">
        <template #append>
          <el-button @click="handleSearch">
            <el-icon><Search /></el-icon>
          </el-button>
        </template>
      </el-input>
      <el-button @click="resetForm">重置</el-button>
    </el-space>
    <el-space>
      <UploadRpt type="GET_DATE_RANGE_FINANCIAL_TRANSACTION_DATA" ref="uploadRptRef" @upload="handleUploadDone" />
      <el-button type="success" @click="handleExport" :loading="exporting">导出</el-button>
    </el-space>
  </div>

  <el-table :data="tableData" border stripe style="width: 100%"
    v-loading="loading" height="calc(100vh - 260px)" :default-sort="{ prop: 'date_time', order: 'descending' }">
    <el-table-column prop="date_time" label="交易日期" width="160" sortable fixed>
      <template #default="scope">{{ formatDate(scope.row.date_time) }}</template>
    </el-table-column>
    <el-table-column prop="transaction_type" label="交易类型" width="110" />
    <el-table-column prop="description" label="交易描述" width="200" show-overflow-tooltip />
    <el-table-column prop="order_id" label="订单号" width="140" show-overflow-tooltip />
    <el-table-column prop="sku" label="SKU" width="130" show-overflow-tooltip />
    <el-table-column prop="quantity" label="数量" width="70" align="right" />
    <el-table-column prop="fulfillment" label="履约" width="70" />
    <el-table-column prop="product_sales" label="产品销售" align="right" width="100">
      <template #default="scope">{{ outputmoney(scope.row.product_sales) }}</template>
    </el-table-column>
    <el-table-column prop="product_sales_tax" label="销售税" align="right" width="90">
      <template #default="scope">{{ outputmoney(scope.row.product_sales_tax) }}</template>
    </el-table-column>
    <el-table-column prop="shipping_credits" label="运费抵扣" align="right" width="100">
      <template #default="scope">{{ outputmoney(scope.row.shipping_credits) }}</template>
    </el-table-column>
    <el-table-column prop="giftwrap_credits" label="礼品包装" align="right" width="100">
      <template #default="scope">{{ outputmoney(scope.row.giftwrap_credits) }}</template>
    </el-table-column>
    <el-table-column prop="promotional_rebates" label="促销返利" align="right" width="100">
      <template #default="scope">{{ outputmoney(scope.row.promotional_rebates) }}</template>
    </el-table-column>
    <el-table-column prop="selling_fees" label="销售费用" align="right" width="100">
      <template #default="scope">{{ outputmoney(scope.row.selling_fees) }}</template>
    </el-table-column>
    <el-table-column prop="fba_fees" label="FBA费用" align="right" width="100">
      <template #default="scope">{{ outputmoney(scope.row.fba_fees) }}</template>
    </el-table-column>
    <el-table-column prop="other_transaction_fees" label="其他交易费" align="right" width="110">
      <template #default="scope">{{ outputmoney(scope.row.other_transaction_fees) }}</template>
    </el-table-column>
    <el-table-column prop="marketplace_withheld_tax" label="代扣税" align="right" width="90">
      <template #default="scope">{{ outputmoney(scope.row.marketplace_withheld_tax) }}</template>
    </el-table-column>
    <el-table-column prop="other" label="其他" align="right" width="80">
      <template #default="scope">{{ outputmoney(scope.row.other) }}</template>
    </el-table-column>
    <el-table-column prop="total" label="合计" align="right" width="100" fixed="right">
      <template #default="scope">
        <span :class="scope.row.total >= 0 ? 'positive' : 'negative'">{{ outputmoney(scope.row.total) }}</span>
      </template>
    </el-table-column>
    <el-table-column prop="settlement_id" label="结算ID" width="120" show-overflow-tooltip fixed="right" />
  </el-table>

  <div style="margin-top: 12px; display: flex; justify-content: flex-end;">
    <el-pagination
      v-model:current-page="pagination.page"
      v-model:page-size="pagination.size"
      :page-sizes="[50, 100, 200, 500]"
      layout="total, sizes, prev, pager, next, jumper"
      :total="pagination.total"
      @size-change="handleSearch"
      @current-change="handleSearch"
    />
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import Group from "@/components/header/group.vue";
import Datepicker from "@/components/header/datepicker.vue";
import { Search } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import UploadRpt from '@/components/Upload/uploadRpt.vue'
import transactionReportApi from '@/api/amazon/finances/transactionReportApi.js'
import { outputmoney } from '@/utils/index.js'

const loading = ref(false)
const exporting = ref(false)
const uploadRptRef = ref(null)
const tableData = ref([])
const queryParams = reactive({
  groupid: '',
  marketplaceid: '',
  startDate: '',
  endDate: '',
  searchType: 'orderId',
  searchValue: ''
})
const pagination = reactive({ page: 1, size: 50, total: 0 })

function groupChange(obj) {
  queryParams.groupid = obj.groupid
  queryParams.marketplaceid = obj.marketplaceid
  handleSearch()
}

function changedate(dates) {
  queryParams.startDate = dates.start
  queryParams.endDate = dates.end
}

function handleSearch() {
  if (!queryParams.groupid) return
  loading.value = true
  transactionReportApi.getDetailPage({
    ...queryParams,
    page: pagination.page,
    size: pagination.size
  }).then(res => {
    tableData.value = res.data.list || []
    pagination.total = res.data.total || 0
  }).finally(() => {
    loading.value = false
  })
}

function resetForm() {
  queryParams.searchType = 'orderId'
  queryParams.searchValue = ''
  pagination.page = 1
  handleSearch()
}

function handleExport() {
  if (!queryParams.groupid) {
    ElMessage.warning('请先选择店铺')
    return
  }
  exporting.value = true
  transactionReportApi.exportDetail({
    groupid: queryParams.groupid,
    marketplaceid: queryParams.marketplaceid,
    startDate: queryParams.startDate,
    endDate: queryParams.endDate,
    searchType: queryParams.searchType,
    searchValue: queryParams.searchValue
  }).then(res => {
    const blob = new Blob([res], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `交易报告明细_${queryParams.startDate}_${queryParams.endDate}.xlsx`
    link.click()
    window.URL.revokeObjectURL(url)
  }).catch(() => {
    ElMessage.error('导出失败')
  }).finally(() => {
    exporting.value = false
  })
}

function handleUploadDone() {
  handleSearch()
}

function formatDate(dateStr) {
  if (!dateStr) return ''
  return dateStr.replace('T', ' ').replace(/\.\d+/, '')
}
</script>

<style scoped>
.positive { color: #006600; }
.negative { color: #cc0000; }
</style>
