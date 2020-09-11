import api from './index'
import http from '@/utils/request'

export function get(params) {
  return http.post(api.Config.Get, params)
}

export function list(queryParam) {
  return http.post(api.Config.List, queryParam)
}

