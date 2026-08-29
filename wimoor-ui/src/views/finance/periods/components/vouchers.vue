<template>
  <div class="flex-between" style="margin-bottom:10px">
    <div>
      <el-checkbox v-model="isAllSelected" :indeterminate="isIndeterminate" @change="handleSelectAll">全选</el-checkbox>
    </div>
    <div><el-space>
      <el-button>重新测算</el-button>
      <el-button type="warning" plain icon="Download" @click="handleExportConfig">导出配置</el-button>
      <el-button type="warning" plain icon="Upload" @click="handleImportConfig">导入配置</el-button>
      <el-dropdown split-button type="primary" @click="handleClick">
        生成凭证
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item>设置</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
      <el-button type="primary" 
                 @click="handleLossVoucherGenerate(lossTemplates[0])" 
                 :disabled="!lossTemplates.length || isCurrentPeriod"
                 :loading="lossTemplates.length > 0 && loadingTemplates.has(lossTemplates[0].id)">
        结账
      </el-button>
      <el-button type="primary" @click="emit('switch-tab', 'third')">反结账</el-button></el-space></div>
  </div>
  <el-space wrap>
  <!-- 第一行：结转损益(loss) -->
  <div v-for="(template,index) in lossTemplates" :key="'loss-'+template.id" class="vouchers-card vouchers-card-loss">
    <div class="card-header">
      <el-space>
        <el-checkbox v-model="template.selected" @change="handleCheckboxChange"></el-checkbox>
        <div class="card-title">{{template.name}}</div>
        <el-button link type="primary">禁用</el-button>
        <el-button link type="primary" @click="handleSettings(template)">设置</el-button>
      </el-space>
    </div>
    <div class="card-body">
      <div class="flex-between"  style="padding-top:5px">
        <div class="font-extraSmall">已结转：</div>
        <div>0.00</div>
      </div>
      <div class="flex-between" style="padding-top:5px">
        <div class="font-extraSmall">未结转：</div>
        <div>0.00</div>
      </div>
      <div style="padding-top:10px"><el-button link type="primary" @click="handleShowCalculation(template)">查看金额计算逻辑</el-button></div>
      <div class="flex-between" style="padding-top:10px"><div>
        <el-input :value="currentPeriodName" size="small" disabled style="width:100px;margin-top:5px"></el-input>
      </div><div>
        <el-button plain type="primary"
                   @click="handleLossVoucherGenerate(template)"
                   :loading="loadingTemplates.has(template.id)"
                   :disabled="isCurrentPeriod">
          生成凭证
        </el-button>
      </div></div>
    </div>
  </div>
  <!-- 第一行：期末调汇(fct)，使用与普通模板相同的显示逻辑 -->
  <div v-for="(template,index) in fctTemplates" :key="'fct-'+template.id" class="vouchers-card vouchers-card-loss">
    <div class="card-header">
      <el-space>
        <el-checkbox v-model="template.selected" @change="handleCheckboxChange"></el-checkbox>
        <div class="card-title">{{template.name}}</div>
        <el-button link type="primary">禁用</el-button>
        <el-button link type="primary" @click="handleSettings(template)">设置</el-button>
      </el-space>
    </div>
    <div class="card-body">
      <div class="flex-between"  style="padding-top:5px">
        <div class="font-extraSmall"> <el-button link type="info" v-if="template.vourchesId" @click="handleShowVoucherLog(template)">查看凭证生成日志</el-button></div>
        <div></div>
      </div>
      <div class="flex-between" style="padding-top:5px">
        <div class="font-extraSmall"></div>
        <div></div>
      </div>
      <div style="padding-top:10px"><el-button link type="primary" @click="handleShowCalculation(template)">查看金额计算逻辑</el-button></div>
      <div class="flex-between" style="padding-top:10px"><div>
        <el-select v-model="template.period" size="small" @change="handlePeriodChange(template,index)" style="width:100px;margin-top:5px" placeholder="请选择">
            <el-option v-for="(item,index) in periods"
              :key="item.value"
              :label="item.name"
              :value="item.value">
            </el-option>
        </el-select>
      </div><div>
        <el-button plain type="success"
                   v-if="template.vourchesId"
                   @click="handleVoucherGenerate(template.id,template.period)"
                   :loading="loadingTemplates.has(template.id)">
          继续生成凭证
        </el-button>
        <el-button v-else plain type="primary"
                   @click="handleVoucherGenerate(template.id,template.period)"
                   :loading="loadingTemplates.has(template.id)">
          生成凭证
        </el-button>
      </div></div>
    </div>
  </div>
  </el-space>
  <el-space wrap>
  <div class="vouchers-card" v-for="(template,index) in normalTemplates" :key="'normal-'+template.id">
    <div class="card-header">
      <el-space>
        <el-checkbox v-model="template.selected" @change="handleCheckboxChange"></el-checkbox>
        <div class="card-title">{{template.name}}</div>
        <el-button link type="primary">禁用</el-button>
        <el-button link type="primary" @click="handleSettings(template)">设置</el-button>
      </el-space>
    </div>
    <div class="card-body">
      <div class="flex-between"  style="padding-top:5px">
        <div class="font-extraSmall">
        <el-button link type="info" v-if="template.vourchesId" @click="handleShowVoucherLog(template)">查看凭证生成日志</el-button>
        </div>
        <div>&nbsp; </div>
      </div>
      <div class="flex-between" style="padding-top:5px">
        <div class="font-extraSmall"> </div>
        <div> </div>
      </div>
      <div  style="padding-top:10px"><el-button link type="primary" @click="handleShowCalculation(template)">查看金额计算逻辑</el-button></div>
      <div class="flex-between" style="padding-top:10px"><div>
        <el-select v-model="template.period" size="small" @change="handlePeriodChange(template,index)" style="width:100px;margin-top:5px" placeholder="请选择">
            <el-option v-for="(item,index) in periods"
              :key="item.value"
              :label="item.name"
              :value="item.value">
            </el-option>
        </el-select>
      </div><div>
        <el-button plain type="success"
                   v-if="template.vourchesId"
                   @click="handleVoucherGenerate(template.id,template.period)"
                   :loading="loadingTemplates.has(template.id)">
          继续生成凭证
        </el-button>
        <el-button v-else plain type="primary"
                   @click="handleVoucherGenerate(template.id,template.period)"
                   :loading="loadingTemplates.has(template.id)">
          生成凭证
        </el-button>

      </div></div>
    </div>
  </div>
  <div class="vouchers-card-add flex-center pointer" @click.stop="handleAdd">
   <div style="margin-left:30%">
     <el-space direction="vertical">
       <el-icon style="font-size:45px;"  class="font-extraSmall"><Plus /></el-icon>
       <div>新增自定义模版</div>
     </el-space>

   </div>
  </div>
  </el-space>
  <Template ref="templateRef" @change="load"></Template>
  <VoucherLogDialog ref="voucherLogRef" />

  <!-- 金额计算逻辑弹窗 -->
  <el-dialog v-model="calcDialogVisible" :title="calcDialogTitle" width="800px" destroy-on-close>
    <div v-loading="calcLoading" element-loading-text="正在计算金额逻辑..." style="min-height: 100px;">
      <template v-if="!calcLoading && calcDetail">
      <!-- 公式说明 -->
      <div style="margin-bottom: 16px; padding: 12px; background: #f5f7fa; border-radius: 4px;">
        <div style="font-weight: bold; margin-bottom: 8px;">计算公式：</div>
        <div style="color: #606266;">{{ calcDetail.formula }}</div>
      </div>
      
      <!-- 结转损益明细 -->
      <template v-if="calcDetail.incomeItems || calcDetail.expenseItems">
        <div v-if="calcDetail.incomeItems && calcDetail.incomeItems.length > 0" style="margin-bottom: 16px;">
          <div style="font-weight: bold; margin-bottom: 8px; color: #67c23a;">收入类科目（贷方余额）</div>
          <el-table :data="calcDetail.incomeItems" border size="small" max-height="300">
            <el-table-column prop="subjectName" label="科目" min-width="250" />
            <el-table-column label="期末余额" align="right" width="150">
              <template #default="{ row }">
                {{ formatAbsAmount(row.balance) }}
              </template>
            </el-table-column>
          </el-table>
          <div style="text-align: right; margin-top: 8px; font-weight: bold;">
            合计：{{ formatAbsAmount(calcDetail.totalIncome) }}
          </div>
        </div>
        
        <div v-if="calcDetail.expenseItems && calcDetail.expenseItems.length > 0" style="margin-bottom: 16px;">
          <div style="font-weight: bold; margin-bottom: 8px; color: #f56c6c;">费用类科目（借方余额）</div>
          <el-table :data="calcDetail.expenseItems" border size="small" max-height="300">
            <el-table-column prop="subjectName" label="科目" min-width="250" />
            <el-table-column label="期末余额" align="right" width="150">
              <template #default="{ row }">
                {{ formatAbsAmount(row.balance) }}
              </template>
            </el-table-column>
          </el-table>
          <div style="text-align: right; margin-top: 8px; font-weight: bold;">
            合计：{{ formatAbsAmount(calcDetail.totalExpense) }}
          </div>
        </div>
        
        <div v-if="calcDetail.targetSubjectCode" style="padding: 12px; background: #ecf5ff; border-radius: 4px;">
          <div style="font-weight: bold; color: #409eff;">结转目标科目：{{ calcDetail.targetSubjectCode }} {{ calcDetail.targetSubjectName }}</div>
        </div>
      </template>
      
      <!-- 期末调汇明细 -->
      <template v-if="calcDetail.details">
        <div style="margin-bottom: 16px;">
          <div style="font-weight: bold; margin-bottom: 8px;">调汇明细</div>
          <el-table :data="calcDetail.details" border size="small" max-height="400">
            <el-table-column prop="subjectName" label="科目" min-width="200" />
            <el-table-column prop="currency" label="币种" width="80" />
            <el-table-column label="外币余额" align="right" width="150">
              <template #default="{ row }">
                {{ formatAmount(row.foreignBalance) }}
              </template>
            </el-table-column>
            <el-table-column label="期末汇率" align="right" width="120">
              <template #default="{ row }">
                {{ row.exchangeRate }}
              </template>
            </el-table-column>
            <el-table-column label="本位币金额" align="right" width="150">
              <template #default="{ row }">
                {{ formatAmount(row.localAmount) }}
              </template>
            </el-table-column>
            <el-table-column prop="type" label="类型" width="80" align="center">
              <template #default="{ row }">
                <el-tag :type="row.type === '收益' ? 'success' : 'danger'" size="small">
                  {{ row.type }}
                </el-tag>
              </template>
            </el-table-column>
          </el-table>
        </div>
        
        <div style="display: flex; justify-content: space-between; padding: 12px; background: #f5f7fa; border-radius: 4px;">
          <div style="color: #67c23a; font-weight: bold;">汇兑收益：{{ formatAmount(calcDetail.totalGain) }}</div>
          <div style="color: #f56c6c; font-weight: bold;">汇兑损失：{{ formatAmount(calcDetail.totalLoss) }}</div>
        </div>
      </template>
      
      <!-- 飞书模板配置明细 -->
      <template v-if="calcDetail.feishuConfig">
        <div style="margin-bottom: 16px;">
          <div style="font-weight: bold; margin-bottom: 8px;">飞书表格配置</div>
          <el-descriptions :column="2" border size="small">
            <el-descriptions-item label="表格ID">{{ calcDetail.feishuConfig.feishuTableId }}</el-descriptions-item>
            <el-descriptions-item label="查询过滤">{{ formatFilter(calcDetail.feishuConfig.filter) }}</el-descriptions-item>
            <el-descriptions-item label="摘要字段">{{ calcDetail.feishuConfig.summaryField }}</el-descriptions-item>
            <el-descriptions-item label="日期字段">{{ calcDetail.feishuConfig.voucherDateField }}</el-descriptions-item>
            <el-descriptions-item label="科目字段">{{ calcDetail.feishuConfig.subjectField }}</el-descriptions-item>
            <el-descriptions-item label="金额字段">{{ calcDetail.feishuConfig.amountField }}</el-descriptions-item>
            <el-descriptions-item label="汇总方式">{{ calcDetail.feishuConfig.datetypeDesc }}</el-descriptions-item>
          </el-descriptions>
        </div>
        <div v-if="calcDetail.items && calcDetail.items.length > 0" style="margin-bottom: 16px;">
          <div style="font-weight: bold; margin-bottom: 8px;">科目映射配置</div>
          <el-table :data="calcDetail.items" border size="small" max-height="400">
            <el-table-column prop="fieldMapping" label="借方科目" min-width="150" />
            <el-table-column prop="subjectName" label="贷方科目" min-width="200" />
            <el-table-column prop="filterCondition" label="过滤条件" min-width="150" />
            <el-table-column label="方向" width="80" align="center">
              <template #default="{ row }">
                {{ row.direction === 1 ? '借方' : '贷方' }}
              </template>
            </el-table-column>
          </el-table>
        </div>
      </template>

      <!-- 亚马逊模板配置明细 -->
      <template v-if="calcDetail.items && calcDetail.items.length > 0 && !calcDetail.feishuConfig">
        <div style="margin-bottom: 16px;">
          <div style="font-weight: bold; margin-bottom: 8px;">模板科目配置</div>
          <el-table :data="calcDetail.items" border size="small" max-height="400">
            <el-table-column prop="summary" label="摘要" />
            <el-table-column prop="subjectName" label="科目" min-width="200" />
            <el-table-column prop="amountField" label="金额字段" width="150" />
            <el-table-column label="方向" width="80" align="center">
              <template #default="{ row }">
                {{ row.direction === 1 ? '借方' : '贷方' }}
              </template>
            </el-table-column>
          </el-table>
        </div>
        
        <div v-if="calcDetail.dataSource" style="padding: 12px; background: #fdf6ec; border-radius: 4px;">
          <div style="font-weight: bold; color: #e6a23c;">数据来源：{{ calcDetail.dataSource }}</div>
          <div v-if="calcDetail.country" style="margin-top: 4px; color: #909399;">国家/地区：{{ calcDetail.country }}</div>
        </div>
      </template>
      </template><!-- v-if 结束 -->
    </div><!-- v-loading 结束 -->
    <template #footer>
      <el-button @click="calcDialogVisible = false">关闭</el-button>
    </template>
  </el-dialog>

  <!-- 导入配置弹框 -->
  <el-dialog v-model="importDialogVisible" title="导入配置" width="500px">
    <div style="margin-bottom: 16px;">
      <el-alert title="导入说明" type="info" :closable="false">
        <template #default>
          <p>1. 请使用导出的Excel模板进行导入</p>
          <p>2. 不同类型和名称的配置通过页签分开</p>
          <p>3. 会计科目使用编码，其他字段使用名称</p>
        </template>
      </el-alert>
    </div>
    <el-upload
      :drag="true"
      action
      ref="uploadRef"
      :http-request="handleUploadFile"
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
          <el-button type="success" @click.stop="downloadImportTemplate" plain>下载导入模板</el-button>
          <div>
            <el-button @click="importDialogVisible = false">取消</el-button>
            <el-button type="primary" v-loading="importLoading" @click.stop="handleImportExcel">
              开始导入
            </el-button>
          </div>
        </div>
      </span>
    </template>
  </el-dialog>
