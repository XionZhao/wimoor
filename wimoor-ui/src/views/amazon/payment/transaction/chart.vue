
<template>
  <div>
    <div class="flex-between">
      <el-space style="margin-bottom: 20px;">
        <Group @change="groupChange" />
        <div>
          <Datepicker ref="datepickers" :shortIndex="4" @changedate="changedate" />
        </div>
        <el-button type="primary" @click="handleSearch" :loading="searchLoading" size="default">查询</el-button>
        <el-button @click="resetForm" size="default">重置</el-button>
      </el-space>
    </div>

    <!-- 粒度切换 -->
    <el-space :size="24" style="margin-bottom: 16px;">
      <el-radio-group v-model="groupType" size="small" @change="handleSearch">
        <el-radio-button label="day">按日</el-radio-button>
        <el-radio-button label="week">按周</el-radio-button>
        <el-radio-button label="month">按月</el-radio-button>
      </el-radio-group>
      <el-divider direction="vertical" />
      <div class="summary-item">
        <div class="summary-label">结算(settlement report)销量合计</div>
        <div class="summary-value" style="color: #FF6700;">{{ settlementTotal }}</div>
      </div>
      <div class="summary-item">
        <div class="summary-label">订单(order data general report)销量合计</div>
        <div class="summary-value" style="color: #409EFF;">{{ orderTotal }}</div>
      </div>
      <div class="summary-item" v-if="transactionOrderTotal > 0">
        <div class="summary-label">交易报告（payment report) 销量合计</div>
        <div class="summary-value" style="color: #67C23A;">{{ transactionOrderTotal }}</div>
      </div>
    </el-space>

    <!-- 销量图表 -->
    <el-card shadow="hover" style="margin-bottom: 16px;">
      <template #header>
        <div class="card-header-title">{{ { day: '每日', week: '每周', month: '每月' }[groupType] }}销量趋势</div>
      </template>
      <div v-loading="chartLoading" element-loading-text="图表加载中..." ref="quantityChartRef" class="chart-container"></div>
    </el-card>

    <!-- 费用图表 -->
    <el-card shadow="hover" style="margin-bottom: 16px;">
      <template #header>
        <div class="flex-between">
          <div class="card-header-title">{{ { day: '每日', week: '每周', month: '每月' }[groupType] }}费用趋势</div>
          <el-space>
            <!-- 费用类型动态下拉 -->
            <el-select v-model="selectedFeeField" placeholder="选择费用项" size="small" style="width: 220px;" @change="onFeeFieldChange">
              <el-option
                v-for="item in incomeFields"
                :key="item.key"
                :label="item.label"
                :value="item.key"
              />
            </el-select>
            <!-- 币种切换 -->
            <el-radio-group v-model="currencyMode" size="small" @change="onCurrencyModeChange">
              <el-radio-button label="market">站点币种</el-radio-button>
              <el-radio-button label="rmb">RMB</el-radio-button>
            </el-radio-group>
          </el-space>
        </div>
      </template>
      <div v-loading="chartLoading" ref="feeChartRef" class="chart-container"></div>
    </el-card>

    <!-- 每日费用趋势表格 -->
    <el-card style="margin-top:10px;" v-if="dailyTableData.length > 0">
      <el-scrollbar style="width:100%;" always>
        <table class="sd-table">
          <tr>
            <td width="120px;">项目名称</td>
            <td width="100px;">汇总</td>
            <td v-for="label in dailyLabels" width="80px;">{{ label }}</td>
          </tr>
          <tr>
            <td>当天汇率</td>
            <td>-</td>
            <td v-for="rate in dailyRates">{{ rate }}</td>
          </tr>
          <tr>
            <td>{{ selectedFeeFieldLabel }}（站点币种）</td>
            <td>{{ selectedFieldSummary }}</td>
            <td v-for="val in selectedFieldDailyData">{{ val }}</td>
          </tr>
          <tr>
            <td>{{ selectedFeeFieldLabel }}（RMB）</td>
            <td>{{ selectedFieldSummaryRmb }}</td>
            <td v-for="val in selectedFieldDailyDataRmb">{{ val }}</td>
          </tr>
        </table>
      </el-scrollbar>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, toRefs, nextTick, onBeforeUnmount, computed } from 'vue'
