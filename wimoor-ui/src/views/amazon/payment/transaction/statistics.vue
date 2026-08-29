<template>
  <div class="flex-between">
    <el-space style="margin-bottom: 16px;">
      <Group @change="groupChange" defaultValue="only" />
      <Datepicker ref="datepickers" :shortIndex="1" @changedate="changedate" />
      <el-button type="primary" @click="handleSearch" :loading="loading" size="default">查询</el-button>
      <el-button @click="resetForm" size="default">重置</el-button>
    </el-space>
  </div>

  <!-- 费用类型饼图 -->
  <el-row :gutter="16" style="margin-bottom: 16px;">
    <el-col :span="12">
      <el-card shadow="never">
        <template #header>费用类型分布（按 total）</template>
        <div ref="pieChartRef" style="height: 350px;"></div>
      </el-card>
    </el-col>
    <el-col :span="12">
      <el-card shadow="never">
        <template #header>各费用项对比</template>
        <div ref="barChartRef" style="height: 350px;"></div>
      </el-card>
    </el-col>
  </el-row>

  <!-- 按交易类型汇总表格 -->
  <el-card shadow="never" style="margin-bottom: 16px;">
    <template #header>按交易类型汇总</template>
    <el-table :data="summaryData" border stripe style="width: 100%" show-summary :summary-method="getSummaryRow">
      <el-table-column prop="transaction_type" label="交易类型" width="150" fixed />
      <el-table-column prop="record_count" label="记录数" width="90" align="right" />
      <el-table-column prop="product_sales" label="产品销售" align="right">
        <template #default="scope">{{ outputmoney(scope.row.product_sales) }}</template>
      </el-table-column>
      <el-table-column prop="product_sales_tax" label="销售税" align="right">
        <template #default="scope">{{ outputmoney(scope.row.product_sales_tax) }}</template>
      </el-table-column>
      <el-table-column prop="shipping_credits" label="运费抵扣" align="right">
        <template #default="scope">{{ outputmoney(scope.row.shipping_credits) }}</template>
      </el-table-column>
      <el-table-column prop="giftwrap_credits" label="礼品包装" align="right">
        <template #default="scope">{{ outputmoney(scope.row.giftwrap_credits) }}</template>
      </el-table-column>
      <el-table-column prop="promotional_rebates" label="促销返利" align="right">
        <template #default="scope">{{ outputmoney(scope.row.promotional_rebates) }}</template>
      </el-table-column>
      <el-table-column prop="selling_fees" label="销售费用" align="right">
        <template #default="scope">{{ outputmoney(scope.row.selling_fees) }}</template>
      </el-table-column>
      <el-table-column prop="fba_fees" label="FBA费用" align="right">
        <template #default="scope">{{ outputmoney(scope.row.fba_fees) }}</template>
      </el-table-column>
      <el-table-column prop="other_transaction_fees" label="其他交易费" align="right">
        <template #default="scope">{{ outputmoney(scope.row.other_transaction_fees) }}</template>
      </el-table-column>
      <el-table-column prop="total" label="合计" align="right">
        <template #default="scope">
          <span :class="scope.row.total >= 0 ? 'positive' : 'negative'">{{ outputmoney(scope.row.total) }}</span>
        </template>
      </el-table-column>
    </el-table>
  </el-card>

  <!-- 按日期趋势图 -->
  <el-card shadow="never">
    <template #header>
      <el-row justify="space-between" align="middle">
        <span>按日期费用趋势</span>
        <el-select v-model="selectedFeeField" style="width: 200px;" @change="renderDailyChart">
          <el-option label="产品销售" value="product_sales" />
          <el-option label="运费抵扣" value="shipping_credits" />
          <el-option label="促销返利" value="promotional_rebates" />
          <el-option label="销售费用" value="selling_fees" />
          <el-option label="FBA费用" value="fba_fees" />
          <el-option label="其他交易费" value="other_transaction_fees" />
          <el-option label="合计" value="total" />
        </el-select>
      </el-row>
    </template>
    <div ref="dailyChartRef" style="height: 350px;"></div>
  </el-card>
</template>

<script setup>
import { ref, reactive, onMounted, onBeforeUnmount, nextTick } from 'vue'
import Group from "@/components/header/group.vue";
import Datepicker from "@/components/header/datepicker.vue";
import transactionReportApi from '@/api/amazon/finances/transactionReportApi.js'
import { outputmoney } from '@/utils/index.js'
import * as echarts from 'echarts'

const loading = ref(false)
const queryParams = reactive({ groupid: '', marketplaceid: '', startDate: '', endDate: '' })
const summaryData = ref([])
const dailyData = ref([])
const selectedFeeField = ref('product_sales')

