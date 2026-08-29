<template>
  <div class="pay-config-container" style="padding:10px;">
    <el-row :gutter="20">
      <!-- 左侧：费用类型映射 -->
      <el-col :span="12">
        <el-card class="mapping-card" shadow="never">
          <template #header>
            <div class="card-title">
              <span>费用类型映射</span>
              <div>
                <el-button type="success" plain icon="Setting" size="small" @click="handleAccountInit">初始化</el-button>
                <el-button type="primary" plain icon="Plus" size="small" @click="handleAccountAdd">新增</el-button>
              </div>
            </div>
          </template>

          <!-- 查询条件 -->
          <el-form :model="accountQueryParams" ref="accountQueryRef" :inline="true" label-width="70px" class="query-form">
            <el-form-item label="费用类型" prop="feeTypeId">
              <el-select v-model="accountQueryParams.feeTypeId" placeholder="核算什么" clearable style="width: 120px">
                <el-option
                  v-for="item in feeTypeOptions"
                  :key="item.id"
                  :label="item.name"
                  :value="item.id"
                />
              </el-select>
            </el-form-item>
            <el-form-item label="科目" prop="subjectId">
              <el-select v-model="accountQueryParams.subjectId" placeholder="关联科目" clearable filterable style="width: 120px">
                <el-option
                  v-for="item in subjectOptions"
                  :key="item.subjectId"
                  :label="`${item.subjectCode} ${item.subjectName}`"
                  :value="item.subjectId"
                />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" icon="Search" size="small" @click="handleAccountQuery">搜索</el-button>
              <el-button icon="Refresh" size="small" @click="resetAccountQuery">重置</el-button>
            </el-form-item>
          </el-form>

          <!-- 方向说明 -->
          <div class="direction-tip">
            <el-alert type="info" :closable="false" show-icon>
              <template #title>
                <span>费用支出：费用类型为借方</span>
                <span style="margin-left: 16px">费用收入：费用类型为贷方</span>
              </template>
            </el-alert>
          </div>

          <!-- 操作按钮 -->
          <el-row :gutter="10" class="mb8">
            <el-col :span="1.5">
              <el-button type="warning" plain icon="Upload" size="small" @click="handleAccountImport">导入</el-button>
            </el-col>
            <el-col :span="1.5">
              <el-button type="warning" plain icon="Download" size="small" @click="handleAccountExport">导出</el-button>
            </el-col>
          </el-row>

          <!-- 卡片列表 -->
          <div v-loading="accountLoading" class="card-list">
            <div v-if="accountRuleList && accountRuleList.length > 0">
              <el-card v-for="item in accountRuleList" :key="item.id" class="rule-card" shadow="hover">
                <template #header>
                  <div class="card-header">
                    <div class="card-actions">
                      <el-button link type="primary" icon="Edit" @click="handleAccountUpdate(item)"></el-button>
                      <el-button link type="danger" icon="Delete" @click="handleAccountDelete(item)"></el-button>
                    </div>
                  </div>
                </template>
                
                <div class="rule-flow">
                  <div class="flow-input">
                    <div class="flow-section-label">费用类型</div>
                    <div class="flow-item">
                      <span class="flow-label">核算什么</span>
                      <span class="flow-value">{{ item.feeTypeName }}</span>
                    </div>
                  </div>
                  
                  <div class="flow-arrow">
                    <el-icon><ArrowRight /></el-icon>
                  </div>
                  
                  <div class="flow-output">
                    <div class="flow-section-label">会计科目</div>
                    <div class="flow-item debit">
                      <span class="flow-label">借方科目</span>
                      <span class="flow-value">{{ item.subjectName }}</span>
                    </div>
                  </div>
                </div>
              </el-card>
            </div>
            
            <el-card v-if="!accountLoading && (!accountRuleList || accountRuleList.length === 0)" class="empty-card" shadow="never">
              <div class="empty-state">
                <el-icon class="empty-icon"><Document /></el-icon>
                <h3 class="empty-title">还没有费用类型映射规则</h3>
                <p class="empty-desc">配置费用类型与会计科目的对应关系，系统将自动生成凭证分录</p>
                <el-button type="primary" icon="Plus" @click="handleAccountAdd">立即创建</el-button>
              </div>
            </el-card>
          </div>
        </el-card>
      </el-col>

      <!-- 右侧：采购账户映射 -->
      <el-col :span="12">
        <el-card class="mapping-card" shadow="never">
          <template #header>
            <div class="card-title">
              <span>采购账户映射</span>
              <div>
                <el-button type="success" plain icon="Setting" size="small" @click="handleFeetypeInit">初始化</el-button>
                <el-button type="primary" plain icon="Plus" size="small" @click="handleFeetypeAdd">新增</el-button>
              </div>
            </div>
          </template>

          <!-- 查询条件 -->
          <el-form :model="feetypeQueryParams" ref="feetypeQueryRef" :inline="true" label-width="70px" class="query-form">
            <el-form-item label="采购账户" prop="accountId">
              <el-select v-model="feetypeQueryParams.accountId" placeholder="从哪里出" clearable style="width: 120px">
                <el-option
                  v-for="item in accountOptions"
                  :key="item.id"
                  :label="item.name"
                  :value="item.id"
                />
              </el-select>
            </el-form-item>
            <el-form-item label="科目" prop="subjectId">
              <el-select v-model="feetypeQueryParams.subjectId" placeholder="关联科目" clearable filterable style="width: 120px">
                <el-option
                  v-for="item in subjectOptions"
                  :key="item.subjectId"
                  :label="`${item.subjectCode} ${item.subjectName}`"
                  :value="item.subjectId"
                />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" icon="Search" size="small" @click="handleFeetypeQuery">搜索</el-button>
              <el-button icon="Refresh" size="small" @click="resetFeetypeQuery">重置</el-button>
            </el-form-item>
          </el-form>

          <!-- 方向说明 -->
          <div class="direction-tip">
            <el-alert type="info" :closable="false" show-icon>
              <template #title>
                <span>费用支出：采购账户为贷方</span>
                <span style="margin-left: 16px">费用收入：采购账户为借方</span>
              </template>
            </el-alert>
          </div>

          <!-- 操作按钮 -->
          <el-row :gutter="10" class="mb8">
            <el-col :span="1.5">
              <el-button type="warning" plain icon="Upload" size="small" @click="handleFeetypeImport">导入</el-button>
            </el-col>
            <el-col :span="1.5">
              <el-button type="warning" plain icon="Download" size="small" @click="handleFeetypeExport">导出</el-button>
            </el-col>
          </el-row>

          <!-- 卡片列表 -->
          <div v-loading="feetypeLoading" class="card-list">
            <div v-if="feetypeRuleList && feetypeRuleList.length > 0">
              <el-card v-for="item in feetypeRuleList" :key="item.id" class="rule-card" shadow="hover">
                <template #header>
                  <div class="card-header">
                    <div class="card-actions">
                      <el-button link type="primary" icon="Edit" @click="handleFeetypeUpdate(item)"></el-button>
                      <el-button link type="danger" icon="Delete" @click="handleFeetypeDelete(item)"></el-button>
                    </div>
                  </div>
                </template>
                
                <div class="rule-flow">
                  <div class="flow-input">
                    <div class="flow-section-label">采购账户</div>
                    <div class="flow-item">
                      <span class="flow-label">从哪里出</span>
                      <span class="flow-value">{{ item.accountName }}</span>
                    </div>
                  </div>
                  
                  <div class="flow-arrow">
                    <el-icon><ArrowRight /></el-icon>
                  </div>
                  
                  <div class="flow-output">
                    <div class="flow-section-label">会计科目</div>
                    <div class="flow-item credit">
                      <span class="flow-label">贷方科目</span>
                      <span class="flow-value">{{ item.subjectName }}</span>
                    </div>
                  </div>
                </div>
              </el-card>
            </div>
            
            <el-card v-if="!feetypeLoading && (!feetypeRuleList || feetypeRuleList.length === 0)" class="empty-card" shadow="never">
              <div class="empty-state">
                <el-icon class="empty-icon"><Document /></el-icon>
                <h3 class="empty-title">还没有采购账户映射规则</h3>
                <p class="empty-desc">配置采购账户与会计科目的对应关系，系统将自动生成凭证分录</p>
                <el-button type="primary" icon="Plus" @click="handleFeetypeAdd">立即创建</el-button>
              </div>
            </el-card>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 费用类型映射 - 添加或修改对话框 -->
    <el-dialog :title="accountDialogTitle" v-model="accountDialogOpen" width="600px" append-to-body>
      <el-form ref="accountFormRef" :model="accountForm" :rules="accountRules" label-width="100px">
        <el-divider content-position="left">核心配置</el-divider>
        <el-form-item label="费用类型" prop="feeTypeId">
          <el-select v-model="accountForm.feeTypeId" placeholder="请选择费用类型" clearable>
            <el-option
              v-for="item in feeTypeOptions"
              :key="item.id"
              :label="item.name"
              :value="item.id"
            />
          </el-select>
          <span class="el-form-item__tip">核算什么</span>
        </el-form-item>
        
        <el-divider content-position="left">科目配置</el-divider>
        <el-alert type="info" :closable="false" show-icon style="margin-bottom: 16px;">
          <template #title>
            <span>费用支出：费用类型为借方</span>
            <span style="margin-left: 16px">费用收入：费用类型为贷方</span>
          </template>
        </el-alert>
        <el-form-item label="会计科目" prop="subjectId">
          <el-select v-model="accountForm.subjectId" placeholder="请选择会计科目" clearable filterable>
            <el-option
              v-for="item in subjectOptions"
              :key="item.subjectId"
              :label="`${item.subjectCode} ${item.subjectName}`"
              :value="item.subjectId"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitAccountForm">确 定</el-button>
          <el-button @click="cancelAccount">取 消</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 采购账户映射 - 添加或修改对话框 -->
    <el-dialog :title="feetypeDialogTitle" v-model="feetypeDialogOpen" width="600px" append-to-body>
      <el-form ref="feetypeFormRef" :model="feetypeForm" :rules="feetypeRules" label-width="100px">
        <el-divider content-position="left">核心配置</el-divider>
        <el-form-item label="采购账户" prop="accountId">
          <el-select v-model="feetypeForm.accountId" placeholder="请选择采购账户" clearable>
            <el-option
              v-for="item in accountOptions"
              :key="item.id"
              :label="item.name"
              :value="item.id"
            />
          </el-select>
          <span class="el-form-item__tip">从哪里出</span>
        </el-form-item>
        
        <el-divider content-position="left">科目配置</el-divider>
        <el-alert type="info" :closable="false" show-icon style="margin-bottom: 16px;">
          <template #title>
            <span>费用支出：采购账户为贷方</span>
            <span style="margin-left: 16px">费用收入：采购账户为借方</span>
          </template>
        </el-alert>
        <el-form-item label="会计科目" prop="subjectId">
          <el-select v-model="feetypeForm.subjectId" placeholder="请选择会计科目" clearable filterable>
            <el-option
              v-for="item in subjectOptions"
              :key="item.subjectId"
              :label="`${item.subjectCode} ${item.subjectName}`"
              :value="item.subjectId"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitFeetypeForm">确 定</el-button>
          <el-button @click="cancelFeetype">取 消</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 导入文件弹框（共用） -->
    <el-dialog v-model="uploadVisible" :title="uploadTitle" width="400px">
      <el-upload
        :drag="true"
        action
        ref="uploadRef"
        :http-request="uploadFiles"
        :limit="1"
        :on-exceed="handleExceed"
        :before-upload="beforeUpload"
        :show-file-list="true"
        accept=".xls,.xlsx"
      >
        <el-icon class="font-large"><upload-filled /></el-icon>
        <div class="el-upload__text">
          拖拽文件到此处或 <em>点击上传</em>
        </div>
        <template #tip>
          <div class="el-upload__tip">仅支持 .xls、.xlsx 格式文件</div>
        </template>
      </el-upload>
      <template #footer>
        <span class="dialog-footer">
          <div class="flex-center-between">
            <el-button type="success" @click.stop="downloadTemp" plain>下载模板</el-button>
            <div>
              <el-button @click="uploadVisible = false">取消</el-button>
              <el-button type="primary" v-loading="uploadloading" @click.stop="uploadExcel">
                上传文件
              </el-button>
            </div>
          </div>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="ErpMappingRule">
