import http from '@/utils/request'

const api = {
  user: '/user',
  role: '/role',
  service: '/service',
  permission: '/permission',
  permissionNoPager: '/permission/no-pager',
  orgTree: '/org/tree'
}

export default api

export function getUserList (parameter) {
  return http.get({
    url: api.user,
    params: parameter
  })
}

export function getRoleList (parameter) {
  return http.get({
    url: api.role,
    params: parameter
  })
}

export function getServiceList (parameter) {
  return http.get({
    url: api.service,
    params: parameter
  })
}

export function getPermissions (parameter) {
  return http.get({
    url: api.permissionNoPager,
    params: parameter
  })
}

export function getOrgTree (parameter) {
  return http.get({
    url: api.orgTree,
    params: parameter
  })
}

// id === 0 add     post
// id !== 0 update  put
export function saveService (parameter) {
  return http[parameter.id === 0 ? 'post' : 'put']({
    url: api.service,
    data: parameter
  })
}
