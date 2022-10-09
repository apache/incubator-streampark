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

const streamParkSystem: AppRouteModule = {
  path: '/system',
  name: 'System',
  component: LAYOUT,
  redirect: '/system/user',
  meta: {
    orderNo: 0,
    icon: 'ant-design:desktop-outlined',
    title: 'System',
  },
  children: [
    {
      path: 'token',
      name: 'SystemToken',
      component: () => import('/@/views/system/token/Token.vue'),
      meta: {
        icon: 'material-symbols:generating-tokens-outline',
        title: 'Token Management',
      },
    },
    {
      path: 'user',
      name: 'SystemUser',
      component: () => import('/@/views/system/user/User.vue'),
      meta: {
        icon: 'ant-design:user-outlined',
        title: 'User Management',
      },
    },
    {
      path: 'role',
      name: 'SystemRole',
      component: () => import('/@/views/system/role/Role.vue'),
      meta: {
        icon: 'carbon:user-role',
        title: 'Role Management',
      },
    },
    {
      path: 'menu',
      name: 'SystemMenu',
      component: () => import('/@/views/system/menu/Menu.vue'),
      meta: {
        icon: 'ant-design:menu-fold-outlined',
        title: 'Router Management',
      },
    },
    {
      path: 'team',
      name: 'SystemTeam',
      component: () => import('/@/views/system/team/Team.vue'),
      meta: {
        icon: 'fluent:people-team-20-regular',
        title: 'Team Management',
      },
    },
  ],
};

export default streamParkSystem;
