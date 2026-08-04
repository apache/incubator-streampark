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

import type { MenuOption, MenuProps } from 'naive-ui'
import type { MaybeRefOrGetter } from '@vueuse/core'
import type { LayoutMode, MenuKey } from '../types'
import { buildMenuMetaMap, splitMenuData } from './menu-utils'
import { toValue } from '@vueuse/core'
import type { Ref } from 'vue'
import { computed, ref, watch } from 'vue'

export interface LayoutMenuReturn {
    horizontalMenuProps: MenuProps
    verticalMenuProps: MenuProps
    verticalExtraMenuProps: MenuProps
}

interface UseLayoutMenuOptions {
    menus: MaybeRefOrGetter<MenuOption[]>
    mode: MaybeRefOrGetter<LayoutMode>
    accordion?: MaybeRefOrGetter<boolean>
    childrenField?: string
}

function useMenus(menus: MaybeRefOrGetter<MenuOption[]>, childrenField = 'children') {
    const resolvedMenus = computed(() => toValue(menus))

    const menuKeyToMetaMap = computed(() => buildMenuMetaMap(resolvedMenus.value, childrenField))

    const fullKeys = computed(() => Array.from(menuKeyToMetaMap.value.keys()))

    function getAncestorKeys(key: MenuKey) {
        if (key == null) return []
        const keys: (string | number)[] = []
        let currentKey = menuKeyToMetaMap.value.get(key as string | number)?.parentKey
        while (currentKey != null) {
            keys.unshift(currentKey)
            const info = menuKeyToMetaMap.value.get(currentKey as string | number)
            if (!info) break
            currentKey = info.parentKey
        }
        return keys
    }

    function getDescendantKeys(key: MenuKey) {
        if (key == null) return []
        const keys: (string | number)[] = []
        let currentKey = menuKeyToMetaMap.value.get(key as string | number)?.childrenKeys[0]
        while (currentKey != null) {
            keys.push(currentKey)
            const info = menuKeyToMetaMap.value.get(currentKey)
            if (!info) break
            currentKey = info.childrenKeys[0]
        }
        return keys
    }

    function getMenuKeyFullPath(key: MenuKey) {
        if (key == null) return []
        const includeSelf = fullKeys.value.includes(key as string | number)
        return [
            ...getAncestorKeys(key),
            ...(includeSelf ? [key as string | number] : []),
            ...getDescendantKeys(key),
        ]
    }

    return {
        menus: resolvedMenus,
        fullKeys,
        getAncestorKeys,
        menuKeyToMetaMap,
        getMenuKeyFullPath,
    }
}

function bindVerticalMenu(
    menus: MenuOption[],
    activeKey: Ref<MenuKey>,
    expandedKeys: Ref<(string | number)[]>,
): LayoutMenuReturn {
    return {
        horizontalMenuProps: {},
        verticalExtraMenuProps: {},
        verticalMenuProps: {
            mode: 'vertical',
            options: menus,
            value: activeKey.value,
            expandedKeys: expandedKeys.value,
            onUpdateValue: (key) => {
                activeKey.value = key
            },
            onUpdateExpandedKeys: (keys) => {
                expandedKeys.value = keys
            },
        },
    }
}

function bindHorizontalMenu(
    menus: MenuOption[],
    activeKey: Ref<MenuKey>,
    expandedKeys: Ref<(string | number)[]>,
): LayoutMenuReturn {
    return {
        verticalMenuProps: {},
        verticalExtraMenuProps: {},
        horizontalMenuProps: {
            mode: 'horizontal',
            collapsed: false,
            responsive: true,
            options: menus,
            value: activeKey.value,
            expandedKeys: expandedKeys.value,
            onUpdateValue: (key) => {
                activeKey.value = key
            },
            onUpdateExpandedKeys: (keys) => {
                expandedKeys.value = keys
            },
        },
    }
}

