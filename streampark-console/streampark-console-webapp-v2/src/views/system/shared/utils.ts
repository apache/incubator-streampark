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

import type { TreeOption } from 'naive-ui'

export interface MenuTreeNode {
    id: string
    text: string
    permission?: string
    path?: string
    children?: MenuTreeNode[]
}

const permissionIdMap = new Map<string, string>()

export function formatMenuLabel(text: string, t: (key: string) => string) {
    if (/^\w+\.\w+$/.test(text)) return t(`menu.${text}`)
    return text
}

export function transformMenuTree(
    nodes: MenuTreeNode[] | undefined,
    t: (key: string) => string,
): TreeOption[] {
    if (!nodes?.length) return []
    return nodes.map((node) => {
        const mapKey = node.permission || node.path
        if (mapKey && !permissionIdMap.has(mapKey)) permissionIdMap.set(mapKey, node.id)
        return {
            key: node.id,
            label: formatMenuLabel(node.text, t),
            children: node.children?.length ? transformMenuTree(node.children, t) : undefined,
        }
    })
}

export function collectLeafKeys(nodes: MenuTreeNode[] | undefined, keys: string[] = []) {
    if (!nodes?.length) return keys
    for (const node of nodes) {
        if (node.children?.length) collectLeafKeys(node.children, keys)
        else keys.push(node.id)
    }
    return keys
}

export function filterCheckedLeafKeys(allRoleMenuIds: string[], leafKeys: string[]) {
    const leafSet = new Set(leafKeys)
    return allRoleMenuIds.filter((id) => leafSet.has(id))
}

export function getPermissionMenuId(permission: string) {
    return permissionIdMap.get(permission)
}

export function resetPermissionIdMap() {
    permissionIdMap.clear()
}

export function resolveListData<T>(
    data: T[] | { records?: T[]; total?: number | string } | null | undefined,
) {
    if (Array.isArray(data)) return { records: data, total: data.length }
    return {
        records: data?.records ?? [],
        total: Number(data?.total ?? data?.records?.length ?? 0),
    }
}
