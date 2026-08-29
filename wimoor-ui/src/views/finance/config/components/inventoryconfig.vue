<template>
  <div class="inventory-config-container" style="padding:10px;">
    <!-- 查询条件卡片 -->
    <el-card v-show="showSearch" class="search-card" shadow="never">
      <el-form :model="queryParams" ref="queryRef" :inline="true" label-width="100px">
        <el-form-item label="仓库类型" prop="warehouseType">
          <el-select v-model="queryParams.warehouseType" placeholder="仓库类型" clearable style="width: 130px">
            <el-option label="本地仓" :value="1" />
            <el-option label="FBA仓" :value="2" />
            <el-option label="海外仓" :value="3" />
          </el-select>
        </el-form-item>

        <el-form-item label="阶段" prop="stage">
          <el-select v-model="queryParams.stage" placeholder="阶段" clearable style="width: 130px">
            <el-option label="在途确认" :value="1" />
            <el-option label="入库验收" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item label="启用状态" prop="isEnabled">
          <el-select v-model="queryParams.isEnabled" style="width:100px" placeholder="状态" clearable>
            <el-option label="启用" :value="1" />
            <el-option label="停用" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
          <el-button icon="Refresh" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 操作按钮 -->
    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="Plus" @click="handleAdd">新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="warning" plain icon="Download" @click="handleExport">导出</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="success" plain icon="Setting" @click="handleInit">初始化</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <!-- 卡片列表展示 -->
    <div v-loading="loading">
      <el-row :gutter="16" v-if="ruleList && ruleList.length > 0">
        <el-col :xs="24" :sm="12" :md="8" :lg="6" v-for="item in ruleList" :key="item.id">
          <el-card class="rule-card" shadow="hover">
            <template #header>
              <div class="card-header">
                <el-tag :type="item.isEnabled === 1 ? 'success' : 'danger'" size="small">
                  {{ item.isEnabled === 1 ? '启用' : '停用' }}
                </el-tag>
                <el-tag type="info" size="small">{{ warehouseTypeMap[item.warehouseType] }}</el-tag>
                <el-tag :type="item.stage === 1 ? 'warning' : 'success'" size="small">
                  {{ item.stage === 1 ? '在途确认' : '入库验收' }}
                </el-tag>
                <div class="card-actions">
                  <el-button link type="primary" icon="Edit" @click="handleUpdate(item)"></el-button>
                  <el-button link type="danger" icon="Delete" @click="handleDelete(item)"></el-button>
                </div>
              </div>
            </template>

            <!-- 核心对应关系展示 -->
            <div class="rule-flow">
              <!-- 输入：仓库 + 采购来源 -->
              <div class="flow-input">
                <div class="flow-section-label">业务场景</div>
                <div class="flow-item">
                  <span class="flow-label">阶段</span>
                  <span class="flow-value">{{ item.stage === 1 ? '在途确认' : '入库验收' }}</span>
                </div>
              </div>

              <!-- 箭头 -->
              <div class="flow-arrow">
                <el-icon><ArrowRight /></el-icon>
              </div>

              <!-- 输出：借方科目 + 贷方科目 -->
              <div class="flow-output">
                <div class="flow-section-label">凭证科目</div>
                <div class="flow-item debit">
                  <span class="flow-label">借方科目</span>
                  <span class="flow-value">{{ item.debitSubjectName }}</span>
                </div>
                <div class="flow-item credit">
                  <span class="flow-label">贷方科目</span>
                  <span class="flow-value">{{ item.creditSubjectName }}</span>
                </div>
              </div>
            </div>

            <div class="card-footer">
              <span v-if="item.debitAuxiliaryType || item.creditAuxiliaryType" class="aux-info">
                <span v-if="item.debitAuxiliaryType">借方核算: {{ item.debitAuxiliaryType }}</span>
                <span v-if="item.debitAuxiliaryType && item.creditAuxiliaryType"> | </span>
                <span v-if="item.creditAuxiliaryType">贷方核算: {{ item.creditAuxiliaryType }}</span>
              </span>
              <span class="priority">优先级: {{ item.priority }}</span>
            </div>
          </el-card>
        </el-col>
      </el-row>

      <!-- 空状态 -->
      <el-card v-if="!loading && (!ruleList || ruleList.length === 0)" class="empty-card" shadow="never">
        <div class="empty-state">
          <el-icon class="empty-icon"><Document /></el-icon>
          <h3 class="empty-title">还没有存货映射规则</h3>
          <p class="empty-desc">配置仓库的科目映射关系，系统将自动生成在途/入库凭证分录</p>
          <el-button type="primary" icon="Plus" @click="handleAdd">立即创建</el-button>
        </div>
      </el-card>
    </div>

    <!-- 添加或修改映射规则对话框 -->
    <el-dialog :title="title" v-model="open" width="700px" append-to-body>
      <el-form ref="ruleRef" :model="form" :rules="rules" label-width="110px">
        <!-- 仓库配置 -->
        <el-divider content-position="left">仓库配置</el-divider>
        <el-form-item label="仓库类型" prop="warehouseType">
          <el-select v-model="form.warehouseType" placeholder="请选择仓库类型">
            <el-option label="本地仓" :value="1" />
            <el-option label="FBA仓" :value="2" />
            <el-option label="海外仓" :value="3" />
          </el-select>
        </el-form-item>

        <!-- 业务场景配置 -->
        <el-divider content-position="left">业务场景配置</el-divider>
        <el-form-item label="阶段" prop="stage">
          <el-select v-model="form.stage" placeholder="请选择阶段">
            <el-option label="在途确认（付款/发货时）" :value="1" />
            <el-option label="入库验收（收货时）" :value="2" />
          </el-select>
        </el-form-item>

        <!-- 科目配置 -->
        <el-divider content-position="left">科目配置</el-divider>
        <el-alert
          title="在途阶段：借方=在途物资，贷方=应付暂估/预付在途；入库阶段：借方=库存商品，贷方=在途物资"
          type="info"
          :closable="false"
          show-icon
          style="margin-bottom: 18px;"
        />
        <el-form-item label="借方科目" prop="debitSubjectId">
          <el-select v-model="form.debitSubjectId" placeholder="请选择借方科目" clearable filterable @change="handleDebitSubjectChange">
            <el-option
              v-for="item in subjectOptions"
              :key="item.subjectId"
              :label="`${item.subjectCode} ${item.subjectName}`"
              :value="item.subjectId"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="贷方科目" prop="creditSubjectId">
          <el-select v-model="form.creditSubjectId" placeholder="请选择贷方科目" clearable filterable @change="handleCreditSubjectChange">
            <el-option
              v-for="item in subjectOptions"
              :key="item.subjectId"
              :label="`${item.subjectCode} ${item.subjectName}`"
              :value="item.subjectId"
            />
          </el-select>
        </el-form-item>

        <!-- 辅助核算配置（动态显示） -->
        <template v-if="debitAuxiliaryOptions.length > 0 || creditAuxiliaryOptions.length > 0">
          <el-divider content-position="left">辅助核算</el-divider>
          <el-form-item v-if="debitAuxiliaryOptions.length > 0" label="借方辅助核算" prop="debitAuxiliaryType">
            <el-select v-model="form.debitAuxiliaryType" placeholder="请选择借方辅助核算" clearable>
              <el-option
                v-for="item in debitAuxiliaryOptions"
                :key="item.typeCode"
                :label="item.typeName"
                :value="item.typeCode"
              />
            </el-select>
          </el-form-item>
          <el-form-item v-if="creditAuxiliaryOptions.length > 0" label="贷方辅助核算" prop="creditAuxiliaryType">
            <el-select v-model="form.creditAuxiliaryType" placeholder="请选择贷方辅助核算" clearable>
              <el-option
                v-for="item in creditAuxiliaryOptions"
                :key="item.typeCode"
                :label="item.typeName"
                :value="item.typeCode"
              />
            </el-select>
          </el-form-item>
        </template>

        <!-- 其他配置 -->
        <el-divider content-position="left">其他配置</el-divider>
        <el-form-item label="启用状态" prop="isEnabled">
          <el-radio-group v-model="form.isEnabled">
            <el-radio :label="1">启用</el-radio>
            <el-radio :label="0">停用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="优先级" prop="priority">
          <el-input-number v-model="form.priority" :min="1" :max="9999" controls-position="right" placeholder="请输入优先级" />
          <span class="el-form-item__tip">数值越小越优先</span>
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitForm">确 定</el-button>
          <el-button @click="cancel">取 消</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="InventoryConfig">
