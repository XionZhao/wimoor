<template>
  <div class="feishu-chat-container">
    <el-row :gutter="16" style="height: 100%;">
      <!-- 左侧：群组列表 -->
      <el-col :span="6" style="height: 100%;">
        <el-card shadow="never" style="height: 100%; display: flex; flex-direction: column;">
          <template #header>
            <div class="card-header">
              <span>群组列表</span>
              <el-tag type="info" size="small">{{ groups.length }}个群</el-tag>
            </div>
          </template>
          <div class="group-list">
            <div
              v-for="group in groups"
              :key="group.id"
              class="group-item"
              :class="{ active: currentGroupId === group.id }"
              @click="selectGroup(group)"
            >
              <div class="group-avatar">
                <el-avatar :size="40" :src="group.avatarUrl">
                  {{ group.name ? group.name.charAt(0) : '?' }}
                </el-avatar>
              </div>
              <div class="group-info">
                <div class="group-name">{{ group.name || group.id }}</div>
                <div class="group-meta">
                  <span>{{ group.memberCount || 0 }}人</span>
                  <span>{{ group.messageCount || 0 }}条消息</span>
                </div>
                <div v-if="group.lastMessageTime" class="group-time">
                  {{ formatTime(group.lastMessageTime) }}
                </div>
              </div>
            </div>
            <el-empty v-if="groups.length === 0" description="暂无群组数据" :image-size="80" />
          </div>
        </el-card>
      </el-col>

      <!-- 中间：消息列表 -->
      <el-col :span="12" style="height: 100%;">
        <el-card shadow="never" style="height: 100%; display: flex; flex-direction: column;">
          <template #header>
            <div class="card-header">
              <span>{{ currentGroup ? (currentGroup.name || currentGroup.id) : '聊天记录' }}</span>
              <div v-if="currentGroupId" class="header-actions">
                <el-input
                  v-model="keyword"
                  placeholder="搜索消息..."
                  clearable
                  size="small"
                  style="width: 200px; margin-right: 8px;"
                  @keyup.enter="loadMessages"
                >
                  <template #prefix>
                    <el-icon><Search /></el-icon>
                  </template>
                </el-input>
                <el-button size="small" @click="showMembers = true">
                  成员({{ members.length }})
                </el-button>
              </div>
            </div>
          </template>
          <div v-if="currentGroupId" class="message-list" ref="messageListRef">
            <div v-for="msg in messages" :key="msg.id" class="message-item">
              <div class="msg-avatar">
                <el-avatar :size="36">{{ msg.senderName ? msg.senderName.charAt(0) : '?' }}</el-avatar>
              </div>
              <div class="msg-body">
                <div class="msg-header">
                  <span class="msg-sender">{{ msg.senderName || msg.senderId }}</span>
                  <span class="msg-time">{{ formatTime(msg.createTime) }}</span>
                </div>
                <div class="msg-content">
                  <div v-if="msg.msgType === 'text'" class="msg-text" v-html="formatTextContent(msg)"></div>
                  <div v-else-if="msg.msgType === 'post'" class="msg-post" v-html="formatPostContent(msg)"></div>
                  <div v-else-if="msg.msgType === 'interactive'" class="msg-card" v-html="formatCardContent(msg.content)"></div>
                  <span v-else-if="msg.msgType === 'image'" class="msg-media">
                    <el-icon><Picture /></el-icon> [图片]
                  </span>
                  <span v-else-if="msg.msgType === 'file'" class="msg-media">
                    <el-icon><Document /></el-icon> [文件]
                  </span>
                  <span v-else-if="msg.msgType === 'audio'" class="msg-media">
                    <el-icon><Microphone /></el-icon> [语音]
                  </span>
                  <span v-else class="msg-media">[{{ msg.msgType }}消息]</span>
                </div>
              </div>
            </div>
            <el-empty v-if="messages.length === 0" description="暂无消息" :image-size="80" />
          </div>
          <div v-else class="empty-tip">
            <el-empty description="请选择一个群组查看聊天记录" :image-size="120" />
          </div>
          <!-- 分页 -->
          <div v-if="currentGroupId" class="pagination-wrap">
            <el-pagination
              v-model:current-page="pageNum"
              v-model:page-size="pageSize"
              :total="total"
              :page-sizes="[20, 50, 100]"
              layout="total, sizes, prev, pager, next"
              small
              @size-change="loadMessages"
              @current-change="loadMessages"
            />
          </div>
        </el-card>
      </el-col>

      <!-- 右侧：文件列表 -->
      <el-col :span="6" style="height: 100%;">
        <el-card shadow="never" style="height: 100%; display: flex; flex-direction: column;">
          <template #header>
            <div class="card-header">
              <span>文件/图片</span>
            </div>
          </template>
          <div v-if="currentGroupId" class="file-list">
            <div v-for="file in files" :key="file.id" class="file-item" @click="downloadFile(file)">
              <el-icon v-if="file.fileType === 'image'" :size="24" color="#409EFF"><Picture /></el-icon>
              <el-icon v-else-if="file.fileType === 'audio'" :size="24" color="#67C23A"><Microphone /></el-icon>
              <el-icon v-else :size="24" color="#909399"><Document /></el-icon>
              <div class="file-info">
                <div class="file-name">{{ file.fileName || file.fileKey || file.id }}</div>
                <div class="file-meta">{{ formatTime(file.createTime) }}</div>
              </div>
              <el-icon class="download-icon" :size="16" color="#409EFF"><Download /></el-icon>
            </div>
            <el-empty v-if="files.length === 0" description="暂无文件" :image-size="60" />
          </div>
          <div v-else class="empty-tip">
            <el-empty description="选择群组后查看文件" :image-size="80" />
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 成员弹窗 -->
    <el-dialog v-model="showMembers" title="群成员列表" width="500px">
      <el-table :data="members" size="small" max-height="400">
        <el-table-column prop="name" label="名称" />
        <el-table-column prop="memberId" label="Open ID" show-overflow-tooltip />
        <el-table-column prop="messageCount" label="消息数" width="80" align="center" />
        <el-table-column label="最后发言" width="160">
          <template #default="{ row }">
            {{ row.lastMessageTime ? formatTime(row.lastMessageTime) : '-' }}
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, watch } from 'vue'
import { Search, Picture, Document, Microphone, Postcard, Download } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { saveAs } from 'file-saver'
import feishuApi from '@/api/sys/tool/feishuApi.js'
import { renderMarkdown } from '@/views/sys/deepseek/components/markdownParser.js'
import hljs from 'highlight.js'

