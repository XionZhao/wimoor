<template>
  <div class="feishu-table-record">
    <!-- 顶部操作栏 -->
    <div class="top-bar">
      <div class="top-left">
        <el-input v-model="form.name" placeholder="模板名称" clearable size="small" style="width: 200px;"></el-input>
        <el-select v-model="form.voucherType" placeholder="凭证字" size="small" style="width: 80px;">
          <el-option v-for="item in voucherTypeList" :key="item.id" :label="item.name" :value="item.name"></el-option>
        </el-select>
        <el-select v-model="form.country" placeholder="国家" size="small" style="width: 100px;">
          <el-option key="" label="" value=""></el-option>
          <el-option v-for="item in marketList" :key="item.market" :label="item.name" :value="item.market"></el-option>
        </el-select>
      </div>
      <div class="top-right">
        <el-select v-model="selectType" @change="handleTable()" placeholder="选择数据表" size="small" style="width: 180px;">
          <el-option v-for="item in typeList" :key="item.id" :label="item.name" :value="item.id"></el-option>
        </el-select>
        <el-button :icon="Refresh" size="small" @click="loadTypeList()"></el-button>
        <el-button :icon="Setting" size="small" @click="tableDataRef.show()"></el-button>
      </div>
    </div>

    <!-- 步骤指引 -->
    <div class="steps-bar">
      <div class="step-item" :class="{ active: currentStep >= 1 }">
        <span class="step-num">1</span>
        <span class="step-text">选择数据表</span>
      </div>
      <div class="step-line" :class="{ active: currentStep >= 2 }"></div>
      <div class="step-item" :class="{ active: currentStep >= 2 }">
        <span class="step-num">2</span>
        <span class="step-text">设置筛选条件</span>
      </div>
      <div class="step-line" :class="{ active: currentStep >= 3 }"></div>
      <div class="step-item" :class="{ active: currentStep >= 3 }">
        <span class="step-num">3</span>
        <span class="step-text">配置字段映射</span>
      </div>
      <div class="step-line" :class="{ active: currentStep >= 4 }"></div>
      <div class="step-item" :class="{ active: currentStep >= 4 }">
        <span class="step-num">4</span>
        <span class="step-text">配置科目映射</span>
      </div>
    </div>

    <!-- 主内容区：左右分栏 -->
    <div class="main-content">
      <!-- 左侧：配置区域 -->
      <div class="config-panel">
        <!-- 筛选条件 -->
        <div class="section">
          <div class="section-header">
            <span class="section-title">筛选条件
              <el-popover placement="top" :width="350" trigger="click">
                <template #reference>
                  <el-icon class="help-icon"><InfoFilled /></el-icon>
                </template>
                <div>
                  <p style="font-weight: bold; margin-bottom: 8px; color: #303133;">筛选条件说明</p>
                  <p style="margin-bottom: 4px; font-size: 13px;">用于设置从飞书表格中查询数据的过滤规则。</p>
                  <p style="margin-bottom: 4px; font-size: 13px;">例如：只查询"状态"为"已审核"的记录，或只查询金额大于0的记录。</p>
                  <p style="font-size: 13px;">设置后右侧数据预览会自动刷新，显示符合条件的数据。</p>
                  <div style="margin-top: 10px; padding: 8px; background-color: #fef0f0; border-left: 3px solid #f56c6c; border-radius: 2px;">
                    <p style="margin: 0; font-weight: bold; color: #f56c6c; font-size: 13px;">⚠️ 重要提醒：必须筛选公司！</p>
                    <p style="margin: 4px 0 0 0; font-size: 12px; color: #606266;">筛选条件中必须包含公司/组织字段，确保数据对应到正确的公司实体。没有正确筛选公司将导致账务归属错误，造成财务数据混乱，后果非常严重！</p>
                  </div>
                </div>
              </el-popover>
            </span>
          </div>
          <feishu-table-record-filter 
            :fields="filterFields"
            @search="handleSearch"
            :model-value="currentFilter"
            @update:model-value="handleFilterUpdate"
            @filter-generated="handleFilterGenerated" 
          />
        </div>

        <!-- 字段映射配置 -->
        <div class="section">
          <div class="section-header">
            <span class="section-title">字段映射配置
              <el-popover placement="top" :width="350" trigger="click">
                <template #reference>
                  <el-icon class="help-icon"><InfoFilled /></el-icon>
                </template>
                <div>
                  <p style="font-weight: bold; margin-bottom: 8px;">字段映射配置说明</p>
                  <p style="margin-bottom: 4px; font-size: 13px;">将飞书表格中的字段映射到财务凭证的各个字段。</p>
                  <p style="margin-bottom: 4px; font-size: 13px;">需要选择飞书表格中的哪个列对应凭证的摘要、日期、科目和金额。</p>
                  <p style="font-size: 13px;">请先在上方选择数据表，然后从下拉列表中选择对应的飞书字段。</p>
                </div>
              </el-popover>
            </span>
          </div>
          <div class="field-mapping">
            <div class="mapping-row">
              <label class="mapping-label">摘要字段
                <el-popover placement="top" :width="300" trigger="click">
                  <template #reference>
                    <el-icon class="help-icon"><InfoFilled /></el-icon>
                  </template>
                  <p style="font-size: 13px;">选择飞书表格中作为凭证摘要的字段。摘要会显示在生成的会计凭证上，用于说明该笔业务的内容，如"报销差旅费"、"支付供应商货款"等。</p>
                </el-popover>
              </label>
              <el-select 
                v-model="feishuConfig.summaryField"
                filterable
                placeholder="选择摘要字段" 
                size="small"
                style="flex: 1;"
              >
                <el-option value="" label="请选择"></el-option>
                <el-option 
                  v-for="field in fields" 
                  :key="field.field_name" 
                  :label="field.field_name" 
                  :value="field.field_name" 
                />
              </el-select>
            </div>
            <div class="mapping-row">
              <label class="mapping-label">会计时间
                <el-popover placement="top" :width="300" trigger="click">
                  <template #reference>
                    <el-icon class="help-icon"><InfoFilled /></el-icon>
                  </template>
                  <p style="font-size: 13px;">选择飞书表格中记录业务发生日期的字段。系统会根据此日期确定凭证归属的会计期间，并按"汇总类型"设置进行汇总。</p>
                </el-popover>
              </label>
              <el-select
                filterable
                v-model="feishuConfig.voucherDateField" 
                placeholder="选择会计时间字段" 
                size="small"
                style="flex: 1;"
              >
                <el-option value="" label="请选择"></el-option>
                <el-option 
                  v-for="field in fields" 
                  :key="field.field_name" 
                  :label="field.field_name" 
                  :value="field.field_name" 
                />
              </el-select>
            </div>
            <div class="mapping-row">
              <label class="mapping-label">会计科目
                <el-popover placement="top" :width="300" trigger="click">
                  <template #reference>
                    <el-icon class="help-icon"><InfoFilled /></el-icon>
                  </template>
                  <p style="font-size: 13px;">选择飞书表格中记录会计科目的字段。系统会将此字段的值与下方"会计科目-映射配置"中的规则进行匹配，确定最终使用的会计科目。</p>
                </el-popover>
              </label>
              <el-select 
                v-model="feishuConfig.subjectField" 
                placeholder="选择会计科目字段"
                filterable
                size="small"
                style="flex: 1;"
              >
                <el-option value="" label="请选择"></el-option>
                <el-option 
                  v-for="field in fields" 
                  :key="field.field_name" 
                  :label="field.field_name" 
                  :value="field.field_name" 
                />
              </el-select>
            </div>
            <div class="mapping-row">
              <label class="mapping-label">费用字段
                <el-popover placement="top" :width="300" trigger="click">
                  <template #reference>
                    <el-icon class="help-icon"><InfoFilled /></el-icon>
                  </template>
                  <p style="font-size: 13px;">选择飞书表格中记录金额的字段。此字段的值将作为生成凭证的借方或贷方金额。如果金额为空或0，该条记录将被跳过。</p>
                </el-popover>
              </label>
              <el-select 
                v-model="feishuConfig.amountField" 
                placeholder="选择费用字段"
                filterable
                size="small"
                style="flex: 1;"
              >
                <el-option value="" label="请选择"></el-option>
                <el-option 
                  v-for="field in fields" 
                  :key="field.field_name" 
                  :label="field.field_name" 
                  :value="field.field_name" 
                />
              </el-select>
            </div>
            <div class="mapping-row">
              <label class="mapping-label">汇总类型
                <el-popover placement="top" :width="300" trigger="click">
                  <template #reference>
                    <el-icon class="help-icon"><InfoFilled /></el-icon>
                  </template>
                  <p style="font-size: 13px;">设置金额的汇总方式。"按月"表示将同一月份内的所有记录金额合计后生成一张凭证。</p>
                </el-popover>
              </label>
              按日汇总，每个日期生成一张凭证。按月抓取数据。
            </div>
          </div>
        </div>

        <!-- 会计科目-映射配置 -->
        <div class="section">
          <div class="section-header">
            <span class="section-title">会计科目-映射配置
              <el-popover placement="top" :width="350" trigger="click">
                <template #reference>
                  <el-icon class="help-icon"><InfoFilled /></el-icon>
                </template>
                <div>
                  <p style="font-weight: bold; margin-bottom: 8px;">会计科目-映射配置说明</p>
                  <p style="margin-bottom: 4px; font-size: 13px;">配置飞书表格中"会计科目"字段值与系统会计科目的对应关系。</p>
                  <p style="margin-bottom: 4px; font-size: 13px;">每一行代表一条映射规则：当飞书数据匹配"内容配对"规则时，使用对应的借方/贷方科目生成凭证。</p>
                  <p style="font-size: 13px;">"内容配对"支持通配符：* 匹配所有内容，多个值用逗号分隔。</p>
                </div>
              </el-popover>
            </span>
            <el-space>
              <el-dropdown size="small" split-button  type="success" @click="handleExcelCommand('import')"  >
                  <el-icon><Upload /></el-icon> 导入Excel
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item @click="handleExcelCommand('downloadTemplate')" >
                      <el-icon><Download /></el-icon> 下载导入模板
                    </el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
              <el-upload
                ref="uploadRef"
                :auto-upload="false"
                :show-file-list="false"
                :on-change="handleFileChange"
                accept=".xlsx,.xls"
                style="display: none;"
              >
                <el-button ref="importBtnRef" type="success" size="small">
                  <el-icon><Upload /></el-icon> 导入Excel
                </el-button>
              </el-upload>
              <el-button type="primary" size="small" @click="handleAddSubject()">
                <el-icon><Plus /></el-icon> 添加行
              </el-button>
            </el-space>
          </div>
          <el-table :data="reportFields" border size="small"  >
            <el-table-column label="借方科目" min-width="150">
              <template #default="scope">
                <el-select 
                  v-model="scope.row.summary" 
                  placeholder="选择科目" 
                  size="small"
                  style="width: 100%"
                  filterable
                >
                  <el-option
                    v-for="subject in subjects"
                    :key="subject.subjectId"
                    :label="`${subject.subjectCode} ${subject.subjectName}`"
                    :value="subject.subjectId"
                  />
                </el-select>
              </template>
            </el-table-column>
            <el-table-column label="贷方科目" min-width="150">
              <template #default="scope">
                <el-select 
                  v-model="scope.row.subjectId" 
                  placeholder="选择科目" 
                  size="small"
                  style="width: 100%"
                  filterable
                >
                  <el-option
                    v-for="subject in subjects"
                    :key="subject.subjectId"
                    :label="`${subject.subjectCode} ${subject.subjectName}`"
                    :value="subject.subjectId"
                  />
                </el-select>
              </template>
            </el-table-column>
            <el-table-column label="内容配对" min-width="120">
              <template #header>
                <div style="display: flex; align-items: center; gap: 4px;">
                  <span>内容配对</span>
                  <el-tooltip placement="top">
                    <template #content>
                      <div style="max-width: 300px;">
                        <p style="font-weight: bold; margin-bottom: 8px;">通配符规则说明</p>
                        <p style="margin-bottom: 4px;">必须先配置"会计科目字段"</p>
                        <p style="margin-bottom: 4px;">* 匹配所有内容</p>
                        <p style="margin-bottom: 4px;">apple,banana 匹配固定值</p>
                        <p style="margin-bottom: 4px;">*.txt 匹配以.txt结尾</p>
                        <p style="margin-bottom: 4px;">data?.csv 匹配单个字符</p>
                      </div>
                    </template>
                    <el-icon size="14"><InfoFilled /></el-icon>  
                  </el-tooltip>
                </div>
              </template>
              <template #default="scope">
                <el-input v-model="scope.row.amountField" placeholder="输入配对规则" size="small"></el-input>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="60" align="center">
              <template #default="scope">
                <el-button type="danger" link size="small" @click="deleteSubject(scope.row)">
                  <el-icon><Delete /></el-icon>
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </div>

      <!-- 右侧：数据预览 -->
      <div class="preview-panel">
        <div class="section">
          <div class="section-header">
            <span class="section-title">数据预览</span>
            <el-tag v-if="records && records.items" size="small" type="info">
              共 {{ records.items.length }} 条记录
            </el-tag>
          </div>
          <div class="preview-table">
            <el-table 
              v-if="records && records.items" 
              v-loading="loading" 
              :data="records.items" 
              size="small"
              border
              max-height="500"
              style="width: 100%"
            >
              <el-table-column 
                v-for="field in fields"  
                :key="field.field_name"
                :prop="field.field_name" 
                :min-width="getMinColumnWidth(field)"
                show-overflow-tooltip 
                :label="field.field_name" 
              >
                <template #header>
                  <el-tooltip placement="top" :content="field.field_name">
                    <div class="text-omit-1">{{ field.field_name }}</div>
                  </el-tooltip>
                </template>
                <template #default="scope">
                  <div v-if="JSON.stringify(scope.row.fields) !== '{}'">
                    <div :class="field.field_name === formData.feedback ? 'text-success' : ''" v-if="hasFieldValue(scope.row.fields[field.field_name], field)">
                      <template v-if="field.type === 11 || field.ui_type === 'User'">
                        <el-avatar :size="16" :src="getPersonAvatar(scope.row.fields[field.field_name])" style="margin-right: 4px;">
                          {{ getPersonName(scope.row.fields[field.field_name]) }}
                        </el-avatar>
                        <span>{{ getPersonName(scope.row.fields[field.field_name]) }}</span>
                      </template>
                      <template v-else-if="field.type === 5 || field.ui_type === 'DateTime'">
                        {{ renderDateValue(scope.row.fields[field.field_name]) }}
                      </template>
                      <template v-else-if="field.type === 15 || field.ui_type === 'Url'">
                        <el-link v-if="isLinkValue(scope.row.fields[field.field_name])" :href="getLinkUrl(scope.row.fields[field.field_name])" target="_blank">
                          {{ getLinkText(scope.row.fields[field.field_name]) }}
                        </el-link>
                        <span v-else>{{ scope.row.fields[field.field_name] }}</span>
                      </template>
                      <template v-else-if="field.type === 3 || field.ui_type === 'SingleSelect'">
                        <el-tag :type="getTagType(scope.row.fields[field.field_name], field)" size="small">
                          {{ renderFieldValue(scope.row.fields[field.field_name]) }}
                        </el-tag>
                      </template>
                      <template v-else-if="field.type === 13 || field.ui_type === 'MultiSelect' || isArrayValue(scope.row.fields[field.field_name])">
                        <el-tag 
                          v-for="(item, index) in getArrayItems(scope.row.fields[field.field_name])" 
                          :key="index" 
                          :type="getTagTypeForArray(item, field)" 
                          size="small"
                          style="margin-right: 4px;"
                        >
                          {{ item.text || item.name || item }}
                        </el-tag>
                      </template>
                      <template v-else>
                        {{ renderFieldValue(scope.row.fields[field.field_name]) }}
                      </template>
                    </div>
                    <el-button 
                      v-if="field.field_name === formData.feedback && scope.row.record_id && scope.row.fields[field.field_name] !== '已同步'" 
                      size="small" 
                      type="primary" 
                      link
                      @click="handleFeedback(scope.row.record_id)"
                    >回写</el-button>
                  </div>
                  <div v-else>--</div>
                </template>
              </el-table-column>
            </el-table>
            <el-empty v-else description="请选择数据表并设置筛选条件后查询" :image-size="100" />
          </div>
        </div>
      </div>
    </div>

    <TableData ref="tableDataRef"></TableData>
    <AddSubjectDialog
      v-model="addSubjectVisible"
      :subjects="subjects"
      :selected-subject-ids="selectedSubjectIds"
      @confirm="handleAddSubjects"
    />
  </div>
