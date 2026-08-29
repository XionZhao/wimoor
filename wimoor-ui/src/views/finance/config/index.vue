<template>
  <div class="app-container">
    <!-- 预付账款闭环示意图 -->
    <div class="closed-loop-diagram" >
      <div class="loop-title">
        <el-icon><InfoFilled /></el-icon>
        <span>预付账款闭环示意：付款 → 在途确认 → 入库验收 → 发票，科目互相冲销形成闭环</span>
      </div>
      <div class="loop-flow">
        <!-- ① 付款凭证 -->
        <div class="loop-node">
          <div class="node-header pay">① 付款凭证</div>
          <div class="node-body">
            <div class="node-line debit pair-supplier">
              <span class="line-label">借</span>
              <span class="line-value">预付账款（采购供应商）</span>
              <span class="role-badge increase">借 + 增加</span>
            </div>
            <div class="node-line credit">
              <span class="line-label">贷</span>
              <span class="line-value">银行存款</span>
            </div>
          </div>
        </div>

        <div class="loop-arrow">
          <el-icon><ArrowRight /></el-icon>
        </div>

        <!-- ② 在途确认 -->
        <div class="loop-node">
          <div class="node-header inventory">② 在途确认</div>
          <div class="node-body">
            <div class="node-line debit pair-goods">
              <span class="line-label">借</span>
              <span class="line-value">在途物资</span>
              <span class="role-badge increase">借 + 增加</span>
            </div>
            <div class="node-line credit pair-transit">
              <span class="line-label">贷</span>
              <span class="line-value">预付账款（在途发票）</span>
              <span class="role-badge increase">贷 + 确认</span>
            </div>
          </div>
        </div>

        <div class="loop-arrow">
          <el-icon><ArrowRight /></el-icon>
        </div>

        <!-- ③ 入库验收 -->
        <div class="loop-node">
          <div class="node-header receipt">③ 入库验收</div>
          <div class="node-body">
            <div class="node-line debit">
              <span class="line-label">借</span>
              <span class="line-value">库存商品</span>
            </div>
            <div class="node-line credit pair-goods">
              <span class="line-label">贷</span>
              <span class="line-value">在途物资</span>
              <span class="role-badge offset">贷 - 冲销</span>
            </div>
          </div>
        </div>

        <div class="loop-arrow">
          <el-icon><ArrowRight /></el-icon>
        </div>

        <!-- ④ 发票凭证 -->
        <div class="loop-node">
          <div class="node-header invoice">④ 发票凭证</div>
          <div class="node-body">
            <div class="node-line debit pair-transit">
              <span class="line-label">借</span>
              <span class="line-value">预付账款（在途发票）</span>
              <span class="role-badge offset">借 - 冲销</span>
            </div>
            <div class="node-line credit pair-supplier">
              <span class="line-label">贷</span>
              <span class="line-value">预付账款（采购供应商）</span>
              <span class="role-badge offset">贷 - 冲销</span>
            </div>
          </div>
        </div>

        <!-- 闭环回环箭头 -->
        <div class="loop-back">
          <div class="loop-back-line">
            <span class="loop-back-label">冲销闭环</span>
            <el-icon><CaretTop /></el-icon>
          </div>
        </div>
      </div>

      <!-- 颜色图例 -->
      <div class="loop-legend">
        <span class="legend-item">
          <span class="legend-dot supplier"></span>预付账款（采购供应商）一借一贷冲销
        </span>
        <span class="legend-item">
          <span class="legend-dot transit"></span>预付账款（在途发票）一贷一借冲销
        </span>
        <span class="legend-item">
          <span class="legend-dot goods"></span>在途物资 一借一贷冲销
        </span>
      </div>
    </div>

    <el-tabs v-model="activeTab">
      <el-tab-pane label="付款凭证" name="payconfig">
      </el-tab-pane>
      <el-tab-pane label="库存凭证" name="inventoryconfig">
      </el-tab-pane>
      <el-tab-pane label="发票凭证" name="invoiceconfig">
      </el-tab-pane>
      <el-tab-pane label="凭证生成记录" name="voucherlog">
      </el-tab-pane>
    </el-tabs>
     <PayConfig v-if="activeTab === 'payconfig'" />
     <InventoryConfig v-if="activeTab === 'inventoryconfig'" />
     <InvoiceConfig v-if="activeTab === 'invoiceconfig'" />
     <VoucherLog v-if="activeTab === 'voucherlog'" />
  </div>
</template>

<script setup name="ErpConfig">
import { ref } from 'vue'
import PayConfig from './components/payconfig.vue'
import InventoryConfig from './components/inventoryconfig.vue'
import InvoiceConfig from './components/invoiceconfig.vue'
import VoucherLog from './components/voucherlog.vue'
import { ArrowRight, InfoFilled, CaretTop } from '@element-plus/icons-vue'

const activeTab = ref('payconfig')
</script>

