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

import type { App, Directive } from 'vue'
import { usePermission } from '@/hooks'

/** Fine-grained permission directive, e.g. v-auth="'user:add'" */
export function install(app: App) {
    const { hasPermission } = usePermission()

    function updateAuth(el: HTMLElement, permission: string | string[]) {
        if (!permission) return
        if (!hasPermission(permission)) el.parentElement?.removeChild(el)
    }

    const authDirective: Directive<HTMLElement, string | string[]> = {
        mounted(el, binding) {
            updateAuth(el, binding.value)
        },
        updated(el, binding) {
            updateAuth(el, binding.value)
        },
    }

    app.directive('auth', authDirective)
}
