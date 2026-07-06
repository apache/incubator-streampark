/** Backend service environment: `dev` for development, `production` for production. */
type ServiceEnvType = 'dev' | 'production'

interface ImportMetaEnv {
  readonly VITE_BASE_URL: string
  readonly VITE_APP_NAME: string
  readonly VITE_HTTP_PROXY?: 'Y' | 'N'
  readonly VITE_BUILD_COMPRESS?: 'Y' | 'N'
  readonly VITE_COMPRESS_TYPE?:
    | 'gzip'
    | 'brotliCompress'
    | 'deflate'
    | 'deflateRaw'
  readonly VITE_ROUTE_MODE?: 'hash' | 'web'
  readonly VITE_ROUTE_LOAD_MODE: 'static' | 'dynamic'
  readonly VITE_HOME_PATH: string
  readonly VITE_COPYRIGHT_INFO: string
  readonly VITE_AUTO_REFRESH_TOKEN: 'Y' | 'N'
  readonly VITE_DEFAULT_LANG: App.lang
  readonly MODE: ServiceEnvType
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}
