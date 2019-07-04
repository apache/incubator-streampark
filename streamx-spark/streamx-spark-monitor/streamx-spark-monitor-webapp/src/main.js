import Vue from 'vue'
import StreamX from './index'
import router from './router'
import Antd from 'ant-design-vue'
import store from './store'
import request from 'utils/request'
import db from 'utils/localstorage'
import VueApexCharts from 'vue-apexcharts'

import 'ant-design-vue/dist/antd.css'

import 'utils/install'

Vue.config.productionTip = false
Vue.use(Antd)
Vue.use(db)
Vue.use(VueApexCharts)

Vue.component('apexchart', VueApexCharts)

Vue.use({
    install(Vue) {
        Vue.prototype.$db = db
    }
})

Vue.prototype.$post = request.post
Vue.prototype.$get = request.get
Vue.prototype.$put = request.put
Vue.prototype.$delete = request.delete
Vue.prototype.$export = request.export
Vue.prototype.$download = request.download
Vue.prototype.$upload = request.upload

/* eslint-disable no-new */
new Vue({
    router,
    store,
    render: h => h(StreamX)
}).$mount('#streamx')