</template>

<script setup>
import TableData from "@/views/finance/periods/components/template/feishu_table_bind.vue";
import FeishuTableRecordFilter from "./feishu_table_record_filter.vue";
import { onMounted, ref, reactive, toRefs, computed, watch } from 'vue';
import { ElMessage } from 'element-plus';
import {Plus, Setting,InfoFilled,Refresh,Delete,Upload,ArrowDown,Download} from '@element-plus/icons-vue'; 
import feishuApi from '@/api/sys/tool/feishuApi.js';
import { listVoucherTypes } from '@/api/finance/voucher_type'
import { updateFinClosingTemplate, getFinClosingTemplate, addFinClosingTemplate } from '@/api/finance/closing_template.js';
import { getFeishuConfigByTemplateId, addFinClosingTemplateFeishu, updateFinClosingTemplateFeishu, insertOrUpdateFeishuConfig } from '@/api/finance/closing_template_feishu.js';
import { addTemplateItem, delTemplateItem, listTemplateItem, updateTemplateItem } from '@/api/finance/closing_template_item.js';
import { listAll as listSubjects } from '@/api/finance/subjects.js';
import finStore from '@/hooks/store/useFinanceStore.js'
import marketApi from '@/api/amazon/market/marketApi.js'
import {Calculator} from "@icon-park/vue-next";
import AddSubjectDialog from "@/views/finance/periods/components/template/add_subject_dialog.vue";
import { ElMessageBox } from 'element-plus';
import * as XLSX from 'xlsx';

