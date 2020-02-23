import api from './index'
import http from '@/utils/request'

export function roleMenu(queryParam) {
  return http.post(api.Role.MENU, queryParam)
}

export function list(queryParam) {
  return http.post(api.Role.LIST, queryParam)
}

export function remove(queryParam) {
  return http.delete(api.Role.DELETE, queryParam)
}

export function update(queryParam) {
  return http.put(api.Role.UPDATE, queryParam)
}

export function checkName(queryParam) {
  return http.post(api.Role.CHECK_NAME, queryParam)
}

export function post(queryParam) {
  return http.post(api.Role.POST, queryParam)
}

export function $export(queryParam) {
  return http.export(api.Role.EXPORT, queryParam)
}
