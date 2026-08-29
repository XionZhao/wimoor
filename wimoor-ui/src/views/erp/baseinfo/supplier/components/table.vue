<template>
	<GlobalTable ref="globalTable" :tableData="tableData"  height="calc(100vh - 198px)" @selectionChange='handleSelect' :defaultSort="{ prop: 'opttime', order: 'descending' }"  @loadTable="loadTableData" :stripe="false"  style="width: 100%;margin-bottom:16px;">
		<template #field>
		<el-table-column type="selection"></el-table-column>
		<el-table-column label="供应商名称" prop="name"   width="230" sortable="custom" ></el-table-column>
		<el-table-column label="供应商编码" prop="number" width="130"  sortable="custom" ></el-table-column>
		<el-table-column label="货物类型" prop="goodtype" width="120"  sortable="custom" ></el-table-column>
		<el-table-column label="联系人" prop="contacts"   sortable="custom" >
			<template #default="scope">
				<div>{{scope.row.contacts}}</div>
			    <div><span class="font-extraSmall">联系电话:</span>{{scope.row.phone_num}}</div>
			</template>
		</el-table-column>
		<el-table-column label="总采购金额" prop="totalpay"  width="120">
			<template #default="scope">
				<div>￥<span v-if="scope.row.totalpay">{{scope.row.totalpay}}</span><span v-else>0</span></div>
			    <div><span class="font-extraSmall">入库数量:</span><span v-if="scope.row.totalin">{{scope.row.totalin}}</span>
				<span v-else>0</span></div>
			</template>
		</el-table-column>
		<el-table-column label="联系地址" prop="address"></el-table-column>
		<el-table-column label="其它信息" show-overflow-tooltip   width="100" prop="contact_info"></el-table-column>
		<el-table-column label="创建人" width="100" prop="operator2"></el-table-column>
		<el-table-column label="修改时间" prop="opttime"  width="100" sortable="custom" >
			<template #default="scope">
				<span>{{dateTimesFormat(scope.row.opttime)}}</span>
			</template>
		</el-table-column>
		<el-table-column label="操作"  width="100"  fixed="right" >
			<template #default='scope'>
			<el-space>
				<el-button @click="handleDetails(scope.row)" type="primary" link>详情</el-button>
				<el-dropdown :hide-on-click="false">
				  <span class="el-dropdown-link">
				    <more-one class="ic-cen" theme="outline" size="16" fill="#333" :strokeWidth="3"/>
				  </span>
				   <template #dropdown>
				    <el-dropdown-menu>
				      <el-dropdown-item @click="rowRemove(scope.row)">删除</el-dropdown-item>
				    </el-dropdown-menu>
				</template>
				</el-dropdown>
			  </el-space>	
			</template>
		</el-table-column>
		</template>
	</GlobalTable>
</template>

<script setup>
import {ref, reactive, toRefs} from "vue"
import {useRouter} from 'vue-router'
import {MoreOne} from '@icon-park/vue-next';
import { ElMessage, ElMessageBox } from 'element-plus'
import customerApi from '@/api/erp/material/customerApi.js';
import {dateTimesFormat} from '@/utils/index.js';

const router = useRouter();
let globalTable = ref()

let state = reactive({
	tableData: {records:[], total:0},
	selectRows:[],
})

let {tableData, selectRows} = toRefs(state);

defineExpose({
	state,
	loadData,
})

function handleSelect(rows) {
	state.selectRows = rows;
}

function handleDetails(row) {
	router.push({
		path: "/e/b/s/d",
		query: {
			id: row.id,
			title: '供应商详情 - ' + row.name,
			path: "/e/b/s/d",
		},
	})
}

function rowRemove(rows) {
	ElMessageBox.confirm(
		'确定要删除该条供应商信息吗',
		'删除供应商',
		{
			confirmButtonText: '确认',
			cancelButtonText: '取消',
			type: 'warning',
		}
	).then(() => {
		customerApi.deletecust({"ids": rows.id}).then((res) => {
			if (res.data == "OK") {
				ElMessage.success('删除成功');
				loadData('');
			} else {
				ElMessage.error('删除失败');
			}
		});
	}).catch(() => {
		ElMessage.info('取消删除');
	})
}

function loadData(searchs) {
	var data = {};
	var search = "";
	if (searchs == "" || searchs == undefined || searchs == null) {
		search = "";
	} else {
		search = searchs;
	}
	data.search = search;
	globalTable.value.loadTable(data);
}

function loadTableData(data) {
	customerApi.list(data).then((res) => {
		state.tableData.records = res.data.records
		state.tableData.total = res.data.total
	});
}
</script>
