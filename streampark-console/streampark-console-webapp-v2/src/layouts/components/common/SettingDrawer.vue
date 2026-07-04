<script setup lang="ts">
import { useAppStore } from '@/store'
import LayoutSelector from './LayoutSelector.vue'

const appStore = useAppStore()

const { t } = useI18n()

const transitionSelectorOptions = computed(() => {
  return [
    {
      label: t('app.transitionNull'),
      value: '',
    },
    {
      label: t('app.transitionFadeSlide'),
      value: 'fade-slide',
    },
    {
      label: t('app.transitionFadeBottom'),
      value: 'fade-bottom',
    },
    {
      label: t('app.transitionFadeScale'),
      value: 'fade-scale',
    },
    {
      label: t('app.transitionZoomFade'),
      value: 'zoom-fade',
    },
    {
      label: t('app.transitionZoomOut'),
      value: 'zoom-out',
    },
    {
      label: t('app.transitionSoft'),
      value: 'fade',
    },
  ]
})

const palette = [
  '#ffb8b8',
  '#d03050',
  '#F0A020',
  '#fff200',
  '#ffda79',
  '#18A058',
  '#006266',
  '#22a6b3',
  '#18dcff',
  '#2080F0',
  '#c56cf0',
  '#be2edd',
  '#706fd3',
  '#4834d4',
  '#130f40',
  '#4b4b4b',
]

function resetSetting() {
  window.$dialog.warning({
    title: t('app.resetSettingTitle'),
    content: t('app.resetSettingContent'),
    positiveText: t('common.confirm'),
    negativeText: t('common.cancel'),
    onPositiveClick: () => {
      appStore.resetAlltheme()
      window.$message.success(t('app.resetSettingMeaasge'))
    },
  })
}
</script>

<template>
  <n-drawer v-model:show="appStore.showSetting" :width="360">
    <n-drawer-content :title="t('app.systemSetting')" closable>
      <n-space vertical>
        <n-divider>{{ $t('app.layoutSetting') }}</n-divider>
        <LayoutSelector v-model:value="appStore.layoutMode" />
        <n-divider>{{ $t('app.themeSetting') }}</n-divider>
        <n-space justify="space-between">
          {{ $t('app.colorWeak') }}
          <n-switch :value="appStore.colorWeak" @update:value="appStore.toggleColorWeak" />
        </n-space>
        <n-space justify="space-between">
          {{ $t('app.blackAndWhite') }}
          <n-switch :value="appStore.grayMode" @update:value="appStore.toggleGrayMode" />
        </n-space>
        <n-space align="center" justify="space-between">
          {{ $t('app.themeColor') }}
          <n-color-picker
            v-model:value="appStore.primaryColor" class="w-10em" :swatches="palette"
            @update:value="appStore.setPrimaryColor"
          />
        </n-space>
        <n-space align="center" justify="space-between">
          {{ $t('app.pageTransition') }}
          <n-select
            v-model:value="appStore.transitionAnimation" class="w-10em"
            :options="transitionSelectorOptions" @update:value="appStore.reloadPage"
          />
        </n-space>

        <n-divider>{{ $t('app.interfaceDisplay') }}</n-divider>
        <n-space justify="space-between">
          {{ $t('app.logoDisplay') }}
          <n-switch v-model:value="appStore.showLogo" />
        </n-space>
        <n-space justify="space-between">
          {{ $t('app.topProgress') }}
          <n-switch v-model:value="appStore.showProgress" />
        </n-space>
        <n-space justify="space-between">
          {{ $t('app.multitab') }}
          <n-switch v-model:value="appStore.showTabs" />
        </n-space>
        <n-space justify="space-between">
          {{ $t('app.bottomCopyright') }}
          <n-switch v-model:value="appStore.showFooter" />
        </n-space>
        <n-space justify="space-between">
          {{ $t('app.breadcrumb') }}
          <n-switch v-model:value="appStore.showBreadcrumb" />
        </n-space>
        <n-space justify="space-between">
          {{ $t('app.BreadcrumbIcon') }}
          <n-switch v-model:value="appStore.showBreadcrumbIcon" />
        </n-space>
        <n-space justify="space-between">
          {{ $t('app.watermake') }}
          <n-switch v-model:value="appStore.showWatermark" />
        </n-space>
      </n-space>

      <template #footer>
        <n-button type="error" @click="resetSetting">
          {{ $t('app.reset') }}
        </n-button>
      </template>
    </n-drawer-content>
  </n-drawer>
</template>
