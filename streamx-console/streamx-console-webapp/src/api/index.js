export default {
  ForgePassword: '/auth/forge-password',
  twoStepCode: '/auth/2step-code',
  SendSms: '/account/sms',
  SendSmsErr: '/account/sms_err',
  Passport: {
    LOGIN: '/passport/login',
    LOGOUT: '/passport/logout'
  },
  Project: {
    CREATE: '/flink/project/create',
    BUILD: '/flink/project/build',
    LIST: '/flink/project/list',
    FILELIST: '/flink/project/filelist',
    MODULES: '/flink/project/modules',
    LISTCONF: '/flink/project/listconf',
    JARS: '/flink/project/jars',
    DELETE: '/flink/project/delete',
    SELECT: '/flink/project/select'
  },
  Tutorial: {
    GET: '/tutorial/get'
  },
  NoteBook: {
    SUBMIT: '/flink/notebook/submit'
  },
  SavePoint: {
    LASTEST: '/flink/savepoint/lastest',
    HISTORY: '/flink/savepoint/history',
    DELETE: '/flink/savepoint/delete'
  },
  Application: {
    ADD: '/flink/app/add',
    READCONF: '/flink/app/readConf',
    UPDATE: '/flink/app/update',
    DEPLOY: '/flink/app/deploy',
    MAPPING: '/flink/app/mapping',
    YARN: '/flink/app/yarn',
    LIST: '/flink/app/list',
    GET: '/flink/app/get',
    MAIN: '/flink/app/main',
    NAME: '/flink/app/name',
    EXISTS: '/flink/app/exists',
    STOP: '/flink/app/stop',
    DELETE: '/flink/app/delete',
    DELETEBAK: '/flink/app/deletebak',
    CREATE: '/flink/app/create',
    START: '/flink/app/start',
    CLEAN: '/flink/app/clean',
    BACKUPS: '/flink/app/backups',
    STARTLOG: '/flink/app/startlog'
  },
  Config: {
    GET: '/flink/conf/get',
    LIST: '/flink/conf/list',
    DELETE: '/flink/conf/delete'
  },
  User: {
    EXECUSER: '/user/execUser',
    LIST: '/user/list',
    UPDATE: '/user/update',
    GET: '/user/get',
    POST: '/user/post',
    DELETE: '/user/delete',
    EXPORT: '/user/export',
    CHECK_NAME: '/user/check/name',
    CHECK_PASSWORD: '/user/check/password'
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
