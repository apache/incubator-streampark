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