import { listMappingErpInventory, getMappingErpInventory, delMappingErpInventory, addMappingErpInventory, updateMappingErpInventory } from "@/api/finance/mappingErpInventory"
import { listAll } from "@/api/finance/subjects"
import { listSubjectAuxiliarySetting } from "@/api/finance/subject_auxiliary"
import finStore from "@/hooks/store/useFinanceStore.js"
import { ArrowRight, Document } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

const { proxy } = getCurrentInstance()

const ruleList = ref([])
const open = ref(false)
const loading = ref(true)
const showSearch = ref(true)
const ids = ref([])
const single = ref(true)
const multiple = ref(true)
const total = ref(0)
const title = ref("")

// 下拉选项
const subjectOptions = ref([])

// 仓库类型映射
const warehouseTypeMap = { 1: '本地仓', 2: 'FBA仓', 3: '海外仓' }

// 动态辅助核算选项
const debitAuxiliaryOptions = ref([])
const creditAuxiliaryOptions = ref([])

const data = reactive({
  form: {},
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    warehouseType: null,
    stage: null,
    isEnabled: 1
  },
  rules: {
    warehouseType: [
      { required: true, message: "请选择仓库类型", trigger: "change" }
    ],
    stage: [
      { required: true, message: "请选择阶段", trigger: "change" }
    ],
    debitSubjectId: [
      { required: true, message: "借方科目不能为空", trigger: "change" }
    ],
    creditSubjectId: [
      { required: true, message: "贷方科目不能为空", trigger: "change" }
    ],
    isEnabled: [
      { required: true, message: "请选择启用状态", trigger: "change" }
    ]
  }
})

