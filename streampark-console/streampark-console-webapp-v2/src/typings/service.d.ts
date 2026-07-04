/** 请求的相关类型 */
declare namespace Service {
  import type { Method } from 'alova'

  interface AlovaConfig {
    baseURL: string
    timeout?: number
    beforeRequest?: (method: Method<globalThis.Ref<unknown>>) => void
  }

  /** 后端接口返回的数据结构配置 */
  interface BackendConfig {
    /** 表示后端请求状态码的属性字段 */
    codeKey?: string
    /** 表示后端请求数据的属性字段 */
    dataKey?: string
    /** 表示后端消息的属性字段 */
    msgKey?: string
    /** 后端业务上定义的成功请求的状态 */
    successCode?: number | string
  }

  type RequestErrorType = 'Response Error' | 'Business Error' | null
  type RequestCode = string | number

  interface RequestError {
    /** 请求服务的错误类型 */
    errorType: RequestErrorType
    /** 错误码 */
    code: RequestCode
    /** 错误信息 */
    message: string
    /** 返回的数据 */
    data?: any
  }

  interface ResponseResult<T> extends RequestError {
    /** 请求服务是否成功 */
    isSuccess: boolean
    /** 请求服务的错误类型 */
    errorType: RequestErrorType
    /** 错误码 */
    code: RequestCode
    /** 错误信息 */
    message: string
    /** 返回的数据 */
    data: T
    /** HTTP 层是否已弹出错误提示，避免与页面重复 toast */
    errorNotified?: boolean
  }

  type RequestResult<T> = ResponseResult<T>
}
