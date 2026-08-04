import type { MenuOption } from 'naive-ui'
import type { RouteRecordRaw } from 'vue-router'
import { usePermission } from '@/hooks'
import Layout from '@/layouts/index.vue'
import { arrayToTree, renderIcon, translateMenuTitle } from '@/utils'
import { clone, min, omit, pick } from 'radash'
import { RouterLink } from 'vue-router'

import { resolveViewComponentName } from './component-names'

const metaFields: AppRoute.MetaKeys[] = [
    'title',
    'icon',
    'requiresAuth',
    'roles',
    'keepAlive',
    'hide',
    'order',
    'href',
    'activeMenu',
    'withoutTab',
    'pinTab',
    'menuType',
    'componentName',
]

const nativeViewModules = import.meta.glob('/src/views/**/*.vue')

/** Backend menu component path aliases (historical paths -> current views). */
const COMPONENT_ALIASES: Record<string, string> = {
    'spark/app/create.vue': 'spark/app/Add.vue',
    'spark/app/edit.vue': 'spark/app/Edit.vue',
    'base/redirect/index.vue': 'build-in/redirect/index.vue',
    'base/lock/index.vue': 'build-in/lock/index.vue',
    'base/error-log/index.vue': 'build-in/error-log/index.vue',
    'setting/FlinkGateway/index.vue': 'flink/gateway/index.vue',
}

function normalizeComponentRel(componentPath: string): string {
    let rel = componentPath
    if (rel.startsWith('/src/views/')) rel = rel.replace('/src/views/', '')
    else if (rel.startsWith('/views/')) rel = rel.replace(/^\/views\//, '')

    if (COMPONENT_ALIASES[rel]) rel = COMPONENT_ALIASES[rel]

    if (rel.endsWith('/View.vue')) rel = rel.replace(/\/View\.vue$/, '/index.vue')

    return rel
}

function resolveNativeViewPath(componentPath: string): string {
    return `/src/views/${normalizeComponentRel(componentPath)}`
}

function resolveViewComponent(componentPath: string) {
    const normalized = componentPath.startsWith('/src') ? componentPath : `/src${componentPath}`

    const nativePath = resolveNativeViewPath(normalized)
    if (nativeViewModules[nativePath]) return nativeViewModules[nativePath]

    console.warn(`[router] view not found: ${componentPath} -> ${nativePath}`)
    return undefined
}

function standardizedRoutes(route: AppRoute.RowRoute[]) {
    return clone(route).map((i) => {
        const route = omit(i, metaFields)
        Reflect.set(route, 'meta', pick(i, metaFields))
        return route
    }) as AppRoute.Route[]
}

function flattenAuthRoutes(routes: AppRoute.Route[]): AppRoute.Route[] {
    const pages: AppRoute.Route[] = []
    const visit = (items: AppRoute.Route[]) => {
        for (const item of items) {
            if (item.component) pages.push({ ...item, children: undefined })
            if (item.children?.length) visit(item.children)
        }
    }
    visit(routes)
    return pages
}

/** Directory menu paths (PageView) need redirect routes after flattening. */
function buildDirectoryRedirectRoutes(rowRoutes: AppRoute.RowRoute[]): AppRoute.Route[] {
    const redirects: AppRoute.Route[] = []
    const dirs = rowRoutes.filter((r) => r.menuType === 'dir')

    const findFirstVisiblePage = (parentId: number): AppRoute.RowRoute | undefined => {
        const children = rowRoutes
            .filter((r) => r.pid === parentId && !r.hide)
            .sort((a, b) => (a.order ?? 999) - (b.order ?? 999))
        for (const child of children) {
            if (child.menuType === 'page' && child.componentPath) return child
            const nested = findFirstVisiblePage(child.id)
            if (nested) return nested
        }
        return undefined
    }

    for (const dir of dirs) {
        const target = findFirstVisiblePage(dir.id)
        if (!target) continue
        redirects.push({
            id: -Math.abs(dir.id),
            pid: null,
            name: `dir_redirect_${String(dir.name).replace(/\W+/g, '_')}`,
            path: dir.path,
            redirect: target.path,
            component: Layout,
            meta: {
                title: dir.title,
                hide: true,
                requiresAuth: true,
            },
        })
    }
    return redirects
}

export function createRoutes(routes: AppRoute.RowRoute[]) {
    const { hasPermission } = usePermission()

    let resultRouter = standardizedRoutes(routes)
    resultRouter = resultRouter.filter((i) => hasPermission(i.meta.roles))

    resultRouter = resultRouter.map((item: AppRoute.Route) => {
        if (item.componentPath && !item.redirect) {
            item.component = resolveViewComponent(`/src${item.componentPath}`)
            const componentName = resolveViewComponentName(item.componentPath)
            if (componentName) item.meta.componentName = componentName
        }
        return item
    })

    resultRouter = arrayToTree(resultRouter) as AppRoute.Route[]

    setRedirect(resultRouter)
    resultRouter = flattenAuthRoutes(resultRouter)
    resultRouter = [...buildDirectoryRedirectRoutes(routes), ...resultRouter]

    const appRootRoute: RouteRecordRaw = {
        path: '/appRoot',
        name: 'appRoot',
        redirect: import.meta.env.VITE_HOME_PATH,
        component: Layout,
        meta: {
            title: 'StreamPark',
            icon: 'home',
        },
        children: [],
    }

    appRootRoute.children = resultRouter as unknown as RouteRecordRaw[]
    return appRootRoute
}

export function generateCacheRoutes(routes: AppRoute.RowRoute[]) {
    return routes
        .filter((i) => i.keepAlive && i.componentPath)
        .map((i) => resolveViewComponentName(i.componentPath))
        .filter((name): name is string => Boolean(name))
}

function setRedirect(routes: AppRoute.Route[]) {
    routes.forEach((route) => {
        if (route.children) {
            if (!route.redirect) {
                const visibleChilds = route.children.filter((child) => !child.meta.hide)
                let target = visibleChilds[0]
                const orderChilds = visibleChilds.filter((child) => child.meta.order)
                if (orderChilds.length > 0)
                    target = min(orderChilds, (i) => i.meta.order!) as AppRoute.Route
                if (target) route.redirect = target.path
            }
            setRedirect(route.children)
        }
    })
}

export function createMenus(userRoutes: AppRoute.RowRoute[]) {
    const resultMenus = standardizedRoutes(userRoutes)
    const visibleMenus = resultMenus.filter((route) => !route.meta.hide)
    return arrayToTree(transformAuthRoutesToMenus(visibleMenus))
}

function transformAuthRoutesToMenus(userRoutes: AppRoute.Route[]) {
    const { hasPermission } = usePermission()
    return userRoutes
        .filter((i) => hasPermission(i.meta.roles))
        .sort((a, b) => {
            if (a.meta?.order && b.meta?.order) return a.meta.order - b.meta.order
            if (a.meta?.order) return -1
            if (b.meta?.order) return 1
            return 0
        })
        .map((item) => {
            const rawTitle = item.meta.title || String(item.name)
            const target: MenuOption = {
                id: item.id,
                pid: item.pid,
                label:
                    !item.meta.menuType || item.meta.menuType === 'page'
                        ? () =>
                              h(
                                  RouterLink,
                                  { to: { path: item.path } },
                                  { default: () => translateMenuTitle(rawTitle) },
                              )
                        : () => translateMenuTitle(rawTitle),
                key: item.path,
                icon:
                    item.meta.menuType === 'dir' && item.meta.icon
                        ? renderIcon(item.meta.icon)
                        : undefined,
            }
            return target
        })
}
