<template>
  <div class="file-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <el-tabs v-model="activeTab" @tab-change="handleTabChange">
            <el-tab-pane label="我的文件" name="personal" />
            <el-tab-pane label="公司文件" name="company" />
          </el-tabs>
          <el-upload
            :auto-upload="false"
            :show-file-list="false"
            :on-change="(file) => handleFileChange(file, activeTab === 'company')"
            :disabled="uploading"
            multiple
          >
            <el-button type="primary" :icon="Upload" :loading="uploading" :disabled="uploading">
              {{ uploading ? '上传中...' : (activeTab === 'company' ? '上传公司文件' : '上传我的文件') }}
            </el-button>
          </el-upload>
        </div>
      </template>

      <!-- 上传进度面板 -->
      <div v-if="uploadQueue.length > 0" class="upload-progress-panel">
        <div class="upload-progress-header">
          <span>上传队列 ({{ completedCount }}/{{ uploadQueue.length }})</span>
          <el-button v-if="uploading" type="danger" size="small" @click="cancelAllUploads">取消全部</el-button>
        </div>
        <div class="upload-progress-list">
          <div v-for="item in uploadQueue" :key="item.id" class="upload-progress-item">
            <div class="file-info">
              <el-icon :size="16"><Document /></el-icon>
              <span class="filename">{{ item.name }}</span>
              <span class="file-size">{{ formatFileSize(item.size) }}</span>
            </div>
            <div class="progress-wrapper">
              <el-progress
                :percentage="item.progress"
                :status="getProgressStatus(item)"
                :stroke-width="8"
              />
            </div>
            <div class="status-text">
              <template v-if="item.status === 'uploading'">
                <div>{{ formatFileSize(item.uploaded) }} / {{ formatFileSize(item.size) }}</div>
                <div>{{ item.speed }}</div>
              </template>
              <template v-else>{{ getStatusText(item) }}</template>
            </div>
          </div>
        </div>
      </div>

      <!-- 搜索栏 -->
      <div class="search-bar">
        <el-input
          v-model="searchKeyword"
          placeholder="输入文件名搜索..."
          v-debounce-input="handleSearch"
          clearable
          style="width: 300px;"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
      </div>

      <el-table
        :data="filteredFileList"
        v-loading="activeTab === 'personal' ? loading : companyLoading"
        stripe
        style="width: 100%"
        height="calc(100vh - 310px)"
      >
        <template #empty>
          <el-empty :description="searchKeyword ? '未找到匹配文件' : (activeTab === 'company' ? '暂无公司文件，请上传' : '暂无文件，请上传')" />
        </template>
        <el-table-column label="预览" width="80" align="center">
          <template #default="scope">
            <el-image
              v-if="isImage(scope.row.name)"
              :src="scope.row.url"
              style="width: 48px; height: 48px; border-radius: 4px; cursor: pointer;"
              fit="cover"
              @click="handlePreview(scope.row)"
            >
              <template #placeholder>
                <div class="thumb-placeholder">
                  <el-icon :size="20"><PictureFilled /></el-icon>
                </div>
              </template>
              <template #error>
                <div class="thumb-placeholder">
                  <el-icon :size="20"><PictureFilled /></el-icon>
                </div>
              </template>
            </el-image>
            <div v-else class="file-icon" @click="handleOpen(scope.row)">
              <el-icon :size="28"><Document /></el-icon>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="name" label="文件名" min-width="260" show-overflow-tooltip>
          <template #default="scope">
            <el-link type="primary" :underline="false" @click="handleOpen(scope.row)">
              {{ scope.row.name }}
            </el-link>
          </template>
        </el-table-column>
        <el-table-column prop="createtime" label="上传时间" width="200">
          <template #default="scope">
            {{ formatDate(scope.row.createtime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="260" fixed="right">
          <template #default="scope">
            <el-button link type="primary" @click="handleOpen(scope.row)">打开</el-button>
            <el-button link type="success" @click="handleCopyLink(scope.row)">复制链接</el-button>
            <el-button link type="warning" @click="handleRename(scope.row)">重命名</el-button>
            <el-button link type="danger" @click="handleDelete(scope.row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 重命名对话框 -->
    <el-dialog v-model="renameDialogVisible" title="重命名" width="400px">
      <el-form :model="renameForm" label-width="80px">
        <el-form-item label="文件名">
          <el-input v-model="renameForm.name" placeholder="请输入文件名" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="renameDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmRename" :loading="renameLoading">确定</el-button>
      </template>
    </el-dialog>

    <!-- 图片预览对话框 -->
    <el-dialog v-model="previewVisible" title="图片预览" width="800px" :close-on-click-modal="true">
      <div style="text-align: center;">
        <el-image :src="previewUrl" style="max-width: 100%; max-height: 70vh;" fit="contain" />
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { Upload, Search, PictureFilled, Document } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox, ElLoading } from 'element-plus'
import largeFileUserApi from '@/api/sys/tool/largeFileUserApi'

const activeTab = ref('personal')
const loading = ref(false)
const companyLoading = ref(false)
const renameLoading = ref(false)
const uploading = ref(false)
const fileList = ref([])
const companyFileList = ref([])
const renameDialogVisible = ref(false)
const renameForm = ref({ id: null, name: '' })
const searchKeyword = ref('')
const previewVisible = ref(false)
const previewUrl = ref('')

// 上传队列相关
const uploadQueue = ref([])
let uploadIdCounter = 0

const CHUNK_SIZE = 5 * 1024 * 1024 // 5MB 分片大小
const MAX_CONCURRENT = 3 // 最大并发上传数

const imageExts = ['jpg', 'jpeg', 'png', 'gif', 'webp', 'bmp', 'svg', 'ico']

function isImage(filename) {
  if (!filename) return false
  const ext = filename.split('.').pop().toLowerCase()
  return imageExts.includes(ext)
}

function handlePreview(row) {
  largeFileUserApi.getLink(row.id).then(res => {
    previewUrl.value = res.data
    previewVisible.value = true
  })
}

// 搜索过滤后的列表
const filteredFileList = computed(() => {
  const list = activeTab.value === 'personal' ? fileList.value : companyFileList.value
  if (!searchKeyword.value) return list
  const keyword = searchKeyword.value.toLowerCase()
  return list.filter(item => item.name && item.name.toLowerCase().includes(keyword))
})

// 已完成数量
const completedCount = computed(() => {
  return uploadQueue.value.filter(item => item.status === 'success' || item.status === 'error').length
})

function handleSearch(value) {
  // 搜索由 computed 自动处理，此函数用于防抖指令回调
}

function formatDate(dateStr) {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  const y = date.getFullYear()
  const m = String(date.getMonth() + 1).padStart(2, '0')
  const d = String(date.getDate()).padStart(2, '0')
  const h = String(date.getHours()).padStart(2, '0')
  const min = String(date.getMinutes()).padStart(2, '0')
  const s = String(date.getSeconds()).padStart(2, '0')
  return `${y}-${m}-${d} ${h}:${min}:${s}`
}

function formatFileSize(bytes) {
  if (!bytes) return '0 B'
  const units = ['B', 'KB', 'MB', 'GB', 'TB']
  let size = bytes
  let unitIndex = 0
  while (size >= 1024 && unitIndex < units.length - 1) {
    size /= 1024
    unitIndex++
  }
  return size.toFixed(unitIndex === 0 ? 0 : 1) + ' ' + units[unitIndex]
}

function getProgressStatus(item) {
  if (item.status === 'success') return 'success'
  if (item.status === 'error') return 'exception'
  if (item.status === 'cancelled') return 'warning'
  return ''
}

function getStatusText(item) {
  if (item.status === 'pending') return '等待中'
  if (item.status === 'uploading') return `${item.progress}%`
  if (item.status === 'success') return '完成'
  if (item.status === 'error') return '失败'
  if (item.status === 'cancelled') return '已取消'
  return ''
}

function cancelAllUploads() {
  uploadQueue.value.forEach(item => {
    if (item.status === 'pending' || item.status === 'uploading') {
      item.status = 'cancelled'
      if (item.abortController) {
        item.abortController.abort()
      }
    }
  })
  uploading.value = false
}

async function loadFileList() {
  loading.value = true
  try {
    const res = await largeFileUserApi.list()
    fileList.value = res.data || []
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

async function loadCompanyFileList() {
  companyLoading.value = true
  try {
    const res = await largeFileUserApi.listCompany()
    companyFileList.value = res.data || []
  } catch (e) {
    console.error(e)
  } finally {
    companyLoading.value = false
  }
}

function handleTabChange(tab) {
  searchKeyword.value = ''
  if (tab === 'personal') {
    loadFileList()
  } else {
    loadCompanyFileList()
  }
}

// 处理文件选择事件（支持多文件）
async function handleFileChange(file, isCompany = false) {
  // 只处理 ready 状态的文件，避免重复添加
  if (file.status !== 'ready') return

  // 将文件添加到上传队列
  const uploadItem = {
    id: ++uploadIdCounter,
    name: file.name,
    size: file.size,
    raw: file.raw,
    progress: 0,
    uploaded: 0,
    speed: '',
    status: 'pending',
    isCompany,
    abortController: null,
    startTime: null,
    lastLoaded: 0,
    lastTime: 0
  }
  uploadQueue.value.push(uploadItem)

  // 如果没有正在上传，开始处理队列
  if (!uploading.value) {
    uploading.value = true
    processUploadQueue()
  }
}

// 处理上传队列
async function processUploadQueue() {
  const pendingItems = uploadQueue.value.filter(item => item.status === 'pending')

  if (pendingItems.length === 0) {
    // 检查是否全部完成
    const allDone = uploadQueue.value.every(item =>
      item.status === 'success' || item.status === 'error' || item.status === 'cancelled'
    )
    if (allDone) {
      uploading.value = false
      const successCount = uploadQueue.value.filter(item => item.status === 'success').length
      if (successCount > 0) {
        ElMessage.success(`成功上传 ${successCount} 个文件`)
        // 刷新文件列表
        if (activeTab.value === 'personal') {
          loadFileList()
        } else {
          loadCompanyFileList()
        }
      }
      // 延迟清空上传队列
      setTimeout(() => {
        uploadQueue.value = []
      }, 3000)
    }
    return
  }

  // 并发上传
  const concurrentItems = pendingItems.slice(0, MAX_CONCURRENT)
  const promises = concurrentItems.map(item => uploadSingleFile(item))
  await Promise.allSettled(promises)

  // 继续处理剩余文件
  processUploadQueue()
}

// 上传单个文件（支持大文件分片上传）
async function uploadSingleFile(item) {
  item.status = 'uploading'

  // 小于 10MB 的文件使用普通上传
  if (item.size < 10 * 1024 * 1024) {
    return uploadSmallFile(item)
  }

  // 大文件使用分片上传（断点续传）
  return uploadLargeFile(item)
}

// 普通上传小文件
async function uploadSmallFile(item) {
  const formData = new FormData()
  formData.append('file', item.raw)
  formData.append('name', item.name)

  try {
    item.startTime = Date.now()
    item.lastTime = item.startTime
    item.lastLoaded = 0

    const onProgress = (event) => {
      if (event.total > 0) {
        const now = Date.now()
        const timeDiff = (now - item.lastTime) / 1000 // 秒
        const loadedDiff = event.loaded - item.lastLoaded

        // 进度最大95%，等服务器响应后才到100%
        const progress = Math.min(95, Math.round((event.loaded / event.total) * 95))
        item.progress = progress
        item.uploaded = event.loaded

        // 计算瞬时速度（每秒）
        if (timeDiff > 0.1) { // 至少100ms间隔才计算
          const speed = loadedDiff / timeDiff
          item.speed = formatFileSize(speed) + '/s'
          item.lastLoaded = event.loaded
          item.lastTime = now
        }
      }
    }

    // 显示"处理中..."
    item.speed = '处理中...'

    if (item.isCompany) {
      await largeFileUserApi.uploadCompany(formData, onProgress)
    } else {
      await largeFileUserApi.upload(formData, onProgress)
    }

    // 服务器返回成功，才真正完成
    item.progress = 100
    item.uploaded = item.size
    item.speed = ''
    item.status = 'success'
  } catch (e) {
    item.status = 'error'
    item.speed = ''
    console.error('上传失败:', e)
  }
}

// 分片上传大文件（预签名URL直传MinIO，绕过服务器）
async function uploadLargeFile(item) {
  try {
    item.startTime = Date.now()
    item.lastTime = item.startTime
    item.lastLoaded = 0

    // 1. 初始化分片上传
    const initRes = await largeFileUserApi.initChunkUpload(item.name, item.size)
    const uploadId = initRes.data.uploadId
    const chunkTotal = Math.ceil(item.size / CHUNK_SIZE)

    // 2. 获取所有分片的预签名上传URL
    const presignedRes = await largeFileUserApi.getPresignedUrls(uploadId, chunkTotal)
    const presignedUrls = presignedRes.data || []

    // 3. 查询已上传的分片（断点续传）
    let uploadedChunks = []
    try {
      const uploadedRes = await largeFileUserApi.getUploadedChunks(uploadId)
      uploadedChunks = uploadedRes.data || []
    } catch (e) {
      // 忽略查询失败
    }

    const uploadedSet = new Set(uploadedChunks)
    let uploadedCount = uploadedSet.size
    let uploadedBytes = uploadedCount * CHUNK_SIZE

    // 更新初始进度
    if (uploadedCount > 0) {
      item.progress = Math.round((uploadedCount / chunkTotal) * 100)
      item.uploaded = uploadedBytes
      item.lastLoaded = uploadedBytes
    }

    // 4. 并发上传未完成的分片（直传 MinIO，绕过服务器）
    const MAX_UPLOAD_CONCURRENT = 3
    const pendingChunks = []
    for (let i = 0; i < chunkTotal; i++) {
      if (!uploadedSet.has(i)) {
        const start = i * CHUNK_SIZE
        const end = Math.min(start + CHUNK_SIZE, item.size)
        const chunk = item.raw.slice(start, end)
        pendingChunks.push({ index: i, chunk, size: end - start, url: presignedUrls[i].uploadUrl })
      }
    }

    // 统一进度状态：已完成分片字节 + 各进行中分片已传字节，保证进度单调递增
    const progressState = {
      completedBytes: uploadedBytes,
      inFlight: new Map() // 分片 index -> 当前已上传字节
    }

    function updateProgress() {
      let inFlightBytes = 0
      for (const loaded of progressState.inFlight.values()) inFlightBytes += loaded
      const total = Math.min(progressState.completedBytes + inFlightBytes, item.size)
      item.uploaded = total
      item.progress = Math.min(95, Math.round((total / item.size) * 95))
    }

    async function uploadWithConcurrency(chunks) {
      let currentIndex = 0

      async function worker() {
        while (currentIndex < chunks.length) {
          if (item.status === 'cancelled') break
          const idx = currentIndex++
          const { index, chunk, size, url } = chunks[idx]
          await uploadChunkDirect(item, index, chunk, url, progressState, updateProgress)
          // 分片完成后：从进行中移除并计入已完成字节，再统一刷新进度
          progressState.inFlight.delete(index)
          progressState.completedBytes += size
          updateProgress()
        }
      }

      const workers = Array(Math.min(MAX_UPLOAD_CONCURRENT, chunks.length))
        .fill(null)
        .map(() => worker())
      await Promise.all(workers)
    }

    await uploadWithConcurrency(pendingChunks)

    // 5. 合并分片
    item.speed = '合并中...'
    await largeFileUserApi.mergeChunks(uploadId, item.name, chunkTotal)

    item.progress = 100
    item.uploaded = item.size
    item.speed = ''
    item.status = 'success'
  } catch (e) {
    item.speed = ''
    if (e.message === 'cancelled') {
      item.status = 'cancelled'
    } else {
      item.status = 'error'
      console.error('分片上传失败:', e)
    }
  }
}

// 直传单个分片到 MinIO（使用XMLHttpRequest获取上传进度）
function uploadChunkDirect(item, chunkIndex, chunk, presignedUrl, progressState, updateProgress) {
  return new Promise((resolve, reject) => {
    const xhr = new XMLHttpRequest()
    xhr.open('PUT', presignedUrl, true)

    xhr.upload.onprogress = (event) => {
      if (event.lengthComputable) {
        progressState.inFlight.set(chunkIndex, event.loaded)
        updateProgress()

        const now = Date.now()
        const timeDiff = (now - item.lastTime) / 1000
        if (timeDiff > 0.2) {
          const speed = (item.uploaded - item.lastLoaded) / timeDiff
          item.speed = formatFileSize(speed) + '/s'
          item.lastLoaded = item.uploaded
          item.lastTime = now
        }
      }
    }

    xhr.onload = () => {
      if (xhr.status === 200 || xhr.status === 204) {
        resolve()
      } else {
        reject(new Error(`分片 ${chunkIndex} 上传失败: HTTP ${xhr.status}`))
      }
    }

    xhr.onerror = () => reject(new Error(`分片 ${chunkIndex} 网络错误`))
    xhr.ontimeout = () => reject(new Error(`分片 ${chunkIndex} 上传超时`))
    xhr.timeout = 300000 // 5分钟超时

    xhr.send(chunk)
  })
}

async function handleOpen(row) {
  window.open(row.url, '_blank')
}

async function handleCopyLink(row) {
  try {
    await navigator.clipboard.writeText(row.url)
    ElMessage.success('链接已复制到剪贴板')
  } catch (e) {
    ElMessage.error('复制失败')
  }
}

function handleRename(row) {
  renameForm.value = { id: row.id, name: row.name }
  renameDialogVisible.value = true
}

async function confirmRename() {
  if (!renameForm.value.name.trim()) {
    ElMessage.warning('请输入文件名')
    return
  }
  renameLoading.value = true
  try {
    if (activeTab.value === 'personal') {
      await largeFileUserApi.rename(renameForm.value.id, renameForm.value.name)
    } else {
      await largeFileUserApi.renameCompany(renameForm.value.id, renameForm.value.name)
    }
    ElMessage.success('重命名成功')
    renameDialogVisible.value = false
    if (activeTab.value === 'personal') {
      loadFileList()
    } else {
      loadCompanyFileList()
    }
  } catch (e) {
    ElMessage.error('重命名失败')
  } finally {
    renameLoading.value = false
  }
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(`确定删除文件「${row.name}」吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    })
    const loading = ElLoading.service({
      lock: true,
      text: '删除中...',
      background: 'rgba(0, 0, 0, 0.7)',
    })
    try {
      if (activeTab.value === 'personal') {
        await largeFileUserApi.remove(row.id)
      } else {
        await largeFileUserApi.removeCompany(row.id)
      }
      ElMessage.success('删除成功')
      if (activeTab.value === 'personal') {
        loadFileList()
      } else {
        loadCompanyFileList()
      }
    } finally {
      loading.close()
    }
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

onMounted(() => {
  loadFileList()
})
</script>

<style scoped>
.file-container {
  padding: 10px;
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.search-bar {
  margin-bottom: 12px;
}
:deep(.el-tabs__header) {
  margin-bottom: 0;
  background-color: transparent;
}
:deep(.el-tabs__nav-wrap::after) {
  display: none;
}
.thumb-placeholder {
  width: 48px;
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f7fa;
  border-radius: 4px;
  color: #c0c4cc;
}
.file-icon {
  width: 48px;
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f7fa;
  border-radius: 4px;
  color: #909399;
  cursor: pointer;
}
.upload-progress-panel {
  margin-bottom: 16px;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  padding: 12px;
  background: #fafafa;
}
.upload-progress-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
  font-weight: 500;
}
.upload-progress-list {
  max-height: 300px;
  overflow-y: auto;
}
.upload-progress-item {
  display: flex;
  align-items: center;
  padding: 8px 0;
  border-bottom: 1px solid #ebeef5;
}
.upload-progress-item:last-child {
  border-bottom: none;
}
.file-info {
  display: flex;
  align-items: center;
  width: 300px;
  flex-shrink: 0;
}
.file-info .filename {
  margin-left: 8px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 180px;
}
.file-info .file-size {
  margin-left: 8px;
  color: #909399;
  font-size: 12px;
}
.progress-wrapper {
  flex: 1;
  margin: 0 16px;
}
.status-text {
  width: 120px;
  text-align: right;
  color: #909399;
  font-size: 12px;
  line-height: 1.4;
}
</style>