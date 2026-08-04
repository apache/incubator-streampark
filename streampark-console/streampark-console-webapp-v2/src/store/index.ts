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

import type { App } from 'vue'
import type { Pinia } from 'pinia'
import piniaPluginPersistedstate from 'pinia-plugin-persistedstate'

let globalPinia: Pinia | null = null

export * from './app/index'
export * from './auth'
export * from './dict'
export * from './router'
export * from './tab'

export function installPinia(app: App) {
    globalPinia = createPinia()
    globalPinia.use(piniaPluginPersistedstate)
    app.use(globalPinia)
}

export function getPinia(): Pinia {
    if (!globalPinia) throw new Error('Pinia is not installed')
    return globalPinia
}

export const store = new Proxy({} as Pinia, {
    get(_target, prop) {
        return Reflect.get(getPinia(), prop)
    },
})