import { listMappingErpAccount, getMappingErpAccount, delMappingErpAccount, addMappingErpAccount, updateMappingErpAccount } from "@/api/finance/mappingErpAccount"
import { listMappingErpFeetype, getMappingErpFeetype, delMappingErpFeetype, addMappingErpFeetype, updateMappingErpFeetype } from "@/api/finance/mappingErpFeetype"
import { listAll } from "@/api/finance/subjects"
import { getProject, getAccountAll } from "@/api/erp/finance/project"
import finStore from "@/hooks/store/useFinanceStore.js"
import request from '@/utils/request'
import { ArrowRight, Document, UploadFilled } from '@element-plus/icons-vue'
import { ElMessage, genFileId } from 'element-plus'

const { proxy } = getCurrentInstance()

// 共用选项
const feeTypeOptions = ref([])
const accountOptions = ref([])
const subjectOptions = ref([])

// 导入相关
const uploadVisible = ref(false)
const uploadloading = ref(false)
const uploadRef = ref(null)
const myfile = ref(null)
const uploadType = ref('account') // 'account' 或 'feetype'
const uploadTitle = ref('导入映射规则')

// ==================== 费用类型映射（account） ====================
const accountRuleList = ref([])
const accountLoading = ref(true)
const accountDialogOpen = ref(false)
const accountDialogTitle = ref('')

