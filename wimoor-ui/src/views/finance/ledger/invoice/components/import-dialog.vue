<template>
  <el-dialog title="导入税控平台文件" v-model="visible" width="500px" append-to-body>
    <el-alert
      title="支持从税控平台导出的Excel文件，文件包含多个页签"
      type="info"
      :closable="false"
      show-icon
      class="mb16"
    />

    <el-upload
      ref="uploadRef"
      class="upload-area"
      drag
      :auto-upload="false"
      :limit="1"
      :on-exceed="handleExceed"
      :on-change="handleChange"
      :on-remove="handleRemove"
      accept=".xlsx,.xls"
    >
      <el-icon class="el-icon--upload"><upload-filled /></el-icon>
      <div class="el-upload__text">将文件拖到此处，或<em>点击选择</em></div>
      <template #tip>
        <div class="el-upload__tip">只能上传 xlsx/xls 文件</div>
      </template>
    </el-upload>

    <div v-if="fileInfo" class="file-info">
      <el-descriptions :column="1" size="small" border>
        <el-descriptions-item label="文件名">{{ fileInfo.name }}</el-descriptions-item>
        <el-descriptions-item label="页签数">{{ fileInfo.sheetCount }} 个</el-descriptions-item>
        <el-descriptions-item label="页签名称">{{ fileInfo.sheetNames.join('、') }}</el-descriptions-item>
      </el-descriptions>
    </div>

    <template #footer>
      <el-button @click="visible = false">取 消</el-button>
      <el-button type="primary" :loading="loading" :disabled="!form.file" @click="handleSubmit">确认导入</el-button>
    </template>
  </el-dialog>
</template>

<script setup name="ImportDialog">
import { ref, reactive } from 'vue'
import { getCurrentInstance } from '@vue/runtime-core'
import { UploadFilled } from '@element-plus/icons-vue'
import * as XLSX from 'xlsx'

const { proxy } = getCurrentInstance()
const emit = defineEmits(['imported'])

const visible = ref(false)
const loading = ref(false)
const uploadRef = ref()
const fileInfo = ref(null)

const form = reactive({
  file: null
})

// 打开弹窗
function open() {
  visible.value = true
  form.file = null
  fileInfo.value = null
}

// 文件变更
async function handleChange(uploadFile) {
  form.file = uploadFile.raw
  // 解析文件获取页签信息
  try {
    const data = await readFile(uploadFile.raw)
    const workbook = XLSX.read(data, { type: 'array' })
    fileInfo.value = {
      name: uploadFile.name,
      sheetCount: workbook.SheetNames.length,
      sheetNames: workbook.SheetNames
    }
  } catch (error) {
    proxy.$message.error('文件解析失败，请检查文件格式')
    form.file = null
    fileInfo.value = null
  }
}

// 移除文件
function handleRemove() {
  form.file = null
  fileInfo.value = null
}

// 超出限制
function handleExceed() {
  proxy.$message.warning('只能上传一个文件')
}

// 读取文件
function readFile(file) {
  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = e => resolve(e.target.result)
    reader.onerror = reject
    reader.readAsArrayBuffer(file)
  })
}

