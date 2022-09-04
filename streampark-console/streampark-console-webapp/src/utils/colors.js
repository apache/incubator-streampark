const varyColor = require('webpack-theme-color-replacer/client/varyColor')
const {generate} =  require('@ant-design/colors')
const {ADMIN, ANTD} = require('../config/default')
const Config = require('../config')

const themeMode = ADMIN.theme.mode

// 获取 ant design 色系
function getAntdColors(color, mode) {
  const options = mode && (mode === themeMode.NIGHT) ? { theme: 'dark' } : undefined
  return generate(color, options)
}

// 获取功能性颜色
function getFunctionalColors(mode) {
  const options = mode && (mode === themeMode.NIGHT) ? {theme: 'dark'} : undefined
  let {success, warning, error} = ANTD.primary
  const  {success: s1, warning: w1, error: e1} = Config.theme
  success = success && s1
  warning = success && w1
  error = success && e1
  const successColors = generate(success, options)
  const warningColors = generate(warning, options)
  const errorColors = generate(error, options)
  return {
    success: successColors,
    warning: warningColors,
    error: errorColors
  }
}

// 获取菜单色系
function getMenuColors(color, mode) {
  if (mode === themeMode.NIGHT) {
    return ANTD.primary.night.menuColors
  } else if (color === ANTD.primary.color) {
    return ANTD.primary.dark.menuColors
  } else {
    return [varyColor.darken(color, 0.93), varyColor.darken(color, 0.83), varyColor.darken(color, 0.73)]
  }
}

// 获取主题模式切换色系
function getThemeToggleColors(color, mode) {
  //主色系
  const mainColors = getAntdColors(color, mode)
  const primary = mainColors[5]
  //辅助色系，因为 antd 目前没针对夜间模式设计，所以增加辅助色系以保证夜间模式的正常切换
  const subColors = getAntdColors(primary, themeMode.LIGHT)
  //菜单色系
  const menuColors = getMenuColors(color, mode)
  //内容色系（包含背景色、文字颜色等）
  const themeCfg = ANTD.theme[mode]
  let contentColors = Object.keys(themeCfg)
    .map(key => themeCfg[key])
    .map(color => isHex(color) ? color : toNum3(color).join(','))
  // 内容色去重
  contentColors = [...new Set(contentColors)]
  // rgb 格式的主题色
  const rgbColors = [toNum3(primary).join(',')]
  const functionalColors = getFunctionalColors(mode)
  return {primary, mainColors, subColors, menuColors, contentColors, rgbColors, functionalColors}
}

function toNum3(color) {
  if (isHex(color)) {
    return varyColor.toNum3(color)
  }
  let colorStr = ''
  if (isRgb(color)) {
    colorStr = color.slice(5, color.length)
  } else if (isRgba(color)) {
    colorStr = color.slice(6, color.lastIndexOf(','))
  }
  const rgb = colorStr.split(',')
  const r = parseInt(rgb[0])
  const g = parseInt(rgb[1])
  const b = parseInt(rgb[2])
  return [r, g, b]
}

function isHex(color) {
  return color.length >= 4 && color[0] === '#'
}

function isRgb(color) {
  return color.length >= 10 && color.slice(0, 3) === 'rgb'
}

function isRgba(color) {
  return color.length >= 13 && color.slice(0, 4) === 'rgba'
}

module.exports = {
  isHex,
  isRgb,
  toNum3,
  getAntdColors,
  getMenuColors,
  getThemeToggleColors,
  getFunctionalColors
}
