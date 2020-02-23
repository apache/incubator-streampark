import api from './index'
import http from '@/utils/request'

export function login (parameter) {
  return http.post(api.Passport.Login, parameter)
}

export function logout () {
  return http.post(api.Passport.Logout)
}

export function kickout(queryParam) {
  return http.delete(api.Passport.KICKOUT,queryParam)
}