import storage from '@/utils/storage'
import store from '@/store/'
import {
  DEFAULT_COLOR,
  DEFAULT_COLOR_WEAK,
  DEFAULT_CONTENT_WIDTH_TYPE,
  DEFAULT_FIXED_HEADER,
  DEFAULT_FIXED_HEADER_HIDDEN,
  DEFAULT_FIXED_SIDEMENU,
  DEFAULT_LAYOUT_MODE,
  DEFAULT_MULTI_TAB,
  DEFAULT_THEME,
  SIDEBAR_TYPE,
  TOKEN
} from '@/store/mutation-types'
import config from '@/config/defaultSettings'

export default function Initializer () {
  store.commit('SET_SIDEBAR_TYPE', storage.get(SIDEBAR_TYPE, true))
  store.commit('TOGGLE_THEME', storage.get(DEFAULT_THEME, config.theme))
  store.commit('TOGGLE_LAYOUT_MODE', storage.get(DEFAULT_LAYOUT_MODE, config.layout))
  store.commit('TOGGLE_FIXED_HEADER', storage.get(DEFAULT_FIXED_HEADER, config.fixedHeader))
  store.commit('TOGGLE_FIXED_SIDERBAR', storage.get(DEFAULT_FIXED_SIDEMENU, config.fixSiderbar))
  store.commit('TOGGLE_CONTENT_WIDTH', storage.get(DEFAULT_CONTENT_WIDTH_TYPE, config.contentWidth))
  store.commit('TOGGLE_FIXED_HEADER_HIDDEN', storage.get(DEFAULT_FIXED_HEADER_HIDDEN, config.autoHideHeader))
  store.commit('TOGGLE_WEAK', storage.get(DEFAULT_COLOR_WEAK, config.colorWeak))
  store.commit('TOGGLE_COLOR', storage.get(DEFAULT_COLOR, config.primaryColor))
  store.commit('TOGGLE_MULTI_TAB', storage.get(DEFAULT_MULTI_TAB, config.multiTab))
  store.commit('SET_TOKEN', storage.get(TOKEN))
  // last step
}
