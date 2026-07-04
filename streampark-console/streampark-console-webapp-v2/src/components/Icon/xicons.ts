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

import type { Component } from 'vue'
import * as IonIcons from '@vicons/ionicons5'
import { LEGACY_ICON_ALIASES } from './streampark-icon-map'
import { ION_ICON_REGISTRY, ionIdToComponentName } from './vicons-registry'

/** Short icon keys used in forms / permission trees. */
const SHORT_ICON_ALIASES: Record<string, string> = {
  user: 'ant-design:user-outlined',
  star: 'ant-design:star-outlined',
  skin: 'ant-design:skin-outlined',
  mail: 'ant-design:mail-outlined',
  smile: 'ant-design:smile-outlined',
  login: 'ant-design:login-outlined',
  message: 'ant-design:message-outlined',
  'clock-circle': 'ant-design:clock-circle-outlined',
  setting: 'ant-design:setting-outlined',
  team: 'ant-design:team-outlined',
  key: 'ant-design:key-outlined',
  link: 'ant-design:link-outlined',
  code: 'ant-design:code-outlined',
  book: 'ant-design:book-outlined',
  crown: 'ant-design:crown-outlined',
  home: 'ant-design:home-outlined',
  menu: 'ant-design:menu-outlined',
  delete: 'ant-design:delete-outlined',
  edit: 'ant-design:edit-outlined',
  eye: 'ant-design:eye-outlined',
  'eye-invisible': 'ant-design:eye-invisible-outlined',
  'eye-off': 'ant-design:eye-invisible-outlined',
  plus: 'ant-design:plus-outlined',
  search: 'ant-design:search-outlined',
  lock: 'ant-design:lock-outlined',
  unlock: 'ant-design:unlock-outlined',
  warning: 'ant-design:warning-outlined',
  info: 'ant-design:info-circle-outlined',
  question: 'ant-design:question-circle-outlined',
  github: 'ant-design:github-outlined',
  cloud: 'ant-design:cloud-outlined',
  database: 'ant-design:database-outlined',
  container: 'ant-design:container-outlined',
  deployment: 'ant-design:deployment-unit-outlined',
  dashboard: 'ant-design:dashboard-outlined',
  project: 'ant-design:project-outlined',
  alert: 'ant-design:alert-outlined',
  sync: 'ant-design:sync-outlined',
  thunderbolt: 'ant-design:thunderbolt-outlined',
  copy: 'ant-design:copy-outlined',
  history: 'ant-design:history-outlined',
  swap: 'ant-design:swap-outlined',
  form: 'ant-design:form-outlined',
  drag: 'ant-design:drag-outlined',
  ellipsis: 'ant-design:ellipsis-outlined',
  fullscreen: 'ant-design:fullscreen-outlined',
  'fullscreen-exit': 'ant-design:fullscreen-exit-outlined',
  redo: 'ant-design:reload-outlined',
  check: 'ant-design:check-outlined',
  close: 'ant-design:close-outlined',
  trophy: 'ant-design:trophy-outlined',
  bell: 'ant-design:bell-outlined',
  camera: 'ant-design:camera-outlined',
  pause: 'ant-design:pause-circle-outlined',
  play: 'ant-design:play-circle-outlined',
  arrow: 'ant-design:arrow-right-outlined',
  'arrow-left': 'ant-design:arrow-left-outlined',
  'arrow-right': 'ant-design:arrow-right-outlined',
  left: 'ant-design:left-outlined',
  right: 'ant-design:right-outlined',
  'double-left': 'ant-design:double-left-outlined',
  'double-right': 'ant-design:double-right-outlined',
  'menu-fold': 'ant-design:menu-fold-outlined',
  'menu-unfold': 'ant-design:menu-unfold-outlined',
  'user-add': 'ant-design:user-add-outlined',
  'user-switch': 'ant-design:user-switch-outlined',
  'plus-circle': 'ant-design:plus-circle-outlined',
  'down-circle': 'ant-design:down-circle-outlined',
  'question-circle': 'ant-design:question-circle-outlined',
  'pause-circle': 'ant-design:pause-circle-outlined',
  'play-circle': 'ant-design:play-circle-outlined',
  inbox: 'ant-design:inbox-outlined',
  hdd: 'ant-design:hdd-outlined',
  'share-alt': 'ant-design:share-alt-outlined',
  'column-height': 'ant-design:column-height-outlined',
  'file-done': 'ant-design:file-done-outlined',
  'cloud-upload': 'ant-design:cloud-upload-outlined',
}

