import type { NDateLocale, NLocale } from 'naive-ui'
import type { Composer } from 'vue-i18n'
import { dateEnUS, dateZhCN, enUS, zhCN } from 'naive-ui'
import { i18n } from '@/locales/setupI18n'
import type { AppLocale } from '@/utils/locale'
import { normalizeAppLocale } from '@/utils/locale'

function composer(): Composer | undefined {
    return i18n?.global as Composer | undefined
}

export function setLocale(locale: AppLocale | string) {
    const normalized = normalizeAppLocale(locale)
    const c = composer()
    if (!c) return normalized
    c.locale.value = normalized
    return normalized
}

/** Lazy wrapper — i18n is created asynchronously in AppMain bootstrap. */
export function $t(key: string, ...args: unknown[]) {
    const c = composer()
    if (!c) return key
    return c.t(key, ...(args as [string]))
}

/** Backend menu/route titles are i18n keys like `flink.application`. */
const ROUTE_TITLE_KEYS: Record<string, string> = {
    'app add': 'menu.page.appAdd',
    'app detail': 'menu.page.appDetail',
    'app edit flink': 'menu.page.appEditFlink',
    'app edit streampark': 'menu.page.appEditStreamPark',
    'cluster add': 'menu.page.clusterAdd',
    'cluster edit': 'menu.page.clusterEdit',
    'project add': 'menu.page.projectAdd',
    'project edit': 'menu.page.projectEdit',
    'variable depend apps': 'menu.page.variableDependApps',
}

export function translateMenuTitle(text?: string | null) {
    if (!text) return ''
    const value = String(text)
    const routeKey = ROUTE_TITLE_KEYS[value]
    if (routeKey) {
        const translated = $t(routeKey)
        if (translated !== routeKey) return translated
    }
    if (/^\w+\.\w+/.test(value)) {
        const key = value.startsWith('menu.') ? value : `menu.${value}`
        const translated = $t(key)
        return translated === key ? value : translated
    }
    return value
}

export const naiveI18nOptions: Record<
    AppLocale,
    { locale: NLocale | null; dateLocale: NDateLocale | null }
> = {
    zh_CN: {
        locale: zhCN,
        dateLocale: dateZhCN,
    },
    en: {
        locale: enUS,
        dateLocale: dateEnUS,
    },
}
