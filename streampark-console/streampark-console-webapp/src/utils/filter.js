import Vue from 'vue'
import moment from 'moment'
import 'moment/locale/zh-cn'
moment.locale('zh-cn')

Vue.filter('NumberFormat', function (value) {
  if (!value) {
    return '0'
  }
  const intPartFormat = value.toString().replace(/(\d)(?=(?:\d{3})+$)/g, '$1,') // 将整数部分逢三一断
  return intPartFormat
})

Vue.filter('dayjs', function (dataStr, pattern = 'YYYY-MM-DD HH:mm:ss') {
  return moment(dataStr).format(pattern)
})

Vue.filter('moment', function (dataStr, pattern = 'YYYY-MM-DD HH:mm:ss') {
  return moment(dataStr).format(pattern)
})

Vue.filter('duration', function duration (ms) {
  if (ms === 0 || ms === undefined || ms === null) {
    return ''
  }
  const ss = 1000
  const mi = ss * 60
  const hh = mi * 60
  const dd = hh * 24

  const day = parseInt(ms / dd)
  const hour = parseInt((ms - day * dd) / hh)
  const minute = parseInt((ms - day * dd - hour * hh) / mi)
  const seconds = parseInt((ms - day * dd - hour * hh - minute * mi) / ss)
  if (day > 0) {
    return day + 'd ' + hour + 'h ' + minute + 'm ' + seconds + 's'
  } else if (hour > 0) {
    return hour + 'h ' + minute + 'm ' + seconds + 's'
  } else if (minute > 0) {
    return minute + 'm ' + seconds + 's'
  } else {
    return 0 + 'm ' + seconds + 's'
  }
})
