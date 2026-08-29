<template>
  <div class="invoice-config-container" style="padding: 10px;">
    <!-- 操作按钮 -->
    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="Plus" @click="handleAdd">新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="success" plain icon="Setting" @click="handleInit">初始化</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <!-- 映射规则列表 -->
    <div v-loading="loading">
      <el-row :gutter="16" v-if="ruleList && ruleList.length > 0">
        <el-col :xs="24" :sm="12" :md="8" :lg="6" v-for="item in ruleList" :key="item.id">
          <el-card class="rule-card" shadow="hover">
            <template #header>
              <div class="card-header">
                <span class="card-title">{{ item.summary || '未设置摘要' }}</span>
                <div class="card-actions">
                  <el-button link type="primary" icon="Edit" @click="handleUpdate(item)"></el-button>
                  <el-button link type="danger" icon="Delete" @click="handleDelete(item)"></el-button>
                </div>
              </div>
            </template>

            <!-- 映射关系展示 -->
            <div class="rule-flow">
              <!-- 发票 -->
              <div class="flow-input">
                <div class="flow-section-label">发票</div>
                <div class="flow-item">
                  <span class="flow-label">摘要</span>
                  <span class="flow-value">{{ item.summary || '-' }}</span>
                </div>
                <div class="flow-item">
                  <span class="flow-label">发票类型</span>
                  <span class="flow-value">{{ item.invoiceType === 0 ? '采购发票' : item.invoiceType === 1 ? '承运商发票' : '-' }}</span>
                </div>
              </div>

              <!-- 箭头 -->
              <div class="flow-arrow">
                <el-icon><ArrowRight /></el-icon>
              </div>

              <!-- 凭证 -->
              <div class="flow-output">
                <div class="flow-section-label">凭证</div>
                <div class="flow-item debit">
                  <span class="flow-label">借方科目</span>
                  <span class="flow-value">{{ item.debitSubjectName || '-' }}</span>
                </div>
                <div class="flow-item credit">
                  <span class="flow-label">贷方科目</span>
                  <span class="flow-value">{{ item.creditSubjectName || '-' }}</span>
                </div>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>

      <!-- 空状态 -->
      <el-card v-if="!loading && (!ruleList || ruleList.length === 0)" class="empty-card" shadow="never">
        <div class="empty-state">
          <el-icon class="empty-icon"><Document /></el-icon>
          <h3 class="empty-title">还没有发票凭证映射规则</h3>
          <p class="empty-desc">配置发票与凭证科目的对应关系，系统将根据映射规则自动生成凭证分录</p>
          <el-button type="primary" icon="Plus" @click="handleAdd">立即创建</el-button>
        </div>
      </el-card>
    </div>

    <!-- 添加或修改映射规则对话框 -->
    <el-dialog :title="title" v-model="open" width="600px" append-to-body>
      <el-form ref="ruleRef" :model="form" :rules="rules" label-width="100px">
        <el-divider content-position="left">映射配置</el-divider>
        <el-form-item label="摘要" prop="summary">
          <el-input v-model="form.summary" placeholder="请输入摘要（如：采购发票入账）" maxlength="50" />
        </el-form-item>
        <el-form-item label="发票类型" prop="invoiceType">
          <el-select v-model="form.invoiceType" placeholder="请选择发票类型" clearable style="width: 100%">
            <el-option label="采购发票" :value="0" />
            <el-option label="承运商发票" :value="1" />
          </el-select>
        </el-form-item>

        <el-divider content-position="left">借贷科目配置</el-divider>
        <el-alert
          title="配置发票生成凭证时的借方和贷方科目"
          type="info"
          :closable="false"
          show-icon
          style="margin-bottom: 18px;"
        />
        <el-form-item label="借方科目" prop="debitSubjectId">
          <el-select v-model="form.debitSubjectId" placeholder="请选择借方科目" clearable filterable style="width: 100%">
            <el-option
              v-for="item in subjectOptions"
              :key="item.subjectId"
              :label="`${item.subjectCode} ${item.subjectName}`"
              :value="item.subjectId"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="贷方科目" prop="creditSubjectId">
          <el-select v-model="form.creditSubjectId" placeholder="请选择贷方科目" clearable filterable style="width: 100%">
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
          <el-button type="primary" @click="submitForm">确 定</el-button>
          <el-button @click="cancel">取 消</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="InvoiceConfig">
import { ref, reactive, toRefs } from 'vue'
import { getCurrentInstance } from '@vue/runtime-core'
import { listClosingTemplateInvoice, addClosingTemplateInvoice, updateClosingTemplateInvoice, delClosingTemplateInvoice } from '@/api/finance/closing_template_invoice'
import { listAll } from '@/api/finance/subjects'
import finStore from '@/hooks/store/useFinanceStore.js'
import { ArrowRight, Document } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

const { proxy } = getCurrentInstance()

const ruleList = ref([])
const open = ref(false)
const loading = ref(true)
const showSearch = ref(true)
const title = ref('')
const subjectOptions = ref([])