</template>

<script setup>
import Template from './template.vue';
import VoucherLogDialog from './voucher_log_dialog.vue';
import { ref ,onMounted, computed} from 'vue';
import { ElMessage, ElMessageBox, ElLoading, genFileId } from 'element-plus';
import { UploadFilled } from '@element-plus/icons-vue';
import {listFinClosingTemplate,voucher,templateVouchers,getCalculationDetail} from "@/api/finance/closing_template.js";
import {closing} from "@/api/finance/periods.js";
import finStore from "@/hooks/store/useFinanceStore.js";
import request from '@/utils/request';

const emit = defineEmits(['switch-tab']);
const templateRef = ref();
const voucherLogRef = ref();
const feishuVoucherRef=ref();
const enabledTemplates=ref([]);
const disabledTemplates=ref([]);
const loadingTemplates = ref(new Set());
const periods = ref([]);
const currentPeriod = ref(null);

// 金额计算逻辑弹窗相关
const calcDialogVisible = ref(false);
const calcDialogTitle = ref('');
const calcLoading = ref(false);
const calcDetail = ref(null);

// 导入相关变量
const importDialogVisible = ref(false);
const importLoading = ref(false);
const uploadRef = ref(null);
const importFile = ref(null);

// 全选相关计算属性
const isAllSelected = computed(() => {
  const allTemplates = enabledTemplates.value;
  return allTemplates.length > 0 && allTemplates.every(t => t.selected);
});

