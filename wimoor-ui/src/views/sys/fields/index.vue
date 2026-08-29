<template>
	<div class="main-sty">
		<!-- 使用教程 -->
		<el-card shadow="never" class="tutorial-card">
			<template #header>
				<div class="card-header">
					<span class="card-title">
						<el-icon><QuestionFilled /></el-icon>
						列配置插件使用教程
					</span>
					<el-button type="primary" link @click="showTutorial = !showTutorial">
						{{ showTutorial ? '收起' : '展开' }}
					</el-button>
				</div>
			</template>
			
			<el-collapse-transition>
				<div v-show="showTutorial" class="tutorial-content">
					<el-alert 
						title="什么是列配置插件？" 
						description="column_set.vue 是一个通用的列配置组件，允许用户自定义表格的列显示/隐藏和排序，配置会保存到数据库。" 
						type="info" 
						show-icon 
						:closable="false"
					/>
					
					<el-divider content-position="left">集成步骤</el-divider>
					
					<el-steps :active="0" align-center simple>
						<el-step title="引入组件" description="在页面中导入 column_set.vue" />
						<el-step title="添加按钮" description="在表格工具栏添加配置按钮" />
						<el-step title="调用方法" description="点击按钮时调用 show() 方法" />
						<el-step title="处理事件" description="监听 change 事件更新表格" />
						<el-step title="列表应用" description="使用 newConfig 配置渲染动态列" />
					</el-steps>
					
					<el-divider content-position="left">代码示例</el-divider>
					
					<el-card shadow="never" class="code-card">
						<pre v-pre class="code-block"><code>// 1. 引入组件
import columnSet from '@/components/Table/column_set.vue';
const columnSetRef = ref();
const globalTable = ref();

// 2. 添加按钮（点击时调用 show 方法，传入表格标识）
&lt;el-button @click="columnSetRef.show('advcampaigns')"&gt;
  &lt;el-icon&gt;&lt;Tools /&gt;&lt;/el-icon&gt; 列配置
&lt;/el-button&gt;

// 3. 添加列配置组件
&lt;columnSet ref="columnSetRef" @change="getFieldData" /&gt;

// 4. 处理 change 事件，刷新表格字段
function getFieldData(fdata) {
  globalTable.value.refreshField();
}

// 5. 在 GlobalTable 中使用 fieldType 和 v-slot:field
&lt;GlobalTable
  ref="globalTable"
  :tableData="tableData"
  @loadTable="loadTableData"
  fieldType="advcampaigns"
&gt;
  &lt;template v-slot:field="columns"&gt;
    &lt;!-- 固定列：放在最前面 --&gt;
    &lt;el-table-column fixed type="selection" width="60" /&gt;
    &lt;el-table-column fixed label="名称" width="200"&gt;
      &lt;template #default="scope"&gt;
        {{ scope.row.name }}
      &lt;/template&gt;
    &lt;/el-table-column&gt;

    &lt;!-- 动态列：遍历 columns.list --&gt;
    &lt;template v-if="columns.list" v-for="column in columns.list"&gt;
      &lt;el-table-column
        :label="column.label"
        :width="column.width"
        :prop="column.prop"
        :sortable="column.sortable ? 'custom' : false"
      &gt;
        &lt;template #default="scope"&gt;
          &lt;!-- 根据 column.prop 做不同渲染 --&gt;
          &lt;div v-if="column.prop == 'budget'"&gt;
            ${{ scope.row.budget }}
          &lt;/div&gt;
          &lt;div v-else&gt;
            {{ scope.row[column.prop] }}
          &lt;/div&gt;
        &lt;/template&gt;
      &lt;/el-table-column&gt;
    &lt;/template&gt;
  &lt;/template&gt;
