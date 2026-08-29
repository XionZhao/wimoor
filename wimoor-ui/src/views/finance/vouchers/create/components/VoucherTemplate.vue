<template>
  <!-- 从模版生成凭证对话框 -->
  <el-dialog v-model="showFromTemplateDialog" title="选择模版" width="700px" :close-on-click-modal="false">
    <div style="display: flex; gap: 12px; margin-bottom: 16px;">
      <el-select v-model="templateSearchForm.typeId" placeholder="模版类型" clearable style="width: 180px;" @change="loadTemplates" :teleported="false">
        <el-option v-for="item in templateTypeList" :key="item.id" :label="item.name" :value="item.id"></el-option>
      </el-select>
      <el-input v-model="templateSearchForm.name" placeholder="模版名称" clearable style="flex: 1;" @keyup.enter="loadTemplates">
        <template #append>
          <el-button @click="loadTemplates">
            <el-icon><Search /></el-icon>
          </el-button>
        </template>
      </el-input>
      <el-checkbox v-model="templateSearchForm.onlyShowInfo" @change="loadTemplates">仅显示模版类别和名称</el-checkbox>
    </div>
    <!-- 仅显示模版类别和名称模式 -->
    <el-table v-if="templateSearchForm.onlyShowInfo" :data="templateList" v-loading="templateLoading" highlight-current-row @current-change="handleTemplateSelect" style="width: 100%; max-height: 400px; overflow-y: auto;">
      <el-table-column label="操作" width="60" align="center">
        <template #default="{ row }">
          <el-button type="danger" link size="small" @click.stop="deleteTemplate(row)">
            <el-icon><Delete /></el-icon>
          </el-button>
        </template>
      </el-table-column>
      <el-table-column property="typeName" label="模版类型" width="150"></el-table-column>
      <el-table-column property="name" label="模版名称"></el-table-column>
    </el-table>
    <!-- 显示分录详情模式 -->
    <el-table v-else :data="flattenedTemplateList" v-loading="templateLoading" highlight-current-row @current-change="handleTemplateSelect" :span-method="handleSpanMethod" style="width: 100%; max-height: 400px; overflow-y: auto;">
      <el-table-column label="操作" width="60" align="center">
        <template #default="{ row }">
          <el-button v-if="row._isFirstRow" type="danger" link size="small" @click.stop="deleteTemplate(row)">
            <el-icon><Delete /></el-icon>
          </el-button>
        </template>
      </el-table-column>
      <el-table-column label="模版类型" width="100">
        <template #default="{ row }">
          <span>{{ row.typeName }}</span>
        </template>
      </el-table-column>
      <el-table-column label="模版名称" width="150">
        <template #default="{ row }">
          <span>{{ row.name }}</span>
        </template>
      </el-table-column>
      <el-table-column label="摘要" prop="summary" width="120" show-overflow-tooltip></el-table-column>
      <el-table-column label="科目" width="180" show-overflow-tooltip>
        <template #default="{ row }">
          <span v-if="row.subjectCode">{{ row.subjectCode }} {{ row.subjectName }}</span>
        </template>
      </el-table-column>
      <el-table-column label="借方金额" prop="debitAmount" width="100" align="right">
        <template #default="{ row }">
          <span v-if="row.debitAmount">{{ row.debitAmount }}</span>
        </template>
      </el-table-column>
      <el-table-column label="贷方金额" prop="creditAmount" width="100" align="right">
        <template #default="{ row }">
          <span v-if="row.creditAmount">{{ row.creditAmount }}</span>
        </template>
      </el-table-column>
    </el-table>
    <template #footer>
      <el-button @click="showFromTemplateDialog = false">取消</el-button>
      <el-button type="primary" :disabled="!selectedTemplate" @click="confirmFromTemplate">确定</el-button>
    </template>
  </el-dialog>

  <!-- 保存为凭证模版对话框 -->
  <el-dialog v-model="showSaveTemplateDialog" title="保存为凭证模版" width="500px" :close-on-click-modal="false">
    <el-form :model="saveTemplateForm" label-width="100px">
      <el-form-item label="模版名称" required>
        <el-input v-model="saveTemplateForm.name" placeholder="请输入模版名称"></el-input>
      </el-form-item>
      <el-form-item label="模版类别" required>
        <div style="display: flex; align-items: center; gap: 8px; width: 100%;">
          <el-select v-model="saveTemplateForm.typeId" placeholder="请选择模版类别" style="flex: 1;" :teleported="false">
            <el-option v-for="item in templateTypeList" :key="item.id" :label="item.name" :value="item.id"></el-option>
          </el-select>
          <el-button type="primary" link @click="openTemplateTypeManage">
            <el-icon><Edit /></el-icon>
          </el-button>
        </div>
      </el-form-item>
      <el-form-item label="保存内容">
        <el-checkbox v-model="saveTemplateForm.saveSummary">摘要</el-checkbox>
        <el-checkbox v-model="saveTemplateForm.saveSubject">科目</el-checkbox>
        <el-checkbox v-model="saveTemplateForm.saveAmount">金额</el-checkbox>
        <el-checkbox v-model="saveTemplateForm.saveAuxiliary">辅助核算</el-checkbox>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="showSaveTemplateDialog = false">取消</el-button>
      <el-button type="primary" :loading="saveTemplateLoading" @click="confirmSaveTemplate">保存</el-button>
    </template>
  </el-dialog>

  <!-- 模版类别管理对话框 -->
  <el-dialog v-model="showTemplateTypeDialog" title="模版类别管理" width="500px" :close-on-click-modal="false">
    <div style="display: flex; gap: 8px; margin-bottom: 16px;">
      <el-input v-model="newTemplateTypeName" placeholder="输入新类别名称" style="flex: 1;" @keyup.enter="addNewTemplateType"></el-input>
      <el-button type="primary" @click="addNewTemplateType">新增</el-button>
    </div>
    <el-table :data="templateTypeList" v-loading="templateTypeLoading" style="width: 100%; max-height: 350px; overflow-y: auto;">
      <el-table-column property="name" label="类别名称"></el-table-column>
      <el-table-column label="操作" width="180">
        <template #default="{ row }">
          <el-button type="primary" link size="small" @click="editTemplateType(row)">编辑</el-button>
          <el-button type="primary" link size="small" @click="selectTemplateType(row)">选择</el-button>
          <el-button type="danger" link size="small" @click="deleteTemplateType(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <template #footer>
      <el-button @click="showTemplateTypeDialog = false">关闭</el-button>
    </template>
  </el-dialog>

  <!-- 编辑模版类别对话框 -->
  <el-dialog v-model="showEditTemplateTypeDialog" title="编辑模版类别" width="400px" :close-on-click-modal="false">
    <el-form :model="editTemplateTypeForm" label-width="80px">
      <el-form-item label="类别名称" required>
        <el-input v-model="editTemplateTypeForm.name" placeholder="请输入类别名称"></el-input>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="showEditTemplateTypeDialog = false">取消</el-button>
      <el-button type="primary" :loading="editTemplateTypeLoading" @click="confirmEditTemplateType">确定</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, reactive, computed, defineProps, defineEmits } from 'vue'
