import 'vue-router'

declare module 'vue-router' {
  interface RouteMeta extends AppRoute.RouteMeta {}
}
