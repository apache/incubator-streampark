import api from './index';
import http from '/@/adapter/utils/request';

export function signin(parameter) {
  return http.post(api.Passport.SIGN_IN, parameter);
}

export function signout() {
  return http.post(api.Passport.SIGN_OUT);
}