import { Search, Edit, Delete } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  listAllTemplateType,
  addTemplateType,
  updateTemplateType,
  delTemplateType,
  listTemplate,
  addTemplate,
  getTemplateDetail,
  delTemplate
} from "@/api/finance/vouchers_template"
import finStore from "@/hooks/store/useFinanceStore.js"

const props = defineProps({
  accountList: {
    type: Array,
    default: () => []
  },
  entries: {
    type: Array,
    default: () => []
  },
  getChineseAmountDigits: {
    type: Function,
    required: true
  },
  calculateTotal: {
    type: Function,
    required: true
  }
})

const emit = defineEmits(['loadTemplate'])

// 消息提示
const showMessage = (message, type = 'info') => {
  ElMessage({ message, type })
}
const showSuccess = (message) => showMessage(message, 'success')
const showWarning = (message) => showMessage(message, 'warning')
const showError = (message) => showMessage(message, 'error')

// 凭证模版相关状态
const showFromTemplateDialog = ref(false) // 从模版生成凭证对话框
const showSaveTemplateDialog = ref(false) // 保存为凭证模版对话框
const showTemplateTypeDialog = ref(false) // 模版类别管理对话框
const showEditTemplateTypeDialog = ref(false) // 编辑模版类别对话框
const templateLoading = ref(false)
const templateTypeLoading = ref(false)
const saveTemplateLoading = ref(false)
const editTemplateTypeLoading = ref(false)
const templateList = ref([]) // 模版列表
const templateTypeList = ref([]) // 模版类别列表
const selectedTemplate = ref(null) // 选中的模版
const newTemplateTypeName = ref('') // 新类别名称

