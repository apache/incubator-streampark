import api from './index'
import http from '@/utils/request'


export function list(queryParam) {
  return http.post(api.Menu.LIST,queryParam)
}

export function post(queryParam) {
  return http.post(api.Menu.POST,queryParam)
}

export function remove(queryParam) {
  return http.delete(api.Menu.DELETE,queryParam)
}

export function update(queryParam) {
  return http.put(api.Menu.UPDATE,queryParam)
}

export function getRouter(queryParam) {
  return http.post(api.Menu.ROUTER,queryParam)
}

export function $export(queryParam) {
  return http.export(api.Menu.EXPORT, queryParam)
}
