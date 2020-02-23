import api from './index'
import http from '@/utils/request'


export function list(queryParam) {
  return http.post(api.Log.LIST, queryParam)
}

export function remove(queryParam) {
  return http.delete(api.Log.DELETE, queryParam)
}

export function $export(queryParam) {
  return http.export(api.Log.EXPORT, queryParam)
}