// 将模板列表展平为每个分录一行的格式
const flattenedTemplateList = computed(() => {
  if (templateSearchForm.onlyShowInfo) {
    return templateList.value
  }
  const result = []
  templateList.value.forEach(template => {
    if (template.entries && template.entries.length > 0) {
      template.entries.forEach((entry, index) => {
        result.push({
          ...template,
          ...entry,
          _templateId: template.id,
          _isFirstRow: index === 0
        })
      })
    } else {
      result.push({
        ...template,
        _templateId: template.id,
        _isFirstRow: true
      })
    }
  })
  return result
})

const templateSearchForm = reactive({
  typeId: null,
  name: '',
  onlyShowInfo: true
})

const saveTemplateForm = reactive({
  name: '',
  typeId: null,
  saveSummary: true,
  saveSubject: true,
  saveAmount: false,
  saveAuxiliary: false
})

const editTemplateTypeForm = reactive({
  id: null,
  name: ''
})

// 加载模版类别列表
async function loadTemplateTypes() {
  templateTypeLoading.value = true
  try {
    const groupid = await finStore.getCurrentTenantId()
    const res = await listAllTemplateType({ groupid })
    if (res.code === 200) {
      templateTypeList.value = res.data || []
    }
  } catch (error) {
    console.error('加载模版类别失败:', error)
  } finally {
    templateTypeLoading.value = false
  }
}

// 加载模版列表
async function loadTemplates() {
  templateLoading.value = true
  try {
    const groupid = await finStore.getCurrentTenantId()
    const params = {
      groupid,
      typeId: templateSearchForm.typeId || undefined,
      name: templateSearchForm.name || undefined,
      pageSize: 1000,
      pageNum: 1
    }
    const res = await listTemplate(params)
    if (res.code === 200) {
      const templates = res.rows || []
      // 如果取消勾选"仅显示模版类别和名称"，则加载每个模板的分录信息
      if (!templateSearchForm.onlyShowInfo && templates.length > 0) {
        for (const template of templates) {
          const detail = await loadTemplateDetail(template.id)
          if (detail && detail.entries) {
            // 从科目列表中查找对应的科目信息
            template.entries = detail.entries.map(entry => {
              let subjectCode = entry.subjectCode
              let subjectName = entry.subjectName
              if (entry.subjectId && (!subjectCode || !subjectName)) {
                const foundAccount = props.accountList.find(a => a.subjectId === entry.subjectId)
                if (foundAccount) {
                  subjectCode = foundAccount.subjectCode
                  subjectName = foundAccount.subjectName
                }
              }
              return {
                ...entry,
                subjectCode,
                subjectName
              }
            })
          }
        }
      }
      templateList.value = templates
    }
  } catch (error) {
    console.error('加载模版列表失败:', error)
  } finally {
    templateLoading.value = false
  }
}

// 加载模版详情（包含分录信息）
async function loadTemplateDetail(templateId) {
  try {
    const res = await getTemplateDetail(templateId)
    if (res.code === 200 && res.data) {
      return res.data
    }
  } catch (error) {
    console.error('加载模版详情失败:', error)
  }
  return null
}

// 打开从模版生成凭证对话框
async function openFromTemplateDialog() {
  selectedTemplate.value = null
  templateSearchForm.typeId = null
  templateSearchForm.name = ''
  templateSearchForm.onlyShowInfo = true
  await loadTemplateTypes()
  await loadTemplates()
  showFromTemplateDialog.value = true
}

// 选中模版
function handleTemplateSelect(row) {
  if (row) {
    // 从展平后的行中找到对应的模板
    const templateId = row._templateId || row.id
    selectedTemplate.value = templateList.value.find(t => t.id === templateId) || row
  } else {
    selectedTemplate.value = null
  }
}

// 处理行合并
function handleSpanMethod({ row, column, rowIndex, columnIndex }) {
  // 模版类型和模版名称列需要合并
  if (columnIndex === 0 || columnIndex === 1) {
    if (row._isFirstRow) {
      // 计算当前模版的分录数量
      const templateId = row._templateId
      const entries = flattenedTemplateList.value.filter(item => item._templateId === templateId)
      return {
        rowspan: entries.length,
        colspan: 1
      }
    } else {
      return {
        rowspan: 0,
        colspan: 0
      }
    }
  }
  return {
    rowspan: 1,
    colspan: 1
  }
}

