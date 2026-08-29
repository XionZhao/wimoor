<template>
  <div class="period-change-rate">
    <el-card class="box-card" shadow="never">
      <template #header>
        <div class="card-header">
          <div class="voucher-type-info">
            <span class="voucher-type-label">凭证字</span>
            <el-select v-model="form.voucherType" placeholder="记" style="width:70px;margin-right: 5px; border-radius: 0; height: 30px;">
              <el-option v-for="item in voucherTypeList" :key="item.id" :label="item.name" :value="item.name"></el-option>
            </el-select>
            <div class="voucher-date-info">
              <span class="voucher-date-label">凭证日期</span>
              <el-date-picker
                  v-model="form.voucherDate"
                  type="date"
                  value-format="YYYY-MM-DD"
                  style="width: 150px; height: 30px; border-radius: 0;"
              >
              </el-date-picker>
            </div>
          </div>
        </div>
      </template>
      <div class="template-content">
        <div class="form-item">
          <label class="form-label">凭证摘要：
            <el-popover placement="top" :width="300" trigger="click">
              <template #reference>
                <el-icon class="help-icon"><InfoFilled /></el-icon>
              </template>
              <p style="font-size: 13px;">填写生成凭证时使用的摘要说明文字，如"结转汇兑损益"。该文字会显示在凭证的摘要列中。</p>
            </el-popover>
          </label>
          <el-input v-model="form.summary" placeholder="结转汇兑损益" style="width: 300px; border-radius: 0;"></el-input>
        </div>
        <div class="form-item">
          <label class="form-label">凭证分类：
            <el-popover placement="top" :width="300" trigger="click">
              <template #reference>
                <el-icon class="help-icon"><InfoFilled /></el-icon>
              </template>
              <p style="font-size: 13px;">选择收益和损失的结转方式。"分开结转"会分别生成收益凭证和损失凭证；"同时结转"则将收益和损失合并为一张凭证。</p>
            </el-popover>
          </label>
          <el-radio-group v-model="form.voucherClassify">
            <el-radio label="1">收益和损失分开结转（分别生成收益凭证和损失凭证）</el-radio>
            <el-radio label="2">收益和损失同时结转</el-radio>
          </el-radio-group>
        </div>
        <div class="form-item">
          <label class="form-label">汇兑收益科目：
            <el-popover placement="top" :width="300" trigger="click">
              <template #reference>
                <el-icon class="help-icon"><InfoFilled /></el-icon>
              </template>
              <p style="font-size: 13px;">选择汇兑收益对应的会计科目。当外币折算产生收益时，金额记入此科目的贷方。默认科目编码为530105。</p>
            </el-popover>
          </label>
          <el-select
            v-model="form.incomeSubject"
            placeholder="请选择科目"
            filterable
            style="width: 300px; border-radius: 0;"
          >
            <el-option
              v-for="item in subjectList"
              :key="item.subjectId"
              :label="`${item.subjectCode} ${item.subjectName}`"
              :value="item.subjectId"
            />
          </el-select>
        </div>
        <div class="form-item">
          <label class="form-label">汇兑损失科目：
            <el-popover placement="top" :width="300" trigger="click">
              <template #reference>
                <el-icon class="help-icon"><InfoFilled /></el-icon>
              </template>
              <p style="font-size: 13px;">选择汇兑损失对应的会计科目。当外币折算产生损失时，金额记入此科目的借方。默认科目编码为560301。</p>
            </el-popover>
          </label>
          <el-select
            v-model="form.lossSubject"
            placeholder="请选择科目"
            filterable
            style="width: 300px; border-radius: 0;"
          >
            <el-option
              v-for="item in subjectList"
              :key="item.subjectId"
              :label="`${item.subjectCode} ${item.subjectName}`"
              :value="item.subjectId"
            />
          </el-select>
        </div>
        <div class="form-item">
          <el-checkbox v-model="form.transferByDirection">结转方式：按科目方向调汇</el-checkbox>
          <el-popover placement="top" :width="300" trigger="click">
            <template #reference>
              <el-icon class="help-icon"><InfoFilled /></el-icon>
            </template>
            <p style="font-size: 13px;">勾选后，系统会根据科目的默认余额方向（借方/贷方）确定调汇方向。不勾选则按金额的正数方向调汇。</p>
          </el-popover>
        </div>
      </div>

    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { InfoFilled } from '@element-plus/icons-vue'
