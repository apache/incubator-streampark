import api from './index'
import http from '@/utils/request'

export function getExecUser() {
  return http.post(api.User.ExecUser)
}

export function list(queryParam) {
  return http.post(api.User.LIST, queryParam)
}

export function update(queryParam) {
  return http.put(api.User.UPDATE, queryParam)
}

export function get(queryParam) {
  return http.get(api.User.GET, queryParam)
}

export function checkUserName(queryParam) {
  return http.post(api.User.CHECK_NAME, queryParam)
}

export function post(queryParam) {
  return http.post(api.User.POST, queryParam)
}

export function getRouter(queryParam) {
  return http.post(api.Menu.ROUTER, queryParam)
}

export function $export(queryParam) {
  return http.export(api.User.EXPORT, queryParam)
}

export function remove(queryParam) {
  return http.delete(api.User.DELETE, queryParam)
}