const tableDataRef = ref(null);
const uploadRef = ref(null);
const importBtnRef = ref(null);
let currentFilter = ref(null);

// 新增科目对话框显示状态
const addSubjectVisible = ref(false);

// 打开新增科目对话框
const openAddSubjectDialog = () => {
  addSubjectVisible.value = true;
};

// 科目列表
const subjects = ref([]);

// 报告字段列表（存储科目映射关系）
const reportFields = ref([]);

// 已选中的科目ID列表
const selectedSubjectIds = computed(() => {
  return reportFields.value.map(field => field.subjectId);
});

// 定义 props
const props = defineProps({
  selectedTemplate: {
    type: Object,
    default: null
  }
});

// 定义 emit
const emit = defineEmits(['filter-change', 'save']);


let state = reactive({
  typeList: [],
  dialogVisible: false,
  marketList:[],
  formData: { tableUrl: '', tableType: '' },
  fields: [],
  records: {},
  selectType: "",
  dialogRecordVisible: false,
  loading: false,
});

let {
  typeList,
  dialogVisible,
  formData,
  selectType,
  fields,
  records,
  marketList,
  dialogRecordVisible,
  loading,
} = toRefs(state);

// 表单数据（用于模板信息）
const voucherTypeList = ref([])
const form = ref({
  voucherType: '记',
  voucherNo: '001',
  country: null,
  name: ''
})

