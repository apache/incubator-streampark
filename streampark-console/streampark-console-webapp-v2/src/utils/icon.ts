import { Icon } from '@iconify/vue'
import { NIcon } from 'naive-ui'
import { h } from 'vue'
import SvgIcon from '@/components/Icon/src/SvgIcon.vue'
import { resolveIconifyRef, resolveViconComponent } from '@/components/Icon/xicons'

export function renderIcon(icon?: string, props?: import('naive-ui').IconProps) {
    if (!icon) return

    return () => createIcon(icon, props)
}

export function createIcon(icon?: string, props?: import('naive-ui').IconProps) {
    if (!icon) return

    if (icon.endsWith('|svg')) {
        const name = icon.replace('|svg', '')
        const size =
            typeof props?.size === 'number'
                ? props.size
                : Number.parseInt(String(props?.size ?? 18), 10) || 18
        return h(NIcon, props, {
            default: () => h(SvgIcon, { name, size }),
        })
    }

    const isLocal = icon.startsWith('local:')
    if (isLocal) {
        const svgName = icon.replace('local:', '')
        const svg = import.meta.glob('@/assets/svg-icons/*.svg', {
            query: '?raw',
            import: 'default',
            eager: true,
        })
        const target = svg[`/src/assets/svg-icons/${svgName}.svg`]
        return h(NIcon, { ...props, innerHTML: target })
    }

    const iconifyName = icon.includes(':') ? icon : resolveIconifyRef(icon)
    if (iconifyName) return h(NIcon, props, { default: () => h(Icon, { icon: iconifyName }) })

    const vicon = resolveViconComponent(icon)
    if (vicon) return h(NIcon, props, { default: () => h(vicon) })

    return undefined
}
