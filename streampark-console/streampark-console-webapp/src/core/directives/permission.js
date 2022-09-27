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

// Define some Vue directives related to permissions

const doCheck = function (elems, values, filter, status, el) {
  let flag = !filter
  for (const v of values) {
    if (elems.includes(v) === filter) {
      flag = status
    }
  }
  if (!flag) {
    el.parentNode.removeChild(el)
  }
}

// All permissions listed must be included for the element to display
const permit = {
  install (Vue) {
    Vue.directive('permit', {
      inserted (el, binding, vnode) {
        const permissions = vnode.context.$store.getters.permissions
        const value = binding.value.split(',')
        doCheck(permissions, value, false, false, el)
      }
    })
  }
}

// Render the element when the listed permissions are not included
const noPermit = {
  install (Vue) {
    Vue.directive('noPermit', {
      inserted (el, binding, vnode) {
        const permissions = vnode.context.$store.getters.permissions
        const value = binding.value.split(',')
        doCheck(permissions, value, true, false, el)
      }
    })
  }
}

// The element displays as long as any of the listed permissions are included
const anyPermit = {
  install (Vue) {
    Vue.directive('anyPermit', {
      inserted (el, binding, vnode) {
        const permissions = vnode.context.$store.getters.permissions
        const value = binding.value.split(',')
        doCheck(permissions, value, true, true, el)
      }
    })
  }
}

// All roles listed must be included for the element to display
const hasRole = {
  install (Vue) {
    Vue.directive('hasRole', {
      inserted (el, binding, vnode) {
        const roles = vnode.context.$store.getters.roles
        const value = binding.value.split(',')
        doCheck(roles, value, false, false, el)
      }
    })
  }
}

// The element will display as long as it contains any of the listed roles
const hasAnyRole = {
  install (Vue) {
    Vue.directive('hasAnyRole', {
      inserted (el, binding, vnode) {
        const roles = vnode.context.$store.getters.roles
        const value = binding.value.split(',')
        doCheck(roles, value, true, true, el)
      }
    })
  }
}

const Plugins = [
  permit,
  noPermit,
  anyPermit,
  hasRole,
  hasAnyRole
]

Plugins.map((plugin) => {
  Vue.use(plugin)
})

export default Vue
