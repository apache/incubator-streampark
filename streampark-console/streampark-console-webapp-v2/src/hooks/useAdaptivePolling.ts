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

export interface AdaptivePollingOptions {
    idleMs?: number
    activeMs?: number
    isBusy?: () => boolean
}

export function useAdaptivePolling(callback: () => void, options: AdaptivePollingOptions = {}) {
    const idleMs = options.idleMs ?? 5000
    const activeMs = options.activeMs ?? 2000
    const isBusy = options.isBusy ?? (() => false)

    let stopped = false
    let timerId: ReturnType<typeof setTimeout> | null = null

    function getDelay() {
        return isBusy() ? activeMs : idleMs
    }

    function schedule() {
        if (stopped) return
        if (timerId) clearTimeout(timerId)
        timerId = setTimeout(() => {
            timerId = null
            if (stopped) return
            callback()
            schedule()
        }, getDelay())
    }

    function start() {
        stopped = false
        schedule()
    }

    function stop() {
        stopped = true
        if (timerId) {
            clearTimeout(timerId)
            timerId = null
        }
    }

    onUnmounted(stop)

    return { start, stop }
}