// 飞书字段映射配置
const feishuConfig = ref({
  summaryField: '',      // 摘要字段
  voucherDateField: '',  // 会计时间字段
  subjectField: '',      // 会计科目字段
  amountField: '',       // 费用字段
  datetype: 1           // 会计日期汇总类型：0按日，1按月，2单笔生成凭证
})

// 用于传递给 filter 组件的字段列表
const filterFields = computed(() => {
  return fields.value || [];
});

// 当前步骤指示
const currentStep = computed(() => {
  if (!selectType.value) return 1;
  if (!currentFilter.value) return 2;
  if (!feishuConfig.value.summaryField || !feishuConfig.value.subjectField) return 3;
  if (reportFields.value.length === 0) return 4;
  return 4;
});

// 获取市场数据
async function getMarketData(){
  const groupid = await finStore.getCurrentTenantId()
		marketApi.getMarketByGroup({'groupid':groupid}).then((res)=>{
        if(res.data&&res.data.length>0){
          state.marketList=res.data;
          form.value.country = "";
        }
		})
}

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

// 获取科目数据
const fetchSubjects = async () => {
  try {
    const groupid = await finStore.getCurrentTenantId()
    const response = await listSubjects({ groupid, status: 1 })
    if (response.code === 200) {
      subjects.value = response.data
    } else {
      ElMessage.error('获取科目数据失败')
    }
  } catch (error) {
    ElMessage.error('获取科目数据失败')
  }
}

// 处理 Filter 更新
function handleFilterUpdate(filter) {
  currentFilter.value = filter;
}

// 处理 Filter 生成
function handleFilterGenerated(filter) {
  currentFilter.value = filter;
  ElMessage.success('筛选条件已设置');
  // 保存 filter 到后端
  saveFilterToBackend(filter);
  // 重新查询数据
  handleRecord();
}
function handleSearch(){
  handleRecord();
}
// 加载模板字段列表
const loadReportFields = async () => {
  console.log('loadReportFields 开始执行');
  console.log('selectedTemplate:', props.selectedTemplate);

  if (!props.selectedTemplate || !props.selectedTemplate.id) {
    reportFields.value = [];
    currentFilter.value = null;
    console.log('selectedTemplate 为空或没有 id，清空 reportFields 和 filter');
    return;
  }

  try {
    console.log('调用 listTemplateItem，参数:', { closingTemplateId: props.selectedTemplate.id });
    const response = await listTemplateItem({ closingTemplateId: props.selectedTemplate.id });
    console.log('listTemplateItem 返回:', response);

    if (response && response.code === 200) {
      const data = response.rows || response.data || [];
      reportFields.value = data;
      console.log('reportFields 设置为:', data);
      if (!data || data.length === 0) {
        currentFilter.value = null;
        console.log('reportFields 为空，清空 filter');
      }
    } else {
      reportFields.value = [];
      currentFilter.value = null;
      console.log('response.code 不是 200，清空 reportFields 和 filter');
    }
  } catch (error) {
    console.error('加载模板字段失败:', error);
    reportFields.value = [];
    currentFilter.value = null;
  }
};

// 处理新增科目（点击添加按钮）
const handleAddSubject = (row) => {
  if (row && row.id) {
    const index = reportFields.value.findIndex(item => item.id === row.id);
    if (index > -1) {
      reportFields.value.splice(index + 1, 0, {
        summary: '',
        subjectId: '',
        direction: '',
        closingTemplateId: props.selectedTemplate.id,
      });
    } else {
      reportFields.value.push({
        summary: '',
        subjectId: '',
        direction: '',
        closingTemplateId: props.selectedTemplate.id,
      });
    }
  } else {
    reportFields.value.push({
      summary: '',
      subjectId: '',
      direction: '',
      closingTemplateId: props.selectedTemplate.id,
    });
  }
};

// 处理批量新增科目（从弹窗选择）
const handleAddSubjects = async () => {
  reportFields.value.push({
    summary: '',
    subjectId: '',
    direction: '',
    closingTemplateId: props.selectedTemplate.id,
  });

};

// 处理Excel下拉菜单命令
const handleExcelCommand = (command) => {
  if (command === 'import') {
    // 触发文件选择
    uploadRef.value?.$el?.querySelector('input')?.click();
  } else if (command === 'downloadTemplate') {
    // 下载Excel模板
    downloadExcelTemplate();
  }
};

// 下载Excel导入模板（使用exceljs，支持隐藏式批注）
const downloadExcelTemplate = async () => {
  try {
    const ExcelJS = await import('exceljs');
    const workbook = new ExcelJS.Workbook();
    const ws = workbook.addWorksheet('科目映射模板');

    // 设置表头
    ws.addRow(['借方科目', '贷方科目', '内容配对']);
    // 示例数据
    // ws.addRow(['1001 库存现金', '6001 主营业务收入', '*']);
    // ws.addRow(['1002 银行存款', '6001 主营业务收入', 'apple,banana']);
    // ws.addRow(['6602 管理费用', '2202 应付账款', '*.txt']);

    // 设置列宽
    ws.getColumn(1).width = 30;
    ws.getColumn(2).width = 30;
    ws.getColumn(3).width = 25;

    // 表头加粗
    ws.getRow(1).font = { bold: true };

    // 设置隐藏式批注（鼠标悬停才显示）
    ws.getCell('A1').note = '输入借方科目编码';
    ws.getCell('B1').note = '输入贷方科目编码';
    ws.getCell('C1').note = '请查看页面字段内容提示';

    // 下载文件
    const buffer = await workbook.xlsx.writeBuffer();
    const blob = new Blob([buffer], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = '会计科目映射导入模板.xlsx';
    link.click();
    URL.revokeObjectURL(url);
    ElMessage.success('模板下载成功');
  } catch (error) {
    console.error('模板下载失败:', error);
    ElMessage.error('模板下载失败');
  }
};

// 处理Excel文件导入
const handleFileChange = (file) => {
  const reader = new FileReader();
  reader.onload = (e) => {
    try {
      const data = new Uint8Array(e.target.result);
      const workbook = XLSX.read(data, { type: 'array' });
      const firstSheet = workbook.Sheets[workbook.SheetNames[0]];
      const jsonData = XLSX.utils.sheet_to_json(firstSheet, { header: 1 });
      
      if (jsonData.length === 0) {
        ElMessage.error('Excel文件为空');
        return;
      }
      
      // 验证表头
      const headers = jsonData[0];
      const expectedHeaders = ['借方科目', '贷方科目', '内容配对'];
      const headerValid = expectedHeaders.every((header, index) => 
        headers[index] && headers[index].toString().trim() === header
      );
      
      if (!headerValid) {
        ElMessage.error('Excel表头格式错误，应为：借方科目、贷方科目、内容配对');
        return;
      }
      
      // 处理数据行
      const importedData = [];
      for (let i = 1; i < jsonData.length; i++) {
        const row = jsonData[i];
        if (row && row.length >= 3) {
          const debitSubjectName = row[0] ? row[0].toString().trim() : '';
          const creditSubjectName = row[1] ? row[1].toString().trim() : '';
          const matchRule = row[2] ? row[2].toString().trim() : '';
          
          if (debitSubjectName || creditSubjectName || matchRule) {
            // 查找借方科目ID
            const debitSubject = subjects.value.find(s => 
              s.subjectName === debitSubjectName || 
              s.subjectCode === debitSubjectName ||
              `${s.subjectCode} ${s.subjectName}` === debitSubjectName
            );
            
            // 查找贷方科目ID
            const creditSubject = subjects.value.find(s => 
              s.subjectName === creditSubjectName || 
              s.subjectCode === creditSubjectName ||
              `${s.subjectCode} ${s.subjectName}` === creditSubjectName
            );
            
            importedData.push({
              summary: debitSubject ? debitSubject.subjectId : '',
              subjectId: creditSubject ? creditSubject.subjectId : '',
              amountField: matchRule,
              closingTemplateId: props.selectedTemplate.id,
            });
          }
        }
      }
      
      if (importedData.length === 0) {
        ElMessage.warning('Excel中没有有效数据');
        return;
      }
      
      // 添加到现有数据
      reportFields.value = [...reportFields.value, ...importedData];
      ElMessage.success(`成功导入 ${importedData.length} 条映射配置`);
      
    } catch (error) {
      console.error('Excel解析失败:', error);
      ElMessage.error('Excel文件解析失败，请检查文件格式');
    }
  };
  reader.readAsArrayBuffer(file.raw);
};

// 删除科目
const deleteSubject = (subject) => {
  ElMessageBox.confirm('确定要删除这个科目吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      const index = reportFields.value.findIndex(item => item.id === subject.id);
      if (index > -1) {
        const field = reportFields.value[index];
        if (field.id) {
          await delTemplateItem(field.id);
        }
        reportFields.value.splice(index, 1);
        ElMessage.success('删除成功');
      }
    } catch (error) {
      console.error('删除失败', error);
      ElMessage.error('删除失败');
    }
  }).catch(() => {
    // 取消删除
  });
};

