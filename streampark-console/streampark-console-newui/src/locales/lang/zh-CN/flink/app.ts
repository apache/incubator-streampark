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
  applicationTitle: 'Application 信息',
  flinkWebUi: 'Flink Web UI',
  detail: {
    compareConfig: '比较配置',
    candidate: '侯选',
    compare: '比较',
    resetApi: 'Rest Api',
    resetApiToolTip: 'Rest API外部调用接口，其他第三方系统可轻松访问StreamPark。',
    copyStartcURL: '复制启动 cURL',
    copyCancelcURL: '复制取消 cURL',
    apiDocCenter: 'Api文档',
    nullAccessToken: '访问令牌为空，请联系管理员添加.',
    invalidAccessToken: '访问令牌无效，请联系管理员。',

    detailTab: {
      configDetail: '查看日志详情',
      confDeleteTitle: '您确定要删除此记录吗',
      copyPath: '复制路径',
      pointDeleteTitle: '您确定要删除?',
      copySuccess: '已成功复制到剪贴板',
      copyFail: '复制失败',
      check: '检查 Point',
      save: '保存 Point',
      exception: '查看异常',
    },

    different: {
      original: '原始版本',
      target: '目标版本',
    },
    exceptionModal: {
      title: '异常信息',
    },
  },
  view: {
    buildTitle: '应用程序启动进度',
    stepTitle: '步骤详情',
    errorLog: '错误日志',
    errorSummary: '错误摘要',
    errorStack: '错误堆栈',
    logTitle: '启动日志 : 应用名称 [ {0} ]',
    refreshTime: '上次刷新时间',
    refresh: '刷新',
    start: '开启应用',
    stop: '停止应用',
    recheck: '关联的项目已更改，需要重新检查此作业',
    changed: '应用程序已更改。',
  },
  pod: {
    choice: '选择',
    init: '初始化内容',
    host: 'Host别名',
  },
  dependencyError: '请先检查flink 版本.',
  flinkSql: {
    verify: '验证',
    format: '格式化',
    fullScreen: '全屏',
    exit: '退出',
    successful: '验证成功',
    compareFlinkSQL: '比较 Flink SQL',
  },
  hadoopConfigTitle: '系统 Hadoop 配置',
  dragUploadTitle: '单击或拖动 jar 到此区域以上传',
  dragUploadTip: '支持单次上传。您可以在此处上传本地 jar 以支持当前作业',
  editStreamPark: {
    success: '更新成功',
    flinkSqlRequired: 'Flink Sql 为必填项',
    appidCheck: 'appid 不能为空',
    sqlCheck: 'SQL 检查错误',
  },
  launchTitle: '该应用程序的当前启动正在进行中.',
  launchDesc: '您确定要强制进行另一次构建吗',
  launchFail: 'lanuch application 失败,',
  launching: '当前 Application 正在 launching',
  tableAction: {
    edit: '编辑 Application',
    launch: 'Launch Application',
    launchDetail: 'Launching 详情',
    start: '启动 Application',
    cancel: '取消 Application',
    detail: '查看 Application 详情',
    startLog: '查看 Flink 启动日志',
    force: '强制停止Application',
    copy: '复制 Application',
    remapping: 'Remapping Application',
    flameGraph: '查看 FlameGraph',
    deleteTip: '你确定要删除这个job ?',
  },
};
