import api from './index'
import http from '@/utils/request'

export function flamegraph(queryParam, callback, message) {
  return http.export(api.Metrics.FLAMEGRAPH, queryParam, callback, message)
}