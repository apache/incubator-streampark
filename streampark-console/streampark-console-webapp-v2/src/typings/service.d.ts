declare namespace Service {
    import type { Method } from 'alova'

    interface AlovaConfig {
        baseURL: string
        timeout?: number
        beforeRequest?: (method: Method<globalThis.Ref<unknown>>) => void
    }

    interface BackendConfig {
        codeKey?: string
        dataKey?: string
        msgKey?: string
        successCode?: number | string
    }

    type RequestErrorType = 'Response Error' | 'Business Error' | null
    type RequestCode = string | number

    interface RequestError {
        errorType: RequestErrorType
        code: RequestCode
        message: string
        data?: any
    }

    interface ResponseResult<T> extends RequestError {
        isSuccess: boolean
        errorType: RequestErrorType
        code: RequestCode
        message: string
        data: T
        /** True when HTTP layer already showed an error toast. */
        errorNotified?: boolean
    }

    type RequestResult<T> = ResponseResult<T>
}