import * as echarts from 'echarts'
import '@/assets/css/packbox_table.css';
import Group from "@/components/header/group.vue";
import { ElMessage } from 'element-plus'
import Datepicker from '@/components/header/datepicker.vue';
import settlementApi from '@/api/amazon/finances/settlementApi'
import groupApi from '@/api/amazon/group/groupApi'

const quantityChartRef = ref(null)
const feeChartRef = ref(null)
let quantityChart = null
let feeChart = null
const chartLoading = ref(false)
const settlementTotal = ref(0)
const orderTotal = ref(0)
const transactionOrderTotal = ref(0)
const amountTotal = ref('0.00')
const fbaTotal = ref('0.00')
const commissionTotal = ref('0.00')
const groupType = ref('day')
const feeType = ref('amount')
const currencyMode = ref('market')
const selectedFeeField = ref('FBA product sales')
const incomeFields = ref([])
const dailyTableData = ref([])
const dailyLabels = ref([])
const dailyRates = ref([])

// 计算当前选中费用项的中文标签
const selectedFeeFieldLabel = computed(() => {
  const found = incomeFields.value.find(f => f.key === selectedFeeField.value)
  return found ? found.label : selectedFeeField.value
})

// 缓存费用数据，切换时重新渲染
let cachedLabels = []
let cachedFeeData = { amount: [], fba: [], commission: [] }
let cachedDailyData = []
const selectedFieldSummary = ref('0.00')
const selectedFieldDailyData = ref([])
const selectedFieldSummaryRmb = ref('0.00')
const selectedFieldDailyDataRmb = ref([])

// 默认费用配置（用于图表的三个快速选项）
const feeConfig = {
  amount: { name: '结算金额', color: '#67C23A', rgba30: 'rgba(103,194,58,0.3)', rgba05: 'rgba(103,194,58,0.05)' },
  fba: { name: 'FBA费', color: '#F56C6C', rgba30: 'rgba(245,108,108,0.3)', rgba05: 'rgba(245,108,108,0.05)' },
  commission: { name: '佣金', color: '#C03639', rgba30: 'rgba(192,54,57,0.3)', rgba05: 'rgba(192,54,57,0.05)' }
}

const state = reactive({
  queryParams: {
    isload: false,
  },
  searchLoading: false,
});
const { queryParams, searchLoading } = toRefs(state);

function resetForm() {
  state.queryParams.accountId = ''
  state.queryParams.site = ''
  state.queryParams.currency = 'market'
  state.queryParams.datetype = 'settlement'
}

function handleSearch() {
  if (!state.queryParams.groupid) return;
  queryParams.value.isload = true;
  loadData();
}

function groupChange(obj) {
  state.queryParams.groupid = obj.groupid;
  state.queryParams.marketplaceid = obj.marketplaceid;
  state.queryParams.marketplace_name = obj.market.pointName;
  state.queryParams.marketcurrency = obj.market.currency;
  handleSearch();
}

function changedate(dates) {
  state.queryParams.fromDate = dates.start;
  state.queryParams.endDate = dates.end;
  if (state.queryParams.isload) {
    handleSearch();
  }
}

