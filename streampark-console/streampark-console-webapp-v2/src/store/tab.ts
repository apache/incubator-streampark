import type { RouteLocationNormalized } from 'vue-router'
import { router } from '@/router'

interface TabState {
    pinTabs: RouteLocationNormalized[]
    tabs: RouteLocationNormalized[]
    currentTabPath: string
}
export const useTabStore = defineStore('tab-store', {
    state: (): TabState => {
        return {
            pinTabs: [],
            tabs: [],
            currentTabPath: '',
        }
    },
    getters: {
        allTabs: (state) => [...state.pinTabs, ...state.tabs],
    },
    actions: {
        addTab(route: RouteLocationNormalized) {
            if (route.meta.withoutTab) return

            if (this.hasExistTab(route.fullPath as string)) return

            if (route.meta.pinTab) this.pinTabs.push(route)
            else this.tabs.push(route)
        },
        async closeTab(fullPath: string) {
            const tabsLength = this.tabs.length
            if (this.tabs.length > 1) {
                const index = this.getTabIndex(fullPath)
                const isLast = index + 1 === tabsLength
                if (this.currentTabPath === fullPath && !isLast) {
                    router.push(this.tabs[index + 1].fullPath)
                } else if (this.currentTabPath === fullPath && isLast) {
                    router.push(this.tabs[index - 1].fullPath)
                }
            }
            this.tabs = this.tabs.filter((item) => {
                return item.fullPath !== fullPath
            })
            if (tabsLength - 1 === 0) router.push('/')
        },

        closeOtherTabs(fullPath: string) {
            const index = this.getTabIndex(fullPath)
            this.tabs = this.tabs.filter((item, i) => i === index)
        },
        closeLeftTabs(fullPath: string) {
            const index = this.getTabIndex(fullPath)
            this.tabs = this.tabs.filter((item, i) => i >= index)
        },
        closeRightTabs(fullPath: string) {
            const index = this.getTabIndex(fullPath)
            this.tabs = this.tabs.filter((item, i) => i <= index)
        },
        clearAllTabs() {
            this.tabs.length = 0
            this.pinTabs.length = 0
        },
        closeAllTabs() {
            this.tabs.length = 0
            router.push('/')
        },

        hasExistTab(fullPath: string) {
            const _tabs = [...this.tabs, ...this.pinTabs]
            return _tabs.some((item) => {
                return item.fullPath === fullPath
            })
        },
        setCurrentTab(fullPath: string) {
            this.currentTabPath = fullPath
        },
        getTabIndex(fullPath: string) {
            return this.tabs.findIndex((item) => {
                return item.fullPath === fullPath
            })
        },
        modifyTab(fullPath: string, modifyFn: (route: RouteLocationNormalized) => void) {
            const index = this.getTabIndex(fullPath)
            modifyFn(this.tabs[index])
        },
    },
    persist: {
        storage: sessionStorage,
    },
})
