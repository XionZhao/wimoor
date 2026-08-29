<template>
  <el-drawer title="发票详情" v-model="visible" size="65%">
    <el-descriptions :column="2" border class="mb16">
      <el-descriptions-item label="发票号码">{{ invoiceInfo.digitalInvoiceNo || invoiceInfo.invoiceNo }}</el-descriptions-item>
      <el-descriptions-item label="发票类型">
        <el-tag :type="getInvoiceTypeTag(invoiceInfo.invoiceType)">
          {{ getInvoiceTypeText(invoiceInfo.invoiceType) }}
        </el-tag>
      </el-descriptions-item>
      <el-descriptions-item label="供应商名称">{{ invoiceInfo.sellerName }}</el-descriptions-item>
      <el-descriptions-item label="供应商税号">{{ invoiceInfo.sellerTaxNo }}</el-descriptions-item>
      <el-descriptions-item label="开票日期">{{ invoiceInfo.invoiceDate }}</el-descriptions-item>
      <el-descriptions-item label="价税合计">
        <span class="amount">{{ formatNumber(invoiceInfo.amountWithTax) }}</span>
      </el-descriptions-item>
      <el-descriptions-item label="不含税金额">
        <span class="amount">{{ formatNumber(invoiceInfo.amountWithoutTax) }}</span>
      </el-descriptions-item>
      <el-descriptions-item label="税额">
        <span class="amount">{{ formatNumber(invoiceInfo.taxAmount) }}</span>
      </el-descriptions-item>
      <el-descriptions-item label="币种">{{ invoiceInfo.currency }}</el-descriptions-item>
      <el-descriptions-item label="汇率">{{ invoiceInfo.exchangeRate }}</el-descriptions-item>
      <el-descriptions-item label="状态">
        <el-tag :type="getStatusType(invoiceInfo.status)">
          {{ getStatusText(invoiceInfo.status) }}
        </el-tag>
      </el-descriptions-item>
      <el-descriptions-item label="入账状态">
        <el-tag :type="invoiceInfo.postingStatus === 1 ? 'success' : 'info'">
          {{ invoiceInfo.postingStatus === 1 ? '已入账' : '未入账' }}
        </el-tag>
      </el-descriptions-item>
    </el-descriptions>

    <!-- 货物运输服务和铁路电子客票信息 -->
    <template v-if="isTransportInvoice">
      <el-divider content-position="left">运输/旅客信息</el-divider>
      <el-descriptions :column="2" border class="mb16">
        <el-descriptions-item label="起运地/出发地">{{ getExtensionValue('origin') || getExtensionValue('departure') || '-' }}</el-descriptions-item>
        <el-descriptions-item label="到达地/目的地">{{ getExtensionValue('destination') || getExtensionValue('arrival') || '-' }}</el-descriptions-item>
        <el-descriptions-item label="交通工具类型">{{ getExtensionValue('transportToolType') || '-' }}</el-descriptions-item>
        <el-descriptions-item label="交通工具号码">{{ getExtensionValue('transportToolNo') || getExtensionValue('trainNo') || '-' }}</el-descriptions-item>
        <el-descriptions-item label="旅客姓名">{{ getExtensionValue('passengerName') || '-' }}</el-descriptions-item>
        <el-descriptions-item label="证件号码">{{ getExtensionValue('idNo') || '-' }}</el-descriptions-item>
        <el-descriptions-item label="出行日期">{{ getExtensionValue('travelDate') || '-' }}</el-descriptions-item>
        <el-descriptions-item label="出行时间">{{ getExtensionValue('travelTime') || '-' }}</el-descriptions-item>
        <el-descriptions-item label="运输货物名称">{{ getExtensionValue('cargoName') || '-' }}</el-descriptions-item>
      </el-descriptions>
    </template>

    <el-divider content-position="left">商品明细</el-divider>
    <div style="margin-bottom: 10px;">
      <el-button type="primary" size="small" icon="Plus" @click="handleAddDetail">新增明细</el-button>
    </div>
    <el-table :data="invoiceDetails" style="width: 100%" size="small" v-loading="loading">
      <el-table-column label="行号" prop="lineNo" width="60" align="center" />
      <el-table-column label="货物/劳务名称" prop="goodsName" min-width="150" show-overflow-tooltip />
      <el-table-column label="规格型号" prop="specModel" width="100" show-overflow-tooltip />
      <el-table-column label="单位" prop="unit" width="60" align="center" />
      <el-table-column label="数量" prop="quantity" width="80" align="right">
        <template #default="{ row }">
          {{ row.quantity != null ? Number(row.quantity) : '' }}
        </template>
      </el-table-column>
      <el-table-column label="单价" prop="unitPrice" width="100" align="right">
        <template #default="{ row }">
          <span class="amount">{{ row.unitPrice != null ? formatNumber(row.unitPrice) : '' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="金额" prop="amountWithoutTax" width="110" align="right">
        <template #default="{ row }">
          <span class="amount">{{ formatNumber(row.amountWithoutTax) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="税率" prop="taxRate" width="70" align="center">
        <template #default="{ row }">
          {{ row.taxRate != null ? row.taxRate + '%' : '' }}
        </template>
      </el-table-column>
      <el-table-column label="税额" prop="taxAmount" width="100" align="right">
        <template #default="{ row }">
          <span class="amount">{{ formatNumber(row.taxAmount) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="价税合计" prop="amountWithTax" width="110" align="right">
        <template #default="{ row }">
          <span class="amount">{{ formatNumber(row.amountWithTax) }}</span>
        </template>
      </el-table-column>
    </el-table>

    <el-divider content-position="left">关联采购订单</el-divider>
    <el-table :data="invoiceInfo.purchaseOrders || []" style="width: 100%" size="small">
      <el-table-column label="订单号" prop="orderNumber" width="150" />
      <el-table-column label="订单金额" prop="orderAmount" width="120" align="right">
        <template #default="{ row }">
          <span class="amount">{{ formatNumber(row.orderAmount) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="供应商" prop="supplierName" show-overflow-tooltip />
    </el-table>

    <el-divider content-position="left">关联付款记录</el-divider>
    <el-table :data="invoiceInfo.payments || []" style="width: 100%" size="small">
      <el-table-column label="付款单号" prop="paymentId" width="150" />
      <el-table-column label="付款金额" prop="payAmount" width="120" align="right">
        <template #default="{ row }">
          <span class="amount">{{ formatNumber(row.payAmount) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="付款时间" prop="payDate" width="160" />
    </el-table>

    <el-divider content-position="left">关联凭证记录</el-divider>
    <template v-if="voucherInfo">
      <el-table :data="voucherTableData" style="width: 100%" size="small">
        <el-table-column label="凭证号" prop="voucherNo" width="150" />
        <el-table-column label="凭证字" width="100" align="center">
          <template #default="{ row }">
            {{ getVoucherTypeText(row.voucherType) }}
          </template>
        </el-table-column>
        <el-table-column label="凭证日期" prop="voucherDate" width="120" />
        <el-table-column label="金额" prop="totalAmount" width="120" align="right">
          <template #default="{ row }">
            <span class="amount">{{ formatNumber(row.totalAmount) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" prop="voucherStatus" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.voucherStatus === 1 ? 'success' : 'info'" size="small">
              {{ row.voucherStatus === 1 ? '已审核' : '未审核' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100" align="center">
          <template #default>
            <el-button link type="primary" icon="View" @click="handleViewVoucher">查看凭证</el-button>
          </template>
        </el-table-column>
      </el-table>
    </template>
    <el-empty v-else description="暂无关联凭证" :image-size="60" />

    <!-- 新增明细对话框 -->
    <el-dialog title="新增商品明细" v-model="addDetailVisible" width="600px" append-to-body destroy-on-close>
      <el-form ref="addDetailFormRef" :model="addDetailForm" :rules="addDetailRules" label-width="120px">
        <el-row :gutter="20">
          <el-col :span="24">
            <el-form-item label="货物/劳务名称" prop="goodsName">
              <el-input v-model="addDetailForm.goodsName" placeholder="请输入货物或劳务名称" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="规格型号" prop="specModel">
              <el-input v-model="addDetailForm.specModel" placeholder="请输入规格型号" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="单位" prop="unit">
              <el-input v-model="addDetailForm.unit" placeholder="请输入单位" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="数量" prop="quantity">
              <el-input-number v-model="addDetailForm.quantity" :precision="2" :controls="false" placeholder="数量" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="单价" prop="unitPrice">
              <el-input-number v-model="addDetailForm.unitPrice" :precision="4" :controls="false" placeholder="不含税单价" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="金额" prop="amountWithoutTax">
              <el-input-number v-model="addDetailForm.amountWithoutTax" :precision="2" :controls="false" placeholder="不含税金额" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="税率" prop="taxRate">
              <el-input v-model="addDetailForm.taxRate" placeholder="如：13" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="税额" prop="taxAmount">
              <el-input-number v-model="addDetailForm.taxAmount" :precision="2" :controls="false" placeholder="税额" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="价税合计" prop="amountWithTax">
              <el-input-number v-model="addDetailForm.amountWithTax" :precision="2" :controls="false" placeholder="价税合计" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="税收分类编码" prop="taxCategoryCode">
              <el-input v-model="addDetailForm.taxCategoryCode" placeholder="请输入税收分类编码" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="addDetailVisible = false">取消</el-button>
        <el-button type="primary" :loading="addDetailLoading" @click="handleAddDetailSubmit">确定</el-button>
      </template>
    </el-dialog>
  </el-drawer>
</template>

<script setup name="DetailDrawer">
import { ref, reactive, computed } from 'vue'
import { getCurrentInstance } from '@vue/runtime-core'
import { useRouter } from 'vue-router'
import { getInvoiceDetail, createInvoiceDetail } from '@/api/finance/invoiceLedger'

const { proxy } = getCurrentInstance()
const router = useRouter()

const visible = ref(false)
const loading = ref(false)
const invoiceInfo = reactive({})
const invoiceDetails = ref([])
const invoiceExtensions = ref({})
const voucherInfo = ref(null)

// 新增明细对话框相关
const addDetailVisible = ref(false)
const addDetailLoading = ref(false)
const addDetailFormRef = ref()
const addDetailForm = reactive({
  goodsName: '',
  specModel: '',
  unit: '',
  quantity: null,
  unitPrice: null,
  amountWithoutTax: null,
  taxRate: '',
  taxAmount: null,
  amountWithTax: null,
  taxCategoryCode: ''
})

const addDetailRules = {
  goodsName: [{ required: true, message: '请输入货物/劳务名称', trigger: 'blur' }],
  amountWithTax: [{ required: true, message: '请输入价税合计', trigger: 'blur' }]
}

// 凭证信息转为数组供表格渲染
const voucherTableData = computed(() => {
  return voucherInfo.value ? [voucherInfo.value] : []
})

// 凭证字映射（兼容旧数据中的数字编码）
const voucherTypeMap = {
  '1': '记',
  '2': '收',
  '3': '付',
  '记': '记',
  '收': '收',
  '付': '付'
}

function getVoucherTypeText(type) {
  if (!type) return '记'
  return voucherTypeMap[String(type)] || type
}

// 判断是否为运输/旅客类发票（货物运输服务或铁路电子客票）
const isTransportInvoice = computed(() => {
  const type = invoiceInfo.invoiceType
  console.log('发票类型:', type)
  if (!type) return false
  const result = type === 'FREIGHT' || type === 'RAILWAY' ||
         type.includes('运输') || type.includes('铁路') || type.includes('旅客')
  console.log('是否运输发票:', result)
  return result
})

// 从扩展信息中获取属性值
function getExtensionValue(key) {
  return invoiceExtensions.value[key] || ''
}

// 打开抽屉
async function open(id) {
  visible.value = true
  loading.value = true
  invoiceDetails.value = []
  invoiceExtensions.value = {}
  voucherInfo.value = null
  
  try {
    const res = await getInvoiceDetail({ id })
    const data = res.data || {}
    Object.assign(invoiceInfo, data.invoice || data)
    invoiceDetails.value = data.details || []
    invoiceExtensions.value = data.extensions || {}
    voucherInfo.value = data.voucherInfo || null
  } catch (error) {
    console.error('获取发票详情失败:', error)
  } finally {
    loading.value = false
  }
}

// 查看凭证详情
function handleViewVoucher() {
  if (voucherInfo.value && voucherInfo.value.voucherId) {
    router.push({
      path: '/fin/voucher/create',
      query: {
        title: '凭证录入',
        path: '/fin/voucher/create',
        voucherId: voucherInfo.value.voucherId
      }
    })
    visible.value = false
  }
}

// 格式化数字
function formatNumber(num) {
  if (!num || num === 0) return '0.00'
  return Number(num).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

// 票种编码到中文名称的映射
const invoiceTypeMap = {
  'VAT_SPECIAL': '增值税专用发票',
  'VAT_NORMAL': '增值税普通发票',
  'MOTOR': '机动车销售统一发票',
  'FREIGHT': '货物运输业增值税专用发票',
  'TOLL': '通行费电子发票',
  'RAILWAY': '铁路电子客票'
}

// 票种编码到标签颜色的映射
const invoiceTypeColorMap = {
  'VAT_SPECIAL': '',
  'VAT_NORMAL': 'success',
  'MOTOR': 'warning',
  'FREIGHT': 'warning',
  'TOLL': 'warning',
  'RAILWAY': 'warning'
}

function getInvoiceTypeTag(type) {
  if (!type) return 'info'
  // 先按编码查颜色
  if (invoiceTypeColorMap[type] !== undefined) return invoiceTypeColorMap[type]
  // 再按中文名称匹配
  if (type.includes('专用') || type.includes('专票')) return ''
  if (type.includes('普通') || type.includes('普票')) return 'success'
  if (type.includes('机动车')) return 'warning'
  if (type.includes('运输')) return 'warning'
  return 'info'
}

function getInvoiceTypeText(type) {
  if (!type) return '未知'
  return invoiceTypeMap[type] || type
}

// 状态
function getStatusType(status) {
  const types = { NORMAL: 'success', CANCELLED: 'danger', RED_ALL: 'danger', RED_PART: 'warning', ABNORMAL: 'danger' }
  return types[status] || 'info'
}

function getStatusText(status) {
  const texts = { NORMAL: '正常', CANCELLED: '作废', RED_ALL: '红冲全部', RED_PART: '红冲部分', ABNORMAL: '异常' }
  return texts[status] || '未知'
}

// 新增明细
function handleAddDetail() {
  // 重置表单
  addDetailForm.goodsName = ''
  addDetailForm.specModel = ''
  addDetailForm.unit = ''
  addDetailForm.quantity = null
  addDetailForm.unitPrice = null
  addDetailForm.amountWithoutTax = null
  addDetailForm.taxRate = ''
  addDetailForm.taxAmount = null
  addDetailForm.amountWithTax = null
  addDetailForm.taxCategoryCode = ''
  addDetailVisible.value = true
}

// 提交新增明细
async function handleAddDetailSubmit() {
  const valid = await addDetailFormRef.value.validate().catch(() => false)
  if (!valid) return

  addDetailLoading.value = true
  try {
    const data = {
      ...addDetailForm,
      invoiceId: invoiceInfo.id,
      lineNo: invoiceDetails.value.length + 1
    }
    const res = await createInvoiceDetail(data)
    proxy.$message.success(res.msg || '新增成功')
    addDetailVisible.value = false
    // 刷新明细列表
    const detailRes = await getInvoiceDetail({ id: invoiceInfo.id })
    invoiceDetails.value = detailRes.data?.details || []
  } catch (error) {
    console.error('新增明细失败:', error)
    const errMsg = error.response?.data?.msg || error.message || '未知错误'
    proxy.$message.error('新增明细失败：' + errMsg)
  } finally {
    addDetailLoading.value = false
  }
}

defineExpose({ open })
</script>

<style scoped>
.amount {
  font-family: 'DIN Alternate', monospace;
}

.text-gray {
  color: #909399;
}

.mb16 {
  margin-bottom: 16px;
}
</style>
