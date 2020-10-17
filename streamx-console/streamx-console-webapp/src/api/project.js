import api from './index'
import http from '@/utils/request'

export function create (params) {
  return http.post(api.Project.CREATE, params)
}

export function list (params) {
  return http.post(api.Project.LIST, params)
}

export function build (params) {
  return http.post(api.Project.BUILD, params)
}

export function fileList (params) {
  return http.post(api.Project.FILELIST, params)
}

export function modules (params) {
  return http.post(api.Project.MODULES, params)
}

export function listConf (params) {
  return http.post(api.Project.LISTCONF, params)
}

export function jars (params) {
  return http.post(api.Project.JARS, params)
}

export function remove (params) {
  return http.post(api.Project.DELETE, params)
}

export function select (params) {
  return http.post(api.Project.SELECT, params)
}
