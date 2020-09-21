export default {
  ForgePassword: '/auth/forge-password',
  twoStepCode: '/auth/2step-code',
  SendSms: '/account/sms',
  SendSmsErr: '/account/sms_err',
  Passport: {
    Login: '/passport/login',
    Logout: '/passport/logout',
    KICKOUT: '/passport/kickout'
  },
  Job: {
    JobList: '/job/view',
    JobAdd: '/job/addJob',
    AddNode: '/job/addNode',
    GetJob: '/job/getJob'
  },
  Project: {
    Create: '/flink/project/create',
    Build: '/flink/project/build',
    List: '/flink/project/list',
    FileList: '/flink/project/filelist',
    Modules: '/flink/project/modules',
    ListConf: '/flink/project/listconf',
    Jars: '/flink/project/jars',
    Delete: '/flink/project/delete',
    Select: '/flink/project/select'
  },
  SavePoint: {
    Lastest: '/flink/savepoint/lastest',
    History: '/flink/savepoint/history'
  },
  Application: {
    Add: '/flink/app/add',
    ReadConf: '/flink/app/readConf',
    Update: '/flink/app/update',
    Deploy: '/flink/app/deploy',
    Yarn: '/flink/app/yarn',
    List: '/flink/app/list',
    Get: '/flink/app/get',
    Main: '/flink/app/main',
    Name: '/flink/app/name',
    Exists: '/flink/app/exists',
    Stop: '/flink/app/stop',
    Delete: '/flink/app/delete',
    Create: '/flink/app/create',
    Start: '/flink/app/start',
    CloseDeploy: '/flink/app/closeDeploy'
  },

  Config: {
    Get: '/flink/conf/get',
    List: '/flink/conf/list'
  },

  User: {
    ExecUser: '/user/execUser',
    LIST: '/user/list',
    UPDATE: '/user/update',
    GET: '/user/get',
    POST: '/user/post',
    DELETE: '/user/delete',
    EXPORT: '/user/export',
    CHECK_NAME: '/user/check/name',
    CHECK_PASSWORD: '/user/check/password'
  },
  Dept: {
    LIST: '/dept/list',
    DELETE: '/dept/delete',
    POST: '/dept/post',
    EXPORT: '/dept/export',
    UPDATE: '/dept/update'
  },
  Role: {
    POST: '/role/post',
    UPDATE: '/role/update',
    LIST: '/role/list',
    CHECK_NAME: '/role/check/name',
    DELETE: '/role/delete',
    EXPORT: '/role/export',
    MENU: '/role/menu'
  },
  Menu: {
    LIST: '/menu/list',
    DELETE: '/menu/delete',
    POST: '/menu/post',
    EXPORT: '/menu/export',
    UPDATE: '/menu/update',
    ROUTER: '/menu/router'
  },
  Log: {
    LIST: '/log/list',
    DELETE: '/log/delete',
    EXPORT: '/log/export'
  }

}
