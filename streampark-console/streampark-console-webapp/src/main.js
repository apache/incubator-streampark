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

import 'core-js/stable'
import 'regenerator-runtime/runtime'

import Vue from 'vue'
import App from './App.vue'
import router from './router'
import store from './store/'

import bootstrap from './core/bootstrap'
import $ from 'jquery'
import './core/use'
import './core/prototype'
import './utils/filter' // global filter
import Casdoor from 'casdoor-vue-sdk'
import VueCompositionAPI from '@vue/composition-api'

const config = {
  serverUrl: 'http://localhost:7001',
  clientId: 'b6f7a3bdee5d3f424ee2',
  organizationName: 'test',
  appName: 'app-test',
  redirectPath: '/callback',
}

Vue.config.productionTip = false
Vue.use(VueCompositionAPI)
Vue.use(Casdoor,config)

new Vue({
  router,
  store,
  $,
  created () {
    bootstrap()
  },
  render: h => h(App)
}).$mount('#app')
