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

const cluster = {
  state: {
    clusterId: null
  },

  mutations: {
    SET_CLUSTER_ID: (state, clusterId) => {
      state.clusterId = clusterId
    },
    CLEAN_CLUSTER_ID: (state, empty) => {
      state.clusterId = null
    }
  },

  actions: {
    SetClusterId ({ commit }, clusterId) {
      commit('SET_CLUSTER_ID', clusterId)
    },
    CleanClusterId ({ commit }, empty) {
      commit('CLEAN_CLUSTER_ID', empty)
    }
  }

}

export default cluster
