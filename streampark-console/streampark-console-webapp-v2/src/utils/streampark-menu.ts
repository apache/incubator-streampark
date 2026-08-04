/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import type { AppRouteRecordRaw } from '@/router/types'
import { resolveMenuIcon } from '@/components/Icon/streampark-icon-map'

let routeId = 1

function nextId() {
    return routeId++
}

function mapIcon(icon?: string, path?: string, menuType?: AppRoute.MenuType): string | undefined {
    return resolveMenuIcon(icon, path, menuType)
}

function resolveComponentPath(component: string): string {
    const normalized = component.replace(/^\//, '').replace(/\.(vue|tsx)$/, '')
    return `/views/${normalized}.vue`
}

export function streamParkMenuToRowRoutes(
    routes: AppRouteRecordRaw[],
    pid: number | null = null,
    activeMenuPath: string | null = null,
): AppRoute.RowRoute[] {
    const rows: AppRoute.RowRoute[] = []

    for (const route of routes) {
        const componentName = route.component ? String(route.component) : ''
        const layoutKey = componentName.toUpperCase()
        const isLayout =
            !componentName ||
            layoutKey === 'LAYOUT' ||
            layoutKey === 'PAGEVIEW' ||
            layoutKey === 'BASICVIEW'

        const currentId = nextId()
        const path = route.path.startsWith('/') ? route.path : `/${route.path}`
        const hidden = Boolean(route.meta?.hidden)
        const activeMenu =
            hidden && activeMenuPath
                ? activeMenuPath
                : ((route.meta?.currentActiveMenu as string | undefined) ??
                  (route.meta?.activeMenu as string | undefined))

        if (isLayout) {
            rows.push({
                id: currentId,
                pid,
                name: String(route.name),
                path,
                title: String(route.meta?.title || route.name),
                icon: mapIcon(route.meta?.icon as string, path, 'dir'),
                menuType: 'dir',
                componentPath: null,
                requiresAuth: true,
                hide: hidden,
                activeMenu,
                order: route.meta?.orderNo as number | undefined,
            })
            if (route.children?.length)
                rows.push(
                    ...streamParkMenuToRowRoutes(
                        route.children as AppRouteRecordRaw[],
                        currentId,
                        activeMenuPath,
                    ),
                )
        } else {
            rows.push({
                id: currentId,
                pid,
                name: String(route.name),
                path,
                title: String(route.meta?.title || route.name),
                icon: mapIcon(route.meta?.icon as string, path, 'page'),
                menuType: 'page',
                componentPath: resolveComponentPath(componentName),
                requiresAuth: true,
                hide: hidden,
                activeMenu,
                keepAlive: Boolean(route.meta?.keepAlive),
                order: route.meta?.orderNo as number | undefined,
            })
            const childActiveMenu = hidden ? activeMenuPath : path
            if (route.children?.length)
                rows.push(
                    ...streamParkMenuToRowRoutes(
                        route.children as AppRouteRecordRaw[],
                        currentId,
                        childActiveMenu,
                    ),
                )
        }
    }

    return rows
}

export function normalizeStreamParkMenu(raw: AppRouteRecordRaw[]): AppRoute.RowRoute[] {
    routeId = 1
    if (!raw?.length) return []

    let tree = raw
    if (raw.length === 1 && raw[0].children?.length) tree = raw[0].children as AppRouteRecordRaw[]

    return streamParkMenuToRowRoutes(
        tree.map((item) => {
            if (item.children?.length && !item.redirect) {
                const visible = (item.children as AppRouteRecordRaw[]).find((c) => !c.meta?.hidden)
                if (visible)
                    item.redirect = visible.path.startsWith('/') ? visible.path : `/${visible.path}`
            }
            return item
        }),
    )
}

export function resetStreamParkMenuId() {
    routeId = 1
}
