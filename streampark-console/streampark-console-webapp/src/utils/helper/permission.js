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

const PERMISSION_ENUM = {
  'add': { key: 'add', label: 'Add' },
  'delete': { key: 'delete', label: 'Delete' },
  'edit': { key: 'edit', label: 'Edit' },
  'query': { key: 'query', label: 'Query' },
  'get': { key: 'get', label: 'Details' },
  'enable': { key: 'enable', label: 'Enable' },
  'disable': { key: 'disable', label: 'Disable' },
  'import': { key: 'import', label: 'Import' },
  'export': { key: 'export', label: 'Export' }
}

function plugin (Vue) {
  if (plugin.installed) {
    return
  }

  !Vue.prototype.$auth && Object.defineProperties(Vue.prototype, {
    $auth: {
      get () {
        const _this = this
        return (permissions) => {
          const [permission, action] = permissions.split('.')
          const permissionList = _this.$store.getters.permissions
          permissionList.find((val) => {
            return val.permissionId === permission
          }).actionList.findIndex((val) => {
            return val === action
          })
          return false
        }
      }
    }
  })

  !Vue.prototype.$enum && Object.defineProperties(Vue.prototype, {
    $enum: {
      get () {
        // const _this = this;
        return (val) => {
          let result = PERMISSION_ENUM
          val && val.split('.').forEach(v => {
            result = result && result[v] || null
          })
          return result
        }
      }
    }
  })
}

export default plugin