function loadData() {
  if (!queryParams.value.groupid) {
    ElMessage.warning('请先选择店铺');
    return;
  }
  chartLoading.value = true;
  const params = { ...queryParams.value, groupType: groupType.value };

  // 请求销量图表数据
  settlementApi.quantityByDay(params).then(res => {
    const { labels, datas, amountDatas, fbaDatas, commissionDatas, orderDatas } = res.data;
    settlementTotal.value = (datas || []).reduce((sum, v) => sum + (Number(v) || 0), 0);
    orderTotal.value = (orderDatas || []).reduce((sum, v) => sum + (Number(v) || 0), 0);
    amountTotal.value = (amountDatas || []).reduce((sum, v) => sum + (Number(v) || 0), 0).toFixed(2);
    fbaTotal.value = (fbaDatas || []).reduce((sum, v) => sum + (Number(v) || 0), 0).toFixed(2);
    commissionTotal.value = (commissionDatas || []).reduce((sum, v) => sum + (Number(v) || 0), 0).toFixed(2);
    cachedLabels = labels;
    cachedQtyLabels = labels;
    cachedQtyDatas = datas;
    cachedQtyOrderDatas = orderDatas;
    cachedFeeData = { amount: amountDatas || [], fba: fbaDatas || [], commission: commissionDatas || [] };
    // 不在此处渲染销量图表，等每日收入数据返回后一起渲染（保证三根线同时出现）
    if (cachedDailyData.length === 0) {
      renderFeeChart();
    }
  }).catch(() => {
    ElMessage.error('销量数据加载失败');
    chartLoading.value = false;
  });

  // 请求每日收入明细数据（费用图表 + 表格）- 数据来源t_amz_transaction_report
  settlementApi.dailyIncomeReportByTransaction(params).then(res => {
    const incomeData = res.data;
    if (incomeData && incomeData.incomeFields && incomeData.incomeFields.length > 0) {
      incomeFields.value = incomeData.incomeFields;
      // 如果当前选中项不在列表中，选第一项
      if (!incomeFields.value.find(f => f.key === selectedFeeField.value)) {
        selectedFeeField.value = incomeFields.value[0].key;
      }
    }
    if (incomeData && incomeData.dailyData) {
      cachedDailyData = incomeData.dailyData;
      dailyTableData.value = incomeData.dailyData;
      buildSelectedFieldData();
      renderDynamicFeeChart();
      // 从dailyData中提取order_quantity，追加到销量图表
      if (cachedQtyLabels.length > 0 && incomeData.dailyData.length > 0) {
        const dateQtyMap = {};
        incomeData.dailyData.forEach(d => {
          // dailyData的posted_date格式可能为yyyy-MM-dd或yyyy/MM/dd
          const key = d.posted_date.replace(/\//g, '-');
          dateQtyMap[key] = Number(d['order_quantity']) || 0;
        });
        const transactionOrderQty = cachedQtyLabels.map(label => {
          return dateQtyMap[label.replace(/\//g, '-')] || 0;
        });
        transactionOrderTotal.value = transactionOrderQty.reduce((sum, v) => sum + v, 0);
        renderQuantityChart(cachedQtyLabels, cachedQtyDatas, cachedQtyOrderDatas, transactionOrderQty);
      } else {
        // 没有交易数据时，渲染两根线
        renderQuantityChart(cachedQtyLabels, cachedQtyDatas, cachedQtyOrderDatas);
      }
    }
    chartLoading.value = false;
  }).catch(() => {
    console.error('每日收入数据加载失败');
    // 即使收入数据失败，也要渲染销量图表（两根线）
    if (cachedQtyLabels.length > 0) {
      renderQuantityChart(cachedQtyLabels, cachedQtyDatas, cachedQtyOrderDatas);
    }
    chartLoading.value = false;
  });
}

function onFeeFieldChange() {
  renderDynamicFeeChart();
  buildSelectedFieldData();
}

function onCurrencyModeChange() {
  renderDynamicFeeChart();
  buildSelectedFieldData();
}

// 构建选中费用项的表格数据
function buildSelectedFieldData() {
  if (cachedDailyData.length === 0) return;
  dailyLabels.value = cachedDailyData.map(d => {
    const parts = d.posted_date.split('/');
    return parts[1] + '/' + parts[2];
  });
  dailyRates.value = cachedDailyData.map(d => d.exchangeRate ? Number(d.exchangeRate).toFixed(4) : '-');
  const fieldKey = selectedFeeField.value;
  const rmbKey = fieldKey + '_rmb';
  // 站点币种
  let sum = 0;
  const row = cachedDailyData.map(d => {
    const val = d[fieldKey];
    const num = val !== undefined && val !== null ? Number(val) : 0;
    sum += num;
    return num.toFixed(2);
  });
  selectedFieldSummary.value = sum.toFixed(2);
  selectedFieldDailyData.value = row;
  // RMB
  let sumRmb = 0;
  const rowRmb = cachedDailyData.map(d => {
    const val = d[rmbKey] !== undefined ? d[rmbKey] : d[fieldKey];
    const num = val !== undefined && val !== null ? Number(val) : 0;
    sumRmb += num;
    return num.toFixed(2);
  });
  selectedFieldSummaryRmb.value = sumRmb.toFixed(2);
  selectedFieldDailyDataRmb.value = rowRmb;
}

// 缓存销量图表数据，以便dailyIncomeReport返回后追加交易报告销量线
let cachedQtyLabels = []
let cachedQtyDatas = []
let cachedQtyOrderDatas = []

// 渲染销量图表
function renderQuantityChart(labels, datas, orderDatas, transactionOrderQty) {
  if (!quantityChartRef.value || labels.length === 0) return;
  cachedQtyLabels = labels;
  cachedQtyDatas = datas;
  cachedQtyOrderDatas = orderDatas;
  if (quantityChart) quantityChart.dispose();
  quantityChart = echarts.init(quantityChartRef.value);
  const legendData = ['结算(settlement report)销量合计', '订单(order data general report)销量合计'];
  const series = [
    {
      name: '结算(settlement report)销量合计',
      type: 'line',
      smooth: 0.5,
      data: datas,
      lineStyle: { color: '#FF6700', width: 2 },
      itemStyle: { color: '#FF6700' },
      areaStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: 'rgba(255,103,0,0.3)' },
          { offset: 1, color: 'rgba(255,103,0,0.05)' }
        ])
      }
    },
    {
      name: '订单(order data general report)销量合计',
      type: 'line',
      smooth: 0.5,
      data: orderDatas,
      lineStyle: { color: '#409EFF', width: 2 },
      itemStyle: { color: '#409EFF' },
      areaStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: 'rgba(64,158,255,0.3)' },
          { offset: 1, color: 'rgba(64,158,255,0.05)' }
        ])
      }
    }
  ];
  if (transactionOrderQty && transactionOrderQty.length > 0) {
    legendData.push('交易报告（payment report) 销量合计');
    series.push({
      name: '交易报告（payment report) 销量合计',
      type: 'line',
      smooth: 0.5,
      data: transactionOrderQty,
      lineStyle: { color: '#67C23A', width: 2 },
      itemStyle: { color: '#67C23A' },
      areaStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: 'rgba(103,194,58,0.3)' },
          { offset: 1, color: 'rgba(103,194,58,0.05)' }
        ])
      }
    });
  }
  const option = {
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(255, 255, 255, 0.95)',
      borderColor: '#ddd',
      borderWidth: 1,
      textStyle: { color: '#333', fontSize: '12px' },
      padding: [8, 12]
    },
    legend: {
      data: legendData,
      right: 0,
      top: 0,
      icon: 'circle',
      itemWidth: 6,
      itemHeight: 6
    },
    grid: { right: 32, left: 80, bottom: 40, top: 36 },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: labels,
      axisLine: { show: false },
      axisTick: { show: false },
      axisLabel: { color: '#acb0b9', rotate: labels.length > 15 ? 45 : 0 }
    },
    yAxis: {
      type: 'value',
      axisLabel: { color: '#acb0b9' },
      splitLine: { lineStyle: { color: 'rgba(170,170,170,0.2)' } }
    },
    series
  }
  quantityChart.setOption(option);
}

