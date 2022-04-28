/*
 * Copyright (c) 2019 The StreamX Project
 * <p>
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

export default {
  Passport: {
    SIGNIN: '/passport/signin',
    SIGNOUT: '/passport/signout'
  },
  Project: {
    BRANCHES: '/flink/project/branches',
    GITCHECK: '/flink/project/gitcheck',
    EXISTS: '/flink/project/exists',
    CREATE: '/flink/project/create',
    GET: '/flink/project/get',
    UPDATE: '/flink/project/update',
    BUILD: '/flink/project/build',
    BUILDLOG: '/flink/project/buildlog',
    CLOSEBUILD: '/flink/project/closebuild',
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
  Metrics: {
    FLAMEGRAPH: '/metrics/flamegraph',
    NOTICE: '/metrics/notice',
    DELNOTICE: '/metrics/delnotice'
  },
  SavePoint: {
    LATEST: '/flink/savepoint/latest',
    HISTORY: '/flink/savepoint/history',
    DELETE: '/flink/savepoint/delete'
  },
  Application: {
    READCONF: '/flink/app/readConf',
    UPDATE: '/flink/app/update',
    UPLOAD: '/flink/app/upload',
    DEPLOY: '/flink/app/deploy',
    MAPPING: '/flink/app/mapping',
    YARN: '/flink/app/yarn',
    LIST: '/flink/app/list',
    GET: '/flink/app/get',
    DASHBOARD: '/flink/app/dashboard',
    MAIN: '/flink/app/main',
    NAME: '/flink/app/name',
    CHECKNAME: '/flink/app/checkName',
    CANCEL: '/flink/app/cancel',
    DELETE: '/flink/app/delete',
    DELETEBAK: '/flink/app/deletebak',
    CREATE: '/flink/app/create',
    START: '/flink/app/start',
    CLEAN: '/flink/app/clean',
    BACKUPS: '/flink/app/backups',
    ROLLBACK: '/flink/app/rollback',
    REVOKE: '/flink/app/revoke',
    OPTIONLOG: '/flink/app/optionlog',
    DOWNLOG: '/flink/app/downlog',
    CHECKJAR: '/flink/app/checkjar',
    VERIFYSCHEMA: '/flink/app/verifySchema'
  },
  Config: {
    GET: '/flink/conf/get',
    TEMPLATE: '/flink/conf/template',
    LIST: '/flink/conf/list',
    HISTORY: '/flink/conf/history',
    DELETE: '/flink/conf/delete',
    SYS_HADOOP_CONF: '/flink/conf/sysHadoopConf'
  },
  FlinkEnv: {
    LIST: '/flink/env/list',
    CREATE:  '/flink/env/create',
    EXISTS: '/flink/env/exists',
    GET: '/flink/env/get',
    SYNC: '/flink/env/sync',
    UPDATE: '/flink/env/update',
    DEFAULT: '/flink/env/default',
  },
  FlinkCluster: {
    LIST: '/flink/cluster/list',
    ACTIVEURL: '/flink/cluster/activeUrl',
    CREATE:  '/flink/cluster/create',
    CHECK: '/flink/cluster/check',
    GET: '/flink/cluster/get',
    UPDATE: '/flink/cluster/update',
    START: '/flink/cluster/start',
    SHUTDOWN: '/flink/cluster/shutdown',
    DELETE: '/flink/cluster/delete'
  },
  AppBuild: {
    BUILD: '/flink/pipe/build',
    DETAIL: '/flink/pipe/detail',
  },
  FlinkHistory: {
    UPLOAD_JARS: '/flink/history/uploadJars',
    K8S_NAMESPACES: '/flink/history/k8sNamespaces',
    SESSION_CLUSTER_IDS: '/flink/history/sessionClusterIds',
    FLINK_BASE_IMAGES: '/flink/history/flinkBaseImages',
    FLINK_POD_TEMPLATES: '/flink/history/flinkPodTemplates',
    FLINK_JM_POD_TEMPLATES: '/flink/history/flinkJmPodTemplates',
    FLINK_TM_POD_TEMPLATES: '/flink/history/flinkTmPodTemplates'
  },
  FlinkPodTemplate: {
    SYS_HOSTS: '/flink/podtmpl/sysHosts',
    INIT: '/flink/podtmpl/init',
    COMP_HOST_ALIAS: '/flink/podtmpl/compHostAlias',
    EXTRACT_HOST_ALIAS: '/flink/podtmpl/extractHostAlias',
    PREVIEW_HOST_ALIAS: '/flink/podtmpl/previewHostAlias',
  },
  FlinkSQL: {
    VERIFY: '/flink/sql/verify',
    GET: '/flink/sql/get',
    HISTORY: '/flink/sql/history'
  },
  SETTING: {
    GET: '/flink/setting/get',
    WEBURL: '/flink/setting/weburl',
    ALL: '/flink/setting/all',
    CHECK_HADOOP: '/flink/setting/checkHadoop',
    SYNC: '/flink/setting/sync',
    UPDATE: '/flink/setting/update'
  },
  User: {
    EXECUSER: '/user/execUser',
    LIST: '/user/list',
    UPDATE: '/user/update',
    PASSWORD: '/user/password',
    RESET: '/user/password/reset',
    GET: '/user/get',
    GET_NOTOKEN_USER: '/user/getNoTokenUser',
    POST: '/user/post',
    DELETE: '/user/delete',
    CHECK_NAME: '/user/check/name',
    CHECK_PASSWORD: '/user/check/password'
  },
  Token: {
    LIST: '/token/list',
    DELETE: '/token/delete',
    CREATE: '/token/create',
    CHECK: '/token/check',
    CURL: '/token/curl',
    TOGGLE: '/token/toggle'
  },
  Role: {
    POST: '/role/post',
    UPDATE: '/role/update',
    LIST: '/role/list',
    CHECK_NAME: '/role/check/name',
    DELETE: '/role/delete',
    MENU: '/role/menu'
  },
  Menu: {
    LIST: '/menu/list',
    DELETE: '/menu/delete',
    POST: '/menu/post',
    UPDATE: '/menu/update',
    ROUTER: '/menu/router'
  },
  Log: {
    LIST: '/log/list',
    DELETE: '/log/delete',
  }
}
