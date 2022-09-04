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
