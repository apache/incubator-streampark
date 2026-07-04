import { $t } from '@/utils'

/** 默认实例的Aixos配置 */
export const DEFAULT_ALOVA_OPTIONS = {
  // 请求超时时间,默认15秒
  timeout: 15 * 1000,
}

/** 默认实例的后端字段配置 */
export const DEFAULT_BACKEND_OPTIONS = {
  codeKey: 'code',
  dataKey: 'data',
  msgKey: 'message',
  successCode: 200,
}

/** 与 legacy axios 一致：无 code 字段或 code 为 200 视为成功 */
export function isBackendSuccess(
  apiData: Record<string, unknown>,
  config: Required<Service.BackendConfig>,
) {
  const { codeKey, successCode } = config
  if (!apiData || typeof apiData !== 'object')
    return false
  if (!Reflect.has(apiData, codeKey))
    return true
  const code = apiData[codeKey]
  return code === successCode || String(code) === String(successCode)
}

function errorMessage(key: string) {
  return () => $t(key)
}

/** 请求不成功各种状态的错误（延迟读取 i18n，避免 bootstrap 前初始化） */
export const ERROR_STATUS: Record<number | 'default', () => string> = {
  default: errorMessage('http.defaultTip'),
  400: errorMessage('http.400'),
  401: errorMessage('http.401'),
  403: errorMessage('http.403'),
  404: errorMessage('http.404'),
  405: errorMessage('http.405'),
  408: errorMessage('http.408'),
  500: errorMessage('http.500'),
  501: errorMessage('http.501'),
  502: errorMessage('http.502'),
  503: errorMessage('http.503'),
  504: errorMessage('http.504'),
  505: errorMessage('http.505'),
}

/** 没有错误提示的code */
export const ERROR_NO_TIP_STATUS = [10000]
