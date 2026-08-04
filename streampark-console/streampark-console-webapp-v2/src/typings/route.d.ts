declare namespace AppRoute {
    type MenuType = 'dir' | 'page'

    interface RouteMeta {
        title: string
        icon?: string
        requiresAuth?: boolean
        roles?: Entity.RoleType[]
        keepAlive?: boolean
        /** Hidden from the sidebar menu (e.g. edit pages). */
        hide?: boolean
        order?: number
        href?: string
        /** Highlights a menu item while this route is hidden from the sidebar. */
        activeMenu?: string
        /** Excluded from tab bar. */
        withoutTab?: boolean
        /** Pinned tab for always-open pages. */
        pinTab?: boolean
        menuType?: MenuType
        /** Component name used by keep-alive. */
        componentName?: string
    }

    type MetaKeys = keyof RouteMeta

    interface baseRoute {
        name: string
        path: string
        redirect?: string
        componentPath?: string | null
        id: number
        pid: number | null
    }

    /** Route shape returned by the backend in dynamic routing mode. */
    type RowRoute = RouteMeta & baseRoute

    interface Route extends baseRoute {
        children?: Route[]
        component: any
        meta: RouteMeta
    }
}