const isIndeterminate = computed(() => {
  const allTemplates = enabledTemplates.value;
  const selectedCount = allTemplates.filter(t => t.selected).length;
  return selectedCount > 0 && selectedCount < allTemplates.length;
});

// 计算属性：结转损益模板（独立处理逻辑）
const lossTemplates = computed(() => {
  return enabledTemplates.value.filter(t => t.ftype === 'loss');
});

// 计算属性：期末调汇模板（与普通模板相同逻辑，但显示在第一行）
const fctTemplates = computed(() => {
  return enabledTemplates.value.filter(t => t.ftype === 'fct');
});

// 计算属性：普通类型模板
const normalTemplates = computed(() => {
  return enabledTemplates.value.filter(t => t.ftype !== 'loss' && t.ftype !== 'fct');
});

// 计算属性：当前会计期间名称
const currentPeriodName = computed(() => {
  return currentPeriod.value ? currentPeriod.value.periodName : '';
});

// 计算属性：是否是当前会计期间
const isCurrentPeriod = computed(() => {
  if (!currentPeriod.value) return false;
  const now = new Date();
  const currentYear = now.getFullYear();
  const currentMonth = String(now.getMonth() + 1).padStart(2, '0');
  const currentPeriodCode = `${currentYear}${currentMonth}`;
  return currentPeriod.value.periodCode === currentPeriodCode;
});