const groups = ref([])
const members = ref([])
const messages = ref([])
const files = ref([])
const currentGroupId = ref('')
const currentGroup = ref(null)
const keyword = ref('')
const pageNum = ref(1)
const pageSize = ref(20)
const total = ref(0)
const showMembers = ref(false)
const messageListRef = ref(null)

// 加载群组列表
async function loadGroups() {
  try {
    const res = await feishuApi.getChatGroups()
    if (res.data) {
      groups.value = res.data
    }
  } catch (e) {
    console.error('加载群组列表失败', e)
  }
}

// 选择群组
async function selectGroup(group) {
  currentGroupId.value = group.id
  currentGroup.value = group
  pageNum.value = 1
  keyword.value = ''
  await Promise.all([loadMessages(), loadMembers(), loadFiles()])
}

// 加载消息列表
async function loadMessages() {
  if (!currentGroupId.value) return
  try {
    const res = await feishuApi.getChatMessages({
      chatId: currentGroupId.value,
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      keyword: keyword.value || undefined
    })
    if (res.data) {
      messages.value = res.data.records || []
      total.value = res.data.total || 0
    }
  } catch (e) {
    console.error('加载消息失败', e)
  }
}

// 加载成员列表
async function loadMembers() {
  if (!currentGroupId.value) return
  try {
    const res = await feishuApi.getChatMembers(currentGroupId.value)
    if (res.data) {
      members.value = res.data
    }
  } catch (e) {
    console.error('加载成员失败', e)
  }
}

// 加载文件列表
async function loadFiles() {
  if (!currentGroupId.value) return
  try {
    const res = await feishuApi.getChatFiles({
      chatId: currentGroupId.value,
      pageNum: 1,
      pageSize: 100
    })
    if (res.data) {
      files.value = res.data.records || []
    }
  } catch (e) {
    console.error('加载文件失败', e)
  }
}

// 下载文件
function downloadFile(file) {
  if (file.localPath) {
    // 如果已经有localPath（MinIO URL），直接打开
    window.open(file.localPath, '_blank')
  } else {
    // 否则调用后端下载接口
    const downloadUrl = `/admin/api/v1/feishu/chat/file/download/${file.id}`
    window.open(downloadUrl, '_blank')
  }
}

