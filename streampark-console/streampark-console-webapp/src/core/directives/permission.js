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

// 定义一些和权限有关的 Vue指令

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

// 必须包含列出的所有权限，元素才显示
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

// 当不包含列出的权限时，渲染该元素
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

// 只要包含列出的任意一个权限，元素就会显示
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

// 必须包含列出的所有角色，元素才显示
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

// 只要包含列出的任意一个角色，元素就会显示
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
