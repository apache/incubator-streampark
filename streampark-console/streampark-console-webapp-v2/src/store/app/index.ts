import type { GlobalThemeOverrides } from 'naive-ui'
import { local, setLocale } from '@/utils'
import { normalizeAppLocale } from '@/utils/locale'
import type { AppLocale } from '@/utils/locale'
import { colord } from 'colord'
import { set } from 'radash'
import themeConfig from './theme.json'
import type { LayoutMode } from '@/layouts/types'

export type TransitionAnimation = '' | 'fade-slide' | 'fade-bottom' | 'fade-scale' | 'zoom-fade' | 'zoom-out'

const { VITE_DEFAULT_LANG, VITE_COPYRIGHT_INFO } = import.meta.env

const docEle = ref(document.documentElement)

const { isFullscreen, toggle } = useFullscreen(docEle)

const { system, store } = useColorMode({
  emitAuto: true,
})

const isMobile = useMediaQuery('(max-width: 700px)')

export const useAppStore = defineStore('app-store', {
  state: () => {
    return {
      footerText: VITE_COPYRIGHT_INFO,
      lang: normalizeAppLocale(VITE_DEFAULT_LANG) as AppLocale,
      theme: themeConfig as GlobalThemeOverrides,
      primaryColor: themeConfig.common.primaryColor,
      collapsed: false,
      grayMode: false,
      colorWeak: false,
      loadFlag: true,
      showLogo: true,
      showTabs: true,
      showFooter: true,
      showProgress: true,
      showBreadcrumb: true,
      showBreadcrumbIcon: true,
      showWatermark: false,
      showSetting: false,
      transitionAnimation: '' as TransitionAnimation,
      layoutMode: 'vertical' as LayoutMode,
    }
  },
  getters: {
    storeColorMode() {
      return store.value
    },
    colorMode() {
      return store.value === 'auto' ? system.value : store.value
    },
    fullScreen() {
      return isFullscreen.value
    },
    isMobile() {
      return isMobile.value
    },
  },
  actions: {
    // 重置所有设置
    resetAlltheme() {
      this.theme = themeConfig
      this.primaryColor = '#18a058'
      this.collapsed = false
      this.grayMode = false
      this.colorWeak = false
      this.loadFlag = true
      this.showLogo = true
      this.showTabs = true
      this.showFooter = true
      this.showBreadcrumb = true
      this.showBreadcrumbIcon = true
      this.showWatermark = false
      this.transitionAnimation = ''
      this.layoutMode = 'vertical'

      // 重置所有配色
      this.setPrimaryColor(this.primaryColor)
    },
    async setAppLang(lang: AppLocale | string) {
      const normalized = normalizeAppLocale(lang)
      const { changeLocale } = await import('@/locales/useLocale')
      await changeLocale(normalized)
      setLocale(normalized)
      local.set('lang', normalized)
      this.lang = normalized
    },
    /* 设置主题色 */
    setPrimaryColor(color: string) {
      const brightenColor = colord(color).lighten(0.05).toHex()
      const darkenColor = colord(color).darken(0.05).toHex()
      set(this.theme, 'common.primaryColor', color)
      set(this.theme, 'common.primaryColorHover', brightenColor)
      set(this.theme, 'common.primaryColorPressed', darkenColor)
      set(this.theme, 'common.primaryColorSuppl', brightenColor)
    },
    setColorMode(mode: 'light' | 'dark' | 'auto') {
      store.value = mode
    },
    /* 切换侧边栏收缩 */
    toggleCollapse() {
      this.collapsed = !this.collapsed
    },
    /* 切换全屏 */
    toggleFullScreen() {
      toggle()
    },
    /**
     * @description: 页面内容重载
     * @param {number} delay - 延迟毫秒数
     * @return {*}
     */
    async reloadPage(delay = 0) {
      this.loadFlag = false
      await nextTick()
      if (delay > 0) {
        await new Promise(resolve => setTimeout(resolve, delay))
      }
      this.loadFlag = true
    },
    /* 切换色弱模式 */
    toggleColorWeak() {
      docEle.value.classList.toggle('color-weak')
      this.colorWeak = docEle.value.classList.contains('color-weak')
    },
    /* 切换灰色模式 */
    toggleGrayMode() {
      docEle.value.classList.toggle('gray-mode')
      this.grayMode = docEle.value.classList.contains('gray-mode')
    },
  },
  persist: {
    storage: localStorage,
  },
})