const pieChartRef = ref(null)
const barChartRef = ref(null)
const dailyChartRef = ref(null)
let pieChart = null
let barChart = null
let dailyChart = null

// 费用字段映射
const feeFieldMap = {
  product_sales: '产品销售',
  product_sales_tax: '销售税',
  shipping_credits: '运费抵扣',
  shipping_credits_tax: '运费税',
  giftwrap_credits: '礼品包装',
  giftwrap_credits_tax: '礼品税',
  promotional_rebates: '促销返利',
  promotional_rebates_tax: '促销税',
  marketplace_withheld_tax: '代扣税',
  selling_fees: '销售费用',
  fba_fees: 'FBA费用',
  other_transaction_fees: '其他交易费',
  other: '其他',
  total: '合计'
}

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
  Promise.all([
    transactionReportApi.getFeeSummaryByType(queryParams),
    transactionReportApi.getDailyFeeByType(queryParams)
  ]).then(([summaryRes, dailyRes]) => {
    summaryData.value = summaryRes.data || []
    dailyData.value = dailyRes.data || []
    nextTick(() => {
      renderPieChart()
      renderBarChart()
      renderDailyChart()
    })
  }).finally(() => {
    loading.value = false
  })
}

function resetForm() {
  queryParams.startDate = ''
  queryParams.endDate = ''
}

// 饼图：按交易类型的 total 分布
function renderPieChart() {
  if (!pieChartRef.value) return
  if (!pieChart) pieChart = echarts.init(pieChartRef.value)
  const data = summaryData.value
    .filter(item => parseFloat(item.total) !== 0)
    .map(item => ({ name: item.transaction_type, value: Math.abs(parseFloat(item.total)) }))
  pieChart.setOption({
    tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
    legend: { orient: 'vertical', left: 'left', top: 'middle' },
    series: [{
      type: 'pie', radius: ['40%', '70%'], center: ['60%', '50%'],
      label: { formatter: '{b}\n{d}%' },
      data
    }]
  })
}

// 柱状图：各费用项对比
function renderBarChart() {
  if (!barChartRef.value) return
  if (!barChart) barChart = echarts.init(barChartRef.value)
  const feeKeys = ['product_sales', 'shipping_credits', 'promotional_rebates', 'selling_fees', 'fba_fees', 'other_transaction_fees', 'total']
  const types = summaryData.value.map(item => item.transaction_type)
  const series = feeKeys.map(key => ({
    name: feeFieldMap[key],
    type: 'bar',
    data: summaryData.value.map(item => parseFloat(item[key] || 0))
  }))
  barChart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { top: 0 },
    grid: { left: 60, right: 20, top: 40, bottom: 60 },
    xAxis: { type: 'category', data: types, axisLabel: { rotate: 30 } },
    yAxis: { type: 'value' },
    series
  })
}

// 按日期趋势图
function renderDailyChart() {
  if (!dailyChartRef.value || dailyData.value.length === 0) return
  if (!dailyChart) dailyChart = echarts.init(dailyChartRef.value)
  const dateSet = [...new Set(dailyData.value.map(item => item.date_key))].sort()
  const typeSet = [...new Set(dailyData.value.map(item => item.transaction_type))]
  const series = typeSet.map(type => ({
    name: type,
    type: 'line', smooth: true,
    data: dateSet.map(date => {
      const found = dailyData.value.find(item => item.date_key === date && item.transaction_type === type)
      return found ? parseFloat(found[selectedFeeField.value] || 0) : 0
    })
  }))
  dailyChart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { top: 0 },
    grid: { left: 60, right: 20, top: 40, bottom: 30 },
    xAxis: { type: 'category', data: dateSet },
    yAxis: { type: 'value' },
    series
  }, true)
}

function getSummaryRow({ columns, data }) {
  const sums = []
  columns.forEach((col, index) => {
    if (index === 0) { sums[index] = '合计'; return }
    if (index === 1) { sums[index] = ''; return }
    const prop = col.property
    if (prop) {
      const total = data.reduce((sum, item) => sum + parseFloat(item[prop] || 0), 0)
      sums[index] = outputmoney(total)
    }
  })
  return sums
}

function handleResize() {
  pieChart?.resize()
  barChart?.resize()
  dailyChart?.resize()
}

onMounted(() => {
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  pieChart?.dispose()
  barChart?.dispose()
  dailyChart?.dispose()
})
</script>

<style scoped>
.positive { color: #006600; }
.negative { color: #cc0000; }
</style>
