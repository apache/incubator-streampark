const client = require('webpack-theme-color-replacer/client')
const { theme } = require('../config')
const { getMenuColors, getAntdColors, getThemeToggleColors, getFunctionalColors } = require('../utils/colors')
const { ANTD } = require('../config/default')

function getThemeColors(color, $theme) {
  const _color = color || theme.color
  const mode = $theme || theme.mode
  const replaceColors = getThemeToggleColors(_color, mode)
  return [
    ...replaceColors.mainColors,
    ...replaceColors.subColors,
    ...replaceColors.menuColors,
    ...replaceColors.contentColors,
    ...replaceColors.rgbColors,
    ...replaceColors.functionalColors.success,
    ...replaceColors.functionalColors.warning,
    ...replaceColors.functionalColors.error
  ]
}

function changeThemeColor(newColor, $theme) {
  const theme = $theme === 'dark' ? 'night': 'dark'
  return client.changer.changeColor({ newColors: getThemeColors(newColor, theme) })
}

function modifyVars(color) {
  const _color = color || theme.color
  const palettes = getAntdColors(_color, theme.mode)
  const menuColors = getMenuColors(_color, theme.mode)
  const { success, warning, error } = getFunctionalColors(theme.mode)
  const primary = palettes[5]
  return {
    'primary-color': primary,
    'primary-1': palettes[0],
    'primary-2': palettes[1],
    'primary-3': palettes[2],
    'primary-4': palettes[3],
    'primary-5': palettes[4],
    'primary-6': palettes[5],
    'primary-7': palettes[6],
    'primary-8': palettes[7],
    'primary-9': palettes[8],
    'primary-10': palettes[9],
    'info-color': primary,
    'success-color': success[5],
    'warning-color': warning[5],
    'error-color': error[5],
    'alert-info-bg-color': palettes[0],
    'alert-info-border-color': palettes[2],
    'alert-success-bg-color': success[0],
    'alert-success-border-color': success[2],
    'alert-warning-bg-color': warning[0],
    'alert-warning-border-color': warning[2],
    'alert-error-bg-color': error[0],
    'alert-error-border-color': error[2],
    'processing-color': primary,
    'menu-dark-submenu-bg': menuColors[0],
    'layout-header-background': menuColors[1],
    'layout-trigger-background': menuColors[2],
    'btn-danger-bg': error[4],
    'btn-danger-border': error[4],
    ...ANTD.theme[theme.mode]
  }
}

module.exports = {
  getThemeColors,
  changeThemeColor,
  modifyVars
}
