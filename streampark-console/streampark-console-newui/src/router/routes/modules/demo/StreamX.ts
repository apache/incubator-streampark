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
import type { AppRouteModule } from '/@/router/types';

import { LAYOUT } from '/@/router/constant';

const streamxConsole: AppRouteModule = {
  path: '/flink',
  name: 'Flink',
  component: LAYOUT,
  redirect: '/flink/app',
  meta: {
    orderNo: 1,
    icon: 'fluent:stream-input-20-regular',
    title: 'StreamPark',
  },
  children: [
    {
      path: 'project',
      name: 'FlinkProject',
      component: () => import('/@/views/flink/project/View.vue'),
      meta: {
        icon: 'arcticons:projectm',
        title: 'Project',
      },
    },
    {
      path: 'app',
      name: 'FlinkApp',
      component: () => import('/@/views/flink/app/View.vue'),
      meta: {
        icon: 'arcticons:tinc-app',
        title: 'Application',
      },
    },
    {
      path: 'notebook',
      name: 'FlinkNotebook',
      component: () => import('/@/views/flink/notebook/Submit.vue'),
      meta: {
        icon: 'ep:notebook',
        title: 'Notebook',
      },
    },
    {
      path: 'setting',
      name: 'FlinkSetting',
      component: () => import('/@/views/flink/setting/View.vue'),
      meta: {
        icon: 'ion:settings-outline',
        title: 'Setting',
      },
    },
  ],
};

export default streamxConsole;
