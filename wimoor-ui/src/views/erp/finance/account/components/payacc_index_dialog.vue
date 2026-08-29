<template>
	<el-dialog   v-model="dialog.visible" title="设置支付账户顺序" width='600px'>
	<div class="rank-list-title">
	<span>支付账户</span>
	<span>顺序</span>
	</div>	
  <draggable
      class="draggable"
     :list="Options"
	  animation="300"
	 @start="onStart"
	 @end="dragEnd"
    >
	 <template #item="{ element }">
        <div class="item" >
          <el-card shadow="hover">
			 <el-space class="list-title">
			<drag class="ic-cen" theme="outline" size="16" fill="#9a9a9a" :strokeWidth="2"/>
			<span>{{element.name}}<span class="font-extraSmall">[{{element.paymethName}}]</span></span> 
			</el-space>
				<span>{{element.findex}}</span>
		  </el-card>
        </div>
		 </template>
    </draggable>
		<template #footer>
			<span class="dialog-footer">
				<el-button @click="dialog.visible = false">取消</el-button>
				<el-button type="primary" @click="submitFunc">保存</el-button>
			</span>
		</template>
	</el-dialog>
</template>

<script setup>
	import { ref ,reactive,toRefs} from 'vue'
	import {Drag} from '@icon-park/vue-next';
	import draggable from "vuedraggable";
	import '@/assets/css/draggable.css';
	import faccountApi from '@/api/erp/finances/faccountApi.js';
	import { ElMessage } from 'element-plus'
   const emit = defineEmits(['change']);
   const state=reactive({
   		  dialog:{visible:false},
   		  Options:[],
       });
   	const {
   	  dialog,Options
   	} = toRefs(state);
	function show(){
		state.dialog.visible=true;
		faccountApi.getAccountAll().then((res)=>{
			if(res.data && res.data.length>0){
				state.Options=res.data;
				dragEnd();
			}
		});
	}
	function onStart(){
	}
	function dragEnd(){
		state.Options.forEach((item,index)=>{
			item.findex = index+1;
		})
	}
	function submitFunc(){
		faccountApi.saveAccountIndex(state.Options).then(res=>{
		    ElMessage.success("保存成功");
			state.dialog.visible=false;
			 emit("change");
		})
	}
    defineExpose({ show });
</script>

<style>
	.rank-list-title{
		display: flex;
		justify-content: space-between;
		margin:0 16px;
		font-size: 12px;
		color: #999;
	}
</style>
