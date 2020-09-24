import Vue from 'vue'
import Router from 'vue-router'
import LoginView from '@/views/user/Login'
import { BasicView, RouteView, EmptyView, PageView } from '@/layouts'
import store from '@/store'
import storage from '@/utils/storage'

import NProgress from 'nprogress' // progress bar
import 'nprogress/nprogress.css' // progress bar style
import notification from 'ant-design-vue/es/notification'
import { TOKEN } from '@/store/mutation-types'

NProgress.configure({ showSpinner: false }) // NProgress Configuration

Vue.use(Router)

const constRouter = [
  {
    path: '/login',
    name: 'login',
    component: LoginView
  },
  {
    path: '/index',
    name: 'home',
    redirect: '/home'
  }
]

const router = new Router({
  routes: constRouter,
  base: process.env.BASE_URL,
  scrollBehavior: () => ({ y: 0 })
})

const whiteList = ['login']

let asyncRouter

// 导航守卫，渲染动态路由
router.beforeEach((to, from, next) => {
  NProgress.start() // start progress bar
  if (whiteList.indexOf(to.path) !== -1) {
    next()
  }
  const token = storage.get(TOKEN)
  if (token) {
    if (!asyncRouter) {
      // 如果用户路由不存在
      const routers = store.getters.routers
      if (routers) {
        asyncRouter = routers
        go(to, next)
      } else {
        // 获取当前这个用户所在角色可访问的全部路由
        store.dispatch('GetRouter', {}).then((resp) => {
          asyncRouter = resp
          go(to, next)
        }).catch(() => {
          notification.error({
            message: '错误',
            description: '请求用户信息失败，请重试'
          })
          store.dispatch('Logout').then(() => {
            next({ path: '/user/login', query: { redirect: to.fullPath } })
          })
        })
      }
    } else {
      next()
    }
  } else {
    if (whiteList.includes(to.name)) {
      next()
    } else {
      next({ name: 'login', query: { redirect: to.fullPath } })
      NProgress.done()
    }
  }
})

router.afterEach(() => {
  NProgress.done() // finish progress bar
})

function go (to, next) {
  asyncRouter = buildRouter(asyncRouter)
  router.addRoutes(asyncRouter)
  next({ ...to, replace: true })
}

function buildRouter (routes) {
  return routes.filter((route) => {
    if (route.component) {
      switch (route.component) {
        case 'BasicView':
          route.component = BasicView
          break
        case 'RouteView':
          route.component = RouteView
          break
        case 'EmptyView':
          route.component = EmptyView
          break
        case 'PageView':
          route.component = PageView
          break
        default:
          route.component = resolveView(route.component)
      }
      if (route.children && route.children.length) {
        route.children = buildRouter(route.children)
      }
      return true
    }
  })
}

function resolveView (path) {
  return function (resolve) {
    import(`@/views/${path}.vue`).then(mod => {
      resolve(mod)
    })
  }
}

export default router
