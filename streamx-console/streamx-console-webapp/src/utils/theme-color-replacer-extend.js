const {cssResolve} = require('../config/replacer')
// 修正 webpack-theme-color-replacer 插件提取的 css 结果
function resolveCss(output, srcArr) {
  const regExps = []
  // 提取 resolve 配置中所有的正则配置
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

  // 去重
  srcArr = dropDuplicate(srcArr)

  // 处理 css
  const outArr = []
  srcArr.forEach(text => {
    // 转换为 css 对象
    const cssObj = parseCssObj(text)
    // 根据selector匹配配置，匹配成功，则按配置处理 css
    if (cssResolve[cssObj.selector] !== undefined) {
      const cfg = cssResolve[cssObj.selector]
      if (cfg) {
        outArr.push(cfg.resolve(text, cssObj))
      }
    } else {
      let cssText = ''
      // 匹配不成功，则测试是否有匹配的正则配置，有则按正则对应的配置处理
      for (const regExp of regExps) {
        if (regExp[0].test(cssObj.selector)) {
          const cssCfg = regExp[1]
          cssText = cssCfg ? cssCfg.resolve(text, cssObj) : ''
          break
        }
        // 未匹配到正则，则设置 cssText 为默认的 css（即不处理）
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

// 数组去重
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
 * 从字符串解析 css 对象
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
