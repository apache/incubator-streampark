import type { MenuOption } from 'naive-ui'
import { router } from '@/router'
import { registerNotFoundRoute, removeNotFoundRoute } from '@/router/not-found'
import { fetchMenuRouter, fetchUserTeam } from '@/service'
import { normalizeStreamParkMenu, resetStreamParkMenuId } from '@/utils/streampark-menu'
import { usePermissionStoreWithOut } from '@/store/modules/permission'
import { useUserStoreWithOut } from '@/store/modules/user'
import { createMenus, createRoutes, generateCacheRoutes } from './helper'

interface RoutesStatus {
  isInitAuthRoute: boolean
  menus: MenuOption[]
  rowRoutes: AppRoute.RowRoute[]
  activeMenu: string | null
  cacheRoutes: string[]
}

export const useRouteStore = defineStore('route-store', {
  state: (): RoutesStatus => ({
    isInitAuthRoute: false,
    activeMenu: null,
    menus: [],
    rowRoutes: [],
    cacheRoutes: [],
  }),
  actions: {
    resetRouteStore() {
      this.resetRoutes()
      this.$reset()
    },
    resetRoutes() {
      if (router.hasRoute('appRoot'))
        router.removeRoute('appRoot')
      removeNotFoundRoute(router)
      this.isInitAuthRoute = false
    },
    setActiveMenu(key: string) {
      this.activeMenu = key
    },

    async initRouteInfo() {
      resetStreamParkMenuId()
      const userStore = useUserStoreWithOut()
      const userId = userStore.getUserInfo?.userId
      if (userId) {
        const teamResult = await fetchUserTeam({ userId })
        const teams = teamResult.isSuccess && teamResult.data ? teamResult.data : []
        userStore.setTeamList(teams.map((i: { teamName: string, id: string | number }) => ({ label: i.teamName, value: String(i.id) })))
      }

      const permissionStore = usePermissionStoreWithOut()
      const permissions = userStore.getPermissions
      if (permissions?.length)
        permissionStore.setPermCodeList(permissions)

      const result = await fetchMenuRouter()
      if (!result.isSuccess || !result.data)
        throw new Error('Failed to load menu routes')

      return normalizeStreamParkMenu(result.data as any)
    },

    async initAuthRoute() {
      this.isInitAuthRoute = false
      try {
        const rowRoutes = await this.initRouteInfo()
        if (!rowRoutes?.length)
          throw new Error('Empty route list')

        this.rowRoutes = rowRoutes
        const routes = createRoutes(rowRoutes)
        router.addRoute(routes)
        registerNotFoundRoute(router)
        this.menus = createMenus(rowRoutes)
        this.cacheRoutes = generateCacheRoutes(rowRoutes)
        this.isInitAuthRoute = true
      }
      catch (error) {
        this.isInitAuthRoute = false
        throw error
      }
    },
  },
})
