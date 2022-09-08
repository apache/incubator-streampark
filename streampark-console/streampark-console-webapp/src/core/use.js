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
//sweetalert
import VueSweetalert2 from 'vue-sweetalert2'
import 'sweetalert2/dist/sweetalert2.min.css'

VueClipboard.config.autoSetContainer = true

Vue.use(Antd)
Vue.use(VueParticles)
Vue.use(VueClipboard)
Vue.use(PermissionHelper)
Vue.use(VueCropper)

Vue.use(VueSweetalert2)

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