// 生成最近三个月的会计期间
function generateRecentPeriods() {
  const now = new Date();
  const recentPeriods = [];
  for (let i = 0; i < 8; i++) {
    const date = new Date(now.getFullYear(), now.getMonth() - i, 1);
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const periodName = `${year}${month}`;
    const displayName = `${year}-${month}`;
    
    recentPeriods.push({
      name: displayName,
      value: periodName
    });
  }
  
  periods.value = recentPeriods;
}

// 全选处理
function handleSelectAll(val) {
  enabledTemplates.value.forEach(t => {
    t.selected = val;
  });
}

// 单个checkbox变化时更新全选状态
function handleCheckboxChange() {
  // 触发计算属性重新计算
}

// 获取选中的模板
function getSelectedTemplates() {
  return enabledTemplates.value.filter(t => t.selected);
}

// 导出配置
async function handleExportConfig() {
  const selectedTemplates = getSelectedTemplates();
  if (selectedTemplates.length === 0) {
    ElMessage.warning('请先选择要导出的配置');
    return;
  }

  const loading = ElLoading.service({
    lock: true,
    text: '正在导出配置...',
    background: 'rgba(0, 0, 0, 0.7)'
  });

  try {
    const groupid = await finStore.getCurrentTenantId();
    const templateIds = selectedTemplates.map(t => t.id);
    
    console.log('导出配置 - groupid:', groupid, 'templateIds:', templateIds);
    
    // 使用fetch发送请求
    const response = await fetch('/api/finance/closing_template/exportConfig', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'jsessionid': localStorage.getItem('jsessionid') || ''
      },
      body: JSON.stringify({
        templateIds: templateIds,
        groupid: groupid
      })
    });

    console.log('响应状态:', response.status, response.statusText);
    
    // 检查响应类型
    const contentType = response.headers.get('content-type');
    console.log('响应类型:', contentType);
    
    if (contentType && contentType.includes('application/json')) {
      // 如果是JSON响应，说明是错误信息
      const errorData = await response.json();
      console.error('导出失败:', errorData);
      ElMessage.error(errorData.msg || '导出失败');
      return;
    }

    // 下载文件
    const blob = await response.blob();
    const url = window.URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = `结账配置_${new Date().getTime()}.xlsx`;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    window.URL.revokeObjectURL(url);
    
    ElMessage.success('导出成功');
  } catch (error) {
    console.error('导出失败:', error);
    ElMessage.error('导出失败: ' + error.message);
  } finally {
    loading.close();
  }
}

