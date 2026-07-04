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

import type { MenuOption } from 'naive-ui'

const levelSymbol = Symbol('level')

export function bfs(
  data: MenuOption[],
  callback: (item: MenuOption, level: number, stop: () => void, parent?: MenuOption) => void,
  childrenField = 'children',
) {
  let stopped = false
  const stop = () => { stopped = true }

  type QueueItem = MenuOption & { [levelSymbol]: number, __parent?: MenuOption }
  let queue: QueueItem[] = data.map(item => ({ ...item, [levelSymbol]: 1 }))

  while (queue.length > 0) {
    const item = queue.shift()!
    const level = item[levelSymbol]
    const parent = item.__parent
    const { [levelSymbol]: _, __parent: __p, ...originalItem } = item
    callback(originalItem as MenuOption, level, stop, parent)
    if (stopped) {
      queue = []
      break
    }
    const children = item[childrenField as keyof MenuOption] as MenuOption[] | undefined
    if (children?.length) {
      queue.push(...children.map(child => ({
        ...child,
        [levelSymbol]: level + 1,
        __parent: originalItem as MenuOption,
      })))
    }
  }
}

export function splitMenuData(
  menus: MenuOption[],
  _levelOfSplit: number,
  options: { childrenField?: string } = {},
) {
  const { childrenField = 'children' } = options
  const firstLevelMenus: MenuOption[] = []

  bfs(menus, (menu, level, stop) => {
    if (level > 2) {
      stop()
      return
    }
    if (level === 1) {
      const { [childrenField]: _, ...menuWithoutChildren } = menu as MenuOption & Record<string, unknown>
      firstLevelMenus.push(menuWithoutChildren)
    }
  }, childrenField)

  return [firstLevelMenus] as const
}

export function buildMenuMetaMap(menus: MenuOption[], childrenField = 'children') {
  const map = new Map<string | number, {
    item: MenuOption
    parentKey: string | number | null | undefined
    childrenKeys: (string | number)[]
  }>()

  bfs(menus, (item, _level, _stop, parent) => {
    const menuKey = item.key
    const parentMenuKey = parent?.key
    if (menuKey == null)
      return
    if (parentMenuKey != null) {
      const parentEntry = map.get(parentMenuKey as string | number)
      parentEntry?.childrenKeys.push(menuKey as string | number)
    }
    map.set(menuKey as string | number, {
      item,
      childrenKeys: [],
      parentKey: parentMenuKey,
    })
  }, childrenField)

  return map
}
