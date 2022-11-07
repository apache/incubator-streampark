/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { signin, signout ,signinbycasdoor} from '@/api/passport'
import { setTeam, initTeam } from '@/api/user'
import {TOKEN, EXPIRE, PERMISSIONS, USER_INFO, USER_NAME, USER_ROUTER, INVALID, TEAM_ID} from '@/store/mutation-types'
import storage from '@/utils/storage'
import { getRouter } from '@/api/menu'

const user = {
  state: {
    expire: storage.get(EXPIRE),
    token: storage.get(TOKEN),
    info: storage.get(USER_INFO),
    permissions: storage.getSession(PERMISSIONS) || storage.get(PERMISSIONS),
    routers: storage.getSession(USER_ROUTER) || storage.get(USER_ROUTER),
    teamId: storage.getSession(TEAM_ID) || storage.get(TEAM_ID),
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
    SET_INFO: (state, info) => {
      storage.set(USER_INFO, info)
      storage.set(USER_NAME, info.username)
      state.info = info
      state.name = info.username
      state.avatar = info.avatar
    },
    SET_TEAM: (state, teamId) => {
      storage.setSession(TEAM_ID, teamId)
      storage.set(TEAM_ID, teamId)
      state.teamId = teamId
    },
    SET_PERMISSIONS: (state, permissions) => {
      storage.set(PERMISSIONS, permissions)
      storage.setSession(PERMISSIONS, permissions)
      state.permissions = permissions
    },
    SET_ROUTERS: (state, routers) => {
      storage.set(USER_ROUTER, routers)
      storage.setSession(USER_ROUTER, routers)
      state.routers = routers
    },
    CLEAR_ROUTERS: (state, empty) => {
      storage.rm(USER_ROUTER)
      storage.rmSession(USER_ROUTER)
    },

    SET_EMPTY: (state, empty) => {
      state.token = null
      state.info = null
      state.permissions = null
      state.name = null
      state.welcome = null
      state.avatar = null
      storage.rm(USER_INFO)
      storage.rm(USER_NAME)
      storage.rm(TOKEN)
      storage.rm(EXPIRE)

      storage.rm(USER_ROUTER)
      storage.rm(TEAM_ID)
      storage.rm(PERMISSIONS)

      storage.rmSession(USER_ROUTER)
      storage.rmSession(TEAM_ID)
      storage.rmSession(PERMISSIONS)
    }
  },

  actions: {
    // Log in
    SignIn ({ commit }, userInfo) {
      return new Promise((resolve, reject) => {
        signin(userInfo).then(response => {
          if (response.code == 403 || response.code == 1 || response.code == 0) {
            reject(response)
          } else {
            const respData = response.data
            if (respData != null && respData.token) {
              commit('SET_EXPIRE', respData.expire)
              commit('SET_TOKEN', respData.token)
              commit('SET_PERMISSIONS', respData.permissions)
              commit('SET_INFO', respData.user)
            }
            if (respData.user.teamId != null) {
              commit('SET_TEAM', respData.user['teamId'])
            }
            storage.rm(INVALID)
            resolve(response)
          }
        }).catch(error => {
          reject(error)
        })
      })
    },
    SignInByCasdoor({commit},userInfo){
      return new Promise((resolve,reject)=>{
        signinbycasdoor(userInfo).then(response => {
          const respData = response.data
          if (respData != null && respData.token) {
            commit('SET_EXPIRE', respData.expire)
            commit('SET_TOKEN', respData.token)
            commit('SET_ROLES', respData.roles)
            commit('SET_PERMISSIONS', respData.permissions)
            commit('SET_INFO', respData.user)
          }
          if (respData.user.teamId != null) {
            commit('SET_TEAM', respData.user['teamId'])
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
    // Sign out
    SignOut ({ commit, state }) {
      return new Promise((resolve) => {
        signout().then((resp) => {
          commit('SET_EMPTY', null)
          resolve()
        }).catch(() => {
          resolve()
        })
      })
    },

    SetTeam({commit}, data) {
      return new Promise((resolve, reject) => {
        if (data.userId != null) {
          initTeam(data).then(() => resolve()).catch(error => reject(error))
        } else {
          setTeam(data).then(resp => {
            const respData = resp.data
            commit('SET_TEAM', data.teamId)
            commit('SET_PERMISSIONS', respData.permissions)
            commit('SET_INFO', respData.user)
            commit('CLEAR_ROUTERS', null)
            resolve()
          }).catch(error => {
            reject(error)
          })
        }
      })
    }

  }
}

export default user
