import type { RouteRecordRaw } from 'vue-router'

/** Built-in routes such as login and error pages. */
export const routes: RouteRecordRaw[] = [
  {
    path: '/',
    name: 'root',
    children: [
    ],
  },
  {
    path: '/login',
    name: 'login',
    component: () => import('@/views/build-in/login/index.vue'),
    meta: {
      title: '登录',
      withoutTab: true,
    },
  },
  {
    path: '/not-found',
    name: 'not-found-page',
    component: () => import('@/views/build-in/not-found/index.vue'),
    meta: {
      title: '找不到页面',
      icon: 'question',
      withoutTab: true,
      requiresAuth: false,
    },
  },

]
