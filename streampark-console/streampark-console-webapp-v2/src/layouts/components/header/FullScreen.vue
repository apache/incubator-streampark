<script setup lang="ts">
import { useAppStore } from '@/store'

const appStore = useAppStore()

useMagicKeys({
  passive: false,
  onEventFired(e) {
    if (e.key === 'F11' && e.type === 'keydown') {
      e.preventDefault()
      appStore.toggleFullScreen()
    }
  },
})
</script>

<template>
  <n-tooltip placement="bottom" trigger="hover">
    <template #trigger>
      <CommonWrapper @click="appStore.toggleFullScreen">
        <n-icon>
          <IonIcon :name="appStore.fullScreen ? 'ContractOutline' : 'ExpandOutline'" />
        </n-icon>
      </CommonWrapper>
    </template>
    <span>{{ $t('app.toggleFullScreen') }}</span>
  </n-tooltip>
</template>
