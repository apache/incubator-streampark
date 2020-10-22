import api from './index'
import http from '@/utils/request'

export function read (queryParam) {
  return http.post(api.MarkDown.READ, queryParam)
}