// 保存 filter 到后端
function saveFilterToBackend(filter) {
  if (!props.selectedTemplate || !props.selectedTemplate.id) {
    return;
  }
  
  const filterJson = JSON.stringify(filter);
  
  // 保存到飞书配置表
  saveFeishuConfigToBackend(filterJson);
}

// 保存飞书配置到 fin_closing_template_feishu 表
async function saveFeishuConfigToBackend(filterJson) {
  if (!props.selectedTemplate || !props.selectedTemplate.id) {
    ElMessage.warning('请选择模板');
    return;
  }
  
  const feishuConfigData = {
    templateid: props.selectedTemplate.id,
    feishuTableId: selectType.value ? parseInt(selectType.value) : null,
    filter: filterJson,
    summaryField: feishuConfig.value.summaryField || '',
    voucherDateField: feishuConfig.value.voucherDateField || '',
    subjectField: feishuConfig.value.subjectField || '',
    amountField: feishuConfig.value.amountField || '',
    datetype: feishuConfig.value.datetype || 0
  };
  
  console.log('保存飞书配置数据:', feishuConfigData);
  
  // 先查询是否已存在配置
  getFeishuConfigByTemplateId(props.selectedTemplate.id).then(res => {
    if (res.code === 200 && res.data) {
      // 已存在，更新
      updateFinClosingTemplateFeishu(feishuConfigData).then(res => {
        if (res.code === 200) {
          ElMessage.success('飞书配置保存成功');
        } else {
          ElMessage.error('飞书配置保存失败: ' + (res.msg || '未知错误'));
        }
      }).catch(err => {
        console.error('飞书配置保存失败:', err);
        ElMessage.error('飞书配置保存失败');
      });
    } else {
      // 不存在，新增
      addFinClosingTemplateFeishu(feishuConfigData).then(res => {
        if (res.code === 200) {
          ElMessage.success('飞书配置保存成功');
        } else {
          ElMessage.error('飞书配置保存失败: ' + (res.msg || '未知错误'));
        }
      }).catch(err => {
        console.error('飞书配置保存失败:', err);
        ElMessage.error('飞书配置保存失败');
      });
    }
  }).catch(err => {
    console.error('查询飞书配置失败:', err);
    ElMessage.error('飞书配置保存失败');
  });
}

// 批量保存模板信息（包含名称、凭证字、国家等）
const handleBatchSave = async () => {
  if (!props.selectedTemplate || !props.selectedTemplate.id) {
    ElMessage.warning('请选择模板');
    return;
  }
  
  try {
    const filterJson = currentFilter.value ? JSON.stringify(currentFilter.value) : null;
    
    const templateData = {
      id: props.selectedTemplate.id,
      name: form.value.name,
      ftype: props.selectedTemplate.ftype,
      groupid: await finStore.getCurrentTenantId(),
      country: form.value.country,
      voucherType: form.value.voucherType
    };
    
    let templateResponse;
    if (props.selectedTemplate.id) {
      templateResponse = await updateFinClosingTemplate(templateData);
    } else {
      templateResponse = await addFinClosingTemplate(templateData);
    }
    
    if (templateResponse.code === 200) {
      // 保存科目映射关系
      await saveSubjectGroups();
      
      // 保存到飞书配置表
      saveFeishuConfigToBackend(filterJson);
      ElMessage.success('保存成功');
      emit('save', templateData);
    } else {
      ElMessage.error('保存失败');
    }
  } catch (error) {
    console.error('保存失败', error);
    ElMessage.error('保存失败');
  }
};

// 保存科目映射关系
const saveSubjectGroups = async () => {
  if (!props.selectedTemplate || !props.selectedTemplate.id) {
    return;
  }
  
  const now = new Date().toISOString();
  const errors = [];
  
  // 直接从 reportFields 读取数据，确保获取到最新的用户输入
  for (const field of reportFields.value) {
    const templateItem = {
      id: field.id,
      subjectId: field.subjectId,
      amountField: field.amountField || '',
      summary: field.summary || '',
      direction: field.direction,
      closingTemplateId: props.selectedTemplate.id,
      updatedTime: now
    };
    
    console.log('保存科目映射:', templateItem);
    
    try {
      if (field.id) {
        // 修改
        await updateTemplateItem(templateItem);
      } else {
        // 新增
        const response = await addTemplateItem(templateItem);
        if (response.code === 200 && response.data) {
          // 确保获取到新插入记录的ID
          const newId = response.data.id || response.data;
          if (newId) {
            field.id = String(newId);
            console.log('新增科目映射成功，ID:', field.id);
          }
        }
      }
    } catch (error) {
      console.error('保存科目映射失败:', error);
      errors.push(error);
    }
  }
  
  if (errors.length > 0) {
    throw new Error(`保存科目映射失败: ${errors.length} 条记录保存失败`);
  }
};

