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

Vue.filter('duration',function duration(ms) {
  if(ms == 0 || ms == undefined || ms == null) {
    return ""
  }
  let ss = 1000
  let mi = ss * 60
  let hh = mi * 60
  let dd = hh * 24

  let day = parseInt(ms / dd)
  let hour = parseInt((ms - day * dd) / hh)
  let minute = parseInt((ms - day * dd - hour * hh)/ mi)
  let seconds = parseInt((ms - day * dd - hour * hh - minute * mi) / ss)
  if (day > 0) {
    return day + "D " + hour + "h " + minute + "m " + seconds + "s"
  } else if (hour > 0) {
    return hour + "h " + minute + "m " + seconds + "s"
  } else if (minute > 0) {
    return minute + "m " + seconds + "s"
  } else {
    return 0 + "m " + seconds + "s"
  }
})