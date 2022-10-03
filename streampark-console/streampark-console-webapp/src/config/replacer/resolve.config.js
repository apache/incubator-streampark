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
 * resolve config for webpack-theme-color-replacer plugin<br>
 * Configure resolve rules for specific css selectors. <br>
 *
 * key is a css selector value or a valid regular expression string<br>
 * When the key sets the css selector value, it will match the corresponding css<br>
 * When key is set to a regular expression, it will match all css<br> that satisfy this regular expression
 *
 * value can be set to boolean value false or an object<br>
 * When the value is false, this css will be ignored, that is, this css will not be included in the webpack-theme-color-replacer management<br>
 * When value is an object, the resolve function of the object is called, and the cssText (original css text) and cssObj (css object) parameters are passed in; the resolve function should return <br>
 * Return a processed, valid css string (including selector)<br>
 * Note: value cannot be set to true<br>
 */
const cssResolve = {
  '.ant-checkbox-checked .ant-checkbox-inner::after': {
    resolve(cssText, cssObj) {
      cssObj.rules.push('border-top:0', 'border-left:0')
      return cssObj.toText()
    }
  },
  '.ant-tree-checkbox-checked .ant-tree-checkbox-inner::after': {
    resolve(cssText, cssObj) {
      cssObj.rules.push('border-top:0', 'border-left:0')
      return cssObj.toText()
    }
  },
  '.ant-checkbox-checked .ant-checkbox-inner:after': {
    resolve(cssText, cssObj) {
      cssObj.rules.push('border-top:0', 'border-left:0')
      return cssObj.toText()
    }
  },
  '.ant-tree-checkbox-checked .ant-tree-checkbox-inner:after': {
    resolve(cssText, cssObj) {
      cssObj.rules.push('border-top:0', 'border-left:0')
      return cssObj.toText()
    }
  },
  '.ant-menu-dark .ant-menu-inline.ant-menu-sub': {
    resolve(cssText, cssObj) {
      cssObj.rules = cssObj
        .rules
        .filter(rule => rule.indexOf('box-shadow') === -1)
      return cssObj.toText()
    }
  },
  '.ant-menu-horizontal>.ant-menu-item:hover,.ant-menu-horizontal>.ant-menu-submenu:hover,.ant-menu-horizontal>.ant-menu-item-active,.ant-menu-horizontal>.ant-menu-submenu-active,.ant-menu-horizontal>.ant-menu-item-open,.ant-menu-horizontal>.ant-menu-submenu-open,.ant-menu-horizontal>.ant-menu-item-selected,.ant-menu-horizontal>.ant-menu-submenu-selected': {
    resolve(cssText, cssObj) {
      cssObj.selector = cssObj.selector.replace(/.ant-menu-horizontal/g, '.ant-menu-horizontal:not(.ant-menu-dark)')
      return cssObj.toText()
    }
  },
  '.ant-menu-horizontal>.ant-menu-item-active,.ant-menu-horizontal>.ant-menu-item-open,.ant-menu-horizontal>.ant-menu-item-selected,.ant-menu-horizontal>.ant-menu-item:hover,.ant-menu-horizontal>.ant-menu-submenu-active,.ant-menu-horizontal>.ant-menu-submenu-open,.ant-menu-horizontal>.ant-menu-submenu-selected,.ant-menu-horizontal>.ant-menu-submenu:hover': {
    resolve(cssText, cssObj) {
      cssObj.selector = cssObj.selector.replace(/.ant-menu-horizontal/g, '.ant-menu-horizontal:not(.ant-menu-dark)')
      return cssObj.toText()
    }
  },
  '.ant-layout-sider': {
    resolve(cssText, cssObj) {
      cssObj.selector = '.ant-layout-sider-dark'
      return cssObj.toText()
    }
  },
  '/keyframes/': false
}

module.exports = cssResolve
