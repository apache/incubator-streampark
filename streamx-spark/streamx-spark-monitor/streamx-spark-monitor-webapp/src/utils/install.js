import Vue from 'vue'

import {hasPermission, hasNoPermission, hasAnyPermission, hasRole, hasAnyRole} from 'utils/permissionDirect'

const Plugins = [
  hasPermission,
  hasNoPermission,
  hasAnyPermission,
  hasRole,
  hasAnyRole
]

Plugins.map((plugin) => {
  Vue.use(plugin)
})

export default Vue