// 使用原始数据渲染费用图表（降级方案）
function renderFeeChart() {
  if (!feeChartRef.value || cachedLabels.length === 0) return;
  if (feeChart) feeChart.dispose();
  feeChart = echarts.init(feeChartRef.value);
  const datas = cachedFeeData[feeType.value] || [];
  const cfg = feeConfig[feeType.value];
  const option = {
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(255, 255, 255, 0.95)',
      borderColor: '#ddd',
      borderWidth: 1,
      textStyle: { color: '#333', fontSize: '12px' },
      padding: [8, 12]
    },
    legend: {
      data: [cfg.name],
      right: 0,
      top: 0,
      icon: 'circle',
      itemWidth: 6,
      itemHeight: 6
    },
    grid: { right: 32, left: 80, bottom: 40, top: 36 },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: cachedLabels,
      axisLine: { show: false },
      axisTick: { show: false },
      axisLabel: { color: '#acb0b9', rotate: cachedLabels.length > 15 ? 45 : 0 }
    },
    yAxis: {
      type: 'value',
      axisLabel: { color: '#acb0b9' },
      splitLine: { lineStyle: { color: 'rgba(170,170,170,0.2)' } }
    },
    series: [
      {
        name: cfg.name,
        type: 'line',
        smooth: 0.5,
        data: datas,
        lineStyle: { color: cfg.color, width: 2 },
        itemStyle: { color: cfg.color },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: cfg.rgba30 },
            { offset: 1, color: cfg.rgba05 }
          ])
        }
      }
    ]
  }
  feeChart.setOption(option);
}

