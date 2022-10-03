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

const {cssResolve} = require('../config/replacer')
// Fix css results extracted by webpack-theme-color-replacer plugin
function resolveCss(output, srcArr) {
  const regExps = []
  // Extract all regular configuration in resolve configuration
  Object.keys(cssResolve).forEach(key => {
    let isRegExp = false
    let reg = {}
    try {
      reg = eval(key)
      isRegExp = reg instanceof RegExp
    } catch (e) {
      isRegExp = false
    }
    if (isRegExp) {
      regExps.push([reg, cssResolve[key]])
    }
  })

  // deduplication
  srcArr = dropDuplicate(srcArr)

  // handle css
  const outArr = []
  srcArr.forEach(text => {
    // Convert to css object
    const cssObj = parseCssObj(text)
    // Match the configuration according to the selector, if the match is successful, the css is processed according to the configuration
    if (cssResolve[cssObj.selector] !== undefined) {
      const cfg = cssResolve[cssObj.selector]
      if (cfg) {
        outArr.push(cfg.resolve(text, cssObj))
      }
    } else {
      let cssText = ''
      // If the match is unsuccessful, test whether there is a matching regular configuration, and if there is, it will be processed according to the regular configuration
      for (const regExp of regExps) {
        if (regExp[0].test(cssObj.selector)) {
          const cssCfg = regExp[1]
          cssText = cssCfg ? cssCfg.resolve(text, cssObj) : ''
          break
        }
        // If no regularity is matched, set cssText as the default css (ie not processed)
        cssText = text
      }
      if (cssText !== '') {
        outArr.push(cssText)
      }
    }
  })
  output = outArr.join('\n')
  return output
}

// Array deduplication
function dropDuplicate(arr) {
  const map = {}
  const r = []
  for (const s of arr) {
    if (!map[s]) {
      r.push(s)
      map[s] = 1
    }
  }
  return r
}

/**
 * Parse css object from string
 * @param cssText
 * @returns {{
 *   name: String,
 *   rules: Array[String],
 *   toText: function
 * }}
 */
function parseCssObj(cssText) {
  const css = {}
  const ruleIndex = cssText.indexOf('{')
  css.selector = cssText.substring(0, ruleIndex)
  const ruleBody = cssText.substring(ruleIndex + 1, cssText.length - 1)
  const rules = ruleBody.split(';')
  css.rules = rules
  css.toText = function () {
    let body = ''
    this.rules.forEach(item => {body += item + ';'})
    return `${this.selector}{${body}}`
  }
  return css
}

module.exports = {resolveCss}
