<template>
<div class="main-sty">
<el-tabs v-model="activeTab" class="demo-tabs" style="margin-top:-8px;">
	<el-tab-pane label="概览" name="overview">
	</el-tab-pane>
	<el-tab-pane label="记账" name="bookkeeping">	
	</el-tab-pane>
	<el-tab-pane label="转结" name="alibabaSettlement">	
	</el-tab-pane>
</el-tabs>
<AccountOverview v-if="activeTab === 'overview'"/>
	<Bookkeeping v-if="activeTab === 'bookkeeping'"/>
	<AlibabaSettlementTab v-if="activeTab === 'alibabaSettlement'"/>
</div>
</template>
<script>
	export default{ name:"采购记账" };
</script>
<script setup>
import { ref } from 'vue'
import AccountOverview from "./components/account_overview.vue"
import Bookkeeping from "./components/bookkeeping.vue"
import AlibabaSettlementTab from "./components/alibaba_settlement_tab.vue"

const activeTab = ref('overview')
</script>

<style scoped="scoped">
</style>