const accountData = reactive({
  accountForm: {},
  accountQueryParams: {
    feeTypeId: null,
    subjectId: null
  },
  accountRules: {
    feeTypeId: [
      { required: true, message: "费用类型不能为空", trigger: "change" }
    ],
    subjectId: [
      { required: true, message: "会计科目不能为空", trigger: "change" }
    ]
  }
})

const { accountQueryParams, accountForm, accountRules } = toRefs(accountData)

// ==================== 采购账户映射（feetype） ====================
const feetypeRuleList = ref([])
const feetypeLoading = ref(true)
const feetypeDialogOpen = ref(false)
const feetypeDialogTitle = ref('')

const feetypeData = reactive({
  feetypeForm: {},
  feetypeQueryParams: {
    accountId: null,
    subjectId: null
  },
  feetypeRules: {
    accountId: [
      { required: true, message: "采购账户不能为空", trigger: "change" }
    ],
    subjectId: [
      { required: true, message: "会计科目不能为空", trigger: "change" }
    ]
  }
})

const { feetypeQueryParams, feetypeForm, feetypeRules } = toRefs(feetypeData)

// ==================== 共用方法 ====================

/** 根据ID查找名称 */
function findNameById(options, id, nameField = 'name') {
  if (!options || !id) return ''
  const item = options.find(opt => opt.id === id)
  return item ? item[nameField] : id
}

