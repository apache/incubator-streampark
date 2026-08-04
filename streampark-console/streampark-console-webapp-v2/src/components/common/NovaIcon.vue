<script setup lang="ts">
import { Icon } from '@iconify/vue'
import { computed } from 'vue'
import SvgIcon from '@/components/Icon/src/SvgIcon.vue'
import { resolveIconifyRef, resolveViconComponent } from '@/components/Icon/xicons'

interface iconPorps {
    icon?: string
    color?: string
    size?: number
    depth?: 1 | 2 | 3 | 4 | 5
}
const { size = 18, icon } = defineProps<iconPorps>()

const isSvgIcon = computed(() => Boolean(icon?.endsWith('|svg')))
const svgName = computed(() => icon?.replace('|svg', '') ?? '')

const isLocal = computed(() => Boolean(icon?.startsWith('local:')))

const iconifyName = computed(() => {
    if (!icon || isSvgIcon.value || isLocal.value) return ''
    if (icon.includes(':')) return icon
    return resolveIconifyRef(icon)
})

const viconComponent = computed(() => {
    if (!icon || isSvgIcon.value || isLocal.value || iconifyName.value) return undefined
    return resolveViconComponent(icon)
})

function getLocalIcon(iconName: string) {
    const svgName = iconName.replace('local:', '')
    const svg = import.meta.glob<string>('@/assets/svg-icons/*.svg', {
        query: '?raw',
        import: 'default',
        eager: true,
    })

    return svg[`/src/assets/svg-icons/${svgName}.svg`]
}
</script>

<template>
    <n-icon v-if="icon" :size="size" :depth="depth" :color="color">
        <SvgIcon v-if="isSvgIcon" :name="svgName" :size="size" />
        <!-- eslint-disable-next-line vue/no-v-html -- local bundled SVG assets only -->
        <i v-else-if="isLocal" v-html="getLocalIcon(icon)" />
        <Icon v-else-if="iconifyName" :icon="iconifyName" />
        <component :is="viconComponent" v-else-if="viconComponent" />
    </n-icon>
</template>
