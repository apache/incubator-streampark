import type { Router } from 'vue-router'
import { nextTick } from 'vue'
import { useAppStore, useAuthStore, useRouteStore, useTabStore } from '@/store'
import { PageEnum } from '@/enums/pageEnum'
import { getToken } from '@/utils/auth'
import { translateMenuTitle } from '@/utils'

const title = import.meta.env.VITE_APP_NAME

export function setupRouterGuard(router: Router) {
    const appStore = useAppStore()
    const routeStore = useRouteStore()
    const tabStore = useTabStore()

    router.beforeEach(async (to) => {
        if (to.meta.href) {
            window.open(to.meta.href as string)
            return false
        }

        const isLogin = Boolean(getToken())
        const homePath = import.meta.env.VITE_HOME_PATH || PageEnum.BASE_HOME

        if (!isLogin && to.path === homePath && to.query.from === 'sso') {
            try {
                const authStore = useAuthStore()
                await authStore.loginWithSso()
                return { path: homePath, replace: true }
            } catch {
                return { path: '/login', replace: true }
            }
        }

        if (to.name === 'root') {
            return { path: isLogin ? import.meta.env.VITE_HOME_PATH : '/login', replace: true }
        }

        if (to.name === 'login') {
            if (isLogin) return { path: import.meta.env.VITE_HOME_PATH, replace: true }
            return true
        }

        if (to.meta.requiresAuth === false) return true

        if (!isLogin) {
            return { path: '/login', query: { redirect: to.fullPath }, replace: true }
        }

        if (!routeStore.isInitAuthRoute) {
            if (appStore.showProgress) window.$loadingBar?.start()
            try {
                await routeStore.initAuthRoute()
                return { path: to.fullPath, replace: true, query: to.query, hash: to.hash }
            } catch {
                const authStore = useAuthStore()
                await authStore.logout()
                return { path: '/login', query: { redirect: to.fullPath }, replace: true }
            }
        }

        if (appStore.showProgress) window.$loadingBar?.start()

        return true
    })

    router.beforeResolve((to) => {
        const menuKey = (to.meta.activeMenu as string) || to.path
        routeStore.setActiveMenu(menuKey)
        tabStore.addTab(to)
        tabStore.setCurrentTab(to.fullPath as string)
    })

    router.afterEach((to) => {
        const pageTitle = translateMenuTitle(String(to.meta.title || ''))
        document.title = pageTitle ? `${pageTitle} - ${title}` : title
        if (appStore.showProgress) {
            nextTick(() => {
                window.$loadingBar?.finish()
            })
        }
    })
}