/** PascalCase legacy names -> Iconify (fallback when views still pass ionicons names). */
const VICON_TO_ICONIFY: Record<string, string> = {
  CreateOutline: 'clarity:note-edit-line',
  CloudUploadOutline: 'ant-design:cloud-upload-outlined',
  CubeOutline: 'ant-design:container-outlined',
  PlayCircleOutline: 'ant-design:play-circle-outlined',
  PauseCircleOutline: 'ant-design:pause-circle-outlined',
  CameraOutline: 'ant-design:camera-outlined',
  EyeOutline: 'carbon:data-view-alt',
  CodeSlashOutline: 'ant-design:code-outlined',
  CopyOutline: 'ant-design:copy-outlined',
  GitNetworkOutline: 'ant-design:deployment-unit-outlined',
  GitCompareOutline: 'ant-design:swap-outlined',
  TrashOutline: 'ant-design:delete-outlined',
  FlashOutline: 'ant-design:thunderbolt-outlined',
  RefreshOutline: 'ion:reload-sharp',
  CloseOutline: 'clarity:close-line',
  CheckmarkOutline: 'ant-design:check-outlined',
  ChevronBackOutline: 'line-md:arrow-close-left',
  ChevronForwardOutline: 'line-md:arrow-close-right',
  ResizeOutline: 'dashicons:align-center',
  LogOutOutline: 'ion:power-outline',
  BookOutline: 'ion:document-text-outline',
  LogoGithub: 'ant-design:github-outlined',
  AddOutline: 'ant-design:plus-outlined',
  SyncOutline: 'ant-design:sync-outlined',
  ArrowBackOutline: 'ant-design:arrow-left-outlined',
  ArrowForwardOutline: 'ant-design:arrow-right-outlined',
  ArrowUpOutline: 'ant-design:arrow-up-outlined',
  CloudOutline: 'ant-design:cloud-outlined',
  ExpandOutline: 'ant-design:fullscreen-outlined',
  ContractOutline: 'ant-design:fullscreen-exit-outlined',
  MenuOutline: 'ant-design:menu-outlined',
  ReorderThreeOutline: 'ant-design:menu-fold-outlined',
}

function lookupIonComponent(name: string): Component | undefined {
  if (ION_ICON_REGISTRY[name])
    return ION_ICON_REGISTRY[name]

  const componentName = name.startsWith('ion:') ? ionIdToComponentName(name) : name
  const dynamic = (IonIcons as Record<string, Component>)[componentName]
  if (dynamic)
    return dynamic

  if (name.startsWith('ion:'))
    return (IonIcons as Record<string, Component>)[ionIdToComponentName(name)]

  return undefined
}

export function resolveShortIcon(icon: string): string {
  if (SHORT_ICON_ALIASES[icon])
    return SHORT_ICON_ALIASES[icon]
  if (LEGACY_ICON_ALIASES[icon])
    return LEGACY_ICON_ALIASES[icon]
  if (VICON_TO_ICONIFY[icon])
    return VICON_TO_ICONIFY[icon]

  if (icon.endsWith('-outlined') || icon.endsWith('-filled') || icon.endsWith('-twotone')) {
    const base = icon.replace(/-(outlined|filled|twotone)$/, '')
    if (SHORT_ICON_ALIASES[base])
      return SHORT_ICON_ALIASES[base]
    return `ant-design:${icon}`
  }

  if (/^[A-Z]/.test(icon) && lookupIonComponent(icon))
    return ''

  if (icon.includes(':'))
    return icon

  return `ant-design:${icon}-outlined`
}

/** Resolve icon ref to @vicons/ionicons5 component when possible. */
export function resolveViconComponent(icon?: string): Component | undefined {
  if (!icon || icon.endsWith('|svg'))
    return undefined

  if (icon.includes(':') && !icon.startsWith('ion:'))
    return undefined

  if (VICON_TO_ICONIFY[icon])
    return undefined

  const direct = lookupIonComponent(icon)
  if (direct)
    return direct

  if (icon.includes(':'))
    return undefined

  const ionRef = resolveShortIcon(icon)
  if (!ionRef)
    return lookupIonComponent(icon)

  return lookupIonComponent(ionRef)
}

/** Iconify ref for Icon / NovaIcon when vicons/svg unavailable. */
export function resolveIconifyRef(icon?: string): string {
  if (!icon || icon.endsWith('|svg'))
    return icon ?? ''

  if (icon.includes(':'))
    return icon

  if (VICON_TO_ICONIFY[icon])
    return VICON_TO_ICONIFY[icon]

  if (lookupIonComponent(icon))
    return ''

  return resolveShortIcon(icon)
}