// 使用动态收入项数据渲染费用图表
function renderDynamicFeeChart() {
  if (!feeChartRef.value || cachedDailyData.length === 0) return;
  if (feeChart) feeChart.dispose();
  feeChart = echarts.init(feeChartRef.value);

  const labels = cachedDailyData.map(d => d.posted_date);
  const fieldKey = selectedFeeField.value;
  const useRmb = currencyMode.value === 'rmb';
  const dataKey = useRmb ? fieldKey + '_rmb' : fieldKey;
  const datas = cachedDailyData.map(d => {
    const val = d[dataKey] !== undefined ? d[dataKey] : d[fieldKey];
    return val !== undefined ? Number(val) : 0;
  });

  const cfg = feeConfig[feeType.value] || { name: fieldKey, color: '#67C23A', rgba30: 'rgba(103,194,58,0.3)', rgba05: 'rgba(103,194,58,0.05)' };
  const displayName = fieldKey + (useRmb ? '(RMB)' : '');

  const option = {
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(255, 255, 255, 0.95)',
      borderColor: '#ddd',
      borderWidth: 1,
      textStyle: { color: '#333', fontSize: '12px' },
      padding: [8, 12]
    },
    legend: {
      data: [displayName],
      right: 0,
      top: 0,
      icon: 'circle',
      itemWidth: 6,
      itemHeight: 6
    },
    grid: { right: 32, left: 80, bottom: 40, top: 36 },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: labels,
      axisLine: { show: false },
      axisTick: { show: false },
      axisLabel: { color: '#acb0b9', rotate: labels.length > 15 ? 45 : 0 }
    },
    yAxis: {
      type: 'value',
      axisLabel: { color: '#acb0b9' },
      splitLine: { lineStyle: { color: 'rgba(170,170,170,0.2)' } }
    },
    series: [
      {
        name: displayName,
        type: 'line',
        smooth: 0.5,
        data: datas,
        lineStyle: { color: cfg.color, width: 2 },
        itemStyle: { color: cfg.color },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: cfg.rgba30 },
            { offset: 1, color: cfg.rgba05 }
          ])
        }
      }
    ]
  }
  feeChart.setOption(option);
}

// 窗口resize监听
const resizeHandler = () => {
  quantityChart?.resize()
  feeChart?.resize()
}

onMounted(() => {
  window.addEventListener('resize', resizeHandler)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', resizeHandler)
  if (quantityChart) { quantityChart.dispose(); quantityChart = null }
  if (feeChart) { feeChart.dispose(); feeChart = null }
})
</script>

<style scoped>
.chart-container {
  height: 360px;
  width: 100%;
}
.card-header-title {
  font-weight: bold;
  font-size: 14px;
}
.summary-item .summary-label {
  font-size: 12px;
  color: #666;
}
.summary-item .summary-value {
  font-size: 18px;
  font-weight: bold;
}
</style>
