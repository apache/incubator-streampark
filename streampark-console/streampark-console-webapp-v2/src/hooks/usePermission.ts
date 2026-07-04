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

import { useUserStoreWithOut } from '@/store/modules/user'
import { isArray, isString } from 'radash'

const ADMIN_USERNAME = 'admin'

/** StreamPark permission helper (perm codes + roles from backend login payload) */
export function usePermission() {
  function hasPermission(permission?: string | string[], def = true) {
    if (!permission)
      return def

    const userStore = useUserStoreWithOut()
    if (userStore.getUserInfo?.username === ADMIN_USERNAME)
      return true

    const permCodes = userStore.getPermissions || []
    if (permCodes.length) {
      if (isArray(permission))
        return permission.some(code => permCodes.includes(code))
      if (isString(permission))
        return permCodes.includes(permission)
    }

    const roles = (userStore.getRoleList || []) as string[]
    if (roles.includes('admin') || roles.includes('super'))
      return true

    if (isArray(permission))
      return permission.some(role => roles.includes(role))
    if (isString(permission))
      return roles.includes(permission)
    return false
  }

  return { hasPermission }
}
