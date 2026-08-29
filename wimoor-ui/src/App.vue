<template>
   <div id="app">
         <router-view ></router-view>
         <VersionUpdate ref="versionUpdateRef" />
     </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import VersionUpdate from '@/components/VersionUpdate/index.vue'
import { startVersionCheck, stopVersionCheck } from '@/utils/version'

const versionUpdateRef = ref(null)

onMounted(() => {
  // 启动版本检测，每5分钟检查一次
  startVersionCheck(() => {
    if (versionUpdateRef.value) {
      versionUpdateRef.value.show()
    }
  }, 5 * 60 * 1000)
})

onUnmounted(() => {
  stopVersionCheck()
})
</script>