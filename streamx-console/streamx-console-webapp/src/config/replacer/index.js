/**
 * webpack-theme-color-replacer 配置
 * webpack-theme-color-replacer 是一个高效的主题色替换插件，可以实现系统运行时动态切换主题功能。
 * 但有些情景下，我们需要为 webpack-theme-color-replacer 配置一些规则，以达到我们的个性化需求的目的
 *
 * @cssResolve: css处理规则，在 webpack-theme-color-replacer 提取 需要替换主题色的 css 后，应用此规则。一般在
 *              webpack-theme-color-replacer 默认规则无法达到我们的要求时使用。
 */
const cssResolve = require('./resolve.config')
module.exports = {cssResolve}