import { addFinClosingTemplate, updateFinClosingTemplate } from '@/api/finance/closing_template.js'
import { listTemplateItem, updateTemplateItem, addTemplateItem } from '@/api/finance/closing_template_item.js'
import { listVoucherTypes } from '@/api/finance/voucher_type'
import { listAll } from '@/api/finance/subjects.js'
import finStore from '@/hooks/store/useFinanceStore.js'

const props = defineProps({
  selectedTemplate: {
    type: Object,
    default: null
  }
})

const emit = defineEmits(['save', 'cancel'])

const saving = ref(false)
const voucherTypeList = ref([])
const subjectList = ref([])

const form = reactive({
  voucherType: '记',
  voucherDate: new Date().toISOString().split('T')[0],
  summary: '结转汇兑损益',
  voucherClassify: '1',
  incomeSubject: null,
  lossSubject: null,
  transferByDirection: true
})

// 加载凭证字列表
async function loadVoucherTypes() {
  try {
    const groupid = await finStore.getCurrentTenantId()
    const res = await listVoucherTypes({ groupid, pageNum: 1, pageSize: 100 })
    voucherTypeList.value = res.rows || []
  } catch (error) {
    console.error('加载凭证字列表失败', error)
  }
}

// 默认科目编码配置
const defaultSubjectCodes = {
  incomeSubject: '530105',
  lossSubject: '560301'
}

// 加载会计科目列表
async function loadSubjects() {
  try {
    const groupid = await finStore.getCurrentTenantId()
    const res = await listAll({ groupid, status: 1 })
    if (res.data && res.data.length > 0) {
      subjectList.value = res.data
      
      // 科目加载完成后，根据编码设置默认值（仅在没有已保存配置时）
      if (!props.selectedTemplate || !props.selectedTemplate.id) {
        setDefaultSubjects()
      }
    }
  } catch (error) {
    console.error('加载科目列表失败', error)
  }
}

// 根据编码设置默认科目
function setDefaultSubjects() {
  for (const [field, code] of Object.entries(defaultSubjectCodes)) {
    const subject = subjectList.value.find(item => item.subjectCode === code)
    if (subject && !form[field]) {
      form[field] = subject.subjectId
    }
  }
}

// 如果有默认科目编码但没有找到对应ID，尝试设置默认值
function trySetDefaultSubject(field, code) {
  if (!form[field] && subjectList.value.length > 0) {
    const subject = subjectList.value.find(item => item.subjectCode === code)
    if (subject) {
      form[field] = subject.subjectId
    }
  }
}

// 加载已有的模板数据
const loadExistingTemplateItems = async () => {
  if (!props.selectedTemplate || !props.selectedTemplate.id) {
    ElMessage.warning('请选择模板')
    return
  }
  
  const tpl = props.selectedTemplate

  // 更新表单基础数据
  if (tpl.voucherType) {
    form.voucherType = tpl.voucherType
  }
  if (tpl.voucherDate) {
    form.voucherDate = tpl.voucherDate
  }
  if (tpl.voucherClass !== undefined && tpl.voucherClass !== null) {
    form.voucherClassify = String(tpl.voucherClass === 1 ? '1' : '2')
  }
  if (tpl.carryoverType !== undefined && tpl.carryoverType !== null) {
    form.transferByDirection = tpl.carryoverType === 1
  }

  // 从后端加载模板的 item 数据
  try {
    const res = await listTemplateItem({ closingTemplateId: tpl.id })
    if (res && res.data && res.data.length > 0) {
      for (const item of res.data) {
        if (item.amountField === 'income') {
          form.incomeSubject = item.subjectId
          form.summary = item.summary || form.summary
        } else if (item.amountField === 'loss') {
          form.lossSubject = item.subjectId
        }
      }
    }
  } catch (error) {
    console.error('加载模板明细数据失败', error)
  }
  
  // 如果科目仍然为空，设置默认值
  trySetDefaultSubject('incomeSubject', defaultSubjectCodes.incomeSubject)
  trySetDefaultSubject('lossSubject', defaultSubjectCodes.lossSubject)
}

