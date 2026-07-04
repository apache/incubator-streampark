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

import type { LocaleType } from '/#/config'

/** StreamPark vue-i18n locale ids (match lang/zh_CN.ts and lang/en.ts). */
export type AppLocale = LocaleType

const LOCALE_ALIASES: Record<string, AppLocale> = {
  zhCN: 'zh_CN',
  zh_CN: 'zh_CN',
  enUS: 'en',
  en: 'en',
}

export function normalizeAppLocale(value?: string | null): AppLocale {
  if (value && LOCALE_ALIASES[value])
    return LOCALE_ALIASES[value]
  const envDefault = import.meta.env.VITE_DEFAULT_LANG
  if (envDefault && LOCALE_ALIASES[envDefault])
    return LOCALE_ALIASES[envDefault]
  return 'zh_CN'
}

export const APP_LOCALE_OPTIONS: Array<{ label: string, value: AppLocale }> = [
  { label: '中文', value: 'zh_CN' },
  { label: 'English', value: 'en' },
]