/** 根据科目ID查找科目名称 */
function findSubjectNameById(subjects, subjectId) {
  if (!subjects || !subjectId) return ''
  const item = subjects.find(s => s.subjectId === subjectId)
  if (!item) return subjectId
  return item.subjectCode ? `${item.subjectCode} ${item.subjectName}` : item.subjectName
}

/** 费用类型映射 - 初始化 */
async function handleAccountInit() {
  try {
    // 检查科目是否存在：112320（货物费用）、112321（运费）
    const subject112320 = subjectOptions.value.find(s => s.subjectCode === '112320')
    const subject112321 = subjectOptions.value.find(s => s.subjectCode === '112321')

    if (!subject112320 || !subject112321) {
      ElMessage.warning('无法初始化，请确定预付账款科目已经存在（112320、112321）')
      return
    }

    // 查找费用类型：货物费用、运费
    const goodsFeeType = feeTypeOptions.value.find(f => f.name === '货物费用')
    const freightFeeType = feeTypeOptions.value.find(f => f.name === '运费')

    if (!goodsFeeType || !freightFeeType) {
      ElMessage.warning('无法初始化，请确定费用类型"货物费用"和"运费"已存在')
      return
    }

    const groupid = await finStore.getCurrentTenantId()

    // 创建费用类型映射：货物费用 -> 112320
    await addMappingErpAccount({
      groupid,
      feeTypeId: goodsFeeType.id,
      subjectId: subject112320.subjectId
    })

    // 创建费用类型映射：运费 -> 112321
    await addMappingErpAccount({
      groupid,
      feeTypeId: freightFeeType.id,
      subjectId: subject112321.subjectId
    })

    ElMessage.success('费用类型映射初始化成功')
    getAccountList()
  } catch (error) {
    console.error('费用类型映射初始化失败:', error)
    ElMessage.error('初始化失败：' + (error.message || '未知错误'))
  }
}

