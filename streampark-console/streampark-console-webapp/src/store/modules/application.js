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

const application = {
  state: {
    appId: null
  },

  mutations: {
    SET_APP_ID: (state, appId) => {
      state.appId = appId
    },
    CLEAN_APP_ID: (state, empty) => {
      state.appId = null
    }
  },

  actions: {
    SetAppId ({ commit }, appId) {
      commit('SET_APP_ID', appId)
    },
    CleanAppId ({ commit }, empty) {
      commit('CLEAN_APP_ID', empty)
    }
  }

}

export default application
