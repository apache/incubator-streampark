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

import type { LoginParams, LoginResultModel } from '@/types/api/system/model/userModel'
import { request } from '../http'

export function fetchSignin(data: LoginParams) {
  const method = request.Post<LoginResultModel>('/passport/signin', data)
  method.meta = { authRole: null }
  return method
}

export function fetchSignout() {
  return request.Post('/passport/signout')
}

export function fetchSignType() {
  const method = request.Post<string[]>('/passport/signtype', {})
  method.meta = { authRole: null }
  return method
}

export function fetchSsoToken() {
  const method = request.Get<import('@/types/api/system/model/userModel').LoginResultModel>('/sso/token')
  method.meta = { authRole: null }
  return method
}