const { queryParams, form, rules } = toRefs(data)

/** 根据科目ID查找科目名称（编码+名称） */
function findSubjectNameById(subjects, subjectId) {
  if (!subjects || !subjectId) return ''
  const item = subjects.find(s => s.subjectId === subjectId)
  if (!item) return subjectId
  return item.subjectCode ? `${item.subjectCode} ${item.subjectName}` : item.subjectName
}

/** 填充名称字段 */
function fillNames(rows) {
  if (!rows) return []
  return rows.map(row => ({
    ...row,
    debitSubjectName: findSubjectNameById(subjectOptions.value, row.debitSubjectId),
    creditSubjectName: findSubjectNameById(subjectOptions.value, row.creditSubjectId)
  }))
}

/** 查询映射规则列表 */
async function getList() {
  loading.value = true
  try {
    queryParams.value.groupid = await finStore.getCurrentTenantId()
    const response = await listMappingErpInventory(queryParams.value)
    const rows = response?.data || []
    ruleList.value = fillNames(rows)
    total.value = response?.total || 0
  } catch (error) {
    console.error('查询存货映射规则失败:', error)
    ruleList.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

/** 查询下拉选项数据 */
async function getOptions() {
  const groupid = await finStore.getCurrentTenantId()
  await Promise.all([
    listAll({groupid: groupid, status: 1}).then(response => {
      subjectOptions.value = response.data || []
    })
  ])
}

// 取消按钮
function cancel() {
  open.value = false
  reset()
}

// 表单重置
function reset() {
  form.value = {
    id: null,
    warehouseType: null,
    stage: 1,
    debitSubjectId: null,
    creditSubjectId: null,
    debitAuxiliaryType: null,
    creditAuxiliaryType: null,
    isEnabled: 1,
    priority: 100
  }
  debitAuxiliaryOptions.value = []
  creditAuxiliaryOptions.value = []
  proxy.resetForm("ruleRef")
}

/** 根据科目ID加载辅助核算选项 */
async function loadAuxiliaryOptions(subjectId, type) {
  if (!subjectId) {
    if (type === 'debit') {
      debitAuxiliaryOptions.value = []
      form.value.debitAuxiliaryType = null
    } else {
      creditAuxiliaryOptions.value = []
      form.value.creditAuxiliaryType = null
    }
    return
  }
  try {
    const response = await listSubjectAuxiliarySetting(subjectId)
    // TableDataInfo 返回格式: {rows: [], total: 0}
    const options = response?.rows || []
    if (type === 'debit') {
      debitAuxiliaryOptions.value = options
      // 如果当前选中的辅助核算不在新的选项中，清空
      if (form.value.debitAuxiliaryType && !options.find(o => o.typeCode === form.value.debitAuxiliaryType)) {
        form.value.debitAuxiliaryType = null
      }
    } else {
      creditAuxiliaryOptions.value = options
      if (form.value.creditAuxiliaryType && !options.find(o => o.typeCode === form.value.creditAuxiliaryType)) {
        form.value.creditAuxiliaryType = null
      }
    }
  } catch (error) {
    console.error('加载辅助核算选项失败:', error)
  }
}

/** 借方科目变化处理 */
function handleDebitSubjectChange(subjectId) {
  const subject = subjectOptions.value.find(s => s.subjectId === subjectId)
  if (subject && subject.isAuxiliary) {
    loadAuxiliaryOptions(subjectId, 'debit')
  } else {
    debitAuxiliaryOptions.value = []
    form.value.debitAuxiliaryType = null
  }
}

/** 贷方科目变化处理 */
function handleCreditSubjectChange(subjectId) {
  const subject = subjectOptions.value.find(s => s.subjectId === subjectId)
  if (subject && subject.isAuxiliary) {
    loadAuxiliaryOptions(subjectId, 'credit')
  } else {
    creditAuxiliaryOptions.value = []
    form.value.creditAuxiliaryType = null
  }
}

/** 搜索按钮操作 */
function handleQuery() {
  queryParams.value.pageNum = 1
  getList()
}

/** 重置按钮操作 */
function resetQuery() {
  proxy.resetForm("queryRef")
  handleQuery()
}

/** 新增按钮操作 */
function handleAdd() {
  reset()
  open.value = true
  title.value = "添加存货映射规则"
}

/** 修改按钮操作 */
function handleUpdate(row) {
  reset()
  const _id = row.id || ids.value
  getMappingErpInventory(_id).then(response => {
    form.value = response.data
    open.value = true
    title.value = "修改存货映射规则"
    // 加载辅助核算选项（仅当科目启用了辅助核算时）
    if (form.value.debitSubjectId) {
      const debitSubject = subjectOptions.value.find(s => s.subjectId === form.value.debitSubjectId)
      if (debitSubject && debitSubject.isAuxiliary) {
        loadAuxiliaryOptions(form.value.debitSubjectId, 'debit')
      }
    }
    if (form.value.creditSubjectId) {
      const creditSubject = subjectOptions.value.find(s => s.subjectId === form.value.creditSubjectId)
      if (creditSubject && creditSubject.isAuxiliary) {
        loadAuxiliaryOptions(form.value.creditSubjectId, 'credit')
      }
    }
  })
}

/** 提交按钮 */
async function submitForm() {
  proxy.$refs["ruleRef"].validate(async valid => {
    if (valid) {
      form.value.groupid = await finStore.getCurrentTenantId()
      if (form.value.id != null) {
        updateMappingErpInventory(form.value).then(response => {
          proxy.$modal.msgSuccess("修改成功")
          open.value = false
          getList()
        })
      } else {
        addMappingErpInventory(form.value).then(response => {
          proxy.$modal.msgSuccess("新增成功")
          open.value = false
          getList()
        })
      }
    }
  })
}

/** 删除按钮操作 */
function handleDelete(row) {
  const _ids = row.id || ids.value
  proxy.$modal.confirm('是否确认删除存货映射规则编号为"' + _ids + '"的数据项？').then(function() {
    return delMappingErpInventory(_ids)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess("删除成功")
  }).catch(() => {})
}

/** 导出按钮操作 */
function handleExport() {
  proxy.download('/api/finance/mappingErpInventory/export', {
    ...queryParams.value
  }, `存货映射规则_${new Date().getTime()}.xlsx`)
}

/** 初始化按钮操作 */
async function handleInit() {
  try {
    const groupid = await finStore.getCurrentTenantId()

    // 查找科目：1405（库存商品）、1402（在途物资）、112302（预付账款-在途发票）
    const subject1405 = subjectOptions.value.find(s => s.subjectCode === '1405')
    const subject1402 = subjectOptions.value.find(s => s.subjectCode === '1402')
    const subject112302 = subjectOptions.value.find(s => s.subjectCode === '112302')

    if (!subject1405 || !subject1402 || !subject112302) {
      ElMessage.warning('无法初始化，请确定以下科目已存在：1405（库存商品）、1402（在途物资）、112302（预付账款-在途发票）')
      return
    }

    // 入库验收阶段：借方1405，贷方1402
    await addMappingErpInventory({
      groupid,
      warehouseType: 1, // 本地仓
      stage: 2, // 入库验收
      debitSubjectId: subject1405.subjectId,
      creditSubjectId: subject1402.subjectId,
      isEnabled: 1,
      priority: 100
    })

    // 在途确认阶段：借方1402，贷方112302
    await addMappingErpInventory({
      groupid,
      warehouseType: 1, // 本地仓
      stage: 1, // 在途确认
      debitSubjectId: subject1402.subjectId,
      creditSubjectId: subject112302.subjectId,
      isEnabled: 1,
      priority: 100
    })

    ElMessage.success('库存凭证映射初始化成功')
    getList()
  } catch (error) {
    console.error('库存凭证映射初始化失败:', error)
    ElMessage.error('初始化失败：' + (error.message || '未知错误'))
  }
}

// 先加载选项数据，再查询列表
getOptions().then(() => {
  getList()
})
</script>

<style scoped>
.inventory-config-container {
  height: 100%;
}

.el-form-item__tip {
  font-size: 12px;
  color: #909399;
  margin-left: 10px;
}

.search-card {
  margin-bottom: 16px;
}

.search-card :deep(.el-card__body) {
  padding-bottom: 0;
}

.rule-card {
  margin-bottom: 16px;
  transition: all 0.3s;
}

.rule-card:hover {
  transform: translateY(-2px);
}

.card-header {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.card-actions {
  margin-left: auto;
}

.rule-flow {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 0;
}

.flow-input,
.flow-output {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.flow-section-label {
  font-size: 12px;
  font-weight: 600;
  color: #606266;
  padding: 2px 8px;
  background: #f0f2f5;
  border-radius: 3px;
  text-align: center;
  margin-bottom: 4px;
}

.flow-item {
  display: flex;
  flex-direction: column;
  padding: 8px;
  background: #f5f7fa;
  border-radius: 4px;
  border-left: 3px solid #409eff;
}

.flow-item.debit {
  border-left-color: #67c23a;
}

.flow-item.credit {
  border-left-color: #e6a23c;
}

.flow-label {
  font-size: 12px;
  color: #909399;
  margin-bottom: 4px;
}

.flow-value {
  font-size: 14px;
  color: #303133;
  font-weight: 500;
}

.flow-arrow {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  background: #ecf5ff;
  border-radius: 50%;
  color: #409eff;
  font-size: 20px;
  flex-shrink: 0;
}

.card-footer {
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid #ebeef5;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.aux-info {
  font-size: 12px;
  color: #909399;
}

.priority {
  font-size: 12px;
  color: #909399;
}

.empty-card {
  margin-top: 20px;
}

.empty-state {
  text-align: center;
  padding: 40px 0;
}

.empty-icon {
  font-size: 48px;
  color: #c0c4cc;
}

.empty-title {
  margin-top: 16px;
  color: #606266;
  font-size: 16px;
}

.empty-desc {
  margin-top: 8px;
  color: #909399;
  font-size: 14px;
}
</style>
