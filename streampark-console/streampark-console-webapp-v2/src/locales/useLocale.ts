import type { LocaleType } from '/#/config'

import { i18n } from './setupI18n'
import { useLocaleStoreWithOut } from '@/store/modules/locale'
import { unref, computed } from 'vue'
import { loadLocalePool, setHtmlPageLang } from './helper'
import { normalizeAppLocale } from '@/utils/locale'

interface LangModule {
    message: Recordable
    dateLocale: Recordable
    dateLocaleName: string
}

function setI18nLanguage(locale: LocaleType) {
    const localeStore = useLocaleStoreWithOut()

    if (i18n.mode === 'legacy') {
        i18n.global.locale = locale
    } else {
        ;(i18n.global.locale as any).value = locale
    }
    localeStore.setLocaleInfo({ locale })
    setHtmlPageLang(locale)
}

export async function changeLocale(locale: LocaleType | string) {
    const normalized = normalizeAppLocale(locale)
    const globalI18n = i18n.global
    const currentLocale = unref(globalI18n.locale)
    if (currentLocale === normalized) {
        return normalized
    }

    if (loadLocalePool.includes(normalized)) {
        setI18nLanguage(normalized)
        return normalized
    }
    const langModule = ((await import(`./lang/${normalized}.ts`)) as any).default as LangModule
    if (!langModule) return normalized

    const { message } = langModule

    globalI18n.setLocaleMessage(normalized, message)
    loadLocalePool.push(normalized)

    setI18nLanguage(normalized)
    return normalized
}

export function useLocale() {
    const localeStore = useLocaleStoreWithOut()
    const getLocale = computed(() => localeStore.getLocale)
    const getShowLocalePicker = computed(() => localeStore.getShowPicker)

    return {
        getLocale,
        getShowLocalePicker,
        changeLocale,
    }
}
