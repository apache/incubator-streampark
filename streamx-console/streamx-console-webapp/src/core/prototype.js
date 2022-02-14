import Vue from 'vue'
import storage from '@/utils/storage'

import {DEFAULT_THEME} from '@/store/mutation-types'
import SockJS from 'sockjs-client'

Vue.prototype.randomNum = function (minNum, maxNum) {
  switch (arguments.length) {
    case 1:
      return parseInt(Math.random() * minNum + 1)
    case 2:
      return parseInt(Math.random() * (maxNum - minNum + 1) + minNum)
    default:
      return 0
  }
}

Vue.prototype.exceptionPropWidth = function () {
  const width = document.documentElement.clientWidth || document.body.clientWidth
  if (width > 1200) {
    return 1080
  }
  return width * 0.96
}

Vue.prototype.getSocket = function (url) {
  window.WebSocket = window.WebSocket || window.MozWebSocket
  if (window.WebSocket) {
    return new WebSocket(url.replace(/^http/,'ws'))
  } else {
    return new SockJS(url)
  }
}


Vue.prototype.clientWidth = function () {
  return document.documentElement.clientWidth || document.body.clientWidth
}

Vue.prototype.uuid = function () {
  const temp_url = URL.createObjectURL(new Blob())
  const uuid = temp_url.toString()
  URL.revokeObjectURL(temp_url)
  return uuid.substr(uuid.lastIndexOf('/') + 1)
}

Vue.prototype.clientHeight = function () {
  return document.documentElement.clientHeight || document.body.clientHeight
}

Vue.prototype.ideTheme = function () {
  return storage.get(DEFAULT_THEME) === 'dark' ? 'vs-dark' : 'vs'
}

String.prototype.trim = function () {
  return this.replace(/^[\s\uFEFF\xA0]+|[\s\uFEFF\xA0]+$/g, '')
}
