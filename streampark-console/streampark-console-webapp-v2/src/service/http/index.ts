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

import { createAlovaInstance } from './alova'

const alova = createAlovaInstance({
    baseURL: __URL_MAP__.api.path,
})

export const blankInstance = createAlovaInstance({
    baseURL: '',
})

type AlovaInstance = ReturnType<typeof createAlovaInstance>
type AlovaMethod = ReturnType<AlovaInstance['Post']>

/** Alova Method that resolves to the StreamPark service envelope. */
export type TypedMethod<T> = AlovaMethod & Promise<Service.RequestResult<T>>

function createTypedRequest(instance: AlovaInstance) {
    return {
        Post<T = unknown>(
            url: string,
            data?: Parameters<AlovaInstance['Post']>[1],
            config?: Parameters<AlovaInstance['Post']>[2],
        ): TypedMethod<T> {
            return instance.Post(url, data, config) as TypedMethod<T>
        },
        Get<T = unknown>(
            url: string,
            config?: Parameters<AlovaInstance['Get']>[1],
        ): TypedMethod<T> {
            return instance.Get(url, config) as TypedMethod<T>
        },
        Put<T = unknown>(
            url: string,
            data?: Parameters<AlovaInstance['Put']>[1],
            config?: Parameters<AlovaInstance['Put']>[2],
        ): TypedMethod<T> {
            return instance.Put(url, data, config) as TypedMethod<T>
        },
        Delete<T = unknown>(
            url: string,
            data?: Parameters<AlovaInstance['Delete']>[1],
            config?: Parameters<AlovaInstance['Delete']>[2],
        ): TypedMethod<T> {
            return instance.Delete(url, data, config) as TypedMethod<T>
        },
    }
}

export const request = createTypedRequest(alova)
