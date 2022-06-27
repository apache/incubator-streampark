import Vue from 'vue'

// base library
import Antd from 'ant-design-vue'
import VueCropper from 'vue-cropper'
import 'ant-design-vue/dist/antd.less'

// ext library
import VueClipboard from 'vue-clipboard2'
import PermissionHelper from '@/utils/helper/permission'
import './directives/permission'

// VueParticles
import VueParticles from 'vue-particles'
import http from '@/utils/request'
import util from '@/utils/util'
import Adapter, { isInIframe } from '@/adapter'
//sweetalert
import VueSweetalert2 from 'vue-sweetalert2'
import 'sweetalert2/dist/sweetalert2.min.css'

VueClipboard.config.autoSetContainer = true

Vue.use(Antd)

if (isInIframe) {
  Vue.use(Adapter)
} else {
  Vue.use(VueSweetalert2)
}

Vue.use(VueParticles)
Vue.use(VueClipboard)
Vue.use(PermissionHelper)
Vue.use(VueCropper)

Vue.prototype.$post = http.post
Vue.prototype.$get = http.get
Vue.prototype.$patch = http.patch
Vue.prototype.$put = http.put
Vue.prototype.$upload = http.upload

Vue.prototype.timeFix = util.timeFix
Vue.prototype.welcome = util.welcome
Vue.prototype.triggerWindowResize = util.triggerWindowResize
Vue.prototype.handleScrollHeader = util.handleScrollHeader
Vue.prototype.removeLoadingAnimate = util.removeLoadingAnimate
Vue.prototype.addClass = util.addClass
Vue.prototype.hasClass = util.hasClass
Vue.prototype.removeClass = util.removeClass