&lt;/GlobalTable&gt;</code></pre>
					</el-card>
					
					<el-divider content-position="left">参数说明</el-divider>
					
					<el-descriptions :column="1" border>
						<el-descriptions-item label="show(queryname)">打开配置对话框，queryname 为表格标识（如 advcampaigns、orderList 等）</el-descriptions-item>
						<el-descriptions-item label="@change">配置保存后触发，需调用 globalTable.refreshField() 刷新列</el-descriptions-item>
						<el-descriptions-item label="fieldType">GlobalTable 属性，传入表格标识，组件会自动加载对应的列配置</el-descriptions-item>
						<el-descriptions-item label="v-slot:field">GlobalTable 插槽，通过 columns.list 获取动态列数组</el-descriptions-item>
					</el-descriptions>
					
					<el-divider content-position="left">columns.list 列对象结构</el-divider>
					
					<el-card shadow="never" class="code-card">
						<pre v-pre class="code-block"><code>// columns.list 中每个元素的结构：
{
  prop: "fieldName",   // 字段代码（对应数据属性名）
  label: "列标题",     // 显示标题
  width: "120",        // 列宽度
  sortable: "custom"   // 排序方式
}

// 使用示例：根据 prop 做条件渲染
&lt;template v-if="columns.list" v-for="column in columns.list"&gt;
  &lt;el-table-column
    :label="column.label"
    :width="column.width"
    :prop="column.prop"
    :sortable="column.sortable ? 'custom' : false"
  &gt;
    &lt;template #default="scope"&gt;
      &lt;div v-if="column.prop == 'budget'"&gt;${{ scope.row.budget }}&lt;/div&gt;
      &lt;div v-else&gt;{{ scope.row[column.prop] }}&lt;/div&gt;
    &lt;/template&gt;
  &lt;/el-table-column&gt;
&lt;/template&gt;</code></pre>
					</el-card>
				</div>
			</el-collapse-transition>
		</el-card>

		<el-row :gutter="20" style="margin-top: 16px;">
			<!-- 左侧：分类列表 -->
			<el-col :span="6" :xs="24">
				<el-card shadow="never" class="category-card">
					<template #header>
						<div class="card-header">
							<span class="card-title">
								<el-icon><Folder /></el-icon>
								表格分类
							</span>
							<el-button type="primary" size="small" @click="handleAddCategory">
								<el-icon><Plus /></el-icon> 新增
							</el-button>
						</div>
					</template>
					
					<div class="category-list">
						<div 
							v-for="item in queryNames" 
							:key="item"
							:class="['category-item', { active: currentQuery === item }]"
							@click="selectCategory(item)"
						>
							<span class="category-name">{{ item }}</span>
							<el-button 
								type="danger" 
								link 
								size="small"
								@click.stop="handleDeleteCategory(item)"
							>
								<el-icon><Delete /></el-icon>
							</el-button>
						</div>
						
						<el-empty v-if="queryNames.length === 0" description="暂无分类" :image-size="60" />
					</div>
				</el-card>
			</el-col>
			
			<!-- 中间：字段列表 -->
			<el-col :span="10" :xs="24">
				<el-card shadow="never" class="config-card">
					<template #header>
						<div class="card-header">
							<span class="card-title">
								<el-icon><Setting /></el-icon>
								字段列表
							</span>
							<el-button 
								v-if="currentQuery" 
								type="primary" 
								size="small" 
								@click="handleAddField"
							>
								<el-icon><Plus /></el-icon> 新增字段
							</el-button>
						</div>
					</template>
					
					<field-table 
						:queryname="currentQuery" 
						:fields="fields" 
						@refresh="loadFields"
						@delete="handleDeleteField"
					/>
				</el-card>
			</el-col>
			
			<!-- 右侧：预览效果 -->
			<el-col :span="8" :xs="24">
				<el-card shadow="never" class="preview-card">
					<template #header>
						<div class="card-header">
							<span class="card-title">
								<el-icon><View /></el-icon>
								插件效果预览
							</span>
							<el-button type="primary" size="small" @click="openColumnSet">
								<el-icon><Tools /></el-icon> 打开列配置
							</el-button>
						</div>
					</template>
					
					<field-preview 
						:queryname="currentQuery" 
						:fields="fields"
					/>
				</el-card>
			</el-col>
		</el-row>
		
		<!-- 列配置组件 -->
		<ColumnSet ref="columnSetRef" @change="handleColumnChange" />
		
		<!-- 新增分类对话框 -->
		<el-dialog v-model="showAddCategory" title="新增分类" width="400px">
			<el-form :model="newCategoryForm" label-width="80px">
				<el-form-item label="分类名称" required>
					<el-input v-model="newCategoryForm.name" placeholder="请输入分类名称，如 orderList" />
				</el-form-item>
			</el-form>
			<template #footer>
				<el-button @click="showAddCategory = false">取消</el-button>
				<el-button type="primary" @click="submitAddCategory">确定</el-button>
			</template>
		</el-dialog>
		
		<!-- 新增字段对话框 -->
		<el-dialog v-model="showAddField" title="新增字段" width="500px">
			<el-form :model="newFieldForm" label-width="80px">
				<el-form-item label="字段代码" required>
					<el-input v-model="newFieldForm.ffield" placeholder="如 orderId、productName" />
				</el-form-item>
				<el-form-item label="列标题" required>
					<el-input v-model="newFieldForm.title" placeholder="如 订单号、商品名称" />
				</el-form-item>
				<el-form-item label="列宽度">
					<el-input v-model="newFieldForm.width" placeholder="默认 120" />
				</el-form-item>
				<el-form-item label="排序">
					<el-input-number v-model="newFieldForm.findex" :min="0" />
				</el-form-item>
			</el-form>
			<template #footer>
				<el-button @click="showAddField = false">取消</el-button>
				<el-button type="primary" @click="submitAddField">确定</el-button>
			</template>
		</el-dialog>
	</div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { Setting, View, QuestionFilled, Tools, Folder, Plus, Delete } from '@element-plus/icons-vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import FieldTable from './components/FieldTable.vue';