// 处理记录查询
function handleRecord() {
  // 检查 formData 是否存在
  if (!state.formData) {
    console.warn('formData 为空，跳过请求');
    return;
  }
  
  const params = {
    url: state.formData.url || state.formData.tableUrl
  };
  
  // 如果有 filter，添加到请求参数中
  if (currentFilter.value) {
    params.filter = currentFilter.value;
  }else{
    params.filter = {"conjunction":"and","conditions":[]};
  }

  // 检查 url 是否存在
  if (!params.url) {
    console.warn('url 为空，跳过请求');
    return;
  }
  
  // 检查是否正在加载中，避免重复请求
  if (state.loading) {
    console.warn('正在加载中，跳过重复请求');
    return;
  }
  
  state.loading = true;
  feishuApi.getRecord(params).then(res => {
    state.loading = false;
    if (res.code === 200) {
      try {
        // 尝试解析数据
        state.records = typeof res.data === 'string' ? JSON.parse(res.data) : res.data;
          // 检查是否有 items
        if (!state.records) {
          state.records = { items: [] };
          return;
        }
        // 检查是否有 items
        if (!state.records.items || !Array.isArray(state.records.items)) {
          state.records = { items: [] };
          return;
        }
        
        // 去掉records中为空的行
        state.records.items = state.records.items.filter(item => {
          return item && item.fields && Object.keys(item.fields).length !== 0;
        });
        
      } catch (e) {
        console.error('数据解析失败:', e);
        state.records = { items: [] };
      }
    } else {
      console.warn('获取数据失败:', res.message);
      state.records = { items: [] };
    }
  }).catch(err => {
    console.error('请求失败:', err);
    state.records = { items: [] };
    state.loading = false;
  });
}

function handleTable() {
  let item = null;
  state.typeList.forEach(typeItem => {
    if (typeItem.id === state.selectType) {
      item = typeItem;
    }
  });
  
  // 检查 item 是否存在
  if (!item) {
    console.warn('未找到对应的表格类型');
    return;
  }
  
  state.formData = item;
  
  // 安全解析 fieldjson
  try {
    let fieldJson = JSON.parse(item.fieldjson);
    fields.value = fieldJson != null && fieldJson.items != null ? fieldJson.items : [];
  } catch (e) {
    console.error('解析 fieldjson 失败:', e);
    fields.value = [];
  }
  
  // 只在有有效 url 时调用 handleRecord
  if (item.url || item.tableUrl) {
    handleRecord();
  }
}

function loadTypeList() {
  state.loading = true;
  feishuApi.getTypeList()
      .then(res => {
        if (res.code === 200) {
          const data = res.data || [];
          // 使用 splice 保持响应式引用
          state.typeList.splice(0, state.typeList.length, ...data);
          state.formData = data[0];
          
          // 检查 selectType 是否还在新的 typeList 中
          const isSelectTypeExists = data.some(item => item.id === state.selectType);
          if (!isSelectTypeExists) {
            // 如果 selectType 不在列表中，清空它并选择第一个元素
            state.selectType = data.length > 0 ? data[0].id : '';
          } else if (!state.selectType && data.length > 0) {
            // 如果 selectType 为空且列表不为空，选择第一个元素
            state.selectType = data[0].id;
          }
          
          handleTable();
        } else {
          ElMessage.error(res.message || '获取数据表类型列表失败');
          state.typeList.splice(0, state.typeList.length);
        }
      })
      .catch(err => {
        ElMessage.error('网络错误，请稍后重试');
        state.typeList.splice(0, state.typeList.length);
      })
      .finally(() => {
        state.loading = false;
      });
}

// 加载已有的模板数据
const loadExistingTemplateItems = async () => {
  if (!props.selectedTemplate || !props.selectedTemplate.id) {
    return;
  }

  // 初始化状态
  currentFilter.value = null;
  reportFields.value = [];
  feishuConfig.value = {
    summaryField: '',
    voucherDateField: '',
    subjectField: '',
    amountField: '',
    datetype: 0
  };

  form.value.name = props.selectedTemplate.name || '';
  if (props.selectedTemplate.voucherType) {
    form.value.voucherType = props.selectedTemplate.voucherType;
  }
  if (props.selectedTemplate.country) {
    form.value.country = props.selectedTemplate.country;
  }

  // 加载模板字段列表
  await loadReportFields();

  // 等待 typeList 加载完成
  while (typeList.value.length === 0) {
    await new Promise(resolve => setTimeout(resolve, 100));
  }

  // 从飞书配置表加载配置
  try {
    const feishuConfigRes = await getFeishuConfigByTemplateId(props.selectedTemplate.id);
    if (feishuConfigRes.code === 200 && feishuConfigRes.data) {
      // 加载 filter
      if (feishuConfigRes.data.filter) {
        try {
          currentFilter.value = JSON.parse(feishuConfigRes.data.filter);
        } catch (e) {
          currentFilter.value = null;
        }
      }else{
        currentFilter.value = null;
      }
      // 加载飞书表格ID
      console.log('飞书配置返回的 feishuTableId:', feishuConfigRes.data.feishuTableId);
      console.log('typeList:', typeList.value);
      if (feishuConfigRes.data.feishuTableId) {
        selectType.value = String(feishuConfigRes.data.feishuTableId);
        console.log('设置 selectType 为:', selectType.value);
        handleTable();
      }
      // 加载字段映射配置
      if (feishuConfigRes.data.summaryField) {
        feishuConfig.value.summaryField = feishuConfigRes.data.summaryField;
      }
      if (feishuConfigRes.data.voucherDateField) {
        feishuConfig.value.voucherDateField = feishuConfigRes.data.voucherDateField;
      }
      if (feishuConfigRes.data.subjectField) {
        feishuConfig.value.subjectField = feishuConfigRes.data.subjectField;
      }
      if (feishuConfigRes.data.amountField) {
        feishuConfig.value.amountField = feishuConfigRes.data.amountField;
      }
      if (feishuConfigRes.data.datetype !== undefined) {
        feishuConfig.value.datetype = feishuConfigRes.data.datetype;
      }
      return;
    }else{
        currentFilter.value = null;
    }
  } catch (e) {
    console.error('加载飞书配置失败:', e);
  }
  
  // 如果飞书配置表没有，从 template 对象中获取
  if (props.selectedTemplate.filter) {
    try {
      currentFilter.value = JSON.parse(props.selectedTemplate.filter);
    } catch (e) {
      currentFilter.value = null;
    }
  } else {
    currentFilter.value = null;
  }
  
  // 如果设置了表格类型，触发数据加载
  if (selectType.value) {
    handleTable();
  }
}