const data = reactive({
  form: {},
  rules: {
    summary: [
      { required: true, message: "摘要不能为空", trigger: "blur" }
    ],
    debitSubjectId: [
      { required: true, message: "借方科目不能为空", trigger: "change" }
    ],
    creditSubjectId: [
      { required: true, message: "贷方科目不能为空", trigger: "change" }
    ]
  }
})

const { form, rules } = toRefs(data)

/** 填充科目名称 */
function fillSubjectNames(rows) {
  if (!rows) return []
  return rows.map(row => {
    const debitSubject = subjectOptions.value.find(s => s.subjectId === row.debitSubjectId)
    const creditSubject = subjectOptions.value.find(s => s.subjectId === row.creditSubjectId)
    return {
      ...row,
      debitSubjectName: debitSubject ? `${debitSubject.subjectCode} ${debitSubject.subjectName}` : row.debitSubjectId,
      creditSubjectName: creditSubject ? `${creditSubject.subjectCode} ${creditSubject.subjectName}` : row.creditSubjectId
    }
  })
}

/** 查询映射规则列表 */
async function getList() {
  loading.value = true
  try {
    const groupid = await finStore.getCurrentTenantId()
    const response = await listClosingTemplateInvoice({ groupid })
    const rows = response?.rows || response?.data?.rows || response?.data || []
    ruleList.value = fillSubjectNames(rows)
  } catch (error) {
    console.error('查询发票凭证映射规则失败:', error)
    ruleList.value = []
  } finally {
    loading.value = false
  }
}

/** 查询会计科目选项 */
async function getSubjectOptions() {
  const groupid = await finStore.getCurrentTenantId()
  try {
    const response = await listAll({ groupid, status: 1 })
    subjectOptions.value = response.data || []
  } catch (error) {
    console.error('查询会计科目失败:', error)
  }
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
    summary: '',
    invoiceType: null,
    debitSubjectId: null,
    creditSubjectId: null
  }
  proxy.resetForm("ruleRef")
}

/** 新增按钮操作 */
function handleAdd() {
  reset()
  open.value = true
  title.value = "添加发票凭证映射规则"
}

/** 修改按钮操作 */
function handleUpdate(row) {
  reset()
  form.value = {
    id: row.id,
    summary: row.summary,
    invoiceType: row.invoiceType,
    debitSubjectId: row.debitSubjectId,
    creditSubjectId: row.creditSubjectId
  }
  open.value = true
  title.value = "修改发票凭证映射规则"
}

/** 提交按钮 */
async function submitForm() {
  proxy.$refs["ruleRef"].validate(async valid => {
    if (valid) {
      const groupid = await finStore.getCurrentTenantId()
      form.value.groupid = groupid
      if (form.value.id != null) {
        updateClosingTemplateInvoice(form.value).then(() => {
          proxy.$modal.msgSuccess("修改成功")
          open.value = false
          getList()
        })
      } else {
        addClosingTemplateInvoice(form.value).then(() => {
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
  proxy.$modal.confirm('是否确认删除该映射规则？').then(function() {
    return delClosingTemplateInvoice(row.id)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess("删除成功")
  }).catch(() => {})
}

/** 初始化按钮操作 */
async function handleInit() {
  try {
    const groupid = await finStore.getCurrentTenantId()

    // 查找科目：112302（预付账款-在途发票）、112320（预付账款-采购供应商）
    const subject112302 = subjectOptions.value.find(s => s.subjectCode === '112302')
    const subject112320 = subjectOptions.value.find(s => s.subjectCode === '112320')

    // 查找科目：56012201（物流费用）、2202（应付账款）
    const subject56012201 = subjectOptions.value.find(s => s.subjectCode === '56012201')
    const subject2202 = subjectOptions.value.find(s => s.subjectCode === '2202')

    if (!subject112302 || !subject112320) {
      ElMessage.warning('无法初始化采购发票映射，请确定以下科目已存在：112302（预付账款-在途发票）、112320（预付账款-采购供应商）')
      return
    }

    if (!subject56012201 || !subject2202) {
      ElMessage.warning('无法初始化承运商发票映射，请确定以下科目已存在：56012201（物流费用）、2202（应付账款）')
      return
    }

    // 创建采购发票凭证映射：摘要=采购发票，借方112302，贷方112320
    await addClosingTemplateInvoice({
      groupid,
      summary: '采购发票',
      invoiceType: 0,
      debitSubjectId: subject112302.subjectId,
      creditSubjectId: subject112320.subjectId
    })

    // 创建承运商发票凭证映射：摘要=承运商发票，借方56012201，贷方2202
    await addClosingTemplateInvoice({
      groupid,
      summary: '承运商发票',
      invoiceType: 1,
      debitSubjectId: subject56012201.subjectId,
      creditSubjectId: subject2202.subjectId
    })

    ElMessage.success('发票凭证映射初始化成功')
    getList()
  } catch (error) {
    console.error('发票凭证映射初始化失败:', error)
    ElMessage.error('初始化失败：' + (error.message || '未知错误'))
  }
}

// 先加载科目选项，再查询列表
getSubjectOptions().then(() => {
  getList()
})
</script>

<style scoped>
.invoice-config-container {
  height: 100%;
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
  justify-content: space-between;
}

.card-title {
  font-weight: 600;
  font-size: 14px;
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
</style>
