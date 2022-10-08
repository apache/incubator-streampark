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