/** 采购账户映射 - 初始化 */
async function handleFeetypeInit() {
  try {
    // 检查科目是否存在：100201
    const subject100201 = subjectOptions.value.find(s => s.subjectCode === '100201')

    if (!subject100201) {
      ElMessage.warning('无法初始化，请确定银行存款科目（100201）已存在')
      return
    }

    const groupid = await finStore.getCurrentTenantId()

    // 获取所有采购账户
    if (!accountOptions.value || accountOptions.value.length === 0) {
      ElMessage.warning('无法初始化，请先添加采购账户')
      return
    }

    // 为每个采购账户创建映射
    for (const account of accountOptions.value) {
      await addMappingErpFeetype({
        groupid,
        accountId: account.id,
        subjectId: subject100201.subjectId
      })
    }

    ElMessage.success('采购账户映射初始化成功')
    getFeetypeList()
  } catch (error) {
    console.error('采购账户映射初始化失败:', error)
    ElMessage.error('初始化失败：' + (error.message || '未知错误'))
  }
}

/** 查询下拉选项数据 */
async function getOptions() {
  try {
    const groupid = await finStore.getCurrentTenantId()
    if (!groupid) {
      console.warn('租户ID为空，跳过加载选项数据')
      return
    }
    
    await Promise.all([
      getProject().then(response => {
        feeTypeOptions.value = response.data || []
      }).catch(err => {
        // 忽略取消的请求（页面切换时正常行为）
        if (err.code !== 'ERR_CANCELED') {
          console.error('获取费用类型失败:', err)
        }
      }),
      getAccountAll().then(response => {
        accountOptions.value = response.data || []
      }).catch(err => {
        if (err.code !== 'ERR_CANCELED') {
          console.error('获取采购账户失败:', err)
        }
      }),
      listAll({groupid: groupid, status: 1}).then(response => {
        subjectOptions.value = response.data || []
      }).catch(err => {
        if (err.code !== 'ERR_CANCELED') {
          console.error('获取会计科目失败:', err)
        }
      })
    ])
  } catch (error) {
    // 忽略取消的请求
    if (error.code !== 'ERR_CANCELED') {
      console.error('获取租户ID失败:', error)
    }
  }
}

// ==================== 费用类型映射方法 ====================

function fillAccountNames(rows) {
  if (!rows) return []
  return rows.map(row => ({
    ...row,
    feeTypeName: findNameById(feeTypeOptions.value, row.feeTypeId),
    subjectName: findSubjectNameById(subjectOptions.value, row.subjectId)
  }))
}

async function getAccountList() {
  accountLoading.value = true
  try {
    accountQueryParams.value.groupid = await finStore.getCurrentTenantId()
    const response = await listMappingErpAccount(accountQueryParams.value)
    const rows = response?.rows || response?.data?.rows || response?.data || []
    accountRuleList.value = fillAccountNames(rows)
  } catch (error) {
    console.error('查询费用类型映射规则失败:', error)
    accountRuleList.value = []
  } finally {
    accountLoading.value = false
  }
}

function handleAccountQuery() {
  getAccountList()
}

function resetAccountQuery() {
  proxy.resetForm("accountQueryRef")
  handleAccountQuery()
}

function handleAccountAdd() {
  resetAccountForm()
  accountDialogOpen.value = true
  accountDialogTitle.value = "添加费用类型映射规则"
}

function handleAccountUpdate(row) {
  resetAccountForm()
  getMappingErpAccount(row.id).then(response => {
    accountForm.value = response.data
    accountDialogOpen.value = true
    accountDialogTitle.value = "修改费用类型映射规则"
  })
}

function resetAccountForm() {
  accountForm.value = {
    id: null,
    feeTypeId: null,
    subjectId: null
  }
  proxy.resetForm("accountFormRef")
}

function cancelAccount() {
  accountDialogOpen.value = false
  resetAccountForm()
}

async function submitAccountForm() {
  proxy.$refs["accountFormRef"].validate(async valid => {
    if (valid) {
      accountForm.value.groupid = await finStore.getCurrentTenantId()
      if (accountForm.value.id != null) {
        updateMappingErpAccount(accountForm.value).then(response => {
          proxy.$modal.msgSuccess("修改成功")
          accountDialogOpen.value = false
          getAccountList()
        })
      } else {
        addErpMappingAccount(accountForm.value).then(response => {
          proxy.$modal.msgSuccess("新增成功")
          accountDialogOpen.value = false
          getAccountList()
        })
      }
    }
  })
}

