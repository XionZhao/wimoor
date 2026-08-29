<template>
	<div class="el-white-bg">
		<el-scrollbar class="he-scr-car" id="supplierPrintArea">
			<div class="supplier-detail">

				<!-- 上部：基本信息 -->
				<div class="section">
			<div class="section-title">基本信息</div>
			<!-- 查看模式 -->
			<el-descriptions v-if="!isEditMode" :column="3" border>
				<el-descriptions-item label="供应商名称">{{ formData.name }}</el-descriptions-item>
				<el-descriptions-item label="供应商编码">{{ formData.number }}</el-descriptions-item>
				<el-descriptions-item label="客户全称">{{ formData.fullname }}</el-descriptions-item>
				<el-descriptions-item label="货物类型">{{ formData.goodtype }}</el-descriptions-item>
				<el-descriptions-item label="联系人">{{ formData.contacts }}</el-descriptions-item>
				<el-descriptions-item label="联系电话">{{ formData.phone_num }}</el-descriptions-item>
				<el-descriptions-item label="创建人">{{ formData.operator2 }}</el-descriptions-item>
				<el-descriptions-item label="修改时间">{{ dateTimesFormat(formData.opttime) }}</el-descriptions-item>
				<el-descriptions-item label="地址" :span="3">{{ formData.address }}</el-descriptions-item>
				<el-descriptions-item label="其它信息" :span="3">{{ formData.contact_info }}</el-descriptions-item>
			</el-descriptions>
			<!-- 编辑模式 -->
			<el-form v-else ref="dataFormRef" :model="formData" :rules="rules" label-width="100px">
				<el-row :gutter="20">
					<el-col :span="8">
						<el-form-item label="供应商名称" prop="name">
							<el-input v-model="formData.name" placeholder="请输入供应商名称" />
						</el-form-item>
					</el-col>
					<el-col :span="8">
						<el-form-item label="供应商编码" prop="number">
							<el-input v-model="formData.number" placeholder="可不填,由系统自动生成" />
						</el-form-item>
					</el-col>
					<el-col :span="8">
						<el-form-item label="客户全称" prop="fullname">
							<el-input v-model="formData.fullname" placeholder="请输入客户全称" />
						</el-form-item>
					</el-col>
				</el-row>
				<el-row :gutter="20">
					<el-col :span="8">
						<el-form-item label="联系人" prop="contacts">
							<el-input v-model="formData.contacts" placeholder="请输入联系人" />
						</el-form-item>
					</el-col>
					<el-col :span="8">
						<el-form-item label="联系电话" prop="phone_num">
							<el-input v-model="formData.phone_num" placeholder="请输入联系电话" />
						</el-form-item>
					</el-col>
					<el-col :span="8">
						<el-form-item label="货物类型" prop="goodtype">
							<el-input v-model="formData.goodtype" placeholder="请输入货物类型" />
						</el-form-item>
					</el-col>
				</el-row>
				<el-row :gutter="20">
					<el-col :span="24">
						<el-form-item label="地址" prop="address">
							<el-input v-model="formData.address" placeholder="请输入地址" type="textarea" />
						</el-form-item>
					</el-col>
				</el-row>
				<el-row :gutter="20">
					<el-col :span="24">
						<el-form-item label="其它信息" prop="contact_info">
							<el-input v-model="formData.contact_info" :rows="3" placeholder="微信，QQ或者网址，工商信息，银行信息..." type="textarea" />
						</el-form-item>
					</el-col>
				</el-row>
			</el-form>
		</div>

		<!-- 中部：收款账户 -->
		<div class="section">
			<div class="section-title">
				<span>收款账户</span>
				<div v-if="formData.id">
					<el-select v-model="accountStatus" size="small" style="width: 120px; margin-right: 8px;" @change="loadAccountList(formData.id)">
						<el-option label="仅启用" :value="1" />
						<el-option label="仅停用" :value="0" />
						<el-option label="全部" :value="null" />
					</el-select>
					<el-button type="primary" size="small" @click="addAccount">新增收款账户</el-button>
				</div>
				<el-button v-else type="primary" size="small" @click="addAccount">新增收款账户</el-button>
			</div>
			<el-table :data="accountList" border size="small">
				<el-table-column label="公司名称" prop="company_name" min-width="120" />
				<el-table-column label="银行账号" prop="account_number" min-width="150" />
				<el-table-column label="开户行" prop="bank_name" min-width="120" />
				<el-table-column label="备注" prop="remark" min-width="120" show-overflow-tooltip />
				<el-table-column label="默认" width="70" align="center">
					<template #default="scope">
						<el-tag v-if="scope.row.is_default" type="success" size="small">默认</el-tag>
						<el-button v-else type="primary" link size="small" @click="handleSetDefault(scope.row)">设为默认</el-button>
					</template>
				</el-table-column>
				<el-table-column label="状态" width="80" align="center">
					<template #default="scope">
						<el-tag :type="scope.row.status === 1 ? 'success' : 'danger'" size="small">
							{{ scope.row.status === 1 ? '启用' : '停用' }}
						</el-tag>
					</template>
				</el-table-column>
				<el-table-column label="操作" width="140" align="center">
					<template #default="scope">
						<el-button type="primary" link size="small" @click="editAccount(scope.row)">编辑</el-button>
						<template v-if="formData.id">
							<el-button :type="scope.row.status === 1 ? 'warning' : 'success'" link size="small" @click="handleToggleStatus(scope.row)">
								{{ scope.row.status === 1 ? '停用' : '启用' }}
							</el-button>
						</template>
						<el-button v-else type="danger" link size="small" @click="removeAccount(scope.$index)">删除</el-button>
					</template>
				</el-table-column>
			</el-table>
		</div>

		<!-- 下部：采购产品汇总 -->
		<div class="section" v-if="formData.id">
			<div class="section-title">采购产品汇总（近半年）</div>
			<div v-if="productList.length === 0" style="text-align: center; color: #909399; padding: 40px 0;">
				暂无近半年采购记录
			</div>
			<el-space v-else wrap :size="16" style="width: 100%;">
				<el-card v-for="item in productList" :key="item.materialid" style="width: 220px; cursor: pointer;" shadow="hover" body-style="padding: 12px;" @click="goToPurchaseOrder(item)">
					<div style="text-align: center; margin-bottom: 8px;">
						<el-image
							v-if="item.image"
							:src="item.image"
							style="width: 80px; height: 80px; border-radius: 4px;"
							fit="cover"
						/>
						<div v-else style="width: 80px; height: 80px; background: #f5f7fa; border-radius: 4px; display: flex; align-items: center; justify-content: center; margin: 0 auto;">
							<span style="color: #c0c4cc; font-size: 12px;">暂无图片</span>
						</div>
					</div>
					<div style="font-size: 14px; font-weight: 500; margin-bottom: 4px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap;" :title="item.mname">
						{{ item.mname }}
					</div>
					<div style="font-size: 12px; color: #909399; margin-bottom: 8px;">SKU: {{ item.sku }}</div>
					<el-divider style="margin: 8px 0;" />
					<div style="display: flex; justify-content: space-between; font-size: 13px;">
						<span class="font-extraSmall">采购金额:</span>
						<span style="color: #e6a23c; font-weight: 500;">￥{{ item.total_pay || 0 }}</span>
					</div>
					<div style="display: flex; justify-content: space-between; font-size: 13px; margin-top: 4px;">
						<span class="font-extraSmall">采购数量:</span>
						<span style="font-weight: 500;">{{ item.total_amount || 0 }}</span>
					</div>
				</el-card>
			</el-space>
		</div>

		<!-- 收款账户编辑弹窗 -->
		<el-dialog v-model="accountDialogVisible" :title="accountForm.id ? '编辑收款账户' : '新增收款账户'" width="500px" destroy-on-close>
			<el-form ref="accountFormRef" :model="accountForm" :rules="accountRules" label-width="90px">
				<el-form-item label="公司名称" prop="companyName">
					<el-input v-model="accountForm.companyName" placeholder="请输入公司名称" />
				</el-form-item>
				<el-form-item label="银行账号" prop="accountNumber">
					<el-input v-model="accountForm.accountNumber" placeholder="请输入银行账号" />
				</el-form-item>
				<el-form-item label="开户行" prop="bankName">
					<el-input v-model="accountForm.bankName" placeholder="请输入开户行" />
				</el-form-item>
				<el-form-item label="设为默认">
					<el-switch v-model="accountForm.isDefault" />
				</el-form-item>
				<el-form-item label="备注">
					<el-input v-model="accountForm.remark" type="textarea" :rows="3" placeholder="备注信息" />
				</el-form-item>
			</el-form>
			<template #footer>
				<el-button @click="accountDialogVisible = false">取消</el-button>
				<el-button type="primary" @click="submitAccountForm">确定</el-button>
			</template>
		</el-dialog>
		</div>
	</el-scrollbar>
	<!-- 底部按钮 -->
	<div class='text-center mar-top-16'>
		<div style="padding-top:10px;">
			<el-space>
				<el-button @click="goBack">关闭</el-button>
				<el-button v-if="!isEditMode" type="primary" @click="switchToEdit">编辑</el-button>
				<template v-else>
					<el-button @click="switchToView">取消</el-button>
					<el-button type="primary" @click="submitForm">保存</el-button>
				</template>
			</el-space>
		</div>
	</div>
	</div>
