import type { App, Directive } from 'vue'
import { usePermission } from '@/hooks'

export function install(app: App) {
    const { hasPermission } = usePermission()

    function updatapermission(el: HTMLElement, permission: Entity.RoleType | Entity.RoleType[]) {
        if (!permission) throw new Error('v-permission directive requires an explicit role')

        if (!hasPermission(permission)) el.parentElement?.removeChild(el)
    }

    const permissionDirective: Directive<HTMLElement, Entity.RoleType | Entity.RoleType[]> = {
        mounted(el, binding) {
            updatapermission(el, binding.value)
        },
        updated(el, binding) {
            updatapermission(el, binding.value)
        },
    }
    app.directive('permission', permissionDirective)
}
