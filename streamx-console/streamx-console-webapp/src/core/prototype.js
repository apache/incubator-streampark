import Vue from 'vue'
import storage from '@/utils/storage'

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
  return storage.get('THEME') === 'night' ? 'vs-dark' : 'vs'
}

String.prototype.trim = function () {
  return this.replace(/^[\s\uFEFF\xA0]+|[\s\uFEFF\xA0]+$/g, '')
}