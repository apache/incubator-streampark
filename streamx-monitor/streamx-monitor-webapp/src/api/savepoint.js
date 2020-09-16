import api from './index'
import http from '@/utils/request'

export function lastest (params) {
  return http.post(api.SavePoint.Lastest, params)
}

export function history (params) {
  return http.post(api.SavePoint.History, params)
}