function handleAccountDelete(row) {
  const _ids = row.id
  proxy.$modal.confirm('是否确认删除该费用类型映射规则？').then(function() {
    return delMappingErpAccount(_ids)
  }).then(() => {
    getAccountList()
    proxy.$modal.msgSuccess("删除成功")
  }).catch(() => {})
}

function handleAccountExport() {
  proxy.download('/api/finance/mappingErpAccount/export', {
    ...accountQueryParams.value
  }, `费用类型映射_${new Date().getTime()}.xlsx`)
}

function handleAccountImport() {
  uploadType.value = 'account'
  uploadTitle.value = '导入费用类型映射规则'
  uploadVisible.value = true
  uploadloading.value = false
  myfile.value = null
}

// ==================== 采购账户映射方法 ====================

function fillFeetypeNames(rows) {
  if (!rows) return []
  return rows.map(row => ({
    ...row,
    accountName: findNameById(accountOptions.value, row.accountId),
    subjectName: findSubjectNameById(subjectOptions.value, row.subjectId)
  }))
}

async function getFeetypeList() {
  feetypeLoading.value = true
  try {
    feetypeQueryParams.value.groupid = await finStore.getCurrentTenantId()
    const response = await listMappingErpFeetype(feetypeQueryParams.value)
    const rows = response?.rows || response?.data?.rows || response?.data || []
    feetypeRuleList.value = fillFeetypeNames(rows)
  } catch (error) {
    console.error('查询采购账户映射规则失败:', error)
    feetypeRuleList.value = []
  } finally {
    feetypeLoading.value = false
  }
}

function handleFeetypeQuery() {
  getFeetypeList()
}

function resetFeetypeQuery() {
  proxy.resetForm("feetypeQueryRef")
  handleFeetypeQuery()
}

function handleFeetypeAdd() {
  resetFeetypeForm()
  feetypeDialogOpen.value = true
  feetypeDialogTitle.value = "添加采购账户映射规则"
}

function handleFeetypeUpdate(row) {
  resetFeetypeForm()
  getMappingErpFeetype(row.id).then(response => {
    feetypeForm.value = response.data
    feetypeDialogOpen.value = true
    feetypeDialogTitle.value = "修改采购账户映射规则"
  })
}

function resetFeetypeForm() {
  feetypeForm.value = {
    id: null,
    accountId: null,
    subjectId: null
  }
  proxy.resetForm("feetypeFormRef")
}

function cancelFeetype() {
  feetypeDialogOpen.value = false
  resetFeetypeForm()
}

async function submitFeetypeForm() {
  proxy.$refs["feetypeFormRef"].validate(async valid => {
    if (valid) {
      feetypeForm.value.groupid = await finStore.getCurrentTenantId()
      if (feetypeForm.value.id != null) {
        updateMappingErpFeetype(feetypeForm.value).then(response => {
          proxy.$modal.msgSuccess("修改成功")
          feetypeDialogOpen.value = false
          getFeetypeList()
        })
      } else {
        addMappingErpFeetype(feetypeForm.value).then(response => {
          proxy.$modal.msgSuccess("新增成功")
          feetypeDialogOpen.value = false
          getFeetypeList()
        })
      }
    }
  })
}

function handleFeetypeDelete(row) {
  const _ids = row.id
  proxy.$modal.confirm('是否确认删除该采购账户映射规则？').then(function() {
    return delMappingErpFeetype(_ids)
  }).then(() => {
    getFeetypeList()
    proxy.$modal.msgSuccess("删除成功")
  }).catch(() => {})
}

function handleFeetypeExport() {
  proxy.download('/api/finance/mappingErpFeetype/export', {
    ...feetypeQueryParams.value
  }, `采购账户映射_${new Date().getTime()}.xlsx`)
}

function handleFeetypeImport() {
  uploadType.value = 'feetype'
  uploadTitle.value = '导入采购账户映射规则'
  uploadVisible.value = true
  uploadloading.value = false
  myfile.value = null
}

// ==================== 导入相关方法 ====================

