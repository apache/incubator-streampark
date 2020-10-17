import api from './index'
import http from '@/utils/request'

export function lastest (params) {
  return http.post(api.SavePoint.LASTEST, params)
}

export function history (params) {
  return http.post(api.SavePoint.HISTORY, params)
}

export function remove (params) {
  return http.post(api.SavePoint.DELETE, params)
}
