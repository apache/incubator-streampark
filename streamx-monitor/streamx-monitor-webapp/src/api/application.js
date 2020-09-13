import api from './index'
import http from '@/utils/request'

export function add (params) {
  return http.post(api.Application.Add, params)
}

export function readConf (params) {
  return http.post(api.Application.ReadConf, params)
}

export function get (params) {
  return http.post(api.Application.Get, params)
}

export function update (params) {
  return http.post(api.Application.Update, params)
}

export function deploy (params) {
  return http.post(api.Application.Deploy, params)
}

export function yarn (params) {
  return http.post(api.Application.Yarn, params)
}

export function list (params) {
  return http.post(api.Application.List, params)
}

export function name (params) {
  return http.post(api.Application.Name, params)
}

export function exists (params) {
  return http.post(api.Application.Exists, params)
}

export function stop (params) {
  return http.post(api.Application.Stop, params)
}

export function create (params) {
  return http.post(api.Application.Create, params)
}

export function remove (params) {
  return http.post(api.Application.Delete, params)
}

export function startUp (params) {
  return http.post(api.Application.StartUp, params)
}

export function closeDeploy (params) {
  return http.post(api.Application.CloseDeploy, params)
}
