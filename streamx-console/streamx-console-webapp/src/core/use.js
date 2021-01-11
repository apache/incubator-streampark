import Vue from 'vue'

// base library
import Antd from 'ant-design-vue'
import Viser from 'viser-vue'
import VueCropper from 'vue-cropper'
import 'ant-design-vue/dist/antd.less'

// ext library
import VueClipboard from 'vue-clipboard2'
import PermissionHelper from '@/utils/helper/permission'
import './directives/permission'

// VueParticles
import VueParticles from 'vue-particles'

import ElementUI from 'element-ui'
import 'element-ui/lib/theme-chalk/index.css'

import VueApexCharts from 'vue-apexcharts'

import http from '@/utils/request'
Vue.use(VueParticles)

VueClipboard.config.autoSetContainer = true
Vue.use(ElementUI)

Vue.use(Antd)
Vue.use(Viser)
Vue.component('apexchart', VueApexCharts)
Vue.prototype.$post = http.post
Vue.prototype.$get = http.get
Vue.prototype.$patch = http.patch
Vue.prototype.$put = http.put
Vue.prototype.$upload = http.upload

Vue.use(VueClipboard)
Vue.use(PermissionHelper)
Vue.use(VueCropper)
