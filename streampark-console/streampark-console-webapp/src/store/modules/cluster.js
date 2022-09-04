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
