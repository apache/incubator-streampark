<script setup lang="ts">
import { useAppStore } from '@/store'
import { APP_LOCALE_OPTIONS } from '@/utils/locale'

const appStore = useAppStore()
const options = APP_LOCALE_OPTIONS
</script>

<template>
  <n-popselect :value="appStore.lang" :options="options" trigger="click" @update:value="appStore.setAppLang">
    <CommonWrapper>
      <n-icon>
        <IonIcon name="LanguageOutline" />
      </n-icon>
    </CommonWrapper>
  </n-popselect>
</template>

<style scoped></style>
