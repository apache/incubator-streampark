import { $t } from '@/utils'

export const DEFAULT_ALOVA_OPTIONS = {
    timeout: 15 * 1000,
}

export const DEFAULT_BACKEND_OPTIONS = {
    codeKey: 'code',
    dataKey: 'data',
    msgKey: 'message',
    successCode: 200,
}

/** Matches legacy axios: missing `code` or `code === 200` counts as success. */
export function isBackendSuccess(
    apiData: Record<string, unknown>,
    config: Required<Service.BackendConfig>,
) {
    const { codeKey, successCode } = config
    if (!apiData || typeof apiData !== 'object') return false
    if (!Reflect.has(apiData, codeKey)) return true
    const code = apiData[codeKey]
    return code === successCode || String(code) === String(successCode)
}

function errorMessage(key: string) {
    return () => $t(key)
}

/** HTTP status messages resolved lazily so i18n can initialize first. */
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

export const ERROR_NO_TIP_STATUS = [10000]