<style scoped>
.app-container {
  padding: 0;
  height: 100%;
  overflow: auto;
}

/* ==================== 闭环示意图 ==================== */
.closed-loop-diagram {
  margin: 16px 12px;
  background: linear-gradient(135deg, #f0f5ff 0%, #e6f0ff 100%);
  border: 1px solid #b3d4ff;
  border-radius: 8px;
  padding: 12px 16px;
}

.loop-title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  font-weight: 600;
  color: #1d4ed8;
  margin-bottom: 12px;
}

.loop-flow {
  display: flex;
  align-items: center;
  justify-content: center;
  flex-wrap: wrap;
  gap: 0;
}

.loop-node {
  background: #fff;
  border-radius: 6px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);
  min-width: 220px;
  overflow: hidden;
}

.node-header {
  padding: 6px 12px;
  font-size: 13px;
  font-weight: 600;
  color: #fff;
  text-align: center;
}

.node-header.pay {
  background: #409eff;
}

.node-header.inventory {
  background: #e6a23c;
}

.node-header.invoice {
  background: #67c23a;
}

.node-header.receipt {
  background: #9c27b0;
}

.node-body {
  padding: 8px 12px;
}

.node-line {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 4px 0;
  font-size: 12px;
}

.node-line + .node-line {
  border-top: 1px dashed #e4e7ed;
}

.line-label {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 20px;
  height: 20px;
  border-radius: 50%;
  font-size: 11px;
  font-weight: 600;
  color: #fff;
  flex-shrink: 0;
}

.node-line.debit .line-label {
  background: #67c23a;
}

.node-line.credit .line-label {
  background: #e6a23c;
}

.line-value {
  color: #303133;
  word-break: break-all;
}

/* 冲销配对颜色 */
.node-line.pair-supplier {
  background: #e6f4ff;
  border-radius: 4px;
  margin: 2px -4px;
  padding: 4px 4px;
}

.node-line.pair-transit {
  background: #fff7e6;
  border-radius: 4px;
  margin: 2px -4px;
  padding: 4px 4px;
}

.node-line.pair-goods {
  background: #f9f0ff;
  border-radius: 4px;
  margin: 2px -4px;
  padding: 4px 4px;
}

.node-line.pair-supplier .line-value {
  color: #1677ff;
  font-weight: 600;
}

.node-line.pair-transit .line-value {
  color: #d46b08;
  font-weight: 600;
}

.node-line.pair-goods .line-value {
  color: #722ed1;
  font-weight: 600;
}

/* 角色标签：增加 / 冲销 */
.role-badge {
  font-size: 10px;
  padding: 1px 6px;
  border-radius: 10px;
  font-weight: 600;
  white-space: nowrap;
  flex-shrink: 0;
  margin-left: auto;
}

.role-badge.increase {
  background: #f6ffed;
  color: #389e0d;
  border: 1px solid #b7eb8f;
}

.role-badge.offset {
  background: #fff2f0;
  color: #cf1322;
  border: 1px solid #ffccc7;
}

/* 颜色图例 */
.loop-legend {
  display: flex;
  justify-content: center;
  gap: 24px;
  margin-top: 10px;
  padding-top: 8px;
  border-top: 1px dashed #b3d4ff;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 11px;
  color: #606266;
}

.legend-dot {
  width: 10px;
  height: 10px;
  border-radius: 3px;
  flex-shrink: 0;
}

.legend-dot.supplier {
  background: #1677ff;
}

.legend-dot.transit {
  background: #d46b08;
}

.legend-dot.goods {
  background: #722ed1;
}

.loop-arrow {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  color: #909399;
  font-size: 20px;
  flex-shrink: 0;
}

.loop-back {
  display: flex;
  align-items: flex-end;
  margin-left: 8px;
}

.loop-back-line {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
  padding: 16px 8px 0;
  position: relative;
}

.loop-back-line::before {
  content: '';
  position: absolute;
  left: 50%;
  bottom: 100%;
  width: 2px;
  height: 20px;
  background: #f56c6c;
  border-radius: 1px;
}

.loop-back-label {
  font-size: 11px;
  color: #f56c6c;
  font-weight: 600;
  white-space: nowrap;
}

.loop-back-line .el-icon {
  color: #f56c6c;
  font-size: 16px;
}

/* 小屏幕适配 */
@media (max-width: 900px) {
  .loop-flow {
    flex-direction: column;
    gap: 8px;
  }
  .loop-arrow {
    transform: rotate(90deg);
  }
}
/* ==================== 闭环示意图 end ==================== */

:deep(.el-tabs) {
  display: flex;
  flex-direction: column;
  height: 100%;
}

:deep(.el-tabs__header) {
  margin: 0;
}

:deep(.el-tabs__nav) {
  margin-left: 16px;
}

:deep(.el-tabs__content) {
  padding: 0;
  flex: 1;
  overflow: auto;
}

:deep(.el-tab-pane) {
  height: 100%;
}
</style>