// 格式化时间
function formatTime(time) {
  if (!time) return ''
  const d = new Date(time)
  const now = new Date()
  const diff = now - d
  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return Math.floor(diff / 60000) + '分钟前'
  if (diff < 86400000) return Math.floor(diff / 3600000) + '小时前'
  const month = (d.getMonth() + 1).toString().padStart(2, '0')
  const day = d.getDate().toString().padStart(2, '0')
  const hours = d.getHours().toString().padStart(2, '0')
  const minutes = d.getMinutes().toString().padStart(2, '0')
  return `${month}-${day} ${hours}:${minutes}`
}

// 格式化纯文本消息
// 飞书 text 消息 content 格式: {"text": "@_user_1 你好\n第二行"}
// mentions 中包含 @_user_X 对应的用户信息
function formatTextContent(msg) {
  const raw = msg.contentText || ''
  let text = raw
  try {
    const obj = JSON.parse(raw)
    if (obj.text !== undefined) text = obj.text
  } catch {
    // 不是 JSON，直接使用原始文本
  }
  // 转义 HTML
  let html = escapeHtml(text)
  // 处理超链接格式 [文本](地址)
  html = html.replace(/\[([^\]]+)\]\(([^)]+)\)/g, '<a href="$2" target="_blank" rel="noopener noreferrer">$1</a>')
  // 处理 @mention 占位符 @_user_X，替换为实际用户名
  if (msg.mentions && Array.isArray(msg.mentions)) {
    for (const mention of msg.mentions) {
      const placeholder = mention.key || `@_user_${mention.index}`
      const name = mention.name || mention.id || '某人'
      html = html.replace(
        new RegExp(escapeRegExp(escapeHtml(placeholder)), 'g'),
        `<span class="post-at">@${escapeHtml(name)}</span>`
      )
    }
  }
  // 处理换行
  html = html.replace(/\n/g, '<br/>')
  return html
}

// 格式化富文本消息（post）
// 优先使用 content_v2（含 markdown 格式），否则解析 content
function formatPostContent(msg) {
  const content = msg.content || ''
  const contentV2 = msg.contentV2 || ''
  // 优先尝试 content_v2（包含 md 标签）
  if (contentV2) {
    try {
      const result = parsePostJson(contentV2)
      if (result) return result
    } catch { /* 继续用 content */ }
  }
  // 回退到 content
  if (!content) return ''
  try {
    const result = parsePostJson(content)
    if (result) return result
  } catch { /* 继续用 content */ }
  return escapeHtml(content)
}

// 解析飞书 post JSON，兼容两种格式：
// 1. 包装格式: {"zh_cn": {"title": "...", "content": [...]}}
// 2. 直接格式: {"title": "...", "content": [...]}
function parsePostJson(jsonStr) {
  const obj = JSON.parse(jsonStr)
  // 先尝试包装格式
  const lang = obj.zh_cn || obj.en_us || obj.ja_jp
  if (lang && lang.content) {
    return renderPostLines(lang)
  }
  // 再尝试直接格式（content 是数组）
  if (Array.isArray(obj.content)) {
    return renderPostLines(obj)
  }
  return null
}

// 渲染富文本行内容
function renderPostLines(lang) {
  let html = ''
  if (lang.title) {
    html += `<div class="post-title">${escapeHtml(lang.title)}</div>`
  }
  const lines = lang.content
  if (!Array.isArray(lines)) return html
  for (const line of lines) {
    if (!Array.isArray(line)) continue
    // 检测是否包含 md 标签
    const mdItem = line.find(item => item.tag === 'md')
    if (mdItem) {
      html += `<div class="post-line">${renderFeishuMarkdown(mdItem.text)}</div>`
      continue
    }
    let lineHtml = ''
    for (const item of line) {
      lineHtml += renderInlineTag(item)
    }
    if (lineHtml) {
      html += `<div class="post-line">${lineHtml}</div>`
    }
  }
  return html
}

// 渲染行内标签
function renderInlineTag(item) {
  const style = buildStyleClass(item.style)
  switch (item.tag) {
    case 'text':
      return `<span${style}>${escapeHtml(item.text || '')}</span>`
    case 'a':
      return `<a href="${escapeHtml(item.href)}" target="_blank" rel="noopener noreferrer"${style}>${escapeHtml(item.text || item.href)}</a>`
    case 'at':
      return `<span class="post-at"${style}>@${escapeHtml(item.user_name || item.user_id || '某人')}</span>`
    case 'img':
      return item.image_key
        ? `<span class="post-img">[图片]</span>`
        : '<span class="post-img">[图片]</span>'
    case 'media':
      return '<span class="post-media">[视频]</span>'
    case 'emotion':
      return `<span class="post-emoji">${item.emoji_type ? `[${escapeHtml(item.emoji_type)}]` : '[表情]'}</span>`
    case 'hr':
      return '<hr class="post-hr"/>'
    case 'code_block': {
      const code = item.text || ''
      const lang = item.language || ''
      let highlighted = escapeHtml(code)
      if (lang && hljs.getLanguage(lang)) {
        try {
          highlighted = hljs.highlight(code, { language: lang }).value
        } catch { /* 使用转义后的原文 */ }
      }
      const langLabel = lang ? '<span class="code-lang">' + escapeHtml(lang) + '</span>' : ''
      return '<div class="code-block-wrapper">' + langLabel + '<pre class="post-code"><code class="language-' + escapeHtml(lang) + '">' + highlighted + '</code></pre></div>'
    }
    default:
      return item.text ? escapeHtml(item.text) : ''
  }
}

