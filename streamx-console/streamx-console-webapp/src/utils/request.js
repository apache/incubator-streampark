import axios from 'axios'
import $qs from 'qs'
import {notification, message, Modal} from '@/adapter'
import {INVALID, TOKEN} from '@/store/mutation-types'
import storage from '@/utils/storage'
import store from '@/store'
import moment from 'moment'

import {baseUrl} from '@/api/baseUrl'

const http = axios.create({
  baseURL: baseUrl(),
  withCredentials: false,
  timeout: 1000 * 10, // 请求超时时间
  responseType: 'json',
  validateStatus(status) {
    // 200 外的状态码都认定为失败
    return status === 200
  }
})

// request interceptor
http.interceptors.request.use(config => {
  const expire = store.getters.expire
  const now = moment().format('YYYY-MM-DD HH:mm:ss')
  // 让token早10秒种过期，提升“请重新登录”弹窗体验
  if (now > expire) {
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
    // 在请求之前对data传参进行格式转换
    if (config.method === 'get' || config.method === 'post') {
      if (data.sortField && data.sortOrder) {
        data.sortOrder = data.sortOrder === 'descend' ? 'desc' : 'asc'
      } else {
        delete data.sortField
        delete data.sortOrder
      }
    }
    if (config.method === 'get') {
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
        //避免在某些页面有密集的ajax请求数据时反复的弹窗
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
    console.log('最终提交参数： ' + JSON.stringify(data))
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
      timeout: blobTimeout // 上传文件超时10分钟
    }).then((resp) => {
      respBlob(resp, filename)
    }).catch((r) => {
      console.error(r)
      message.error('下载失败')
    })
  },
  upload(url, params) {
    return http.post(url, params, {
      headers: {
        'Content-Type': 'multipart/form-data'
      },
      timeout: blobTimeout // 上传文件超时10分钟
    })
  },
  export(url, params = {}, blobCallback, msg) {
    if (blobCallback == null) {
      blobCallback = respBlob
    }
    msg = msg == null ? {} : msg
    message.loading(msg.loading || '导入文件中...')
    return http.post(url, params, {
      responseType: 'blob'
    }).then((resp) => {
      blobCallback(resp)
    }).catch((r) => {
      console.error(r)
      message.error(msg.error || '导出文件失败!')
    })
  },

}
