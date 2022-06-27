import antNotification from 'ant-design-vue/es/notification'
import {message as antMessage, Modal as AntModal} from 'ant-design-vue'
import { getSender } from './sender'

export const isInIframe = window.self !== window.top

export const message = !isInIframe ? antMessage : {
  success: (...args) => getSender().then(_ => _.$messageSuccess(...args)),
  info: (...args) => getSender().then(_ => _.$messageInfo(...args)),
  warning: (...args) => getSender().then(_ => _.$messageWarning(...args)),
  error: (...args) => getSender().then(_ => _.$messageError(...args)),
  loading: (...args) => getSender().then(_ => _.$messageLoading(...args))
}

export const notification = !isInIframe ? antNotification : {
  success: (...args) => getSender().then(_ => _.$notificationSuccess(...args)),
  warn: (...args) => getSender().then(_ => _.$notificationWarn(...args)),
  error: (...args) => getSender().then(_ => _.$notificationError(...args)),
}

export const Modal = !isInIframe ? AntModal : {
  confirm: (...args) => getSender().then(_ => _.$confirm(...args)),
  info: (...args) => getSender().then(_ => _.$info(...args)),
  success: (...args) => getSender().then(_ => _.$success(...args)),
  error: (...args) => getSender().then(_ => _.$error(...args)),
  warning: (...args) => getSender().then(_ => _.$warning(...args))
}

export default {
  install(Vue) {
    // ant-design-vue
    Vue.prototype.$message = message
    Vue.prototype.$notification = notification
    // ant-design-vue Modal
    Vue.prototype.$confirm = Modal.confirm
    Vue.prototype.$info = Modal.info
    Vue.prototype.$success = Modal.success
    Vue.prototype.$error = Modal.error
    Vue.prototype.$warning = Modal.warning
    // vue-sweetalert2
    Vue.prototype.$swal = (...args) => getSender().then(_ => _.$swal(...args))
  }
}
