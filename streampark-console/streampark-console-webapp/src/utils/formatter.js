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

/**
 * Format the object according to the format of the js configuration file
 * @param obj formatted object
 * @param dep level, this item does not need to pass a value
 * @returns {string}
 */
function formatConfig(obj, dep) {
  dep = dep || 1
  const LN = '\n', TAB = '  '
  let indent = ''
  for (let i = 0; i < dep; i++) {
    indent += TAB
  }
  let isArray = false, arrayLastIsObj = false
  let str = '', prefix = '{', subfix = '}'
  if (Array.isArray(obj)) {
    isArray = true
    prefix = '['
    subfix = ']'
    str = obj.map((item, index) => {
      let format = ''
      if (typeof item == 'function') {
        //
      } else if (typeof item == 'object') {
        arrayLastIsObj = true
        format = `${LN}${indent}${formatConfig(item,dep + 1)},`
      } else if ((typeof item == 'number' && !isNaN(item)) || typeof item == 'boolean') {
        format = `${item},`
      } else if (typeof item == 'string') {
        format = `'${item}',`
      }
      if (index == obj.length - 1) {
        format = format.substring(0, format.length - 1)
      } else {
        arrayLastIsObj = false
      }
      return format
    }).join('')
  } else if (typeof obj != 'function' && typeof obj == 'object') {
    str = Object.keys(obj).map((key, index, keys) => {
      const val = obj[key]
      let format = ''
      if (typeof val == 'function') {
        //
      } else if (typeof val == 'object') {
        format = `${LN}${indent}${key}: ${formatConfig(val,dep + 1)},`
      } else if ((typeof val == 'number' && !isNaN(val)) || typeof val == 'boolean') {
        format = `${LN}${indent}${key}: ${val},`
      } else if (typeof val == 'string') {
        format = `${LN}${indent}${key}: '${val}',`
      }
      if (index == keys.length - 1) {
        format = format.substring(0, format.length - 1)
      }
      return format
    }).join('')
  }
  const len = TAB.length
  if (indent.length >= len) {
    indent = indent.substring(0, indent.length - len)
  }
  if (!isArray || arrayLastIsObj) {
    subfix = LN + indent +subfix
  }
  return`${prefix}${str}${subfix}`
}

module.exports = {formatConfig}