</template>

<script setup>
import {ref, reactive, toRefs, onMounted} from "vue"
import {useRouter, useRoute} from 'vue-router'

import { ElMessage, ElForm } from 'element-plus'
import customerApi from '@/api/erp/material/customerApi.js';
import {dateTimesFormat} from '@/utils/index.js';

const router = useRouter();
const route = useRoute();
const dataFormRef = ref(ElForm);
const accountFormRef = ref(ElForm);

let state = reactive({
	isEditMode: false,
	formData: {},
	rules: {
		name: [{ required: true, message: '请输入供应商名称', trigger: 'blur' }],
	},
	// 收款账户相关
	accountList: [],
	pendingAccounts: [], // 新增供应商时暂存的账户
	accountStatus: 1, // 默认只显示启用的
	accountDialogVisible: false,
	accountForm: {
		companyName: '',
		accountNumber: '',
		bankName: '',
		isDefault: false,
		remark: '',
	},
	accountRules: {
		companyName: [{ required: true, message: '请输入公司名称', trigger: 'blur' }],
		accountNumber: [{ required: true, message: '请输入银行账号', trigger: 'blur' }],
	},
	// 采购产品汇总
	productList: [],
})

let {
	isEditMode, formData, rules,
	accountList, pendingAccounts, accountStatus, accountDialogVisible, accountForm, accountRules, productList
} = toRefs(state);

