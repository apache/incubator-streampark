const getters = {
  // app
  device: state => state.app.device,
  theme: state => state.app.theme,
  color: state => state.app.color,

  // user
  permissions: state => state.user.permissions,
  roles: state => state.user.roles,
  token: state => state.user.token,
  avatar: state => state.user.avatar,
  nickname: state => state.user.name,
  welcome: state => state.user.welcome,
  userInfo: state => state.user.info,

  routers: state => state.user.routers,
  multiTab: state => state.app.multiTab,
  applicationId: state => state.application.appId,
}

export default getters
