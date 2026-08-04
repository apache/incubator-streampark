<script setup lang="ts">
import { useAppStore } from '@/store'
import { ionIconComponent } from '@/utils/ionIcon'
import { NFlex } from 'naive-ui'

const { t } = useI18n()

const appStore = useAppStore()

const options = computed(() => {
    return [
        {
            label: t('app.light'),
            value: 'light',
            icon: ionIconComponent('SunnyOutline'),
        },
        {
            label: t('app.dark'),
            value: 'dark',
            icon: ionIconComponent('MoonOutline'),
        },
        {
            label: t('app.system'),
            value: 'auto',
            icon: ionIconComponent('DesktopOutline'),
        },
    ]
})

function renderLabel(option: any) {
    return h(
        NFlex,
        { align: 'center' },
        {
            default: () => [h(NIcon, null, { default: () => h(option.icon) }), option.label],
        },
    )
}
</script>

<template>
    <n-popselect
        :value="appStore.storeColorMode"
        :render-label="renderLabel"
        :options="options"
        trigger="click"
        @update:value="appStore.setColorMode"
    >
        <CommonWrapper>
            <n-icon>
                <IonIcon v-if="appStore.storeColorMode === 'dark'" name="MoonOutline" />
                <IonIcon v-else-if="appStore.storeColorMode === 'light'" name="SunnyOutline" />
                <IonIcon v-else name="DesktopOutline" />
            </n-icon>
        </CommonWrapper>
    </n-popselect>
</template>

<style scoped></style>
