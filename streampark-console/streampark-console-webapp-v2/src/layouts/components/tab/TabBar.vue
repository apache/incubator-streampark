<script setup lang="ts">
import type { RouteLocationNormalized } from 'vue-router'
import { useAppStore, useTabStore } from '@/store'
import { useTabScroll } from '@/hooks/useTabScroll'
import { useDraggable } from 'vue-draggable-plus'
import { ionIcon } from '@/utils/ionIcon'
import { SP_ICONS as I } from '@/constants/streamparkIcons'
import DropTabs from './DropTabs.vue'
import Reload from './Reload.vue'
import TabBarItem from './TabBarItem.vue'

const tabStore = useTabStore()
const { tabs } = storeToRefs(useTabStore())
const appStore = useAppStore()

const { scrollbar, onWheel } = useTabScroll(computed(() => tabStore.currentTabPath))

const router = useRouter()
function handleTab(route: RouteLocationNormalized) {
    router.push(route.fullPath)
}
const { t } = useI18n()
const options = computed(() => {
    return [
        {
            label: t('common.reload'),
            key: 'reload',
            icon: () => ionIcon(I.tabReload),
        },
        {
            label: t('common.close'),
            key: 'closeCurrent',
            icon: () => ionIcon(I.tabClose),
        },
        {
            label: t('app.closeOther'),
            key: 'closeOther',
            icon: () => ionIcon(I.tabCloseOther),
        },
        {
            label: t('app.closeLeft'),
            key: 'closeLeft',
            icon: () => ionIcon(I.tabCloseLeft),
        },
        {
            label: t('app.closeRight'),
            key: 'closeRight',
            icon: () => ionIcon(I.tabCloseRight),
        },
        {
            label: t('app.closeAll'),
            key: 'closeAll',
            icon: () => ionIcon(I.tabCloseAll),
        },
    ]
})
const showDropdown = ref(false)
const x = ref(0)
const y = ref(0)
const currentRoute = ref()

function handleSelect(key: string) {
    showDropdown.value = false
    interface HandleFn {
        [key: string]: () => void
    }
    const handleFn: HandleFn = {
        reload() {
            appStore.reloadPage()
        },
        closeCurrent() {
            tabStore.closeTab(currentRoute.value.fullPath)
        },
        closeOther() {
            tabStore.closeOtherTabs(currentRoute.value.fullPath)
        },
        closeLeft() {
            tabStore.closeLeftTabs(currentRoute.value.fullPath)
        },
        closeRight() {
            tabStore.closeRightTabs(currentRoute.value.fullPath)
        },
        closeAll() {
            tabStore.closeAllTabs()
        },
    }
    handleFn[key]()
}
function handleContextMenu(e: MouseEvent, route: RouteLocationNormalized) {
    e.preventDefault()
    currentRoute.value = route
    showDropdown.value = false
    nextTick().then(() => {
        showDropdown.value = true
        x.value = e.clientX
        y.value = e.clientY
    })
}
function onClickoutside() {
    showDropdown.value = false
}

const el = ref()

useDraggable(el, tabs, {
    animation: 150,
    ghostClass: 'ghost',
})
</script>

<template>
    <n-scrollbar
        ref="scrollbar"
        class="relative flex h-full tab-bar-scroller-wrapper"
        content-class="h-full pr-34 tab-bar-scroller-content"
        :x-scrollable="true"
        @wheel="onWheel"
    >
        <div class="p-l-2 flex wh-full relative">
            <div class="flex items-end">
                <TabBarItem
                    v-for="item in tabStore.pinTabs"
                    :key="item.fullPath"
                    :value="tabStore.currentTabPath"
                    :route="item"
                    @click="handleTab(item)"
                />
            </div>
            <div ref="el" class="flex items-end flex-1">
                <TabBarItem
                    v-for="item in tabStore.tabs"
                    :key="item.fullPath"
                    :value="tabStore.currentTabPath"
                    :route="item"
                    closable
                    :data-tab-path="item.fullPath"
                    @close="tabStore.closeTab"
                    @click="handleTab(item)"
                    @contextmenu="handleContextMenu($event, item)"
                />
                <n-dropdown
                    placement="bottom-start"
                    trigger="manual"
                    :x="x"
                    :y="y"
                    :options="options"
                    :show="showDropdown"
                    :on-clickoutside="onClickoutside"
                    @select="handleSelect"
                />
            </div>
        </div>
        <n-el class="absolute right-0 top-0 flex items-center gap-1 bg-[var(--card-color)] h-full">
            <Reload />
            <div id="sp-content-fullscreen-trigger" />
            <DropTabs />
        </n-el>
    </n-scrollbar>
</template>

<style scoped>
.ghost {
    opacity: 0.5;
    background: var(--hover-color);
}
</style>
