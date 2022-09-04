const project = {
  state: {
    projectId: null
  },

  mutations: {
    SET_PROJECT_ID: (state, projectId) => {
      state.projectId = projectId
    },
    CLEAN_PROJECT_ID: (state, empty) => {
      state.projectId = null
    }
  },

  actions: {
    SetProjectId ({ commit }, projectId) {
      commit('SET_PROJECT_ID', projectId)
    },
    CleanProjectId ({ commit }, empty) {
      commit('CLEAN_PROJECT_ID', empty)
    }
  }

}

export default project