onMounted(() => {
	const id = route.query.id;
	const isNew = route.query.new === '1';
	if (isNew) {
		// 新增模式
		state.formData = {};
		state.isEditMode = true;
	} else if (id) {
		loadSupplierData(id);
	}
})

function loadSupplierData(id) {
	customerApi.getData({ id }).then((res) => {
		if (res.data) {
			state.formData = res.data;
			loadAccountList(id);
			loadProductSummary(id);
		}
	});
}

function goBack() {
	router.back();
}

function switchToEdit() {
	state.isEditMode = true;
}

function switchToView() {
	// 取消编辑，重新加载原始数据
	state.isEditMode = false;
	state.pendingAccounts = []; // 清空暂存账户
	if (state.formData.id) {
		loadSupplierData(state.formData.id);
	} else {
		router.back();
	}
}

function submitForm() {
	dataFormRef.value.validate((isValid) => {
		if (isValid) {
			state.formData.phoneNum = state.formData.phone_num;
			state.formData.contactInfo = state.formData.contact_info;
			customerApi.saveData(state.formData).then((res) => {
				if (res.data && res.data.cust) {
					state.formData = Object.assign(state.formData, res.data.cust);
					// 新增供应商保存成功后，保存暂存的收款账户
					if (state.pendingAccounts.length > 0) {
						savePendingAccounts(state.formData.id);
					} else {
						loadAccountList(state.formData.id);
					}
				}
				ElMessage.success('保存成功');
				state.isEditMode = false;
			});
		}
	});
}