import FieldPreview from './components/FieldPreview.vue';
import ColumnSet from '@/components/Table/column_set.vue';
import queryFieldApi from '@/api/sys/tool/queryFieldApi.js';

// 查询名称列表
const queryNames = ref([]);
// 当前选中的查询名称
const currentQuery = ref('');
// 字段配置列表
const fields = ref([]);
// 是否显示教程
const showTutorial = ref(true);
// 列配置组件引用
const columnSetRef = ref(null);

// 新增分类相关
const showAddCategory = ref(false);
const newCategoryForm = ref({ name: '' });

// 新增字段相关
const showAddField = ref(false);
const newFieldForm = ref({
	ffield: '',
	title: '',
	width: '120',
	findex: 0
});

// 加载查询名称列表
const loadQueryNames = async () => {
	try {
		const res = await queryFieldApi.getQueryNames();
		if (res.data) {
			queryNames.value = res.data;
		}
	} catch (e) {
		ElMessage.error('获取查询名称列表失败');
	}
};

// 加载字段配置
const loadFields = async () => {
	if (!currentQuery.value) {
		fields.value = [];
		return;
	}
	try {
		const res = await queryFieldApi.getFields({ queryname: currentQuery.value });
		if (res.data) {
			fields.value = res.data;
		}
	} catch (e) {
		ElMessage.error('获取字段配置失败');
	}
};

// 选择分类
const selectCategory = (item) => {
	currentQuery.value = item;
	loadFields();
};

// 新增分类
const handleAddCategory = () => {
	newCategoryForm.value = { name: '' };
	showAddCategory.value = true;
};

// 提交新增分类
const submitAddCategory = async () => {
	if (!newCategoryForm.value.name) {
		ElMessage.warning('请输入分类名称');
		return;
	}
	
	// 检查是否已存在
	if (queryNames.value.includes(newCategoryForm.value.name)) {
		ElMessage.warning('该分类已存在');
		return;
	}
	
	// 调用接口新增分类（实际上分类是通过新增字段自动创建的）
	// 这里先添加一个默认字段来创建分类
	try {
		await queryFieldApi.saveField({
			fquery: newCategoryForm.value.name,
			ffield: 'id',
			title: 'ID',
			width: '100',
			findex: 1
		});
		ElMessage.success('分类创建成功');
		showAddCategory.value = false;
		await loadQueryNames();
		currentQuery.value = newCategoryForm.value.name;
		await loadFields();
	} catch (e) {
		ElMessage.error('创建分类失败');
	}
};

