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

/** Top-level menu dirs — same svg sprites as legacy SimpleSubMenu.vue. */
export const MENU_PATH_SVG_ICONS: Record<string, string> = {
  '/flink': 'flink3|svg',
  '/spark': 'spark|svg',
  '/resource': 'resource|svg',
  '/setting': 'settings|svg',
  '/system': 'management|svg',
}

/** Backend / legacy short icon keys (breadcrumb, permission tree, etc.). */
export const LEGACY_ICON_ALIASES: Record<string, string> = {
  github: 'ant-design:github-outlined',
  user: 'ant-design:user-outlined',
  setting: 'ant-design:setting-outlined',
  menu: 'ant-design:menu-outlined',
}

export function isBlankMenuIcon(icon?: string | null) {
  return !icon || icon === '0'
}

/**
 * Legacy sidebar (SimpleMenu): only top-level directories show custom SVG icons.
 * Leaf menu items do not display icons.
 */
export function resolveMenuIcon(
  icon?: string | null,
  path?: string,
  menuType?: AppRoute.MenuType,
): string | undefined {
  if (menuType === 'dir' && path && MENU_PATH_SVG_ICONS[path])
    return MENU_PATH_SVG_ICONS[path]

  if (menuType === 'page')
    return undefined

  if (!isBlankMenuIcon(icon)) {
    const raw = String(icon)
    if (raw.includes(':') || raw.endsWith('|svg'))
      return raw
    if (LEGACY_ICON_ALIASES[raw])
      return LEGACY_ICON_ALIASES[raw]
    return `ant-design:${raw}-outlined`
  }

  return undefined
}
