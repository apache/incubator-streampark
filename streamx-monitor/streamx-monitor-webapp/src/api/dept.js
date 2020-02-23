import api from './index'
import http from '@/utils/request'

export function list(params) {
  return http.post(api.Dept.LIST, params)
}

export function post(params) {
  return http.post(api.Dept.POST, params)
}

export function remove(params) {
  return http.delete(api.Dept.DELETE, params)
}

export function update(params) {
  return http.put(api.Dept.UPDATE, params)
}

export function $export(queryParam) {
  return http.export(api.Dept.EXPORT, queryParam)
}