// 导入配置
function handleImportConfig() {
  importDialogVisible.value = true;
  importFile.value = null;
}

// 下载导入模板
async function downloadImportTemplate() {
  try {
    const groupid = await finStore.getCurrentTenantId();
    
    // 使用proxy.download下载文件
    proxy.download('finance/closing_template/importTemplate', {
      groupid: groupid
    }, `结账配置导入模板.xlsx`);
    
    ElMessage.success('模板下载成功');
  } catch (error) {
    console.error('模板下载失败:', error);
    ElMessage.error('模板下载失败');
  }
}

// 上传文件前校验
function beforeUpload(file) {
  const FileExt = file.name.replace(/.+\./, "").toLowerCase();
  const isLt5M = file.size / 1024 < 50000;
  if (!isLt5M) {
    ElMessage.error('上传文件大小不能超过 50MB');
    return false;
  }
  return true;
}

// 处理文件上传
function handleUploadFile(item) {
  importFile.value = item.file;
}

// 处理文件超出限制
function handleExceed(files) {
  uploadRef.value.clearFiles();
  const file = files[0];
  file.uid = genFileId();
  uploadRef.value.handleStart(file);
}

// 导入Excel
async function handleImportExcel() {
  if (!importFile.value) {
    ElMessage.warning('请先选择要导入的文件');
    return;
  }

  importLoading.value = true;
  const formData = new FormData();
  formData.append('file', importFile.value);

  try {
    const groupid = await finStore.getCurrentTenantId();
    formData.append('groupid', groupid);

    // 使用request上传文件
    const response = await request({
      url: '/api/finance/closing_template/importConfig',
      method: 'post',
      data: formData,
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    });

    if (response && response.code === 200) {
      ElMessage.success(response.msg || '导入成功');
      importDialogVisible.value = false;
      load(); // 刷新列表
    } else {
      ElMessage.error(response.msg || '导入失败');
    }
  } catch (error) {
    console.error('导入失败:', error);
    ElMessage.error('导入失败');
  } finally {
    importLoading.value = false;
  }
}

