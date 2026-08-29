<template>
  <el-dialog title="新增发票" v-model="visible" width="650px" append-to-body destroy-on-close>
    <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="发票类型" prop="invoiceType">
            <el-select v-model="form.invoiceType" placeholder="请选择发票类型" style="width: 100%">
              <el-option label="增值税专用发票" value="VAT_SPECIAL" />
              <el-option label="增值税普通发票" value="VAT_NORMAL" />
              <el-option label="机动车销售统一发票" value="MOTOR" />
              <el-option label="货物运输业增值税专用发票" value="FREIGHT" />
              <el-option label="通行费电子发票" value="TOLL" />
              <el-option label="铁路电子客票" value="RAILWAY" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="发票代码" prop="invoiceCode">
            <el-input v-model="form.invoiceCode" placeholder="请输入发票代码" />
          </el-form-item>
        </el-col>
      </el-row>
      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="发票号码" prop="invoiceNo">
            <el-input v-model="form.invoiceNo" placeholder="请输入发票号码" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="数电发票号码" prop="digitalInvoiceNo">
            <el-input v-model="form.digitalInvoiceNo" placeholder="数电发票填写" />
          </el-form-item>
        </el-col>
      </el-row>
      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="开票日期" prop="invoiceDate">
            <el-date-picker v-model="form.invoiceDate" type="date" placeholder="请选择开票日期" value-format="YYYY-MM-DD" style="width: 100%" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="发票状态" prop="status">
            <el-select v-model="form.status" placeholder="请选择发票状态" style="width: 100%">
              <el-option label="正常" value="NORMAL" />
              <el-option label="作废" value="CANCELLED" />
              <el-option label="红冲全部" value="RED_ALL" />
              <el-option label="红冲部分" value="RED_PART" />
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>
      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="供应商名称" prop="sellerName">
            <el-input v-model="form.sellerName" placeholder="请输入供应商名称" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="供应商税号" prop="sellerTaxNo">
            <el-input v-model="form.sellerTaxNo" placeholder="请输入供应商税号" />
          </el-form-item>
        </el-col>
      </el-row>
      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="购方名称" prop="buyerName">
            <el-input v-model="form.buyerName" placeholder="请输入购方名称" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="购方税号" prop="buyerTaxNo">
            <el-input v-model="form.buyerTaxNo" placeholder="请输入购方税号" />
          </el-form-item>
        </el-col>
      </el-row>
      <el-row :gutter="20">
        <el-col :span="8">
          <el-form-item label="价税合计" prop="amountWithTax">
            <el-input-number v-model="form.amountWithTax" :precision="2" :controls="false" placeholder="价税合计" style="width: 100%" />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="不含税金额" prop="amountWithoutTax">
            <el-input-number v-model="form.amountWithoutTax" :precision="2" :controls="false" placeholder="不含税金额" style="width: 100%" />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="税额" prop="taxAmount">
            <el-input-number v-model="form.taxAmount" :precision="2" :controls="false" placeholder="税额" style="width: 100%" />
          </el-form-item>
        </el-col>
      </el-row>
      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="币种" prop="currency">
            <el-input v-model="form.currency" placeholder="默认CNY" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="备注" prop="remark">
            <el-input v-model="form.remark" placeholder="请输入备注" />
          </el-form-item>
        </el-col>
      </el-row>
    </el-form>
    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" :loading="loading" @click="handleSubmit">确定</el-button>
    </template>
  </el-dialog>
</template>

<script setup name="AddInvoiceDialog">
import { ref, reactive } from 'vue'
import { getCurrentInstance } from '@vue/runtime-core'
import { createInvoice } from '@/api/finance/invoiceLedger'
import finStore from '@/hooks/store/useFinanceStore.js'

const { proxy } = getCurrentInstance()

const emit = defineEmits(['success'])

const visible = ref(false)
const loading = ref(false)
const formRef = ref()

const form = reactive({
  invoiceType: '',
  invoiceCode: '',
  invoiceNo: '',
  digitalInvoiceNo: '',
  invoiceDate: '',
  status: 'NORMAL',
  sellerName: '',
  sellerTaxNo: '',
  buyerName: '',
  buyerTaxNo: '',
  amountWithTax: null,
  amountWithoutTax: null,
  taxAmount: null,
  currency: 'CNY',
  remark: ''
})

const rules = {
  invoiceType: [{ required: true, message: '请选择发票类型', trigger: 'change' }],
  invoiceNo: [{ required: true, message: '请输入发票号码', trigger: 'blur' }],
  invoiceDate: [{ required: true, message: '请选择开票日期', trigger: 'change' }],
  sellerName: [{ required: true, message: '请输入供应商名称', trigger: 'blur' }],
  amountWithTax: [{ required: true, message: '请输入价税合计', trigger: 'blur' }]
}

function resetForm() {
  form.invoiceType = ''
  form.invoiceCode = ''
  form.invoiceNo = ''
  form.digitalInvoiceNo = ''
  form.invoiceDate = ''
  form.status = 'NORMAL'
  form.sellerName = ''
  form.sellerTaxNo = ''
  form.buyerName = ''
  form.buyerTaxNo = ''
  form.amountWithTax = null
  form.amountWithoutTax = null
  form.taxAmount = null
  form.currency = 'CNY'
  form.remark = ''
}

function open() {
  visible.value = true
  resetForm()
}

async function handleSubmit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  // 发票号码和数电发票号码至少填一个
  if (!form.invoiceNo && !form.digitalInvoiceNo) {
    proxy.$message.warning('发票号码和数电发票号码至少填写一个')
    return
  }

  loading.value = true
  try {
    const groupid = await finStore.getCurrentTenantId()
    const data = { ...form, groupid }
    const res = await createInvoice(data)
    proxy.$message.success(res.msg || '新增成功')
    visible.value = false
    emit('success')
  } catch (error) {
    console.error('新增发票失败:', error)
    const errMsg = error.response?.data?.msg || error.message || '未知错误'
    proxy.$message.error('新增发票失败：' + errMsg)
  } finally {
    loading.value = false
  }
}

defineExpose({ open })
</script>
