/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
export default {
  detail: {
    applicationTitle: 'Application Info',
    flinkWebUi: 'Flink Web UI',
    compareConfig: 'Compare Config',
    candidate: 'Candidate',
    compare: 'Compare',
    resetApi: 'Rest Api',
    resetApiToolTip:
      'Rest API external call interface,other third-party systems easy to access StreamPark',
    copyStartcURL: 'Copy Start cURL',
    copyCancelcURL: 'Copy Cancel cURL',
    apiDocCenter: 'Api Doc Center',
    nullAccessToken: 'access token is null,please contact the administrator to add.',
    invalidAccessToken: 'access token is invalid,please contact the administrator.',

    detailTab: {
      configDetail: 'View Config Detail',
      confDeleteTitle: 'Are you sure delete this record',
      copyPath: 'Copy Path',
      pointDeleteTitle: 'Are you sure delete?',
      copySuccess: 'copied to clipboard successfully',
      copyFail: 'failed',
      check: 'Check Point',
      save: 'Save Point',
      exception: 'View Exception',
    },

    different: {
      original: 'Original version',
      target: 'Target version',
    },
    exceptionModal: {
      title: 'Exception Info',
    },
  },
  view: {
    buildTitle: 'Application Launching Progress',
    stepTitle: 'Steps Detail',
    errorLog: 'Error Log',
    errorSummary: 'Error Summary',
    errorStack: 'Error Stack',
    logTitle: 'Start Log : Application Name [ {0} ]',
    refreshTime: 'last refresh time',
    refresh: 'refresh',
    start: 'Start Application',
    stop: 'Stop application',
    recheck: 'the associated project has changed and this job need to be rechecked',
    changed: 'the application has changed.',
  },
  pod: {
    choice: 'Choice',
    init: 'Init Content',
    host: 'Host Alias',
  },
  dependencyError: 'please set flink version first.',
  flinkSql: {
    verify: 'Verify',
    format: 'Format',
    fullScreen: 'Full Screen',
    exit: 'Exit',
    successful: 'Verification success',
    compareFlinkSQL: 'Compare Flink SQL',
  },
  hadoopConfigTitle: 'System Hadoop Conifguration',
  dragUploadTitle: 'Click or drag jar to this area to upload',
  dragUploadTip:
    'Support for a single upload. You can upload a local jar here to support for current Job',

  editStreamPark: {
    success: 'update successful',
    flinkSqlRequired: 'Flink Sql is required',
    appidCheck: 'appid can not be empty',
    sqlCheck: 'SQL check error',
  },
  launchTitle: 'The current launch of the application is in progress.',
  launchDesc: 'are you sure you want to force another build',
  launchFail: 'lanuch application failed,',
  launching: 'Current Application is launching',
  tableAction: {
    edit: 'Edit Application',
    launch: 'Launch Application',
    launchDetail: 'Launching Progress Detail',
    start: 'Start Application',
    cancel: 'Cancel Application',
    detail: 'View Application Detail',
    startLog: 'See Flink Start log',
    force: 'Forced Stop Application',
    copy: 'Copy Application',
    remapping: 'Remapping Application',
    flameGraph: 'View FlameGraph',
    deleteTip: 'Are you sure delete this job ?',
  },
};
