// 定义一些和权限有关的 Vue指令

// 必须包含列出的所有权限，元素才显示
export const hasPermission = {
  install (Vue) {
    Vue.directive('hasPermission', {
      bind (el, binding, vnode) {
        let permissions = vnode.context.$store.state.account.permissions
        let value = binding.value.split(',')
        let flag = true
        for (let v of value) {
          if (!permissions.includes(v)) {
            flag = false
          }
        }
        if (!flag) {
          if (!el.parentNode) {
            el.style.display = 'none'
          } else {
            el.parentNode.removeChild(el)
          }
        }
      }
    })
  }
}

// 当不包含列出的权限时，渲染该元素
export const hasNoPermission = {
  install (Vue) {
    Vue.directive('hasNoPermission', {
      bind (el, binding, vnode) {
        let permissions = vnode.context.$store.state.account.permissions
        let value = binding.value.split(',')
        let flag = true
        for (let v of value) {
          if (permissions.includes(v)) {
            flag = false
          }
        }
        if (!flag) {
          if (!el.parentNode) {
            el.style.display = 'none'
          } else {
            el.parentNode.removeChild(el)
          }
        }
      }
    })
  }
}

// 只要包含列出的任意一个权限，元素就会显示
export const hasAnyPermission = {
  install (Vue) {
    Vue.directive('hasAnyPermission', {
      bind (el, binding, vnode) {
        let permissions = vnode.context.$store.state.account.permissions
        let value = binding.value.split(',')
        let flag = false
        for (let v of value) {
          if (permissions.includes(v)) {
            flag = true
          }
        }
        if (!flag) {
          if (!el.parentNode) {
            el.style.display = 'none'
          } else {
            el.parentNode.removeChild(el)
          }
        }
      }
    })
  }
}

// 必须包含列出的所有角色，元素才显示
export const hasRole = {
  install (Vue) {
    Vue.directive('hasRole', {
      bind (el, binding, vnode) {
        let permissions = vnode.context.$store.state.account.roles
        let value = binding.value.split(',')
        let flag = true
        for (let v of value) {
          if (!permissions.includes(v)) {
            flag = false
          }
        }
        if (!flag) {
          if (!el.parentNode) {
            el.style.display = 'none'
          } else {
            el.parentNode.removeChild(el)
          }
        }
      }
    })
  }
}

// 只要包含列出的任意一个角色，元素就会显示
export const hasAnyRole = {
  install (Vue) {
    Vue.directive('hasAnyRole', {
      bind (el, binding, vnode) {
        let permissions = vnode.context.$store.state.account.roles
        let value = binding.value.split(',')
        let flag = false
        for (let v of value) {
          if (permissions.includes(v)) {
            flag = true
          }
        }
        if (!flag) {
          if (!el.parentNode) {
            el.style.display = 'none'
          } else {
            el.parentNode.removeChild(el)
          }
        }
      }
    })
  }
}