// 根据 style 数组生成 class 字符串
function buildStyleClass(styles) {
  if (!styles || !Array.isArray(styles) || styles.length === 0) return ''
  const classes = []
  if (styles.includes('bold')) classes.push('style-bold')
  if (styles.includes('italic')) classes.push('style-italic')
  if (styles.includes('underline')) classes.push('style-underline')
  if (styles.includes('lineThrough')) classes.push('style-line-through')
  return classes.length > 0 ? ' class="' + classes.join(' ') + '"' : ''
}

// 渲染飞书 markdown 内容（content_v2 中的 md 标签）
// 使用项目已有的 marked + highlight.js 进行渲染
function renderFeishuMarkdown(text) {
  if (!text) return ''
  // 预处理飞书特有的标记
  let content = text
  // 处理 @mention: <at user_id="xxx"> -> [at:xxx]
  content = content.replace(/<at user_id="([^"]+)"><\/at>/g, (match, id) => `**@${id}**`)
  // 处理换行符 \\n -> 真实换行
  content = content.replace(/\\n/g, '\n')
  // 使用项目的 renderMarkdown 渲染
  let html = renderMarkdown(content)
  // 后处理：将 @mention 包装为高亮样式
  html = html.replace(/<strong>@([^<]+)<\/strong>/g, '<span class="post-at">@$1</span>')
  return html
}

// 格式化卡片消息（interactive）
function formatCardContent(content) {
  if (!content) return '[卡片消息]'
  try {
    const obj = JSON.parse(content)
    let html = ''
    if (obj.title) {
      html += `<div class="card-title">${escapeHtml(typeof obj.title === 'string' ? obj.title : obj.title.content || '')}</div>`
    }
    if (obj.elements && Array.isArray(obj.elements)) {
      for (const element of obj.elements) {
        if (Array.isArray(element)) {
          let lineHtml = ''
          for (const item of element) {
            lineHtml += renderInlineTag(item)
          }
          if (lineHtml) html += `<div class="card-line">${lineHtml}</div>`
        } else if (element.tag === 'hr') {
          html += '<hr class="post-hr"/>'
        } else if (element.tag === 'note') {
          const noteHtml = element.elements
            ? element.elements.map(e => renderInlineTag(e)).join('')
            : ''
          html += `<div class="card-note">${noteHtml}</div>`
        }
      }
    }
    return html || '[卡片消息]'
  } catch {
    return '[卡片消息]'
  }
}

// HTML 转义
function escapeHtml(str) {
  if (!str) return ''
  return String(str)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#039;')
}

// 正则转义
function escapeRegExp(str) {
  return str.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
}

onMounted(() => {
  loadGroups()
})
</script>