// 删除分类
const handleDeleteCategory = async (name) => {
	try {
		await ElMessageBox.confirm(
			`确定删除分类「${name}」吗？该分类下的所有字段配置都将被删除。`,
			'删除确认',
			{ type: 'warning' }
		);
		
		// 删除该分类下的所有字段
		const fieldsRes = await queryFieldApi.getFields({ queryname: name });
		if (fieldsRes.data) {
			for (const field of fieldsRes.data) {
				await queryFieldApi.deleteField({
					fquery: name,
					ffield: field.ffield
				});
			}
		}
		
		ElMessage.success('分类删除成功');
		if (currentQuery.value === name) {
			currentQuery.value = '';
			fields.value = [];
		}
		await loadQueryNames();
	} catch (e) {
		if (e !== 'cancel') {
			ElMessage.error('删除分类失败');
		}
	}
};

// 新增字段
const handleAddField = () => {
	newFieldForm.value = {
		ffield: '',
		title: '',
		width: '120',
		findex: fields.value.length + 1
	};
	showAddField.value = true;
};

// 提交新增字段
const submitAddField = async () => {
	if (!newFieldForm.value.ffield) {
		ElMessage.warning('请输入字段代码');
		return;
	}
	if (!newFieldForm.value.title) {
		ElMessage.warning('请输入列标题');
		return;
	}
	
	try {
		await queryFieldApi.saveField({
			fquery: currentQuery.value,
			ffield: newFieldForm.value.ffield,
			title: newFieldForm.value.title,
			width: newFieldForm.value.width,
			findex: newFieldForm.value.findex
		});
		ElMessage.success('字段添加成功');
		showAddField.value = false;
		await loadFields();
	} catch (e) {
		ElMessage.error('添加字段失败');
	}
};

// 删除字段
const handleDeleteField = async (row) => {
	try {
		await ElMessageBox.confirm(
			`确定删除字段「${row.title}」吗？`,
			'删除确认',
			{ type: 'warning' }
		);
		
		await queryFieldApi.deleteField({
			fquery: row.fquery,
			ffield: row.ffield
		});
		ElMessage.success('字段删除成功');
		await loadFields();
	} catch (e) {
		if (e !== 'cancel') {
			ElMessage.error('删除字段失败');
		}
	}
};

// 打开列配置对话框
const openColumnSet = () => {
	if (!currentQuery.value) {
		ElMessage.warning('请先选择一个表格分类');
		return;
	}
	columnSetRef.value.show(currentQuery.value);
};

// 列配置变化
const handleColumnChange = (newConfig) => {
	ElMessage.success('列配置已更新');
	loadFields();
};

// 页面加载时获取查询名称列表
onMounted(() => {
	loadQueryNames();
});
</script>

<style scoped>
.main-sty {
	padding: 20px;
}

.tutorial-card {
	margin-bottom: 0;
}

.tutorial-content {
	padding: 10px 0;
}

.category-card,
.config-card,
.preview-card {
	height: calc(100vh - 280px);
	overflow-y: auto;
}

.card-header {
	display: flex;
	align-items: center;
	justify-content: space-between;
}

.card-title {
	display: flex;
	align-items: center;
	gap: 8px;
	font-size: 16px;
	font-weight: 500;
	color: #303133;
}

.category-list {
	padding: 0;
}

.category-item {
	display: flex;
	align-items: center;
	justify-content: space-between;
	padding: 10px 12px;
	cursor: pointer;
	border-radius: 4px;
	transition: all 0.3s;
}

.category-item:hover {
	background: #f5f7fa;
}

.category-item.active {
	background: #ecf5ff;
	color: #409eff;
}

.category-name {
	flex: 1;
	overflow: hidden;
	text-overflow: ellipsis;
	white-space: nowrap;
}

.code-card {
	margin: 16px 0;
}

.code-block {
	background: #f5f7fa;
	padding: 16px;
	border-radius: 4px;
	font-size: 13px;
	line-height: 1.6;
	overflow-x: auto;
}

.code-block code {
	font-family: 'Courier New', Courier, monospace;
}

/* 响应式布局 */
@media (max-width: 768px) {
	.el-col {
		margin-bottom: 20px;
	}
}
</style>
