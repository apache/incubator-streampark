<script setup lang="ts">
import { useAppStore } from '@/store'

const appStore = useAppStore()
</script>

<template>
  <n-tooltip placement="bottom" trigger="hover">
    <template #trigger>
      <CommonWrapper @click="appStore.showSetting = !appStore.showSetting">
        <div>
          <n-icon>
            <IonIcon name="SettingsOutline" />
          </n-icon>
        </div>
      </CommonWrapper>
    </template>
    <span>{{ $t('app.setting') }}</span>
  </n-tooltip>
</template>
