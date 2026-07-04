import type { ProxyOptions, UserConfig } from 'vite'
import { mkdirSync, readFileSync, writeFileSync } from 'node:fs'
import { dirname } from 'node:path'

/** 服务配置接口 */
interface ServiceConfig {
  [key: string]: string
}

/** 服务环境类型 */
type ServiceEnvType = string

/** 完整的服务配置类型 */
interface FullServiceConfig {
  [key: ServiceEnvType]: ServiceConfig
}

/** 代理项接口 */
interface ProxyItem {
  /** 代理路径 */
  path: string
  /** 原始地址 */
  rawPath: string
}

/** 代理地址映射接口 */
interface ProxyMapping {
  [serviceName: string]: ProxyItem
}

/** 插件选项接口 */
export interface ServiceProxyPluginOptions {
  /** 服务配置对象（必填） */
  serviceConfig: FullServiceConfig
  /** 代理路径前缀（可选，默认为 'proxy-'） */
  proxyPrefix?: string
  /** 是否启用代理配置 */
  enableProxy?: boolean
  /** 环境变量名（可选，默认为 '__URL_MAP__'） */
  envName?: string
  /** d.ts 类型文件生成路径（可选，如果传入路径则在该路径生成 d.ts 类型文件） */
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
      // 只在开发环境（serve命令）时生成代理配置
      const isDev = command === 'serve'

      // 在非开发环境也注入空的代理映射，避免运行时错误
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
          console.warn(`[auto-proxy] 已加载 ${Object.keys(envConfig).length} 个服务地址`)
        }
        else {
          console.warn(`[auto-proxy] 未找到环境 "${mode}" 的配置`)
        }

        config.define[envName] = JSON.stringify(rawMapping)

        // 生成 d.ts 类型文件（如果指定了路径）
        if (dts) {
          generateDtsFile(rawMapping, dts, envName)
        }
        return
      }

      console.warn(`[auto-proxy] 已加载${mode}模式 ${Object.keys(serviceConfig[mode]).length} 个服务地址`)

      const { proxyConfig, proxyMapping } = generateProxyFromServiceConfig(serviceConfig, mode, proxyPrefix)

      Object.entries(proxyMapping).forEach(([serviceName, proxyItem]) => {
        console.warn(`[auto-proxy] 服务: ${serviceName} | 代理地址: ${proxyItem.path} | 实际地址: ${proxyItem.rawPath}`)
      })

      if (proxyConfig && Object.keys(proxyConfig).length > 0) {
        // 确保 server 对象存在
        if (!config.server) {
          config.server = {}
        }

        // 合并代理配置
        config.server.proxy = {
          ...config.server.proxy,
          ...proxyConfig,
        }
        config.define[envName] = JSON.stringify(proxyMapping)
        console.warn(`[auto-proxy] 代理映射已注入到 ${envName}`)

        // 生成 d.ts 类型文件（如果指定了路径）
        if (dts) {
          generateDtsFile(proxyMapping, dts, envName)
        }
      }
      else {
        console.warn(`[auto-proxy] 未生成任何代理配置`)
        config.define[envName] = JSON.stringify({})

        // 生成空的 d.ts 类型文件（如果指定了路径）
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
    // 获取当前环境的配置
    const envConfig = serviceConfig[mode]
    if (!envConfig) {
      console.warn(`[auto-proxy] 未找到环境 "${mode}" 的配置，使用 development 配置`)
      const defaultConfig = serviceConfig.development
      if (!defaultConfig) {
        console.error(`[auto-proxy] 也未找到 development 配置`)
        return { proxyConfig: {}, proxyMapping: {} }
      }
      return generateProxyFromConfig(defaultConfig, proxyPrefix)
    }

    return generateProxyFromConfig(envConfig, proxyPrefix)
  }
  catch (error) {
    console.error(`[auto-proxy] 生成代理配置失败:`, (error as Error).message)
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
      // 生成代理配置
      proxyConfig[proxyPath] = {
        target: serviceUrl,
        changeOrigin: true,
        ws: isWs,
        rewrite: (path: string): string => path.replace(new RegExp(`^/${proxyPrefix}${serviceName}`), ''),
      }

      // 生成代理映射
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
    console.error(`[auto-proxy] 生成 d.ts 文件失败:`, (error as Error).message)
  }
}