function downloadTemp() {
  if (uploadType.value === 'account') {
    proxy.download('finance/mappingErpAccount/importTemplate', {}, `费用类型映射_template.xlsx`)
  } else {
    proxy.download('finance/mappingErpFeetype/importTemplate', {}, `采购账户映射_template.xlsx`)
  }
}

function beforeUpload(file) {
  const isLt5M = file.size / 1024 < 50000
  if (!isLt5M) {
    ElMessage.error('上传文件大小不能超过 50MB')
    return false
  }
  return true
}

function uploadFiles(item) {
  myfile.value = item.file
}

function handleExceed(files) {
  uploadRef.value.clearFiles()
  const file = files[0]
  file.uid = genFileId()
  uploadRef.value.handleStart(file)
}

async function uploadExcel() {
  if (!myfile.value) {
    ElMessage.warning('请先选择要上传的文件')
    return
  }
  
  uploadloading.value = true
  const formData = new FormData()
  formData.append('file', myfile.value)
  
  try {
    const groupid = await finStore.getCurrentTenantId()
    formData.append('groupid', groupid)
    
    const url = uploadType.value === 'account'
      ? '/api/finance/mappingErpAccount/importData'
      : '/api/finance/mappingErpFeetype/importData'
    
    await request({
      url: url,
      method: 'post',
      data: formData,
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    })
    
    ElMessage.success('导入成功')
    uploadVisible.value = false
    if (uploadType.value === 'account') {
      getAccountList()
    } else {
      getFeetypeList()
    }
  } catch (error) {
    console.error('导入失败:', error)
  } finally {
    uploadloading.value = false
  }
}

// 监听选项数据变化，自动重新填充名称（解决首次进入时选项未加载完成的问题）
watch([subjectOptions, feeTypeOptions, accountOptions], () => {
  if (accountRuleList.value.length > 0) {
    accountRuleList.value = fillAccountNames(accountRuleList.value)
  }
  if (feetypeRuleList.value.length > 0) {
    feetypeRuleList.value = fillFeetypeNames(feetypeRuleList.value)
  }
})

// 初始化：加载选项数据后查询列表
async function initData() {
  await getOptions()
  // 如果选项数据加载成功（租户ID已获取），则查询列表
  if (subjectOptions.value.length > 0 || feeTypeOptions.value.length > 0) {
    getAccountList()
    getFeetypeList()
  }
}

initData()
</script>

<style scoped>
.pay-config-container {
  height: 100%;
}

.mapping-card {
  height: calc(100vh - 120px);
  overflow-y: auto;
}

.mapping-card :deep(.el-card__header) {
  padding: 12px 16px;
  background: linear-gradient(135deg, #f5f7fa 0%, #e4e7ed 100%);
  border-bottom: 1px solid #ebeef5;
}

.card-title {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: 600;
  color: #303133;
}

.query-form {
  margin-bottom: 12px;
}

.query-form :deep(.el-form-item) {
  margin-bottom: 8px;
}

.query-form :deep(.el-form-item__label) {
  font-size: 12px;
  color: #606266;
}

.direction-tip {
  margin-bottom: 12px;
}

.direction-tip :deep(.el-alert) {
  padding: 8px 16px;
}

.direction-tip :deep(.el-alert__title) {
  font-size: 12px;
  color: #409eff;
}

.card-list {
  max-height: calc(100vh - 350px);
  overflow-y: auto;
}

.rule-card {
  margin-bottom: 12px;
  transition: all 0.3s;
}

.rule-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.card-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 0;
}

.card-actions {
  margin-left: auto;
}

.rule-flow {
  display: flex;
  align-items: center;
  gap: 12px;
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

.empty-card {
  margin-top: 20px;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  text-align: center;
}

.empty-icon {
  font-size: 64px;
  color: #c0c4cc;
  margin-bottom: 20px;
}

.empty-title {
  font-size: 18px;
  color: #303133;
  margin: 0 0 12px 0;
  font-weight: 500;
}

.empty-desc {
  font-size: 14px;
  color: #909399;
  margin: 0 0 24px 0;
  max-width: 400px;
  line-height: 1.6;
}

.font-large {
  font-size: 48px;
  color: #c0c4cc;
}

.flex-center-between {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.el-upload__tip {
  color: #909399;
  font-size: 12px;
  margin-top: 8px;
}
</style>
