import type { ProxyOptions, UserConfig } from 'vite'
import { mkdirSync, readFileSync, writeFileSync } from 'node:fs'
import { dirname } from 'node:path'

interface ServiceConfig {
  [key: string]: string
}

type ServiceEnvType = string

interface FullServiceConfig {
  [key: ServiceEnvType]: ServiceConfig
}

interface ProxyItem {
  path: string
  rawPath: string
}

interface ProxyMapping {
  [serviceName: string]: ProxyItem
}

export interface ServiceProxyPluginOptions {
  serviceConfig: FullServiceConfig
  /** Proxy path prefix. Defaults to `proxy-`. */
  proxyPrefix?: string
  enableProxy?: boolean
  /** Global define key for the URL map. Defaults to `__URL_MAP__`. */
  envName?: string
  /** Optional path to emit generated proxy typings. */
  dts?: string
}

export default function createServiceProxyPlugin(options: ServiceProxyPluginOptions) {
  const {
    serviceConfig,
    proxyPrefix = 'proxy-',
    enableProxy = true,
    envName = '__URL_MAP__',
    dts,
  } = options

  return {
    name: 'vite-auto-proxy',
    config(config: UserConfig, { mode, command }: { mode: string, command: 'build' | 'serve' }) {
      const isDev = command === 'serve'

      if (!config.define) {
        config.define = {}
      }

      if (!enableProxy || !isDev) {
        const rawMapping: ProxyMapping = {}
        const envConfig = serviceConfig[mode]

        if (envConfig) {
          Object.entries(envConfig).forEach(([serviceName, serviceUrl]) => {
            rawMapping[serviceName] = {
              path: serviceUrl,
              rawPath: serviceUrl,
            }
          })
          console.warn(`[auto-proxy] Loaded ${Object.keys(envConfig).length} service URLs`)
        }
        else {
          console.warn(`[auto-proxy] No config found for environment "${mode}"`)
        }

        config.define[envName] = JSON.stringify(rawMapping)

        if (dts) {
          generateDtsFile(rawMapping, dts, envName)
        }
        return
      }

      console.warn(`[auto-proxy] Loaded ${Object.keys(serviceConfig[mode]).length} service URLs for ${mode}`)

      const { proxyConfig, proxyMapping } = generateProxyFromServiceConfig(serviceConfig, mode, proxyPrefix)

      Object.entries(proxyMapping).forEach(([serviceName, proxyItem]) => {
        console.warn(`[auto-proxy] ${serviceName} | proxy: ${proxyItem.path} | target: ${proxyItem.rawPath}`)
      })

      if (proxyConfig && Object.keys(proxyConfig).length > 0) {
        if (!config.server) {
          config.server = {}
        }

        config.server.proxy = {
          ...config.server.proxy,
          ...proxyConfig,
        }
        config.define[envName] = JSON.stringify(proxyMapping)
        console.warn(`[auto-proxy] Injected proxy map into ${envName}`)

        if (dts) {
          generateDtsFile(proxyMapping, dts, envName)
        }
      }
      else {
        console.warn(`[auto-proxy] No proxy entries generated`)
        config.define[envName] = JSON.stringify({})

        if (dts) {
          generateDtsFile({}, dts, envName)
        }
      }
    },
  }
}

function generateProxyFromServiceConfig(
  serviceConfig: FullServiceConfig,
  mode: ServiceEnvType,
  proxyPrefix: string,
): { proxyConfig: Record<string, ProxyOptions>, proxyMapping: ProxyMapping } {
  try {
    const envConfig = serviceConfig[mode]
    if (!envConfig) {
      console.warn(`[auto-proxy] No config for "${mode}", falling back to development`)
      const defaultConfig = serviceConfig.development
      if (!defaultConfig) {
        console.error(`[auto-proxy] development config is also missing`)
        return { proxyConfig: {}, proxyMapping: {} }
      }
      return generateProxyFromConfig(defaultConfig, proxyPrefix)
    }

    return generateProxyFromConfig(envConfig, proxyPrefix)
  }
  catch (error) {
    console.error(`[auto-proxy] Failed to generate proxy config:`, (error as Error).message)
    return { proxyConfig: {}, proxyMapping: {} }
  }
}

function generateProxyFromConfig(
  envConfig: ServiceConfig,
  proxyPrefix: string,
): { proxyConfig: Record<string, ProxyOptions>, proxyMapping: ProxyMapping } {
  const proxyConfig: Record<string, ProxyOptions> = {}
  const proxyMapping: ProxyMapping = {}

  Object.entries(envConfig).forEach(([serviceName, serviceUrl]) => {
    if (typeof serviceUrl === 'string' && serviceUrl.trim()) {
      const proxyPath = `/${proxyPrefix}${serviceName}`

      const isWs = serviceUrl.startsWith('ws://') || serviceUrl.startsWith('wss://')
      proxyConfig[proxyPath] = {
        target: serviceUrl,
        changeOrigin: true,
        ws: isWs,
        rewrite: (path: string): string => path.replace(new RegExp(`^/${proxyPrefix}${serviceName}`), ''),
      }

      proxyMapping[serviceName] = {
        path: proxyPath,
        rawPath: serviceUrl,
      }
    }
  })

  return { proxyConfig, proxyMapping }
}

function generateDtsFile(
  mapping: ProxyMapping,
  outputPath: string,
  envName: string,
) {
  try {
    const serviceNames = Object.keys(mapping).map(name => `'${name}'`).join(' | ')
    const serviceNameType = serviceNames || 'never'

    const dtsContent = `/* eslint-disable */
/* prettier-ignore */
// @ts-nocheck
// noinspection JSUnusedGlobalSymbols
// Generated by auto-proxy
// biome-ignore lint: disable
export {}

type serviceName = ${serviceNameType}

declare global {
  const ${envName}: {
    [K in serviceName]: {
      path: string
      rawPath: string
    }
  }
}
`

    const dir = dirname(outputPath)
    if (dir) {
      mkdirSync(dir, { recursive: true })
    }
    try {
      const existing = readFileSync(outputPath, 'utf-8')
      if (existing === dtsContent)
        return
    }
    catch {
      /* file missing, write below */
    }
    writeFileSync(outputPath, dtsContent, 'utf-8')
  }
  catch (error) {
    console.error(`[auto-proxy] Failed to write d.ts file:`, (error as Error).message)
  }
}
