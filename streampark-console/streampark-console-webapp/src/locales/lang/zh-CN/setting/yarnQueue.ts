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
  tableTitle: 'Yarn Queue 列表',
  createQueue: '添加队列',
  success: '成功',
  modifyYarnQueue: '编辑队列',
  deleteYarnQueue: '删除队列',
  deleteConfirm: '确定要删除此队列?',
  descriptionMessage: '超过了512个字符的最大长度限制',
  yarnQueueLabelExpression: "Yarn 队列{'@'}标签",
  placeholder: {
    yarnQueueLabelExpression: "请输入队列，如 {'{queue} 或 {queue}@{lab1,...}'}",
    description: '请输入描述',
  },
  selectionHint:
    "用来快速设置 'yarn.application.name' 与 'yarn.application.node-label'。如果没有可用队列请联系管理员在 'Settings' -> 'Yarn Queue' 页面中创建",
  checkResult: {
    emptyHint: '队列标签不能为空',
    invalidFormatHint: '队列标签格式错误',
    existedHint: '该队列标签已经存在于当前 Team',
  },
  noteInfo:
    'Queue label 信息。例如，输入 "queue1" 表示队列名字，而输入 "queue1{\'@\'}label1,label2" 则表示队列名字为 "queue1" 且队列标签设置为 "label1" 和 "label2"。',
};
