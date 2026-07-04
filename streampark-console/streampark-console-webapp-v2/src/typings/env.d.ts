/**
 *后台服务的环境类型
 * - dev: 后台开发环境
 * - prod: 后台生产环境
 */
type ServiceEnvType = 'dev' | 'production'

interface ImportMetaEnv {
  /** 项目基本地址 */
  readonly VITE_BASE_URL: string
  /** 项目标题 */
  readonly VITE_APP_NAME: string
  /** 开启请求代理 */
  readonly VITE_HTTP_PROXY?: 'Y' | 'N'
  /** 是否开启打包压缩 */
  readonly VITE_BUILD_COMPRESS?: 'Y' | 'N'
  /** 压缩算法类型 */
  readonly VITE_COMPRESS_TYPE?:
    | 'gzip'
    | 'brotliCompress'
    | 'deflate'
    | 'deflateRaw'
  /** 路由模式 */
  readonly VITE_ROUTE_MODE?: 'hash' | 'web'
  /** 路由加载模式 */
  readonly VITE_ROUTE_LOAD_MODE: 'static' | 'dynamic'
  /** 首次加载页面 */
  readonly VITE_HOME_PATH: string
  /** 版权信息 */
  readonly VITE_COPYRIGHT_INFO: string
  /** 是否自动刷新token */
  readonly VITE_AUTO_REFRESH_TOKEN: 'Y' | 'N'
  /** 默认语言 */
  readonly VITE_DEFAULT_LANG: App.lang
  /** 后端服务的环境类型 */
  readonly MODE: ServiceEnvType
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}
