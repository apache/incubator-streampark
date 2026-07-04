declare module 'md-editor-v3' {
  import type { DefineComponent } from 'vue'

  export type ToolbarNames = string
  export const MdEditor: DefineComponent<any, any, any>
  export const MdPreview: DefineComponent<any, any, any>
  const plugin: DefineComponent<any, any, any>
  export default plugin
}

declare module 'quill' {
  export default class Quill {
    constructor(container: string | Element, options?: Record<string, unknown>)
    on(event: string, handler: (...args: unknown[]) => void): void
    enable(enabled?: boolean): void
    root: HTMLElement
    getSemanticHTML?: () => string
  }
}

declare module 'crypto-js/aes'
declare module 'crypto-js/enc-utf8'
declare module 'crypto-js/pad-pkcs7'
declare module 'crypto-js/mode-ecb'
declare module 'crypto-js/md5'
declare module 'crypto-js/enc-base64'