// 保存暂存的收款账户
async function savePendingAccounts(customerId) {
	try {
		for (const account of state.pendingAccounts) {
			await customerApi.saveAccount({
				...account,
				customerId: customerId,
				companyName: account.company_name,
				accountNumber: account.account_number,
				bankName: account.bank_name,
				isDefault: account.is_default,
				status: 1,
			});
		}
		state.pendingAccounts = [];
		loadAccountList(customerId);
	} catch (error) {
		ElMessage.error('部分账户保存失败，请手动补充');
		loadAccountList(customerId);
	}
}

// ========== 收款账户相关 ==========
function loadAccountList(customerId) {
	customerApi.getAccountList({ customerId, status: state.accountStatus }).then((res) => {
		state.accountList = res.data || [];
	});
}

function addAccount() {
	state.accountForm = {
		customerId: state.formData.id || '',
		companyName: '',
		accountNumber: '',
		bankName: '',
		isDefault: false,
		remark: '',
		status: 1, // 默认启用
	};
	if (state.formData.id) {
		state.accountStatus = 1; // 确保显示启用状态
	}
	state.accountDialogVisible = true;
}

function editAccount(row) {
	state.accountForm = {
		id: row.id || '',
		customerId: state.formData.id || '',
		companyName: row.company_name,
		accountNumber: row.account_number,
		bankName: row.bank_name,
		isDefault: row.is_default === true || row.is_default === 1,
		remark: row.remark || '',
		status: row.status || 1,
	};
	state.accountDialogVisible = true;
}

function submitAccountForm() {
	accountFormRef.value.validate((isValid) => {
		if (isValid) {
			if (state.formData.id) {
				// 已有供应商，直接调用API保存
				customerApi.saveAccount(state.accountForm).then(() => {
					ElMessage.success('保存成功');
					state.accountDialogVisible = false;
					loadAccountList(state.formData.id);
				});
			} else {
				// 新增供应商，暂存到本地
				const account = {
					...state.accountForm,
					company_name: state.accountForm.companyName,
					account_number: state.accountForm.accountNumber,
					bank_name: state.accountForm.bankName,
					is_default: state.accountForm.isDefault,
				};
				// 如果设为默认，重置其他账户的默认状态
				if (account.is_default) {
					state.pendingAccounts.forEach(item => item.is_default = false);
				}
				state.pendingAccounts.push(account);
				state.accountList = [...state.pendingAccounts];
				ElMessage.success('已暂存，保存供应商后将同步保存');
				state.accountDialogVisible = false;
			}
		}
	});
}

function handleSetDefault(row) {
	if (state.formData.id) {
		customerApi.setDefaultAccount({ id: row.id, customerId: state.formData.id }).then(() => {
			ElMessage.success('设置成功');
			loadAccountList(state.formData.id);
		});
	} else {
		// 本地设置默认
		state.pendingAccounts.forEach(item => item.is_default = false);
		row.is_default = true;
		state.accountList = [...state.pendingAccounts];
	}
}

function handleToggleStatus(row) {
	customerApi.toggleAccountStatus({ id: row.id }).then(() => {
		ElMessage.success('账户状态切换成功');
		loadAccountList(state.formData.id);
	});
}

function removeAccount(index) {
	state.pendingAccounts.splice(index, 1);
	state.accountList = [...state.pendingAccounts];
}

// ========== 采购产品汇总 ==========
function loadProductSummary(supplierId) {
	customerApi.summaryProduct({ supplierId }).then((res) => {
		state.productList = res.data || [];
	});
}

// 跳转到采购单页面
function goToPurchaseOrder(item) {
	router.push({
		path: '/erp/purchase/orders',
		query: {
			supplierid: state.formData.id,
			sku: item.sku,
			title: '采购单',
			path: '/erp/purchase/orders',
		},
	});
}
</script>

<style scoped>
.he-scr-car{
	height:calc(100vh - 176px);
	margin-bottom: 20px;
}
.supplier-detail {
	padding: 0;
}
.section {
	background: #fff;
	margin-bottom: 16px;
	padding: 16px;
	border-radius: 4px;
}
.section-title {
	font-size: 16px;
	font-weight: 600;
	margin-bottom: 16px;
	padding-bottom: 12px;
	border-bottom: 1px solid #ebeef5;
	display: flex;
	justify-content: space-between;
	align-items: center;
}
.font-extraSmall {
	font-size: 12px;
	color: #909399;
}
</style>
