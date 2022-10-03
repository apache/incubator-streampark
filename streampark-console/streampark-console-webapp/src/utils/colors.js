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

const varyColor = require('webpack-theme-color-replacer/client/varyColor')
const {generate} =  require('@ant-design/colors')
const {ADMIN, ANTD} = require('../config/default')
const Config = require('../config')

const themeMode = ADMIN.theme.mode

// Get ant design colorways
function getAntdColors(color, mode) {
  const options = mode && (mode === themeMode.NIGHT) ? { theme: 'dark' } : undefined
  return generate(color, options)
}

// Get functional colors
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

// get menu color
function getMenuColors(color, mode) {
  if (mode === themeMode.NIGHT) {
    return ANTD.primary.night.menuColors
  } else if (color === ANTD.primary.color) {
    return ANTD.primary.dark.menuColors
  } else {
    return [varyColor.darken(color, 0.93), varyColor.darken(color, 0.83), varyColor.darken(color, 0.73)]
  }
}

// Get the theme mode switch color system
function getThemeToggleColors(color, mode) {
  // main color
  const mainColors = getAntdColors(color, mode)
  const primary = mainColors[5]
  // Auxiliary color system, because antd is not currently designed for night mode, so add auxiliary color system to ensure the normal switching of night mode
  const subColors = getAntdColors(primary, themeMode.LIGHT)
  // menu color
  const menuColors = getMenuColors(color, mode)
  // Content color system (including background color, text color, etc.)
  const themeCfg = ANTD.theme[mode]
  let contentColors = Object.keys(themeCfg)
    .map(key => themeCfg[key])
    .map(color => isHex(color) ? color : toNum3(color).join(','))
  // Content color deduplication
  contentColors = [...new Set(contentColors)]
  // Theme color in rgb format
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
