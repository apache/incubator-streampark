import type { App } from 'vue'
import type { I18nOptions } from 'vue-i18n'
import type { LocaleSetting } from '/#/config'

import { createI18n } from 'vue-i18n'
import { setHtmlPageLang, setLoadLocalePool } from './helper'
import { localeSetting } from '@/settings/localeSetting'
import { LOCALE_KEY } from '@/enums/cacheEnum'
import { createLocalStorage } from '@/utils/cache'
import { normalizeAppLocale } from '@/utils/locale'

const { fallback, availableLocales } = localeSetting
const ls = createLocalStorage()

export let i18n: ReturnType<typeof createI18n>

function resolveLocale() {
    const stored = (ls.get(LOCALE_KEY) || localeSetting) as LocaleSetting
    if (stored?.locale) return normalizeAppLocale(stored.locale)
    return normalizeAppLocale(import.meta.env.VITE_DEFAULT_LANG)
}

async function createI18nOptions(): Promise<I18nOptions> {
    const locale = resolveLocale()
    const defaultLocal = await import(`./lang/${locale}.ts`)
    const message = defaultLocal.default?.message ?? {}

    setHtmlPageLang(locale)
    setLoadLocalePool((loadLocalePool) => {
        loadLocalePool.push(locale)
    })

    return {
        legacy: false,
        locale,
        fallbackLocale: fallback,
        messages: {
            [locale]: message,
        },
        availableLocales: availableLocales,
        sync: true, //If you don’t want to inherit locale from global scope, you need to set sync of i18n component option to false.
        silentTranslationWarn: true, // true - warning off
        missingWarn: false,
        silentFallbackWarn: true,
    }
}

// setup i18n instance with glob
export async function setupI18n(app: App) {
    const options = await createI18nOptions()
    i18n = createI18n(options) as ReturnType<typeof createI18n>
    app.use(i18n)

    const { useLocaleStoreWithOut } = await import('@/store/modules/locale')
    const localeStore = useLocaleStoreWithOut()
    localeStore.setLocaleInfo({ locale: options.locale as LocaleSetting['locale'] })

    const { useAppStore } = await import('@/store')
    useAppStore().lang = options.locale as LocaleSetting['locale']
}
