import type { App } from 'vue'
import { createRouter, createWebHashHistory, createWebHistory } from 'vue-router'
import { setupRouterGuard } from './guard'
import { routes } from './routes.inner'

const { VITE_ROUTE_MODE = 'hash', VITE_BASE_URL } = import.meta.env
export const router = createRouter({
    history:
        VITE_ROUTE_MODE === 'hash'
            ? createWebHashHistory(VITE_BASE_URL)
            : createWebHistory(VITE_BASE_URL),
    routes,
})
export async function installRouter(app: App) {
    setupRouterGuard(router)
    app.use(router)
    await router.isReady()
}