// 解析所有页签
function parseAllSheets(workbook) {
  // 表头行识别关键字：包含这些关键字的单元格越多，越可能是真正的表头行
  // 注意：汇总行（如"合计金额|12345|合计税额|678"）也可能命中少量关键字，
  // 所以必须扫描完所有候选行，选得分最高的，不能提前break
  const headerKeywords = ['发票', '号码', '代码', '日期', '金额', '税额', '名称', '购方', '销方',
    '价税', '票种', '类型', '承运', '旅客', '车次', '出发', '到达', '电子客票', '运输', '税率', '项目']

  const sheets = {}
  workbook.SheetNames.forEach(sheetName => {
    const worksheet = workbook.Sheets[sheetName]
    if (!worksheet['!ref']) {
      sheets[sheetName] = { headers: [], rows: [] }
      return
    }
    const range = XLSX.utils.decode_range(worksheet['!ref'])

    // 遍历前15行，计算每行的关键字得分，选得分最高的行作为表头
    // 不提前break，避免汇总行（得分较低）被误选为表头
    let headerRow = range.s.r
    let bestScore = -1
    const scanEnd = Math.min(range.s.r + 15, range.e.r)
    for (let R = range.s.r; R <= scanEnd; ++R) {
      let nonEmptyCount = 0
      let keywordScore = 0
      for (let C = range.s.c; C <= range.e.c; ++C) {
        const cell = worksheet[XLSX.utils.encode_cell({ c: C, r: R })]
        if (cell && cell.v !== undefined && cell.v !== null && cell.v !== '') {
          nonEmptyCount++
          const cellText = String(cell.v).replace(/[\s\n\r]+/g, '')
          if (headerKeywords.some(kw => cellText.includes(kw))) {
            keywordScore++
          }
        }
      }
      if (nonEmptyCount >= 3 && keywordScore > bestScore) {
        bestScore = keywordScore
        headerRow = R
      }
    }

    console.log(`[税控导入] 页签"${sheetName}"：表头行=${headerRow}，关键字得分=${bestScore}，总行数=${range.e.r}`)

    // 读取表头
    const headers = []
    for (let C = range.s.c; C <= range.e.c; ++C) {
      const cell = worksheet[XLSX.utils.encode_cell({ c: C, r: headerRow })]
      let hdr = '列' + (C + 1)
      if (cell && cell.t) hdr = XLSX.utils.format_cell(cell)
      // 去除表头中的空格和换行
      hdr = hdr.replace(/[\s\n\r]+/g, '').trim()
      headers.push(hdr)
    }
    console.log(`[税控导入] 页签"${sheetName}"解析后的表头:`, headers)

    // 读取数据行（从表头下一行开始）
    const rows = []
    for (let R = headerRow + 1; R <= range.e.r; ++R) {
      const row = {}
      let hasData = false
      for (let C = range.s.c; C <= range.e.c; ++C) {
        const cell = worksheet[XLSX.utils.encode_cell({ c: C, r: R })]
        const key = headers[C - range.s.c]
        if (cell && cell.v !== undefined && cell.v !== null && cell.v !== '') {
          // 处理日期类型：Excel序列号转为yyyy-MM-dd格式
          if (cell.t === 'd' && cell.v instanceof Date) {
            const d = cell.v
            row[key] = `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
          } else if (cell.t === 'n' && cell.z && cell.z.indexOf('yy') >= 0) {
            // 数字格式但包含日期格式串，使用XLSX转换
            const d = XLSX.SSF.parse_date_code(cell.v)
            if (d) {
              row[key] = `${d.y}-${String(d.m).padStart(2, '0')}-${String(d.d).padStart(2, '0')}`
            } else {
              row[key] = cell.v
            }
          } else {
            row[key] = cell.v
          }
          hasData = true
        } else {
          row[key] = ''
        }
      }
      if (hasData) rows.push(row)
    }

    sheets[sheetName] = { headers, rows }
  })
  return sheets
}

// 提交
async function handleSubmit() {
  if (!form.file) {
    proxy.$message.warning('请上传文件')
    return
  }

  loading.value = true
  try {
    const data = await readFile(form.file)
    const workbook = XLSX.read(data, { type: 'array' })
    const sheets = parseAllSheets(workbook)

    visible.value = false
    emit('imported', sheets)
    proxy.$message.success('解析成功')
  } catch (error) {
    proxy.$message.error('文件解析失败：' + (error.message || '未知错误'))
  } finally {
    loading.value = false
  }
}

defineExpose({ open })
</script>

<style scoped>
.mb16 {
  margin-bottom: 16px;
}

.upload-area {
  width: 100%;
}

.file-info {
  margin-top: 16px;
}
</style>
