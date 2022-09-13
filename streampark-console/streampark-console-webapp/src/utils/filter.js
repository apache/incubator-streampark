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
  if (ms === '0' || ms === undefined || ms === null) {
    return ''
  }
  const ss = 1000
  const mi = ss * 60
  const hh = mi * 60
  const dd = hh * 24

  const day = Math.floor(ms / dd)
  const hour = Math.floor((ms - day * dd) / hh)
  const minute = Math.floor((ms - day * dd - hour * hh) / mi)
  const seconds = Math.floor((ms - day * dd - hour * hh - minute * mi) / ss)
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