function handleAdd(){
  console.log(templateRef)
  templateRef.value.show();
}

// 处理设置按钮点击
function handleSettings(template){
  templateRef.value.show(template);
}
function handleVoucherGenerate(templateid,period){
  const template = enabledTemplates.value.find(t => t.id === templateid);
    loadingTemplates.value.add(templateid);
    ElMessage({ message: '正在生成凭证，请稍候...', type: 'info' });
    voucher(templateid,period).then(response=>{
      if(response && response.code === 200){
        ElMessage({ message: '凭证生成成功！', type: 'success' });
        load();
      }else{
        ElMessage({ message: '凭证生成失败：' + (response.msg || '未知错误'), type: 'error' });
      }
    }).catch(error=>{
      ElMessage({ message: '请求失败：' + error.message, type: 'error' });
    }).finally(()=>{
      loadingTemplates.value.delete(templateid);
    })
}

// 显示凭证生成日志
function handleShowVoucherLog(template) {
  if (voucherLogRef.value) {
    voucherLogRef.value.open(template.id, template.period);
  }
}

// 处理loss类型结账（调用完整结账流程）
async function handleLossVoucherGenerate(template){
  if (!currentPeriod.value) {
    ElMessage({ message: '当前会计期间不存在', type: 'error' });
    return;
  }
  
  const templateid = template.id;
  const period = currentPeriod.value.periodCode;
  
  loadingTemplates.value.add(templateid);
  ElMessage({ message: '正在执行结账流程，请稍候...', type: 'info' });
  
  try {
    const tenantId = await finStore.getCurrentTenantId();
    // 调用完整结账接口
    const response = await closing(tenantId, period);
    
    if (response && response.code === 200) {
      // 检查是否有警告信息
      if(response.data && response.data.warnings && response.data.warnings.length > 0){
        const warningMsg = response.data.warnings.join('\n');
        ElMessageBox.alert(warningMsg, '结账完成（有警告）', {
          confirmButtonText: '确定',
          type: 'warning'
        });
      } else {
        ElMessage({ message: response.msg || '结账成功！', type: 'success' });
      }
      // 刷新当前会计期间
      await finStore.refreshCurrentPeriod();
      currentPeriod.value = await finStore.getCurrentPeriod();
      load();
    } else {
      ElMessage({ message: '结账失败：' + (response.msg || '未知错误'), type: 'error' });
    }
  } catch (error) {
    ElMessage({ message: '请求失败：' + error.message, type: 'error' });
  } finally {
    loadingTemplates.value.delete(templateid);
  }
}

