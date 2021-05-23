import { signin, signout } from '@/api/passport'
import {TOKEN, EXPIRE, PERMISSIONS, ROLES, USER_INFO,USER_NAME, USER_ROUTER, INVALID} from '@/store/mutation-types'
import storage from '@/utils/storage'
import { getRouter } from '@/api/menu'

const user = {
  state: {
    expire: storage.get(EXPIRE),
    token: storage.get(TOKEN),
    info: storage.get(USER_INFO),
    roles: storage.get(ROLES),
    permissions: storage.get(PERMISSIONS),
    routers: storage.get(USER_ROUTER),
    name: '',
    welcome: '',
    avatar: ''
  },

  mutations: {
    SET_EXPIRE: (state, expire) => {
      storage.set(EXPIRE, expire)
      state.expire = expire
    },
    SET_TOKEN: (state, token) => {
      storage.set(TOKEN, token)
      state.token = token
    },
    SET_ROLES: (state, roles) => {
      storage.set(ROLES, roles)
      state.roles = roles
    },
    SET_PERMISSIONS: (state, permissions) => {
      storage.set(PERMISSIONS, permissions)
      state.permissions = permissions
    },
    SET_ROUTERS: (state, routers) => {
      storage.set(USER_ROUTER, routers)
      state.routers = routers
    },
    SET_INFO: (state, info) => {
      storage.set(USER_INFO, info)
      storage.set(USER_NAME, info.username)
      state.info = info
      state.name = info.username
      state.avatar = info.avatar
    },
    SET_EMPTY: (state, empty) => {
      state.token = null
      state.info = null
      state.roles = null
      state.permissions = null
      state.name = null
      state.welcome = null
      state.avatar = null
      storage.rm(USER_INFO)
      storage.rm(USER_ROUTER)
      storage.rm(TOKEN)
      storage.rm(ROLES)
      storage.rm(PERMISSIONS)
      storage.rm(EXPIRE)
    }
  },

  actions: {
    // 登录
    SignIn ({ commit }, userInfo) {
      return new Promise((resolve, reject) => {
        signin(userInfo).then(response => {
          const respData = response.data
          if (respData != null && respData.token) {
            commit('SET_EXPIRE', respData.expire)
            commit('SET_TOKEN', respData.token)
            commit('SET_ROLES', respData.roles)
            commit('SET_PERMISSIONS', respData.permissions)
            commit('SET_INFO', respData.user)
          }
          storage.rm(INVALID)
          resolve(response)
        }).catch(error => {
          reject(error)
        })
      })
    },
    GetRouter ({ commit }, data) {
      return new Promise((resolve, reject) => {
        getRouter({}).then(resp => {
          const respData = resp.data
          commit('SET_ROUTERS', respData)
          resolve(respData)
        }).catch(error => {
          reject(error)
        })
      })
    },
    // 登出
    SignOut ({ commit, state }) {
      return new Promise((resolve) => {
        signout().then((resp) => {
          commit('SET_EMPTY', null)
          resolve()
        }).catch(() => {
          resolve()
        })
      })
    }

  }
}

export default user
