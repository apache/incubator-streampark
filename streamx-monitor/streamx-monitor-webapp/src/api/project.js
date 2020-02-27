import api from './index'
import http from '@/utils/request'

export function upload (params) {
  return http.upload(api.Project.Upload, params)
}

export function list (params) {
  return http.post(api.Project.List, params)
}

export function fileList (params) {
  return http.post(api.Project.FileList, params)
}

export function create (params) {
  return http.post(api.Project.Create, params)
}

export function remove (params) {
  return http.post(api.Project.Delete, params)
}

export function select (params) {
  return http.post(api.Project.Select, params)
}
