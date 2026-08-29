<template>
  <div v-if="visible" class="version-update-overlay">
    <div class="version-update-dialog">
      <div class="version-update-header">
        <el-icon class="update-icon"><Refresh /></el-icon>
        <span>系统更新</span>
      </div>
      <div class="version-update-content">
        <p>检测到新版本可用，建议立即刷新页面以获取最新功能和修复。</p>
      </div>
      <div class="version-update-footer">
        <el-button @click="handleLater">稍后再说</el-button>
        <el-button type="primary" @click="handleRefresh">
          立即刷新
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import { refreshPage } from '@/utils/version'

const visible = ref(false)

/**
 * 显示更新提示
 */
function show() {
  visible.value = true
}

/**
 * 隐藏更新提示
 */
function hide() {
  visible.value = false
}

/**
 * 处理刷新
 */
function handleRefresh() {
  refreshPage()
}

/**
 * 处理稍后再说
 */
function handleLater() {
  hide()
}

defineExpose({
  show,
  hide
})
</script>

<style scoped>
.version-update-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 9999;
}

.version-update-dialog {
  background: white;
  border-radius: 12px;
  padding: 24px;
  width: 400px;
  max-width: 90%;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.2);
}

.version-update-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
  font-size: 18px;
  font-weight: 600;
  color: #303133;
}

.update-icon {
  font-size: 24px;
  color: #409eff;
}

.version-update-content {
  margin-bottom: 24px;
  line-height: 1.6;
  color: var(--el-text-color-regular);
}

.version-update-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}
</style>
