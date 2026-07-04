import UnoCSS from 'unocss/vite'
import { resolve } from 'node:path'
import vue from '@vitejs/plugin-vue'
import vueJsx from '@vitejs/plugin-vue-jsx'
import AutoImport from 'unplugin-auto-import/vite'
import { FileSystemIconLoader } from 'unplugin-icons/loaders'
import IconsResolver from 'unplugin-icons/resolver'
import Icons from 'unplugin-icons/vite'
import { NaiveUiResolver } from 'unplugin-vue-components/resolvers'
import Components from 'unplugin-vue-components/vite'
import viteCompression from 'vite-plugin-compression'
import { createSvgIconsPlugin } from 'vite-plugin-svg-icons'
import AutoProxy from './autoProxy'
import { createStreamParkAppConfigPlugin } from './streamparkAppConfig'
import { serviceConfig } from '../service.config'

export function createVitePlugins(env: ImportMetaEnv, mode: string) {
  const plugins = [
    vue(),
    vueJsx(),

    UnoCSS(),

    createSvgIconsPlugin({
      iconDirs: [resolve(process.cwd(), 'src/assets/icons')],
      symbolId: 'icon-[dir]-[name]',
    }),

    AutoImport({
      imports: [
        'vue',
        'vue-router',
        'pinia',
        '@vueuse/core',
        'vue-i18n',
        {
          '@/utils/errorMessage': [
            'showResultError',
            'showCatchError',
            'resolveResultMessage',
            'throwApiFailure',
          ],
        },
        {
          'naive-ui': [
            'useDialog',
            'useMessage',
            'useNotification',
            'useLoadingBar',
            'useModal',
            'NBadge',
            'NButton',
            'NDropdown',
            'NFlex',
            'NIcon',
            'NPopconfirm',
            'NSpace',
            'NSwitch',
            'NTag',
            'NTooltip',
          ],
        },
      ],
      include: [
        /\.[tj]sx?$/,
        /\.vue$/,
        /\.vue\?vue/,
        /\.md$/,
      ],
      dts: 'src/typings/auto-imports.d.ts',
    }),

    Components({
      dts: 'src/typings/components.d.ts',
      dirs: ['src/components', 'src/layouts', 'src/views/build-in'],
      exclude: [/[\\/]node_modules[\\/]/],
      resolvers: [
        IconsResolver({
          prefix: false,
          customCollections: [
            'svg-icons',
          ],
        }),
        NaiveUiResolver(),
      ],
    }),

    Icons({
      defaultStyle: 'display:inline-block',
      compiler: 'vue3',
      customCollections: {
        'svg-icons': FileSystemIconLoader(
          'src/assets/svg-icons',
          svg => svg.replace(/^<svg /, '<svg fill="currentColor" width="1.2em" height="1.2em"'),
        ),
      },
    }),

    AutoProxy({
      enableProxy: env.VITE_HTTP_PROXY === 'Y',
      serviceConfig,
      dts: 'src/typings/auto-proxy.d.ts',
    }),

    createStreamParkAppConfigPlugin(mode),
  ]

  if (env.VITE_BUILD_COMPRESS === 'Y') {
    const { VITE_COMPRESS_TYPE = 'gzip' } = env
    plugins.push(viteCompression({
      algorithm: VITE_COMPRESS_TYPE,
    }))
  }

  return plugins
}