// 确认从模版生成凭证
async function confirmFromTemplate() {
  if (!selectedTemplate.value) {
    showWarning('请选择一个模版')
    return
  }
  try {
    const templateData = await loadTemplateDetail(selectedTemplate.value.id)
    if (templateData) {
      // 后端返回的数据结构是 {template: {...}, entries: [...]}
      const entriesList = templateData.entries || []
      // 将模版数据加载到当前凭证
      if (entriesList.length > 0) {
        const newEntries = entriesList.map((entry, index) => {
          // 从科目列表中查找对应的科目信息
          let subjectCode = entry.subjectCode
          let subjectName = entry.subjectName
          if (entry.subjectId && (!subjectCode || !subjectName)) {
            const foundAccount = props.accountList.find(a => a.subjectId === entry.subjectId)
            if (foundAccount) {
              subjectCode = foundAccount.subjectCode
              subjectName = foundAccount.subjectName
            }
          }
          
          const newEntry = {
            summary: templateSearchForm.onlyShowInfo || saveTemplateForm.saveSummary ? entry.summary : '',
            subjectId: entry.subjectId,
            subjectCode: subjectCode,
            subjectName: subjectName,
            debitAmount: saveTemplateForm.saveAmount ? entry.debitAmount : '',
            creditAmount: saveTemplateForm.saveAmount ? entry.creditAmount : '',
            quantity: entry.quantity,
            unitPrice: entry.unitPrice,
            currency: entry.currency,
            exchangeRate: entry.exchangeRate,
            originalAmount: entry.originalAmount,
            isQuantity: !!entry.quantity,
            isForeignCurrency: !!entry.exchangeRate,
            isAuxiliary: !!(entry.auxiliaryList && entry.auxiliaryList.length > 0),
            auxiliaryList: entry.auxiliaryList || [],
            auxiliaryTypes: [],
            auxiliaryText: entry.auxiliaryText || '',
            debitAmountChinese: Array(11).fill(' '),
            creditAmountChinese: Array(11).fill(' ')
          }
          // 计算中文金额
          if (newEntry.debitAmount) {
            newEntry.debitAmountChinese = props.getChineseAmountDigits(parseFloat(newEntry.debitAmount)) || Array(11).fill(' ')
          }
          if (newEntry.creditAmount) {
            newEntry.creditAmountChinese = props.getChineseAmountDigits(parseFloat(newEntry.creditAmount)) || Array(11).fill(' ')
          }
          return newEntry
        })
        // 确保至少有4行
        while (newEntries.length < 4) {
          newEntries.push({ summary: '', account: '', debitAmount: '', creditAmount: '', creditAmountChinese: Array(11).fill(' '), debitAmountChinese: Array(11).fill(' ') })
        }
        emit('loadTemplate', newEntries)
      }
      showFromTemplateDialog.value = false
      showSuccess('模版加载成功')
    } else {
      showError('加载模版详情失败')
    }
  } catch (error) {
    console.error('加载模版详情失败:', error)
    showError('加载模版详情失败')
  }
}

// 打开保存为凭证模版对话框
async function openSaveTemplateDialog() {
  // 检查是否有分录数据
  const hasData = props.entries.some(item => item.subjectId)
  if (!hasData) {
    showWarning('请先录入凭证数据')
    return
  }
  saveTemplateForm.name = ''
  saveTemplateForm.typeId = null
  saveTemplateForm.saveSummary = true
  saveTemplateForm.saveSubject = true
  saveTemplateForm.saveAmount = false
  saveTemplateForm.saveAuxiliary = false
  await loadTemplateTypes()
  showSaveTemplateDialog.value = true
}