// 导出方法
defineExpose({
  handleBatchSave,
  loadExistingTemplateItems
})

onMounted(() => {
  loadTypeList();
  getMarketData();
  fetchSubjects();
  loadVoucherTypes();
});

// 监听 selectedTemplate 变化
watch(() => props.selectedTemplate, () => {
  loadExistingTemplateItems();
}, { deep: true });

// 根据飞书字段类型渲染不同的显示方式
function renderFieldValue(value, field) {
  if (!value) {
    return '';
  }

  // 公式/错误类型 (type:1) 且 value 是数组结构，如 {"type":1,"value":[{"text":"存在错误","type":"text"}]}
  if (typeof value === 'object' && value !== null && value.type === 1 && Array.isArray(value.value) && value.value.length > 0) {
    const firstItem = value.value[0];
    if (firstItem && firstItem.text) {
      return firstItem.text;
    }
  }

  // 日期类型：时间戳转换
  if (typeof value === 'number' && value > 1e10) {
    const date = new Date(value);
    return formatDate(date);
  }

  // 人员类型：数组，显示人员名称
  if (Array.isArray(value) && value.length > 0) {
    const person = value[0];
    if (person && person.name) {
      return person.name;
    }
    return JSON.stringify(value);
  }

  // 附件/链接类型：包含 link 和 text
  if (typeof value === 'object' && value !== null) {
    if (value.text) {
      return value.text;
    }
    if (value.link) {
      return '链接';
    }
    return JSON.stringify(value);
  }

  // 文本类型：直接返回
  return value;
}

// 判断是否为人员字段
function isPersonField(value) {
  if (!value) {
    return false;
  }
  
  // 人员字段是数组，且包含 name 属性
  if (Array.isArray(value) && value.length > 0) {
    const person = value[0];
    return person && (person.name || person.avatar_url);
  }
  
  return false;
}

// 获取人员头像
function getPersonAvatar(value) {
  if (!value || !Array.isArray(value) || value.length === 0) {
    return '';
  }
  const person = value[0];
  return person.avatar_url || '';
}

// 获取人员名称
function getPersonName(value) {
  if (!value || !Array.isArray(value) || value.length === 0) {
    return '';
  }
  const person = value[0];
  return person.name || '';
}

// 渲染日期值
function renderDateValue(value) {
  if (!value) {
    return '';
  }

  // 时间戳格式（毫秒）
  if (typeof value === 'number') {
    const date = new Date(value);
    return formatDate(date);
  }

  return value;
}

// 判断字段是否有值（用于控制不显示空值）
function hasFieldValue(value, field) {
  if (!value) {
    return false;
  }

  // 公式/错误类型 (type:1)
  if (typeof value === 'object' && value !== null && value.type === 1 && Array.isArray(value.value) && value.value.length > 0) {
    const firstItem = value.value[0];
    return !!(firstItem && firstItem.text);
  }

  // 日期类型
  if (typeof value === 'number' && value > 1e10) {
    return true;
  }

  // 数组类型（包括人员类型和普通数组，如[{"text":"YS项目部","type":"text"}]）
  if (Array.isArray(value) && value.length > 0) {
    const firstItem = value[0];
    return !!(firstItem && (firstItem.name || firstItem.avatar_url || firstItem.text));
  }

  // 链接类型
  if (typeof value === 'object' && value !== null) {
    if (value.link || value.text) {
      return true;
    }
    return false;
  }

  // 字符串空值检查
  if (typeof value === 'string' && value.trim() === '') {
    return false;
  }

  return true;
}

// 判断是否为链接值
function isLinkValue(value) {
  if (!value || typeof value !== 'object') {
    return false;
  }
  return value.link !== undefined;
}

// 获取链接URL
function getLinkUrl(value) {
  if (!value || typeof value !== 'object') {
    return '';
  }
  return value.link || '';
}

// 获取链接文本
function getLinkText(value) {
  if (!value || typeof value !== 'object') {
    return '';
  }
  return value.text || '链接';
}

// 渲染数组值（用于多选字段）
function renderArrayValue(value) {
  if (!value || !Array.isArray(value)) {
    return '';
  }
  
  // 如果是人员类型的数组
  if (value.length > 0 && value[0].name) {
    return value.map(item => item.name).join(', ');
  }
  
  // 如果是单选/多选选项类型的数组
  if (value.length > 0 && value[0].text) {
    return value.map(item => item.text).join(', ');
  }
  
  return value.join(', ');
}

// 根据字段配置获取 tag 类型
function getTagType(value, field) {
  if (!value || !field || !field.property || !field.property.options) {
    return '';
  }
  
  // 查找当前值对应的选项
  const options = field.property.options;
  let foundOption = null;
  
  if (Array.isArray(value)) {
    // 如果值是数组（多选），取第一个
    if (value.length > 0 && value[0].text) {
      foundOption = options.find(opt => opt.name === value[0].text);
    }
  } else if (typeof value === 'object' && value.text) {
    // 如果值是对象（单选）
    foundOption = options.find(opt => opt.name === value.text);
  } else {
    // 普通文本值
    foundOption = options.find(opt => opt.name === value);
  }
  
  // 根据颜色值返回对应的 tag 类型
  if (foundOption) {
    return getColorTagType(foundOption.color);
  }
  
  return '';
}

// 根据飞书颜色值返回 Element Plus tag 类型
function getColorTagType(color) {
  // 飞书颜色值对应关系：
  // 0: 灰色, 1: 红色, 2: 橙色, 3: 黄色, 4: 绿色, 5: 蓝色, 6: 紫色, 7: 粉色
  const colorMap = {
    0: '',        // 灰色 -> 默认
    1: 'danger',  // 红色 -> danger
    2: 'warning', // 橙色 -> warning
    3: 'warning', // 黄色 -> warning
    4: 'success', // 绿色 -> success
    5: 'primary', // 蓝色 -> primary
    6: '',        // 紫色 -> 默认
    7: ''         // 粉色 -> 默认
  };
  return colorMap[color] || '';
}

// 判断是否为数组值
function isArrayValue(value) {
  return Array.isArray(value) && value.length > 0;
}

