<script setup lang="ts">
import { naiveI18nOptions } from '@/utils'
import { darkTheme } from 'naive-ui'
import { useAppStore } from './store'

const appStore = useAppStore()

const naiveLocale = computed(() => {
    const lang = appStore.lang as keyof typeof naiveI18nOptions
    return naiveI18nOptions[lang] ?? naiveI18nOptions.zh_CN
})
</script>

<template>
    <n-config-provider
        class="wh-full"
        inline-theme-disabled
        :theme="appStore.colorMode === 'dark' ? darkTheme : null"
        :locale="naiveLocale.locale"
        :date-locale="naiveLocale.dateLocale"
        :theme-overrides="appStore.theme"
    >
        <naive-provider>
            <router-view />
        </naive-provider>
    </n-config-provider>
</template>
