import api from './index'
import http from '@/utils/request'

export function add (params) {
  return http.post(api.Application.Add, params)
}

export function update (params) {
  return http.post(api.Application.Update, params)
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

export function create (params) {
  return http.post(api.Application.Create, params)
}

export function remove (params) {
  return http.post(api.Application.Delete, params)
}

export function startUp (params) {
  return http.post(api.Application.StartUp, params)
}
