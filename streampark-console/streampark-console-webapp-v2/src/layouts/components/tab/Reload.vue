<script setup lang="ts">
import { useAppStore } from '@/store'

const appStore = useAppStore()

const loading = ref(false)

function handleReload() {
  loading.value = true
  appStore.reloadPage()
  setTimeout(() => {
    loading.value = false
  }, 300)
}
</script>

<template>
  <n-tooltip placement="bottom" trigger="hover">
    <template #trigger>
      <CommonWrapper @click="handleReload">
        <n-icon :class="{ 'animate-spin': loading }">
          <IonIcon name="RefreshOutline" />
        </n-icon>
      </CommonWrapper>
    </template>
    <span>{{ $t('common.reload') }}</span>
  </n-tooltip>
</template>

<style scoped></style>
