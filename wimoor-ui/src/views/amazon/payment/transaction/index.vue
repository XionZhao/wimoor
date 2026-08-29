<template>
  <div class="main-sty">
    <el-tabs v-model="activeName" class="demo-tabs">
      <el-tab-pane label="月度经营报告" name="report"></el-tab-pane>
      <el-tab-pane label="费用统计" name="statistics"></el-tab-pane>
      <el-tab-pane label="销售图表" name="chart"></el-tab-pane>
      <el-tab-pane label="交易明细表" name="detail"></el-tab-pane>
    </el-tabs>
    <div v-if="activeName === 'report'">
      <Report />
    </div>
    <div v-if="activeName === 'statistics'">
      <Statistics />
    </div>
    <div v-if="activeName === 'chart'">
      <Chart />
    </div>
    <div v-if="activeName === 'detail'">
      <Detail />
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import Report from '@/views/amazon/payment/record/report.vue'
import Statistics from './statistics.vue'
import Chart from './chart.vue'
import Detail from './detail.vue'

const activeName = ref('report')
</script>

<style scoped>
</style>
