import api from './index'
import http from '@/utils/request'

export function add (params) {
  return http.post(api.Application.ADD, params)
}

export function readConf (params) {
  return http.post(api.Application.READCONF, params)
}

export function get (params) {
  return http.post(api.Application.GET, params)
}

export function main (params) {
  return http.post(api.Application.MAIN, params)
}

export function update (params) {
  return http.post(api.Application.UPDATE, params)
}

export function deploy (params) {
  return http.post(api.Application.DEPLOY, params)
}

export function mapping (params) {
  return http.post(api.Application.MAPPING, params)
}

export function yarn (params) {
  return http.post(api.Application.YARN, params)
}

export function list (params) {
  return http.post(api.Application.LIST, params)
}

export function name (params) {
  return http.post(api.Application.NAME, params)
}

export function exists (params) {
  return http.post(api.Application.EXISTS, params)
}

export function stop (params) {
  return http.post(api.Application.STOP, params)
}

export function create (params) {
  return http.post(api.Application.CREATE, params)
}

export function remove (params) {
  return http.post(api.Application.DELETE, params)
}

export function removeBak (params) {
  return http.post(api.Application.DELETEBAK, params)
}

export function start (params) {
  return http.post(api.Application.START, params)
}

export function clean (params) {
  return http.post(api.Application.CLEAN, params)
}

export function backUps (params) {
  return http.post(api.Application.BACKUPS, params)
}

export function startLog (params) {
  return http.post(api.Application.STARTLOG, params)
}
