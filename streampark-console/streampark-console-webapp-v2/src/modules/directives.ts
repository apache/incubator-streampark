import type { App } from 'vue'

export function install(app: App) {
  Object.values(
    import.meta.glob<{ install: (app: App) => void }>('@/directives/*.ts', {
      eager: true,
    }),
  ).map(i => app.use(i))
}
