import { signin, signout } from '/@/adapter/api/passport';
import {
  TOKEN,
  EXPIRE,
  PERMISSIONS,
  ROLES,
  USER_INFO,
  USER_NAME,
  INVALID,
  USER_ROUTER,
} from '/@/adapter/store/mutation-types';
import storage from '/@/adapter/utils/storage';

export const state = {
  expire: storage.get(EXPIRE),
  token: storage.get(TOKEN),
  info: storage.get(USER_INFO),
  roles: storage.get(ROLES),
  permissions: storage.get(PERMISSIONS),
  name: '',
  welcome: '',
  avatar: '',
};

const SET_EXPIRE = (expire) => {
  storage.set(EXPIRE, expire);
  state.expire = expire;
};
const SET_TOKEN = (token) => {
  storage.set(TOKEN, token);
  state.token = token;
};
const SET_ROLES = (roles) => {
  storage.set(ROLES, roles);
  state.roles = roles;
};
const SET_PERMISSIONS = (permissions) => {
  storage.set(PERMISSIONS, permissions);
  state.permissions = permissions;
};
const SET_INFO = (info) => {
  storage.set(USER_INFO, info);
  storage.set(USER_NAME, info.username);
  state.info = info;
  state.name = info.username;
  state.avatar = info.avatar;
};
const SET_EMPTY = () => {
  state.token = null;
  state.info = null;
  state.roles = null;
  state.permissions = null;
  state.name = '';
  state.welcome = '';
  state.avatar = '';
  storage.rm(USER_INFO);
  storage.rm(USER_ROUTER);
  storage.rm(TOKEN);
  storage.rm(ROLES);
  storage.rm(PERMISSIONS);
  storage.rm(EXPIRE);
};

export async function SignIn(userInfo) {
  const response = await signin(userInfo);
  const respData = response.data;

  if (respData != null && respData.token) {
    SET_EXPIRE(respData.expire);
    SET_TOKEN(respData.token);
    SET_ROLES(respData.roles);
    SET_PERMISSIONS(respData.permissions);
    SET_INFO(respData.user);
  }

  storage.rm(INVALID);

  return response;
}

export async function SignOut() {
  await signout().catch(() => {});
  SET_EMPTY();
}
