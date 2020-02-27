import axios from 'axios'
import $qs from 'qs'
import notification from 'ant-design-vue/es/notification'
import { TOKEN } from '@/store/mutation-types'
import storage from '@/utils/storage'
import store from '@/store'
import moment from 'moment'
import { message, Modal } from 'ant-design-vue'

import baseUrl from '@/api/baseUrl'

const http = axios.create({
  baseURL: baseUrl,
  withCredentials: false,
  timeout: 5000, // 请求超时时间
  responseType: 'json',
  validateStatus (status) {
    // 200 外的状态码都认定为失败
    return status === 200
  }
})

// request interceptor
http.interceptors.request.use(config => {
  const expire = store.getters.expire
  const now = moment().format('YYYYMMDDHHmmss')
  // 让token早10秒种过期，提升“请重新登录”弹窗体验
  if (now - expire >= -10) {
    Modal.error({
      title: '登录已过期',
      content: '很抱歉，登录已过期，请重新登录',
      okText: '重新登录',
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
  return config
}, error => {
  return Promise.reject(error)
})

// response interceptor
http.interceptors.response.use((response) => {
  return response.data
}, error => {
  if (error.response) {
    const errorMessage = error.response.data === null ? '系统内部异常，请联系网站管理员' : error.response.data.message
    switch (error.response.status) {
      case 404:
        notification.error({
          message: '系统提示',
          description: '很抱歉，资源未找到',
          duration: 4
        })
        break
      case 403:
      case 401:
        notification.warn({
          message: '系统提示',
          description: '很抱歉，您无法访问该资源，可能是因为没有相应权限或者登录已失效',
          duration: 4
        })
        store.dispatch('Logout', {}).then((resp) => {
          storage.clear()
          location.reload()
        })
        break
      default:
        notification.error({
          message: '系统提示',
          description: errorMessage,
          duration: 4
        })
        break
    }
  }
  return Promise.reject(error)
})

const respBlob = (content, fileName) => {
  const blob = new Blob([content])
  fileName = fileName || `${new Date().getTime()}_导出结果.xlsx`
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

export default {
  get (url, data = {}, headers = null) {
    if (headers) {
    }
    return http.get(url, { params: data })
  },
  post (url, data = {}, headers = null) {
    return http.post(url, $qs.stringify(data))
  },
  put (url, data = {}, headers = null) {
    return http.put(url, $qs.stringify(data))
  },
  delete (url, params = {}, headers = null) {
    return http.delete(url, { data: $qs.stringify(params) })
  },
  patch (url, data = {}, headers = null) {
    return http.patch(url, $qs.stringify(data))
  },
  export (url, params = {}) {
    message.loading('导出数据中')
    return http.post(url, params, {
      transformRequest: [(params) => {
        return $qs.stringify(params)
      }],
      responseType: 'blob'
    }).then((resp) => {
      respBlob(resp)
    }).catch((r) => {
      console.error(r)
      message.error('导出失败')
    })
  },
  download (url, params, filename) {
    message.loading('文件传输中')
    return http.post(url, params, {
      transformRequest: [(params) => {
        return $qs.stringify(params)
      }],
      responseType: 'blob',
      timeout: 1000 * 60 * 10 // 下载文件超时10分钟
    }).then((resp) => {
      respBlob(resp, filename)
    }).catch((r) => {
      console.error(r)
      message.error('下载失败')
    })
  },
  upload (url, params) {
    return http.post(url, params, {
      headers: {
        'Content-Type': 'multipart/form-data'
      },
      timeout: 1000 * 60 * 10 // 上传文件超时10分钟
    })
  }

}