export function useLayoutMenu(options: UseLayoutMenuOptions) {
    const activeKey = ref<MenuKey>(null)
    const expandedKeys = ref<(string | number)[]>([])
    const childrenField = options.childrenField ?? 'children'

    const mode = computed(() => toValue(options.mode))
    const accordion = computed(() => toValue(options.accordion ?? false))

    const { menus, getAncestorKeys, menuKeyToMetaMap, getMenuKeyFullPath } = useMenus(
        options.menus,
        childrenField,
    )

    const layout = computed<LayoutMenuReturn>(() => {
        const m = menus.value
        switch (mode.value) {
            case 'horizontal':
                return bindHorizontalMenu(m, activeKey, expandedKeys)
            case 'two-column': {
                const verticalMenuActiveKey = getMenuKeyFullPath(activeKey.value)[0] ?? null
                const verticalMenuData = splitMenuData(m, 1, { childrenField })[0] ?? []
                const info =
                    verticalMenuActiveKey != null
                        ? menuKeyToMetaMap.value.get(verticalMenuActiveKey as string | number)
                        : undefined
                const verticalExtraMenuData =
                    (info?.item[childrenField as keyof MenuOption] as MenuOption[] | undefined) ??
                    []
                return {
                    horizontalMenuProps: {},
                    verticalMenuProps: {
                        mode: 'vertical',
                        options: verticalMenuData,
                        value: verticalMenuActiveKey,
                        collapsed: true,
                        onUpdateValue: (key) => {
                            activeKey.value = key
                        },
                    },
                    verticalExtraMenuProps: {
                        mode: 'vertical',
                        value: activeKey.value,
                        expandedKeys: expandedKeys.value,
                        options: verticalExtraMenuData,
                        onUpdateValue: (key) => {
                            activeKey.value = key
                        },
                        onUpdateExpandedKeys: (keys) => {
                            expandedKeys.value = keys
                        },
                    },
                }
            }
            case 'mixed-sidebar': {
                const horizontalMenuActiveKey = getMenuKeyFullPath(activeKey.value)[0] ?? null
                const horizontalMenuData = splitMenuData(m, 1, { childrenField })[0] ?? []
                const info =
                    horizontalMenuActiveKey != null
                        ? menuKeyToMetaMap.value.get(horizontalMenuActiveKey as string | number)
                        : undefined
                const verticalMenuData =
                    (info?.item[childrenField as keyof MenuOption] as MenuOption[] | undefined) ??
                    []
                return {
                    horizontalMenuProps: {
                        mode: 'horizontal',
                        responsive: true,
                        collapsed: false,
                        options: horizontalMenuData,
                        value: horizontalMenuActiveKey,
                        onUpdateValue: (key) => {
                            activeKey.value = key
                        },
                    },
                    verticalMenuProps: {
                        mode: 'vertical',
                        value: activeKey.value,
                        options: verticalMenuData,
                        expandedKeys: expandedKeys.value,
                        onUpdateValue: (key) => {
                            activeKey.value = key
                        },
                        onUpdateExpandedKeys: (keys) => {
                            expandedKeys.value = keys
                        },
                    },
                    verticalExtraMenuProps: {},
                }
            }
            case 'mixed-two-column': {
                const horizontalMenuActiveKey = getMenuKeyFullPath(activeKey.value)[0] ?? null
                const verticalMenuActiveKey = (() => {
                    const topLevelKey = horizontalMenuActiveKey
                    const secondLevelKey = getMenuKeyFullPath(activeKey.value)[1] ?? null
                    if (activeKey.value !== topLevelKey && activeKey.value !== secondLevelKey)
                        return secondLevelKey
                    return activeKey.value
                })()
                const horizontalMenuData = splitMenuData(m, 1, { childrenField })[0] ?? []
                const topInfo =
                    horizontalMenuActiveKey != null
                        ? menuKeyToMetaMap.value.get(horizontalMenuActiveKey as string | number)
                        : undefined
                const childData =
                    (topInfo?.item[childrenField as keyof MenuOption] as
                        MenuOption[] | undefined) ?? []
                const verticalMenuData = splitMenuData(childData, 1, { childrenField })[0] ?? []
                const extraInfo =
                    verticalMenuActiveKey != null
                        ? menuKeyToMetaMap.value.get(verticalMenuActiveKey as string | number)
                        : undefined
                const verticalExtraMenuData =
                    (extraInfo?.item[childrenField as keyof MenuOption] as
                        MenuOption[] | undefined) ?? []
                return {
                    horizontalMenuProps: {
                        mode: 'horizontal',
                        responsive: true,
                        collapsed: false,
                        options: horizontalMenuData,
                        value: horizontalMenuActiveKey,
                        onUpdateValue: (key) => {
                            activeKey.value = key
                        },
                    },
                    verticalMenuProps: {
                        mode: 'vertical',
                        options: verticalMenuData,
                        value: verticalMenuActiveKey,
                        collapsed: true,
                        onUpdateValue: (key) => {
                            activeKey.value = key
                        },
                    },
                    verticalExtraMenuProps: {
                        mode: 'vertical',
                        value: activeKey.value,
                        expandedKeys: expandedKeys.value,
                        options: verticalExtraMenuData,
                        onUpdateValue: (key) => {
                            activeKey.value = key
                        },
                        onUpdateExpandedKeys: (keys) => {
                            expandedKeys.value = keys
                        },
                    },
                }
            }
            case 'full-content':
                return {
                    horizontalMenuProps: {},
                    verticalMenuProps: {},
                    verticalExtraMenuProps: {},
                }
            case 'sidebar':
            case 'vertical':
            default:
                return bindVerticalMenu(m, activeKey, expandedKeys)
        }
    })

    watch(activeKey, () => {
        const keys = accordion.value
            ? getAncestorKeys(activeKey.value)
            : [...expandedKeys.value, ...getAncestorKeys(activeKey.value)]
        expandedKeys.value = Array.from(new Set(keys))
    })

    return {
        activeKey,
        layout,
        getMenuKeyFullPath,
    }
}
