declare namespace Entity {
}

declare namespace Api {

}

interface Window {
  $loadingBar: import('naive-ui').LoadingBarApi
  $dialog: import('naive-ui').DialogApi
  $message: import('naive-ui').MessageApi
  $notification: import('naive-ui').NotificationApi
}

declare const AMap: any
declare const BMap: any

declare module '*.vue' {
  import type { DefineComponent } from 'vue'

  const component: DefineComponent
  export default component
}

declare namespace NaiveUI {
  type ThemeColor = 'default' | 'error' | 'primary' | 'info' | 'success' | 'warning'
}

declare namespace Storage {
  interface Session {
    dict: DictMap
  }

  interface Local {
    userInfo: Api.Login.Info
    accessToken: string
    refreshToken: string
    loginAccount: any
    lang: App.lang
  }
}

declare namespace App {
  type lang = 'zh_CN' | 'en'
}

interface DictMap {
  [key: string]: Entity.Dict[]
}