<style scoped>
.feishu-chat-container {
  height: calc(100vh - 40px);
  padding: 16px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-actions {
  display: flex;
  align-items: center;
}

.group-list {
  flex: 1;
  overflow-y: auto;
}

.group-item {
  display: flex;
  align-items: center;
  padding: 10px 8px;
  cursor: pointer;
  border-radius: 6px;
  transition: background-color 0.2s;
}

.group-item:hover {
  background-color: #f5f7fa;
}

.group-item.active {
  background-color: #ecf5ff;
}

.group-avatar {
  margin-right: 10px;
  flex-shrink: 0;
}

.group-info {
  flex: 1;
  min-width: 0;
}

.group-name {
  font-size: 14px;
  font-weight: 500;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.group-meta {
  font-size: 12px;
  color: #909399;
  margin-top: 2px;
}

.group-meta span {
  margin-right: 8px;
}

.group-time {
  font-size: 11px;
  color: #c0c4cc;
  margin-top: 2px;
}

.message-list {
  flex: 1;
  overflow-y: auto;
  padding: 8px 0;
}

.message-item {
  display: flex;
  padding: 8px 12px;
  margin-bottom: 4px;
}

.msg-avatar {
  margin-right: 10px;
  flex-shrink: 0;
}

.msg-body {
  flex: 1;
  min-width: 0;
}

.msg-header {
  display: flex;
  align-items: center;
  margin-bottom: 4px;
}

.msg-sender {
  font-size: 13px;
  font-weight: 500;
  color: #303133;
  margin-right: 8px;
}

.msg-time {
  font-size: 11px;
  color: #c0c4cc;
}

.msg-content {
  font-size: 14px;
  color: #606266;
  word-break: break-all;
  line-height: 1.6;
}

/* 纯文本消息 */
.msg-text {
  white-space: pre-wrap;
  word-break: break-word;
}

.msg-text a,
.msg-post a,
.msg-card a {
  color: #409eff;
  text-decoration: none;
}

.msg-text a:hover,
.msg-post a:hover,
.msg-card a:hover {
  text-decoration: underline;
}

/* 富文本消息 */
.msg-post {
  background: #f5f7fa;
  border-radius: 6px;
  padding: 10px 12px;
  border-left: 3px solid #409eff;
}

.msg-post .post-title,
.msg-card .card-title {
  font-weight: 600;
  font-size: 15px;
  color: #303133;
  margin-bottom: 6px;
}

.msg-post .post-line,
.msg-card .card-line {
  margin-bottom: 4px;
  line-height: 1.6;
}

.msg-post .post-line:last-child,
.msg-card .card-line:last-child {
  margin-bottom: 0;
}

/* @提及 */
.post-at {
  color: #409eff;
  font-weight: 500;
  background: #ecf5ff;
  padding: 0 4px;
  border-radius: 3px;
}

/* 代码块 */
.code-block-wrapper {
  position: relative;
  margin: 6px 0;
  border-radius: 6px;
  overflow: hidden;
  border: 1px solid #e4e7ed;
}

.code-block-wrapper .code-lang {
  display: block;
  background: #f5f7fa;
  padding: 4px 12px;
  font-size: 12px;
  color: #909399;
  border-bottom: 1px solid #e4e7ed;
  font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
  text-transform: uppercase;
}

.post-code {
  background: #fafafa;
  color: #303133;
  margin: 0;
  padding: 12px;
  overflow-x: auto;
  font-size: 13px;
  line-height: 1.5;
}

.post-code code {
  font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
}

.inline-code {
  background: #f0f0f0;
  color: #c7254e;
  padding: 2px 6px;
  border-radius: 3px;
  font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
  font-size: 13px;
}

/* 分割线 */
.post-hr {
  border: none;
  border-top: 1px solid #dcdfe6;
  margin: 8px 0;
}

/* 文本样式 */
:deep(.style-bold) {
  font-weight: 600;
}

:deep(.style-italic) {
  font-style: italic;
}

:deep(.style-underline) {
  text-decoration: underline;
}

:deep(.style-line-through) {
  text-decoration: line-through;
}

/* 表情和媒体 */
.post-emoji {
  color: #e6a23c;
}

.post-img {
  color: #909399;
}

.post-media {
  color: #909399;
}

/* 卡片消息 */
.msg-card {
  background: #f5f7fa;
  border-radius: 6px;
  padding: 10px 12px;
  border-left: 3px solid #67c23a;
}

.msg-card .card-note {
  font-size: 12px;
  color: #909399;
  margin-top: 6px;
  padding-top: 6px;
  border-top: 1px solid #ebeef5;
}

.msg-media {
  color: #909399;
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.file-list {
  flex: 1;
  overflow-y: auto;
}

.file-item {
  display: flex;
  align-items: center;
  padding: 8px;
  margin-bottom: 4px;
  border-radius: 4px;
  cursor: pointer;
}

.file-item:hover {
  background-color: #f5f7fa;
}

.download-icon {
  opacity: 0;
  transition: opacity 0.2s;
}

.file-item:hover .download-icon {
  opacity: 1;
}

.file-info {
  margin-left: 8px;
  flex: 1;
  min-width: 0;
}

.file-name {
  font-size: 13px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.file-meta {
  font-size: 11px;
  color: #c0c4cc;
  margin-top: 2px;
}

.pagination-wrap {
  padding: 8px 0;
  display: flex;
  justify-content: flex-end;
  border-top: 1px solid #ebeef5;
}

.empty-tip {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
}

:deep(.el-card__body) {
  flex: 1;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  padding: 12px;
}
</style>
