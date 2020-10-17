import api from './index'
import http from '@/utils/request'

export function login (parameter) {
  return http.post(api.Passport.LOGIN, parameter)
}

export function logout () {
  return http.post(api.Passport.LOGOUT)
}
