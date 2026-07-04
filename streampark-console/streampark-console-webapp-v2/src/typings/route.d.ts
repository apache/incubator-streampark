declare namespace AppRoute {

  type MenuType = 'dir' | 'page'
  /** 单个路由所携带的meta标识 */
  interface RouteMeta {
    /* 页面标题，通常必选。 */
    title: string
    /* 图标，一般配合菜单使用 */
    icon?: string
    /* 是否需要登录权限。 */
    requiresAuth?: boolean
    /* 可以访问的角色 */
    roles?: Entity.RoleType[]
    /* 是否开启页面缓存 */
    keepAlive?: boolean
    /* 有些路由我们并不想在菜单中显示，比如某些编辑页面。 */
    hide?: boolean
    /* 菜单排序。 */
    order?: number
    /* 嵌套外链  */
    href?: string
    /** 当前路由不在左侧菜单显示，但需要高亮某个菜单的情况 */
    activeMenu?: string
    /** 当前路由是否会被添加到Tab中 */
    withoutTab?: boolean
    /** 当前路由是否会被固定在Tab中,用于一些常驻页面 */
    pinTab?: boolean
    /** 当前路由在左侧菜单是目录还是页面,不设置默认为page */
    menuType?: MenuType
    /** 页面组件 name，用于 keep-alive */
    componentName?: string
  }

  type MetaKeys = keyof RouteMeta

  interface baseRoute {
    /** 路由名称(路由唯一标识) */
    name: string
    /** 路由路径 */
    path: string
    /** 路由重定向 */
    redirect?: string
    /* 页面组件地址 */
    componentPath?: string | null
    /* 路由id */
    id: number
    /* 父级路由id，顶级页面为null */
    pid: number | null
  }

  /** 单个路由的类型结构(动态路由模式：后端返回此类型结构的路由) */
  type RowRoute = RouteMeta & baseRoute

  /**
   * 挂载到项目上的真实路由结构
   */
  interface Route extends baseRoute {
    /** 子路由 */
    children?: Route[]
    /* 页面组件 */
    component: any
    /** 路由描述 */
    meta: RouteMeta
  }

}