// 保存模板
const handleBatchSave = async () => {
  saving.value = true
  try {
    if (!props.selectedTemplate || !props.selectedTemplate.name) {
      ElMessage.warning('请选择模板')
      saving.value = false
      return
    }
    
    const templateData = {
      name: props.selectedTemplate.name,
      ftype: props.selectedTemplate.ftype,
      groupid: await finStore.getCurrentTenantId(),
      voucherType: form.voucherType,
      carryoverType: form.transferByDirection ? 1 : 0,
      voucherClass: form.voucherClassify === '1' ? 1 : 0
    }
    
    let response
    if (props.selectedTemplate.id) {
      templateData.id = props.selectedTemplate.id
      response = await updateFinClosingTemplate(templateData)
    } else {
      response = await addFinClosingTemplate(templateData)
    }
    
    if (response.code === 200) {
      const templateId = props.selectedTemplate.id || response.data.id
      
      // 查询已有的 item 数据
      let existingItems = []
      try {
        const res = await listTemplateItem({ closingTemplateId: templateId })
        if (res && res.data) {
          existingItems = res.data
        }
      } catch (e) {
        console.error('查询已有item失败', e)
      }
      
      // 准备要保存的 item 数据
      const itemsToSave = [
        {
          amountField: 'income',
          summary: form.summary,
          subjectId: form.incomeSubject,
          direction: 1
        },
        {
          amountField: 'loss',
          summary: form.summary,
          subjectId: form.lossSubject,
          direction: 2
        }
      ]
      
      // 保存每个 item
      for (const itemData of itemsToSave) {
        // 查找已有的 item
        const existingItem = existingItems.find(e => e.amountField === itemData.amountField)
        
        if (existingItem) {
          // 更新已有 item
          await updateTemplateItem({
            id: existingItem.id,
            closingTemplateId: templateId,
            summary: itemData.summary,
            subjectId: itemData.subjectId,
            direction: itemData.direction,
            amountField: itemData.amountField
          })
        } else {
          // 新增 item
          await addTemplateItem({
            closingTemplateId: templateId,
            summary: itemData.summary,
            subjectId: itemData.subjectId,
            direction: itemData.direction,
            amountField: itemData.amountField
          })
        }
      }
      
      ElMessage.success('保存成功')
      emit('save')
    } else {
      ElMessage.error('保存失败')
    }
  } catch (error) {
    console.error('保存模板失败', error)
    ElMessage.error('保存失败')
  } finally {
    saving.value = false
  }
}

// 导出方法
defineExpose({
  loadExistingTemplateItems,
  handleBatchSave
})

onMounted(() => {
  loadVoucherTypes()
  loadSubjects()
  if (props.selectedTemplate) {
    loadExistingTemplateItems()
  }
})

// 监听选中模板变化
watch(() => props.selectedTemplate, (newTemplate) => {
  if (newTemplate) {
    loadExistingTemplateItems()
  }
}, { deep: true })
</script>

<style>
.box-card, .box-card .el-card__body{
  padding: 0;
}
</style>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
  padding: 15px;
  border-bottom: 1px solid #ebeef5;
}

.voucher-type-info {
  display: flex;
  align-items: center;
  gap: 15px;
}

.voucher-type-label,
.voucher-date-label {
  font-size: 14px;
  font-weight: 500;
}

.voucher-date-info {
  display: flex;
  align-items: center;
  gap: 5px;
}

.template-content {
  padding: 20px;
}

.form-item {
  margin-bottom: 20px;
  display: flex;
  align-items: center;
}

.form-label {
  width: 120px;
  font-size: 14px;
  font-weight: 500;
  margin-right: 10px;
}

.card-footer {
  padding: 15px;
  border-top: 1px solid #ebeef5;
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.el-radio-group {
  display: flex;
  gap: 20px;
}

.el-checkbox {
  font-size: 14px;
}

.help-icon {
  font-size: 14px;
  color: var(--el-text-color-secondary);
  cursor: pointer;
  margin-left: 4px;
  vertical-align: middle;
}

.help-icon:hover {
  color: #409eff;
}
</style>