import api from './index'
import http from '@/utils/request'

export function get (queryParam) {
  return http.post(api.Tutorial.GET, queryParam)
}
