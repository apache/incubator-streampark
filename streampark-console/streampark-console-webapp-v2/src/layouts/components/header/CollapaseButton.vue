<script setup lang="ts">
import { useAppStore } from '@/store'
import { SP_ICONS as I } from '@/constants/streamparkIcons'

const appStore = useAppStore()
</script>

<template>
  <n-tooltip placement="bottom" trigger="hover">
    <template #trigger>
      <CommonWrapper @click="appStore.toggleCollapse()">
        <n-icon>
          <IonIcon :name="appStore.collapsed ? I.menuUnfold : I.menuFold" />
        </n-icon>
      </CommonWrapper>
    </template>
    <span>{{ $t('app.toggleSider') }}</span>
  </n-tooltip>
</template>

<style scoped></style>
