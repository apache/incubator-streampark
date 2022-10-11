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

import axios from 'axios'
import $qs from 'qs'
import notification from 'ant-design-vue/es/notification'
import {INVALID, TEAM_ID, TOKEN} from '@/store/mutation-types'
import storage from '@/utils/storage'
import store from '@/store'
import moment from 'moment'
import {message, Modal} from 'ant-design-vue'
import Swal from 'sweetalert2/dist/sweetalert2.js'

import {baseUrl} from '@/api/baseUrl'

const http = axios.create({
  baseURL: baseUrl(),
  withCredentials: false,
  timeout: 1000 * 10, // request timeout
  responseType: 'json',
  validateStatus(status) {
    // Status codes other than 200 are considered failures
    return status === 200
  }
})

// request interceptor
http.interceptors.request.use(config => {
  const expire = store.getters.expire
  const now = moment().format('YYYYMMDDHHmmss')
  // Let the token expire 10 seconds earlier to improve the "Please log in again" pop-up experience
  if (now - expire >= -10) {
    Modal.error({
      title: 'Sign in expired',
      content: 'Sorry, Sign in has expired. Please Sign in again',
      okText: 'Sign in',
      mask: false,
      onOk: () => {
        return new Promise((resolve, reject) => {
          storage.clear()
          location.reload()
        })
      }
    })
  }
  config.headers = {
    'X-Requested-With': 'XMLHttpRequest',
    'Content-Type': config.headers['Content-Type'] || 'application/x-www-form-urlencoded; charset=UTF-8',
    'Access-Control-Allow-Origin': '*'
  }
  const token = storage.get(TOKEN)
  if (token) {
    config.headers['Authorization'] = token
  }
  config.transformRequest = [function (data) {

    // Format the data parameter before the request
    if (config.method === 'get' || config.method === 'post') {
      if (data.sortField && data.sortOrder) {
        data.sortOrder = data.sortOrder === 'descend' ? 'desc' : 'asc'
      } else {
        delete data.sortField
        delete data.sortOrder
      }
    }
    const teamId = sessionStorage.getItem(TEAM_ID)
    if (teamId) {
      data['teamId'] = teamId
    }
    if (config.method === 'get') {
      // filter undefined params
      data = Object.fromEntries(Object.entries(data).filter(([_,value]) => value !== undefined))
      data = {params: data}
    } else if (config.headers['Content-Type'] !== 'multipart/form-data') {
      if (!data.isJsonType){
        data = $qs.stringify(data)
      } else { // Content-Type : json
        data = JSON.stringify(data)
      }
    }
    return data
  }]
  return config
}, error => {
  return Promise.reject(error)
})

// response interceptor
http.interceptors.response.use((response) => {
  return response.data
}, error => {
  if (error.response) {
    if (error.response.data.code == 501) {
      Swal.fire({
        icon: 'error',
        title: 'Oops...',
        text: error.response.data.message,
        footer: '<a href="https://streampark.apache.org/">View the official documentation?</a>'
      })
    } else if (error.response.data.code == 502) {
      let width = document.documentElement.clientWidth || document.body.clientWidth
      if (width > 1200) {
        width = 1080
      }
      width *= 0.96
      Swal.fire({
        icon: 'error',
        title: 'Oops...',
        width: width,
        html: '<pre class="propException">' + error.response.data.message + '</pre>',
        footer: '<a href="https://github.com/apache/incubator-streampark/issues/new/choose">report issue ?</a>',
        focusConfirm: false
      })
    } else {
      const errorMessage = error.response.data === null ? 'System error，Please contact the administrator' : error.response.data.message
      switch (error.response.status) {
        case 404:
          notification.error({
            message: 'Sorry, resource not found',
            duration: 4
          })
          break
        case 403:
        case 401:
          // Avoid repeated pop-ups when some pages have dense ajax request data
          if (!storage.get(INVALID, false)) {
            storage.set(INVALID, true)
            notification.warn({
              message: 'Sorry, you can\'t access. May be because you don\'t have permissions or the Sign In is invalid',
              duration: 4
            })
            store.dispatch('SignOut', {}).then((resp) => {
              storage.clear()
              location.reload()
            })
          }
          break
        default:
          notification.error({
            message: errorMessage,
            duration: 4
          })
          break
      }
    }
  }
  return Promise.reject(error)
})

const respBlob = (content, fileName) => {
  const blob = new Blob([content])
  fileName = fileName || `${new Date().getTime()}_export.xlsx`
  if ('download' in document.createElement('a')) {
    const link = document.createElement('a')
    link.download = fileName
    link.style.display = 'none'
    link.href = URL.createObjectURL(blob)
    document.body.appendChild(link)
    link.click()
    URL.revokeObjectURL(link.href)
    document.body.removeChild(link)
  } else {
    navigator.msSaveBlob(blob, fileName)
  }
}

const blobTimeout = 1000 * 60 * 10
export default {
  postJson(url, data) {
    data.isJsonType = true
    console.log('final submission parameters： ' + JSON.stringify(data))
    return http.post(url, data , {
      headers: {
        'Content-Type': 'application/json; charset=UTF-8'
      }
    })
  },
  get(url, data = {}) {
    return http.get(url, data)
  },
  post(url, data = {}) {
    return http.post(url, data)
  },
  put(url, data = {}) {
    return http.put(url, data)
  },
  delete(url, data = {}) {
    return http.delete(url, { data: data })
  },
  patch(url, data = {}) {
    return http.patch(url, data)
  },
  download(url, params, filename) {
    message.loading('File transfer in progress')
    return http.post(url, params, {
      responseType: 'blob',
      timeout: blobTimeout // Uploading files timed out for 10 minutes
    }).then((resp) => {
      respBlob(resp, filename)
    }).catch((r) => {
      console.error(r)
      message.error('download failed')
    })
  },
  upload(url, params) {
    return http.post(url, params, {
      headers: {
        'Content-Type': 'multipart/form-data'
      },
      timeout: blobTimeout // Uploading files timed out for 10 minutes
    })
  },
  export(url, params = {}, blobCallback, msg) {
    if (blobCallback == null) {
      blobCallback = respBlob
    }
    msg = msg == null ? {} : msg
    message.loading(msg.loading || 'import file...')
    return http.post(url, params, {
      responseType: 'blob'
    }).then((resp) => {
      blobCallback(resp)
    }).catch((r) => {
      console.error(r)
      message.error(msg.error || 'Failed to export file!')
    })
  },

}