// 确认保存为凭证模版
async function confirmSaveTemplate() {
  if (!saveTemplateForm.name || !saveTemplateForm.name.trim()) {
    showWarning('请输入模版名称')
    return
  }
  if (!saveTemplateForm.typeId) {
    showWarning('请选择模版类别')
    return
  }
  saveTemplateLoading.value = true
  try {
    const groupid = await finStore.getCurrentTenantId();
    const templateData = {
      name: saveTemplateForm.name.trim(),
      typeId: saveTemplateForm.typeId,
      groupid: groupid,
      entries: []
    }
    // 处理分录数据
    const validEntries = props.entries.filter(item => item.subjectId)
    validEntries.forEach((item, index) => {
      const entry = {
        entryNo: index + 1,
        subjectId: item.subjectId,
        summary: saveTemplateForm.saveSummary ? item.summary : '',
        debitAmount: saveTemplateForm.saveAmount ? (item.debitAmount || 0) : 0,
        creditAmount: saveTemplateForm.saveAmount ? (item.creditAmount || 0) : 0,
        quantity: item.quantity,
        unitPrice: item.unitPrice,
        currency: item.currency,
        exchangeRate: item.exchangeRate,
        originalAmount: item.originalAmount,
        auxiliaryList: saveTemplateForm.saveAuxiliary ? (item.auxiliaryList || []) : []
      }
      templateData.entries.push(entry)
    })
    const res = await addTemplate(templateData)
    if (res.code === 200) {
      showSuccess('保存成功')
      showSaveTemplateDialog.value = false
    } else {
      showError(res.msg || '保存失败')
    }
  } catch (error) {
    console.error('保存模版失败:', error)
    showError('保存模版失败')
  } finally {
    saveTemplateLoading.value = false
  }
}

// 打开模版类别管理对话框
async function openTemplateTypeManage() {
  await loadTemplateTypes()
  showTemplateTypeDialog.value = true
}

// 新增模版类别
async function addNewTemplateType() {
  if (!newTemplateTypeName.value || !newTemplateTypeName.value.trim()) {
    showWarning('请输入类别名称')
    return
  }
  try {
    const groupid = await finStore.getCurrentTenantId()
    const res = await addTemplateType({
      name: newTemplateTypeName.value.trim(),
      groupid: groupid
    })
    if (res.code === 200) {
      showSuccess('新增成功')
      newTemplateTypeName.value = ''
      await loadTemplateTypes()
    } else {
      showError(res.msg || '新增失败')
    }
  } catch (error) {
    console.error('新增类别失败:', error)
    showError('新增类别失败')
  }
}

// 编辑模版类别
function editTemplateType(row) {
  editTemplateTypeForm.id = row.id
  editTemplateTypeForm.name = row.name
  showEditTemplateTypeDialog.value = true
}

// 确认编辑模版类别
async function confirmEditTemplateType() {
  if (!editTemplateTypeForm.name || !editTemplateTypeForm.name.trim()) {
    showWarning('请输入类别名称')
    return
  }
  editTemplateTypeLoading.value = true
  try {
    const groupid = await finStore.getCurrentTenantId()
    const res = await updateTemplateType({
      id: editTemplateTypeForm.id,
      name: editTemplateTypeForm.name.trim(),
      groupid: groupid
    })
    if (res.code === 200) {
      showSuccess('修改成功')
      showEditTemplateTypeDialog.value = false
      await loadTemplateTypes()
    } else {
      showError(res.msg || '修改失败')
    }
  } catch (error) {
    console.error('修改类别失败:', error)
    showError('修改类别失败')
  } finally {
    editTemplateTypeLoading.value = false
  }
}

// 删除模版类别
async function deleteTemplateType(row) {
  try {
    await ElMessageBox.confirm('确定要删除该类别吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    const res = await delTemplateType(row.id)
    if (res.code === 200) {
      showSuccess('删除成功')
      await loadTemplateTypes()
    } else {
      showError(res.msg || '删除失败')
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除类别失败:', error)
      showError('删除类别失败')
    }
  }
}

// 选择模版类别（从管理对话框中选择）
function selectTemplateType(row) {
  saveTemplateForm.typeId = row.id
  showTemplateTypeDialog.value = false
}

// 删除模版
async function deleteTemplate(row) {
  try {
    await ElMessageBox.confirm(`确定要删除模版"${row.name}"吗？删除后将无法恢复！`, '警告', {
      confirmButtonText: '确定删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
    const res = await delTemplate(row.id)
    if (res.code === 200) {
      showSuccess('删除成功')
      await loadTemplates()
    } else {
      showError(res.msg || '删除失败')
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除模版失败:', error)
      showError('删除模版失败')
    }
  }
}

// 暴露方法给父组件
defineExpose({
  openFromTemplateDialog,
  openSaveTemplateDialog
})
</script>
