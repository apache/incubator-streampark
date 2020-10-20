import api from './index'
import http from '@/utils/request'

export function get (params) {
  return http.post(api.Config.GET, params)
}

export function list (params) {
  return http.post(api.Config.LIST, params)
}

export function remove (params) {
  return http.post(api.Config.DELETE, params)
}
