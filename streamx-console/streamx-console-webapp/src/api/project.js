import api from './index'
import http from '@/utils/request'

export function create (params) {
  return http.post(api.Project.Create, params)
}

export function list (params) {
  return http.post(api.Project.List, params)
}

export function build (params) {
  return http.post(api.Project.Build, params)
}

export function fileList (params) {
  return http.post(api.Project.FileList, params)
}

export function modules (params) {
  return http.post(api.Project.Modules, params)
}

export function listConf (params) {
  return http.post(api.Project.ListConf, params)
}

export function jars (params) {
  return http.post(api.Project.Jars, params)
}

export function remove (params) {
  return http.post(api.Project.Delete, params)
}

export function select (params) {
  return http.post(api.Project.Select, params)
}
