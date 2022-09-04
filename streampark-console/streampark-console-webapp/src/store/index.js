import Vue from 'vue'
import Vuex from 'vuex'

import app from './modules/app'
import application from './modules/application'
import project from './modules/project'
import cluster from './modules/cluster'
import user from './modules/user'
import getters from './getters'

Vue.use(Vuex)

export default new Vuex.Store({
  modules: {
    app,
    user,
    application,
    cluster,
    project
  },
  state: {},
  mutations: {},
  actions: {},
  getters
})
