import { message } from 'ant-design-vue/es'

let lessNodesAppended

const updateTheme = primaryColor => {
  if (!primaryColor) {
    return
  }
  const hideMessage = message.loading('加载主题...', 0)
  function buildIt () {
    if (!window.less) {
      return
    }
    setTimeout(() => {
      window.less
        .modifyVars({
          '@primary-color': primaryColor
        })
        .then(() => {
          hideMessage()
        })
        .catch((e) => {
          console.log(e)
          message.error('Failed to update theme')
          hideMessage()
        })
    }, 200)
  }
  if (!lessNodesAppended) {
    // insert less.js and color.less
    const lessStyleNode = document.createElement('link')
    const lessConfigNode = document.createElement('script')
    const lessScriptNode = document.createElement('script')
    lessStyleNode.setAttribute('rel', 'stylesheet/less')
    lessStyleNode.setAttribute('href', '/static/less/Color.less')
    lessConfigNode.innerHTML = `
      window.less = {
        async: true,
        env: 'production',
        javascriptEnabled: true
      }
    `
    lessScriptNode.src = 'https://cdn.bootcss.com/less.js/3.9.0/less.min.js'
    lessScriptNode.async = true
    lessScriptNode.onload = () => {
      buildIt()
      lessScriptNode.onload = null
    }
    document.body.appendChild(lessStyleNode)
    document.body.appendChild(lessConfigNode)
    document.body.appendChild(lessScriptNode)
    lessNodesAppended = true
  } else {
    buildIt()
  }
}
export { updateTheme }
