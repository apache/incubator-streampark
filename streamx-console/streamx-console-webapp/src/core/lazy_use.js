import Vue from 'vue'

// base library
import '@/core/lazy_lib/components_use'
import Viser from 'viser-vue'

// ext library
import VueClipboard from 'vue-clipboard2'
import PermissionHelper from '@/utils/helper/permission'
import './directives/permission'

VueClipboard.config.autoSetContainer = true

Vue.use(Viser)
Vue.use(VueClipboard)
Vue.use(PermissionHelper)
