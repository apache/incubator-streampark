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

import type { RouteRecordRaw } from 'vue-router'

export const NOT_FOUND_ROUTE_NAME = 'not-found'

/** Must be registered after dynamic auth routes so it does not shadow real pages. */
export const NOT_FOUND_ROUTE: RouteRecordRaw = {
  path: '/:pathMatch(.*)*',
  name: NOT_FOUND_ROUTE_NAME,
  component: () => import('@/views/build-in/not-found/index.vue'),
  meta: {
    title: '找不到页面',
    withoutTab: true,
  },
}

export function registerNotFoundRoute(router: import('vue-router').Router) {
  if (router.hasRoute(NOT_FOUND_ROUTE_NAME))
    router.removeRoute(NOT_FOUND_ROUTE_NAME)
  router.addRoute(NOT_FOUND_ROUTE)
}

export function removeNotFoundRoute(router: import('vue-router').Router) {
  if (router.hasRoute(NOT_FOUND_ROUTE_NAME))
    router.removeRoute(NOT_FOUND_ROUTE_NAME)
}
