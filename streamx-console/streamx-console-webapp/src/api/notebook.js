import api from './index'
import http from '@/utils/request'

export function submit (queryParam) {
  return http.post(api.NoteBook.SUBMIT, queryParam)
}
