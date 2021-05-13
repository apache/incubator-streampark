import Vue from 'vue'
import storage from '@/utils/storage'

import {DEFAULT_THEME} from '@/store/mutation-types'

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

Vue.prototype.ideTheme = function () {
  return storage.get(DEFAULT_THEME) === 'dark' ? 'vs-dark' : 'vs'
}

String.prototype.trim = function () {
  return this.replace(/^[\s\uFEFF\xA0]+|[\s\uFEFF\xA0]+$/g, '')
}
