import Vue from 'vue'

Vue.prototype.randomNum = function (minNum, maxNum) {
  switch (arguments.length) {
    case 1:
      return parseInt(Math.random() * minNum + 1)
      break
    case 2:
      return parseInt(Math.random() * (maxNum - minNum + 1) + minNum)
      break
    default:
      return 0
      break
  }
}

String.prototype.trim = function () {
  return this.replace(/^[\s\uFEFF\xA0]+|[\s\uFEFF\xA0]+$/g, '')
}