async function load(){
  let formData={pageSize:2000,pageNum:1,orderByColumn:'findex'};
  formData.groupid= await finStore.getCurrentTenantId();
  currentPeriod.value = await finStore.getCurrentPeriod();
  listFinClosingTemplate(formData).then(response => {
    if (response && response.code === 200 && response.rows) {
      // 根据disabled字段将模板分为启用和禁用两组
      enabledTemplates.value = response.rows.filter(item => item.disabled === 0).map(item => ({
        ...item,
        period: item.period || currentPeriod.value.periodCode
      }));
      disabledTemplates.value = response.rows.filter(item => item.disabled === 1);
    } else {
      console.error('Invalid response structure:', response);
      enabledTemplates.value = [];
      disabledTemplates.value = [];
    }
  }).catch(error => {
    console.error('Failed to load template list:', error);
    enabledTemplates.value = [];
    disabledTemplates.value = [];
  });
}
function handlePeriodChange(template,index){
  loadingTemplates.value.add(template.id);
  templateVouchers(template.id,template.period).then(response=>{
    loadingTemplates.value.delete(template.id);
    if(response && response.code === 200){
      template.vourchesId=response.data.vourchesId;
    }
  }).catch(error=>{
    loadingTemplates.value.delete(template.id);
    template.loading=false;
  })
}

// 打开金额计算逻辑弹窗
function handleShowCalculation(template){
  calcDialogTitle.value = '金额计算逻辑 - ' + template.name;
  calcDialogVisible.value = true;
  calcLoading.value = true;
  calcDetail.value = null;
  
  const period = template.period || currentPeriod.value?.periodCode;
  getCalculationDetail(template.id, period).then(response => {
    if(response && response.code === 200){
      calcDetail.value = response.data;
    } else {
      ElMessage({ message: '获取计算逻辑失败：' + (response.msg || '未知错误'), type: 'error' });
    }
  }).catch(error => {
    ElMessage({ message: '请求失败：' + error.message, type: 'error' });
  }).finally(() => {
    calcLoading.value = false;
  });
}

// 格式化金额
function formatAmount(amount){
  if(amount === null || amount === undefined) return '0.00';
  return Number(amount).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
}

// 格式化绝对值金额
function formatAbsAmount(amount){
  if(amount === null || amount === undefined) return '0.00';
  return Math.abs(Number(amount)).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
}

// 运算符中文映射
const operatorMap = {
  'is': '=',
  'isNot': '!=',
  'contains': 'like',
  'doesNotContain': 'not like',
  'isEmpty': 'is null',
  'isNotEmpty': 'is not null',
  'isGreater': '>',
  'isGreaterEqual': '>=',
  'isLess': '<',
  'isLessEqual': '<='
};

// 格式化筛选条件为可读文本
function formatFilter(filterStr) {
  if (!filterStr) return '无';
  try {
    const filterObj = typeof filterStr === 'string' ? JSON.parse(filterStr) : filterStr;
    if (!filterObj.conditions || filterObj.conditions.length === 0) return '无';
    
    const conjunction = filterObj.conjunction === 'and' ? ' 并且 ' : ' 或者 ';
    return filterObj.conditions.map(c => {
      const field = c.field_name || '';
      const op = operatorMap[c.operator] || c.operator;
      let val = '';
      if (c.operator === 'isEmpty') {
        val = '空';
      } else if (c.operator === 'isNotEmpty') {
        val = '非空';
      } else if (c.value && c.value.length > 0) {
        val = c.value.join(', ');
      }
      return `${field} ${op} ${val}`.trim();
    }).join(conjunction);
  } catch (e) {
    return filterStr;
  }
}

onMounted(()=>{
  generateRecentPeriods();
  load();
})
</script>

<style scoped>
.vouchers-card{
  width: 300px;
  height:200px;
  border-radius: 4px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  border: solid 1px #e4e7ed;
  margin-right:20px;
}
.vouchers-card-loss{
     margin-bottom: 10px;
}
.vouchers-card-add{
  width: 300px;
  height:200px;
  border-radius: 4px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  border: dashed 1px #e4e7ed;
}
.card-title{
  width:150px;
  font-size:14px;
  word-break: break-all;
  overflow: hidden;
  white-space: nowrap;
}
.card-body{
  padding:20px;
}
.card-header{
  border-bottom: 1px solid #dedede;
  padding: 8px;
}
</style>