// 根据字段类型获取列宽度
function getColumnWidth(field) {
  if (!field) return '';
  
  // 根据字段类型设置不同宽度
  const widthMap = {
    // 人员字段 - 需要显示头像和名称
    11: '150',           // User
    // 日期字段 - 需要显示完整日期时间
    5: '180',            // DateTime
    // 链接字段 - 可能较长
    15: '200',           // Url
    // 单选/多选字段 - 根据选项内容
    3: '',               // SingleSelect - 自适应
    13: '200',           // MultiSelect - 可能有多个选项
    // 数字字段 - 通常较短
    2: '100',            // Number
    // 复选框
    7: '80',             // Checkbox
    // 自动编号
    17: '120',           // AutoNumber
  };
  
  // 也可以根据 ui_type 判断
  const uiTypeWidthMap = {
    'User': '150',
    'DateTime': '180',
    'Url': '200',
    'SingleSelect': '',
    'MultiSelect': '200',
    'Text': '',
    'LongText': '300',
    'Number': '100',
    'Checkbox': '80',
  };
  
  // 优先使用 ui_type
  if (field.ui_type && uiTypeWidthMap[field.ui_type]) {
    return uiTypeWidthMap[field.ui_type];
  }
  
  // 其次使用 type
  if (field.type && widthMap[field.type]) {
    return widthMap[field.type];
  }
  
  // 默认不设置宽度，让表格自适应
  return '';
}

// 根据字段类型获取最小列宽度
function getMinColumnWidth(field) {
  if (!field) return '80';
  
  // 根据字段类型设置最小宽度
  const minWidthMap = {
    1: '120',            // Text
    2: '80',             // Number
    3: '100',            // SingleSelect
    5: '150',            // DateTime
    7: '60',             // Checkbox
    11: '120',           // User
    13: '150',           // MultiSelect
    15: '150',           // Url
    17: '100',           // AutoNumber
  };
  
  const uiTypeMinWidthMap = {
    'Text': '120',
    'LongText': '200',
    'Number': '80',
    'SingleSelect': '100',
    'DateTime': '150',
    'Checkbox': '60',
    'User': '120',
    'MultiSelect': '150',
    'Url': '150',
    'AutoNumber': '100',
  };
  
  // 优先使用 ui_type
  if (field.ui_type && uiTypeMinWidthMap[field.ui_type]) {
    return uiTypeMinWidthMap[field.ui_type];
  }
  
  // 其次使用 type
  if (field.type && minWidthMap[field.type]) {
    return minWidthMap[field.type];
  }
  
  // 默认最小宽度
  return '100';
}

// 获取数组项
function getArrayItems(value) {
  if (!Array.isArray(value)) {
    return [];
  }
  return value;
}

// 为数组项获取对应的 tag 类型
function getTagTypeForArray(item, field) {
  if (!field || !field.property || !field.property.options) {
    return '';
  }
  
  const options = field.property.options;
  const itemText = item.text || item.name || item;
  const foundOption = options.find(opt => opt.name === itemText);
  
  if (foundOption) {
    return getColorTagType(foundOption.color);
  }
  
  return '';
}

// 格式化日期
function formatDate(date) {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  const hours = String(date.getHours()).padStart(2, '0');
  const minutes = String(date.getMinutes()).padStart(2, '0');
  const seconds = String(date.getSeconds()).padStart(2, '0');
  return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`;
}
</script>

<style>
.box-card .el-card__body {
  padding: 0;
}
</style>
<style scoped>
.feishu-table-record {
  padding: 0;
  height: 100%;
  display: flex;
  flex-direction: column;
}

/* 顶部操作栏 */
.top-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background: #fff;
  border-bottom: 1px solid #e4e7ed;
  flex-shrink: 0;
}

.top-left,
.top-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

/* 步骤指引 */
.steps-bar {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 12px 16px;
  background: #f8fafc;
  border-bottom: 1px solid #e4e7ed;
  flex-shrink: 0;
}

.step-item {
  display: flex;
  align-items: center;
  gap: 6px;
  opacity: 0.5;
  transition: opacity 0.3s;
}

.step-item.active {
  opacity: 1;
}

.step-num {
  width: 22px;
  height: 22px;
  border-radius: 50%;
  background: #c0c4cc;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 600;
}

.step-item.active .step-num {
  background: #409eff;
}

.step-text {
  font-size: 13px;
  color: #606266;
}

.step-item.active .step-text {
  color: #303133;
  font-weight: 500;
}

.step-line {
  width: 40px;
  height: 2px;
  background: #dcdfe6;
  margin: 0 8px;
}

.step-line.active {
  background: #409eff;
}

/* 主内容区 */
.main-content {
  display: flex;
  flex: 1;
  overflow: hidden;
}

/* 左侧配置面板 */
.config-panel {
  width: 50%;
  overflow-y: auto;
  padding: 16px;
  border-right: 1px solid #e4e7ed;
}

/* 右侧预览面板 */
.preview-panel {
  width: 50%;
  overflow-y: auto;
  padding: 16px;
}

/* 区块 */
.section {
  margin-bottom: 16px;
}

.section:last-child {
  margin-bottom: 0;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
  padding-bottom: 8px;
  border-bottom: 1px solid #ebeef5;
}

.section-title {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
}

/* 字段映射 */
.field-mapping {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.mapping-row {
  display: flex;
  align-items: center;
  gap: 12px;
}

.mapping-label {
  width: 90px;
  font-size: 13px;
  color: var(--el-text-color-regular);
  text-align: right;
  flex-shrink: 0;
  white-space: nowrap;
}

/* 预览表格 */
.preview-table {
  max-height: 600px;
  overflow-y: auto;
}

.text-omit-1 {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.text-success {
  color: #67c23a;
}

.help-icon {
  font-size: 14px;
  color: #909399;
  cursor: pointer;
  margin-left: 4px;
  vertical-align: middle;
}

.help-icon:hover {
  color: #409eff;
}
</style>

<style>
/* 暗黑模式适配 */
.dark .top-bar {
  background: var(--el-bg-color);
  border-bottom-color: var(--el-border-color);
}

.dark .steps-bar {
  background: var(--el-fill-color-lighter);
  border-bottom-color: var(--el-border-color);
}

.dark .step-text {
  color: var(--el-text-color-regular);
}

.dark .step-item.active .step-text {
  color: var(--el-text-color-primary);
}

.dark .step-line {
  background: var(--el-border-color);
}

.dark .config-panel {
  border-right-color: var(--el-border-color);
}

.dark .section-header {
  border-bottom-color: var(--el-border-color-lighter);
}

.dark .section-title {
  color: var(--el-text-color-primary);
}
</style>
