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

import config from '@/config/defaultSettings'

const storage = {
  /**
   * Set storage
   *
   * @param name
   * @param content
   * @param maxAge
   */

  set: (name, content, maxAge = null) => {
    name = config.storageOptions.namespace + name
    if (!global.window || !name) {
      return
    }

    if (typeof content !== 'string') {
      content = JSON.stringify(content)
    }

    const storage = global.window.localStorage

    storage.setItem(name, content)
    if (maxAge && !isNaN(parseInt(maxAge))) {
      const timeout = parseInt(new Date().getTime() / 1000)
      storage.setItem(`${name}_EXPIRE`, timeout + maxAge)
    }
  },

  /**
   * Get storage
   * @param name
   * @param defValue
   * @returns {string|any}
   */
  get: (name, defValue) => {
    name = config.storageOptions.namespace + name
    defValue = defValue === undefined ? null : defValue
    if (!global.window || !name) {
      return defValue
    }
    const content = window.localStorage.getItem(name)

    if (!content) return defValue

    const _expire = window.localStorage.getItem(`${name}_EXPIRE`)
    if (_expire) {
      const now = parseInt(new Date().getTime() / 1000)
      if (now > _expire) {
        return defValue
      }
    }
    try {
      return JSON.parse(content)
    } catch (e) {
      return content
    }
  },

  rm: (name) => {
    name = config.storageOptions.namespace + name
    if (!global.window || !name) {
      return
    }
    window.localStorage.removeItem(name)
    window.localStorage.removeItem(`${name}_EXPIRE`)
  },

  clear: () => {
    if (!global.window || !name) {
      return
    }
    window.localStorage.clear()
  },

  setSession: (name, content) => {
    name = config.storageOptions.namespace + name
    if (!global.window || !name) {
      return
    }
    if (typeof content !== 'string') {
      content = JSON.stringify(content)
    }
    const storage = global.window.sessionStorage
    storage.setItem(name, content)
  },

  getSession: (name, defValue) => {
    name = config.storageOptions.namespace + name
    defValue = defValue === undefined ? null : defValue
    if (!global.window || !name) {
      return defValue
    }
    const storage = global.window.sessionStorage
    const content = storage.getItem(name)
    if (!content) return defValue
    try {
      return JSON.parse(content)
    } catch (e) {
      return content
    }
  },

  rmSession: (name) => {
    name = config.storageOptions.namespace + name
    if (!global.window || !name) {
      return
    }
    window.sessionStorage.removeItem(name)
  },


}

export default storage
